package io.github.sor2171.backend.service

import io.github.sor2171.backend.database.AccountRepository
import io.github.sor2171.backend.entity.dto.Account
import io.github.sor2171.backend.entity.dto.EmailVerificationMsg
import io.github.sor2171.backend.entity.enums.EmailType
import io.github.sor2171.backend.entity.vo.request.EmailRegisterVO
import io.github.sor2171.backend.entity.vo.request.PasswordResetVO
import io.github.sor2171.backend.entity.vo.response.AuthorizeVO
import io.github.sor2171.backend.entity.vo.response.ReloginVO
import io.github.sor2171.security.JwtUtils
import io.github.sor2171.backend.utils.Const
import io.github.sor2171.backend.utils.DateUtils
import io.github.sor2171.backend.utils.DateUtils.getCurrentDateTime
import io.github.sor2171.backend.utils.PasswordEncoder
import org.slf4j.LoggerFactory

class AuthorizationService(
    private val accountRepository: AccountRepository,
    private val jwtUtils: JwtUtils,
    private val redisService: RedisService,
    private val rabbitService: RabbitService
) {
    private val logger = LoggerFactory.getLogger(AuthorizationService::class.java)

    suspend fun askEmailVerifyCode(type: EmailType, email: String, ip: String): String {
        logger.info("IP {} requested verification code for email: {}, type: {}", ip, email, type)
        val allowed = redisService.limitOnceCheck(Const.VERIFY_EMAIL_LIMIT + ip, 60)
        if (!allowed) {
            logger.warn("Verification code request limit exceeded for IP: {}", ip)
            return "Request limit exceeded. Please try again later."
        }

        val code = (100000..999999).random().toString()
        val data = EmailVerificationMsg(email, type, code)

        rabbitService.sendMailMessage(data)
        redisService.set(
            Const.VERIFY_EMAIL_DATA + email,
            code,
            180 // 3 minutes
        )
        logger.info("Verification code generated and dispatched for email: {}", email)
        return ""
    }

    suspend fun registerEmailAccount(vo: EmailRegisterVO): String {
        logger.info("Processing registration for username: {}, email: {}", vo.username, vo.email)
        val validationErr = vo.validate()
        if (validationErr != null) {
            logger.warn("Registration validation failed for user {}: {}", vo.username, validationErr)
            return validationErr
        }

        val (email, code, username, password) = vo
        val verifyErr = verifyCode(email, code)
        if (verifyErr.isNotBlank()) {
            logger.warn("Registration verify code check failed for email {}: {}", email, verifyErr)
            return verifyErr
        }

        if (accountRepository.existByEmail(email)) {
            logger.warn("Registration failed: email {} already exists", email)
            return "account with the same email already exists."
        }
        if (accountRepository.existByName(username)) {
            logger.warn("Registration failed: username {} already exists", username)
            return "username already exists."
        }

        val encodedPassword = PasswordEncoder.encode(password)
        val account = Account(
            id = null,
            username = username,
            password = encodedPassword,
            email = email,
            role = "user",
            registerTime = getCurrentDateTime()
        )

        val id = accountRepository.save(account)
        if (id > 0) {
            redisService.delete(Const.VERIFY_EMAIL_DATA + email)
            logger.info("User {} successfully registered with id: {}", username, id)
            return ""
        }
        logger.error("Failed to save user account for {}", username)
        return Const.INTERNAL_ERROR_STR
    }

    suspend fun resetEmailAccountPassword(vo: PasswordResetVO): String {
        logger.info("Processing password reset for email: {}", vo.email)
        val validationErr = vo.validate()
        if (validationErr != null) {
            logger.warn("Password reset validation failed for email {}: {}", vo.email, validationErr)
            return validationErr
        }

        val (email, code, password) = vo
        val verifyErr = verifyCode(email, code)
        if (verifyErr.isNotBlank()) {
            logger.warn("Password reset verify code check failed for email {}: {}", email, verifyErr)
            return verifyErr
        }

        val encodedPassword = PasswordEncoder.encode(password)
        val updated = accountRepository.resetPasswordByEmail(email, encodedPassword)
        return if (updated) {
            redisService.delete(Const.VERIFY_EMAIL_DATA + email)
            logger.info("Password successfully reset for email: {}", email)
            "Password reset successfully"
        } else {
            logger.warn("Password reset failed: account not found for email {}", email)
            "Account not found"
        }
    }

    suspend fun login(usernameOrEmail: String, password: String): AuthorizeVO? {
        logger.info("User login attempt for: {}", usernameOrEmail)
        val account = accountRepository.findByNameOrEmail(usernameOrEmail)
        if (account == null) {
            logger.warn("Login failed: account not found for '{}'", usernameOrEmail)
            return null
        }
        if (!PasswordEncoder.matches(password, account.password)) {
            logger.warn("Login failed: password mismatch for user '{}'", account.username)
            return null
        }
        val token = jwtUtils.createJwt(
            id = account.id ?: -1,
            username = account.username,
            authorities = listOf(account.role)
        )
        logger.info("User '{}' logged in successfully", account.username)
        return AuthorizeVO(
            username = account.username,
            role = account.role,
            token = token,
            expire = DateUtils.dateToLocalDateTime(jwtUtils.expiresTime())
        )
    }

    suspend fun jwtTokenRelogin(headerToken: String?): ReloginVO? {
        val jwt = jwtUtils.resolveJwt(headerToken)
        if (jwt == null) {
            logger.warn("Relogin failed: token invalid or expired")
            return null
        }
        val username = jwtUtils.toUsername(jwt)
        val account = accountRepository.findByName(username)
        if (account == null) {
            logger.warn("Relogin failed: account '{}' not found", username)
            return null
        }

        val newToken = jwtUtils.createJwt(
            id = account.id ?: -1,
            username = account.username,
            authorities = listOf(account.role)
        )
        logger.info("Token successfully renewed for user '{}'", username)
        return ReloginVO(
            token = newToken,
            expire = DateUtils.dateToLocalDateTime(jwtUtils.expiresTime())
        )
    }

    suspend fun invalidateJwt(headerToken: String?): Boolean {
        logger.info("Invalidating JWT token")
        return jwtUtils.invalidateJwt(headerToken)
    }

    private suspend fun verifyCode(email: String, receivedCode: String?): String {
        val code = redisService.get(Const.VERIFY_EMAIL_DATA + email)
        if (code == null) {
            logger.warn("Verify code check failed: code not found or expired for email {}", email)
            return "verify code has not been sent"
        }

        return if (receivedCode == null || code != receivedCode) {
            logger.warn("Verify code check failed: code mismatch for email {}", email)
            "verify code is wrong."
        } else {
            ""
        }
    }
}
