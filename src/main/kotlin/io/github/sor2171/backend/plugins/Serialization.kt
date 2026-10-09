package io.github.sor2171.backend.plugins

import io.github.sor2171.backend.utils.BigDecimalSerializer
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            serializersModule = SerializersModule {
                contextual(BigDecimalSerializer)
            }
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = false
            prettyPrint = true
        })
    }
}
