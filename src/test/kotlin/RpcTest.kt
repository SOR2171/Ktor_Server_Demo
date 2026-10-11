package io.github.sor2171.backend

import io.github.sor2171.backend.entity.enums.EmailType
import io.github.sor2171.backend.entity.vo.request.LoginVO
import io.github.sor2171.backend.routes.rpcRoutes
import io.github.sor2171.backend.rpc.AuthRpcService
import io.github.sor2171.backend.rpc.StreamRpcService
import io.github.sor2171.backend.rpc.TestRpcService
import io.github.sor2171.backend.service.AuthorizationService
import io.github.sor2171.backend.service.WsTokenService
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import kotlin.test.Test
import kotlin.test.assertEquals

class RpcTest {

    @Test
    fun testRealRpcServices() = testApplication {
        install(Krpc)
        val appScope = CoroutineScope(Dispatchers.Default)

        routing {
            val redis = io.github.sor2171.backend.service.RedisService("redis://localhost:6379")
            val wsTokenService = WsTokenService(redis)
            val accountRepo = io.github.sor2171.backend.database.AccountRepository()
            val jwt = io.github.sor2171.security.JwtUtils("dummy-key-for-test-348572394857293847529384", 24, redis)
            val mail = io.github.sor2171.backend.service.MailService("", 465, "", "")
            val rabbit = io.github.sor2171.backend.service.RabbitService("localhost", 5672, "/", "admin", "123456", "mail", mail, appScope)
            val authService = AuthorizationService(accountRepo, jwt, redis, rabbit)

            rpcRoutes(authService, wsTokenService)
        }

        val rpcClient = createClient {
            installKrpc {
                serialization {
                    json()
                }
            }
        }.rpc {
            url {
                path("/rpc")
            }
        }

        // 1. Test TestRpcService
        val testService = rpcClient.withService<TestRpcService>()
        val helloResult = testService.hello()
        assertEquals("Hello Kotlin Ktor RPC!", helloResult)

        // 2. Test StreamRpcService - single call & streaming Flow
        val streamService = rpcClient.withService<StreamRpcService>()
        val echoResult = streamService.echo("hello stream")
        assertEquals("Kotlin Ktor RPC received: hello stream", echoResult)

        val streamResponses = streamService.echoStream(flowOf("A", "B", "C")).toList()
        assertEquals(
            listOf(
                "Kotlin Ktor RPC received: A",
                "Kotlin Ktor RPC received: B",
                "Kotlin Ktor RPC received: C"
            ),
            streamResponses
        )

        // 3. Test AuthRpcService validation
        val authRpcService = rpcClient.withService<AuthRpcService>()
        val emptyLoginRes = authRpcService.login(LoginVO("", ""))
        assertEquals(400, emptyLoginRes.code)
        assertEquals("Username and password cannot be empty", emptyLoginRes.message)

        val emptyCodeRes = authRpcService.askCode("", EmailType.REGISTER)
        assertEquals(400, emptyCodeRes.code)
        assertEquals("Email cannot be blank", emptyCodeRes.message)
    }
}
