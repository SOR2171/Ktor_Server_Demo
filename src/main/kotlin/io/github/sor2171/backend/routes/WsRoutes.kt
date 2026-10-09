package io.github.sor2171.backend.routes

import io.github.sor2171.backend.service.WsTokenService
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*

fun Route.wsRoutes(wsTokenService: WsTokenService) {
    webSocket("/ws/test") {
        for (frame in incoming) {
            if (frame is Frame.Text) {
                val text = frame.readText()
                send(Frame.Text("Kotlin springboot4 received: $text"))
            }
        }
    }

    webSocket("/ws/v1/example") {
        val token = call.request.queryParameters["token"]
        val isValid = wsTokenService.validateToken("ExampleHandler", token)
        if (!isValid) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid or expired token"))
            return@webSocket
        }

        for (frame in incoming) {
            if (frame is Frame.Text) {
                val text = frame.readText()
                send(Frame.Text("Kotlin springboot4 received: $text"))
            }
        }
    }
}
