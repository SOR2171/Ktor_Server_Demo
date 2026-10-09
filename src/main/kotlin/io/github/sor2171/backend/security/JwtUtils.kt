package io.github.sor2171.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.github.sor2171.backend.service.RedisService
import io.github.sor2171.backend.utils.Const
import org.slf4j.LoggerFactory
import java.util.*

class JwtUtils(
    key: String,
    private val expireHours: Int,
    private val redisService: RedisService
) {
    val algorithm: Algorithm = Algorithm.HMAC256(key)
    val jwtVerifier: JWTVerifier = JWT.require(algorithm).build()
    private val logger = LoggerFactory.getLogger(JwtUtils::class.java)

    suspend fun invalidateJwt(headerToken: String?): Boolean {
        val token = convertToToken(headerToken) ?: return false
        return try {
            val jwt = jwtVerifier.verify(token)
            val id = jwt.id ?: return false
            deleteToken(id, jwt.expiresAt)
        } catch (e: Exception) {
            logger.error("Error occurred while invalidating JWT", e)
            false
        }
    }

    private suspend fun deleteToken(uuid: String, expiresAt: Date?): Boolean {
        val now = Date()
        val expireMillis = if (expiresAt != null) (expiresAt.time - now.time).coerceAtLeast(0) else 0L
        val expireSeconds = (expireMillis / 1000).coerceAtLeast(1)
        redisService.set(
            Const.JWT_BLACK_LIST + uuid,
            "deleted",
            expireSeconds
        )
        return true
    }

    suspend fun isInvalidToken(uuid: String): Boolean =
        redisService.hasKey(Const.JWT_BLACK_LIST + uuid)

    suspend fun resolveJwt(headerToken: String?): DecodedJWT? {
        val token = convertToToken(headerToken) ?: return null
        return try {
            val decodedJWT = jwtVerifier.verify(token)
            val uuid = decodedJWT.id
            if (uuid != null && isInvalidToken(uuid)) return null
            val expiresAt = decodedJWT.expiresAt
            if (expiresAt != null && Date().after(expiresAt)) null
            else decodedJWT
        } catch (e: Exception) {
            logger.error("Error occurred while resolving JWT: ${e.message}")
            null
        }
    }

    fun createJwt(
        id: Int,
        username: String,
        authorities: List<String> = listOf("user")
    ): String {
        return JWT.create()
            .withJWTId(UUID.randomUUID().toString())
            .withClaim("id", id)
            .withClaim("username", username)
            .withClaim("authorities", authorities)
            .withExpiresAt(expiresTime())
            .withIssuedAt(Date())
            .sign(algorithm)
    }

    fun toId(jwt: DecodedJWT): Int = jwt.claims["id"]?.asInt() ?: -1

    fun toUsername(jwt: DecodedJWT): String = jwt.claims["username"]?.asString() ?: ""

    fun expiresTime(): Date {
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR, expireHours)
        return cal.time
    }

    fun convertToToken(headerToken: String?): String? {
        val token = headerToken?.replace("Bearer ", "")?.trim()
        return token?.takeIf { it.isNotEmpty() }
    }
}
