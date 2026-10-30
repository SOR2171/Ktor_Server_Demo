package io.github.sor2171.backend.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.tryGetString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

object DatabaseFactory : AutoCloseable {
    private var dataSource: HikariDataSource? = null

    fun init(config: ApplicationConfig) {
        val url = config.tryGetString("datasource.url")
            ?: "jdbc:mysql://localhost:3306/ktor_rpc?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
        val driver = config.tryGetString("datasource.driver")
            ?: "com.mysql.cj.jdbc.Driver"
        val username = config.tryGetString("datasource.username") ?: "root"
        val password = config.tryGetString("datasource.password") ?: "123456"
        val maxPoolSize = config.tryGetString("datasource.max-pool-size")?.toIntOrNull() ?: 10

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = url
            driverClassName = driver
            this.username = username
            this.password = password
            maximumPoolSize = maxPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val ds = HikariDataSource(hikariConfig)
        dataSource = ds
        Database.connect(ds)
    }

    override fun close() {
        dataSource?.close()
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        withContext(Dispatchers.IO) {
            suspendTransaction { block() }
        }
}
