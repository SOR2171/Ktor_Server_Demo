package io.github.sor2171.backend.rpc

import io.github.sor2171.backend.entity.ApiResponse
import io.github.sor2171.backend.entity.enums.EmailType
import io.github.sor2171.backend.entity.vo.request.EmailRegisterVO
import io.github.sor2171.backend.entity.vo.request.LoginVO
import io.github.sor2171.backend.entity.vo.request.PasswordResetVO
import io.github.sor2171.backend.entity.vo.response.AuthorizeVO
import io.github.sor2171.backend.entity.vo.response.ReloginVO
import io.github.sor2171.backend.service.AuthorizationService

class AuthRpcServiceImpl(
    private val authorizationService: AuthorizationService
) : AuthRpcService {

    override suspend fun login(loginVO: LoginVO): ApiResponse<AuthorizeVO?> {
        if (loginVO.username.isBlank() || loginVO.password.isBlank()) {
            return ApiResponse.failure(400, null, "Username and password cannot be empty")
        }
        val result = authorizationService.login(loginVO.username, loginVO.password)
        return if (result != null) {
            ApiResponse.success(data = result)
        } else {
            ApiResponse.failure(401, null, "Invalid username or password")
        }
    }

    override suspend fun askCode(email: String, type: EmailType, clientIp: String): ApiResponse<String?> {
        if (email.isBlank()) {
            return ApiResponse.failure(400, null, "Email cannot be blank")
        }
        val ip = clientIp.ifBlank { "127.0.0.1" }
        val msg = authorizationService.askEmailVerifyCode(type, email, ip)
        return if (msg.isBlank()) {
            ApiResponse.success(data = null)
        } else {
            ApiResponse.failure(400, null, msg)
        }
    }

    override suspend fun register(registerVO: EmailRegisterVO): ApiResponse<String?> {
        val msg = authorizationService.registerEmailAccount(registerVO)
        return if (msg.isBlank()) {
            ApiResponse.success(data = null)
        } else {
            ApiResponse.failure(400, null, msg)
        }
    }

    override suspend fun resetPassword(resetVO: PasswordResetVO): ApiResponse<String?> {
        val msg = authorizationService.resetEmailAccountPassword(resetVO)
        return if (msg == "Password reset successfully") {
            ApiResponse.success(data = null, message = "Password reset successfully")
        } else {
            ApiResponse.failure(400, null, msg)
        }
    }

    override suspend fun logout(token: String): ApiResponse<String?> {
        val success = authorizationService.invalidateJwt(token)
        return if (success) {
            ApiResponse.success(data = null)
        } else {
            ApiResponse.failure(400, null, "Logout Failed")
        }
    }

    override suspend fun relogin(token: String): ApiResponse<ReloginVO?> {
        val vo = authorizationService.jwtTokenRelogin(token)
        return if (vo != null) {
            ApiResponse.success(data = vo)
        } else {
            ApiResponse.failure(401, null, "Please login with password.")
        }
    }
}
