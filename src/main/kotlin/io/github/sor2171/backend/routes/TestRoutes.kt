package io.github.sor2171.backend.routes

import io.github.sor2171.backend.entity.ApiResponse
import io.github.sor2171.backend.service.WsTokenService
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.testRoutes(wsTokenService: WsTokenService) {
    route("/api/test") {
        get("/hello") {
            call.respondText("Hello Kotlin WebFlux!")
        }

        get("/wstoken") {
            try {
                val token = wsTokenService.registerToken("ExampleHandler")
                call.respond(ApiResponse.success(token))
            } catch (e: Exception) {
                call.respond(ApiResponse.failure(500, null, e.message))
            }
        }
    }
}
