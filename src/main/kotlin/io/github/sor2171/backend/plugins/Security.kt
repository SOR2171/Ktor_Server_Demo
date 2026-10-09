package io.github.sor2171.backend.plugins

import io.github.sor2171.backend.entity.ApiResponse
import io.github.sor2171.security.JwtUtils
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

fun Application.configureSecurity(jwtUtils: JwtUtils) {
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(jwtUtils.jwtVerifier)
            validate { credential ->
                val uuid = credential.payload.id ?: return@validate null
                if (jwtUtils.isInvalidToken(uuid)) {
                    return@validate null
                }
                val username = credential.payload.getClaim("username").asString()
                if (username.isNullOrBlank()) {
                    return@validate null
                }
                JWTPrincipal(credential.payload)
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse.unauthenticated("Authentication failed or token expired.")
                )
            }
        }
    }
}
