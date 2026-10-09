package io.github.sor2171.backend.service

import io.github.sor2171.backend.utils.Const.WS_REQUEST_TOKEN
import java.util.*

class WsTokenService(
    private val redisService: RedisService,
    private val defaultTokenExpireTime: Long = 300
) {
    suspend fun registerToken(
        handlerName: String,
        expireTime: Long? = null,
    ): String {
        val token = UUID.randomUUID().toString()
        val key = "$WS_REQUEST_TOKEN:$handlerName:$token"
        redisService.set(
            key,
            "1",
            expireTime ?: defaultTokenExpireTime
        )
        return token
    }

    suspend fun validateToken(
        handlerName: String,
        token: String?,
    ): Boolean {
        if (token.isNullOrBlank()) return false
        val key = "$WS_REQUEST_TOKEN:$handlerName:$token"
        return redisService.delete(key)
    }
}
