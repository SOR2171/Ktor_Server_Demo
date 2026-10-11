package io.github.sor2171.backend.rpc

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.annotations.Rpc

@Rpc
interface StreamRpcService {
    suspend fun echo(message: String): String
    fun echoStream(messages: Flow<String>): Flow<String>
}
