package io.github.sor2171

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlin.test.*

class ServerTest {
    @Test
    fun `test basic test route`() = testApplication {
        routing {
            get("/api/test/hello") {
                call.respondText("Hello Kotlin WebFlux!")
            }
        }
        val response = client.get("/api/test/hello")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Hello Kotlin WebFlux!", response.bodyAsText())
    }
}
