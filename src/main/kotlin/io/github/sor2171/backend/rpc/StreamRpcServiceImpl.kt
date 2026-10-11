package io.github.sor2171.backend.rpc

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StreamRpcServiceImpl : StreamRpcService {

    override suspend fun echo(message: String): String {
        return "Kotlin Ktor RPC received: $message"
    }

    override fun echoStream(messages: Flow<String>): Flow<String> {
        return messages.map { message ->
            "Kotlin Ktor RPC received: $message"
        }
    }
}
