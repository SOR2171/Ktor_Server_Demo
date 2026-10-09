package io.github.sor2171.backend.routes

import io.github.sor2171.backend.entity.ApiResponse
import io.github.sor2171.backend.entity.enums.EmailType
import io.github.sor2171.backend.entity.vo.request.EmailRegisterVO
import io.github.sor2171.backend.entity.vo.request.LoginVO
import io.github.sor2171.backend.entity.vo.request.PasswordResetVO
import io.github.sor2171.backend.service.AuthorizationService
import io.github.sor2171.backend.utils.Const
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authorizationService: AuthorizationService) {
    val handleLogin: suspend (ApplicationCall) -> Unit = { call ->
        val (username, password) = try {
            if (call.request.contentType().match(ContentType.Application.FormUrlEncoded)) {
                val params = call.receiveParameters()
                Pair(params["username"].orEmpty(), params["password"].orEmpty())
            } else {
                val body = call.receive<LoginVO>()
                Pair(body.username, body.password)
            }
        } catch (_: Exception) {
            Pair("", "")
        }

        if (username.isBlank() || password.isBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse.failure(400, null, "Username and password cannot be empty")
            )
        } else {
            val result = authorizationService.login(username, password)
            if (result != null) {
                call.respond(ApiResponse.success(result))
            } else {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse.unauthenticated("Invalid username or password")
                )
            }
        }
    }

    val handleAskCode: suspend (ApplicationCall) -> Unit = { call ->
        val email = call.parameters["email"].orEmpty()
        val typeStr = call.parameters["type"].orEmpty()
        val type = try {
            EmailType.valueOf(typeStr.uppercase())
        } catch (_: Exception) {
            EmailType.REGISTER
        }
        val ip = call.request.origin.remoteHost

        if (email.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, ApiResponse.failure(400, null, "Email cannot be blank"))
        } else {
            val msg = authorizationService.askEmailVerifyCode(type, email, ip)
            call.respond(Const.messageHandler(msg))
        }
    }

    val handleRegister: suspend (ApplicationCall) -> Unit = { call ->
        val vo = call.receive<EmailRegisterVO>()
        val msg = authorizationService.registerEmailAccount(vo)
        call.respond(Const.messageHandler(msg))
    }

    val handleReset: suspend (ApplicationCall) -> Unit = { call ->
        val vo = call.receive<PasswordResetVO>()
        val msg = authorizationService.resetEmailAccountPassword(vo)
        call.respond(Const.messageHandler(msg))
    }

    // Public auth endpoints supporting both /api/v1/auth and /api/auth
    route("/api") {
        listOf("/v1/auth", "/auth").forEach { prefix ->
            route(prefix) {
                post("/login") { handleLogin(call) }
                get("/ask-code") { handleAskCode(call) }
                post("/register") { handleRegister(call) }
                post("/reset") { handleReset(call) }
            }
        }
    }

    // Protected auth endpoints
    authenticate("auth-jwt") {
        route("/api") {
            listOf("/v1/auth", "/auth").forEach { prefix ->
                route(prefix) {
                    get("/logout") {
                        val header = call.request.headers[HttpHeaders.Authorization]
                        val success = authorizationService.invalidateJwt(header)
                        if (success) {
                            call.respond(ApiResponse.success())
                        } else {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse.logoutFailed())
                        }
                    }

                    get("/relogin") {
                        val header = call.request.headers[HttpHeaders.Authorization]
                        val vo = authorizationService.jwtTokenRelogin(header)
                        if (vo != null) {
                            call.respond(ApiResponse.success(vo))
                        } else {
                            call.respond(
                                HttpStatusCode.Unauthorized,
                                ApiResponse.failure(401, null, "Please login with password.")
                            )
                        }
                    }
                }
            }
        }
    }
}
