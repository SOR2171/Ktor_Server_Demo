package io.github.sor2171.backend.plugins

import io.github.sor2171.backend.entity.ApiResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory

fun Application.configureStatusPages() {
    val logger = LoggerFactory.getLogger("StatusPages")

    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            logger.warn("BadRequestException: ${cause.message}")
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse.failure(400, null, cause.message ?: "Invalid request format")
            )
        }
        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception: ${cause.message}", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse.failure(500, null, cause.message ?: "Internal server error")
            )
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                status,
                ApiResponse.failure(404, null, "Endpoint not found")
            )
        }
    }
}
