package io.github.sor2171.backend.rpc

import io.github.sor2171.backend.entity.ApiResponse
import io.github.sor2171.backend.service.WsTokenService

class TestRpcServiceImpl(
    private val wsTokenService: WsTokenService
) : TestRpcService {

    override suspend fun hello(): String {
        return "Hello Kotlin Ktor RPC!"
    }

    override suspend fun getWsToken(handlerName: String): ApiResponse<String> {
        return try {
            val token = wsTokenService.registerToken(handlerName)
            ApiResponse.success(data = token)
        } catch (e: Exception) {
            ApiResponse.failure<String>(500, "", e.message)
        }
    }
}
