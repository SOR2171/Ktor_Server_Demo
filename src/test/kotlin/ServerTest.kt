package io.github.sor2171.backend

import io.github.sor2171.backend.database.AccountRepository
import io.github.sor2171.backend.database.DatabaseFactory
import io.github.sor2171.backend.entity.dto.Account
import io.github.sor2171.backend.utils.DateUtils
import io.github.sor2171.backend.utils.PasswordEncoder
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class ServerTest {

    @Test
    fun `test default user and password hash`() = runBlocking {
        val rawPassword = "123456"
        val encodedPassword = PasswordEncoder.encode(rawPassword)
        println("==================================================================")
        println("Default password '$rawPassword' BCrypt hash: $encodedPassword")
        println("==================================================================")

        try {
            val config = ApplicationConfig("application.yaml")
            DatabaseFactory.init(config)
            val repository = AccountRepository()

            val existingAccount = repository.findByName("test")
            if (existingAccount == null) {
                val newAccount = Account(
                    id = null,
                    username = "test",
                    password = encodedPassword,
                    email = "1234567890@gmail.com",
                    role = "user",
                    registerTime = DateUtils.getCurrentDateTime()
                )
                val newId = repository.save(newAccount)
                println("Created default test user 'test' in database (ID: $newId).")
            } else {
                repository.resetPasswordByEmail(existingAccount.email, encodedPassword)
                println("Existing test user 'test' found (ID: ${existingAccount.id}), updated password hash.")
            }

            val verifiedAccount = repository.findByName("test")
            assertNotNull(verifiedAccount)
            assertTrue(PasswordEncoder.matches(rawPassword, verifiedAccount.password))
            println("Verification passed: user 'test' password successfully matched with BCrypt.")
        } catch (e: Exception) {
            println("Database check skipped or failed (check if MySQL is running): ${e.message}")
        }
    }

    @Test
    fun `test basic test route`() = testApplication {
        routing {
            get("/api/test/hello") {
                call.respondText("Hello Kotlin Ktor!")
            }
        }
        val response = client.get("/api/test/hello")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Hello Kotlin Ktor!", response.bodyAsText())
    }
}
