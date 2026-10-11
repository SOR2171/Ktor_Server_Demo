package io.github.sor2171.backend.plugins

import io.ktor.server.application.*
import kotlinx.rpc.krpc.ktor.server.Krpc

fun Application.configureRpc() {
    install(Krpc)
}
