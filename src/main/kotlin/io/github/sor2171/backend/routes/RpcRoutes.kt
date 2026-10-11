package io.github.sor2171.backend.routes

import io.github.sor2171.backend.rpc.AuthRpcService
import io.github.sor2171.backend.rpc.AuthRpcServiceImpl
import io.github.sor2171.backend.rpc.StreamRpcService
import io.github.sor2171.backend.rpc.StreamRpcServiceImpl
import io.github.sor2171.backend.rpc.TestRpcService
import io.github.sor2171.backend.rpc.TestRpcServiceImpl
import io.github.sor2171.backend.service.AuthorizationService
import io.github.sor2171.backend.service.WsTokenService
import io.ktor.server.routing.*
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json

fun Route.rpcRoutes(
    authorizationService: AuthorizationService,
    wsTokenService: WsTokenService
) {
    rpc("/rpc") {
        rpcConfig {
            serialization {
                json()
            }
        }
        registerService<AuthRpcService> {
            AuthRpcServiceImpl(authorizationService)
        }
        registerService<TestRpcService> {
            TestRpcServiceImpl(wsTokenService)
        }
        registerService<StreamRpcService> {
            StreamRpcServiceImpl()
        }
    }
}
