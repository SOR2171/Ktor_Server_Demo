package io.github.sor2171.backend.rpc

import io.github.sor2171.backend.entity.ApiResponse
import kotlinx.rpc.annotations.Rpc

@Rpc
interface TestRpcService {
    suspend fun hello(): String
    suspend fun getWsToken(handlerName: String = "ExampleHandler"): ApiResponse<String>
}
