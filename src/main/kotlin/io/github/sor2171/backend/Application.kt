package io.github.sor2171.backend

import io.github.sor2171.backend.plugins.FlowLimitPlugin
import io.github.sor2171.backend.plugins.configureCORS
import io.github.sor2171.backend.plugins.configureSecurity
import io.github.sor2171.backend.plugins.configureSerialization
import io.github.sor2171.backend.plugins.configureStatusPages
import io.github.sor2171.backend.plugins.configureWebsockets
import io.github.sor2171.backend.service.AuthorizationService
import io.github.sor2171.backend.service.MailService
import io.github.sor2171.backend.service.RabbitService
import io.github.sor2171.backend.service.RedisService
import io.github.sor2171.backend.service.WsTokenService
import io.github.sor2171.backend.database.AccountRepository
import io.github.sor2171.backend.database.DatabaseFactory
import io.github.sor2171.backend.routes.authRoutes
import io.github.sor2171.backend.routes.testRoutes
import io.github.sor2171.backend.routes.wsRoutes
import io.github.sor2171.security.JwtUtils
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.slf4j.LoggerFactory

fun Application.module() {
    val logger = LoggerFactory.getLogger("Application")
    val appScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // 1. Initialize Database
    try {
        DatabaseFactory.init(environment.config)
        logger.info("Database initialized successfully.")
    } catch (e: Exception) {
        logger.error("Failed to initialize database: ${e.message}", e)
    }

    // 2. Initialize Repositories
    val accountRepository = AccountRepository()

    // 3. Initialize Redis
    val redisUrl = environment.config.tryGetString("redis.url") ?: "redis://localhost:6379"
    val redisService = try {
        RedisService(redisUrl).also {
            logger.info("Redis connected successfully.")
        }
    } catch (e: Exception) {
        logger.error("Failed to connect Redis: ${e.message}", e)
        RedisService(redisUrl)
    }

    // 4. Initialize JWT
    val jwtKey = environment.config.tryGetString("jwt.key")
        ?: "2837452984690238475628394756203847562038475"
    val jwtExpireHours = environment.config.tryGetString("jwt.expire-hours")?.toIntOrNull() ?: 168
    val jwtUtils = JwtUtils(jwtKey, jwtExpireHours, redisService)

    // 5. Initialize Mail
    val mailHost = environment.config.tryGetString("mail.host") ?: "smtp.qq.com"
    val mailPort = environment.config.tryGetString("mail.port")?.toIntOrNull() ?: 465
    val mailUser = environment.config.tryGetString("mail.username") ?: ""
    val mailPass = environment.config.tryGetString("mail.password") ?: ""
    val mailService = MailService(mailHost, mailPort, mailUser, mailPass)

    // 6. Initialize RabbitMQ
    val rabbitHost = environment.config.tryGetString("rabbitmq.host") ?: "localhost"
    val rabbitPort = environment.config.tryGetString("rabbitmq.port")?.toIntOrNull() ?: 5672
    val rabbitVhost = environment.config.tryGetString("rabbitmq.virtual-host") ?: "/"
    val rabbitUser = environment.config.tryGetString("rabbitmq.username") ?: "admin"
    val rabbitPass = environment.config.tryGetString("rabbitmq.password") ?: "123456"
    val rabbitQueue = environment.config.tryGetString("rabbitmq.queue") ?: "mail"

    val rabbitService = try {
        RabbitService(
            rabbitHost,
            rabbitPort,
            rabbitVhost,
            rabbitUser,
            rabbitPass,
            rabbitQueue,
            mailService,
            appScope
        ).also {
            logger.info("RabbitMQ initialized successfully.")
        }
    } catch (e: Exception) {
        logger.error("Failed to connect RabbitMQ: ${e.message}", e)
        RabbitService(rabbitHost, rabbitPort, rabbitVhost, rabbitUser, rabbitPass, rabbitQueue, mailService, appScope)
    }

    // 7. Register Graceful Shutdown Hook for AutoCloseable resources
    val closeableResources: List<AutoCloseable> = listOf(
        rabbitService,
        redisService,
        DatabaseFactory
    )

    monitor.subscribe(ApplicationStopping) {
        logger.info("Application is stopping, releasing resources...")
        closeableResources.forEach { resource ->
            runCatching { resource.close() }
                .onFailure { logger.error("Failed to close resource ${resource::class.simpleName}: ${it.message}", it) }
        }
        logger.info("All resources released successfully.")
    }

    // 8. Initialize Business Services
    val wsTokenExpire = environment.config.tryGetString("ws.token-expire-time")?.toLongOrNull() ?: 300L
    val wsTokenService = WsTokenService(redisService, wsTokenExpire)
    val authorizationService = AuthorizationService(accountRepository, jwtUtils, redisService, rabbitService)

    // 8. Configure Ktor Plugins
    configureSerialization()
    configureCORS()
    configureSecurity(jwtUtils)
    configureStatusPages()
    configureWebsockets()

    // 9. Configure FlowLimit Plugin
    val flowCount = environment.config.tryGetString("flow-limit.count")?.toIntOrNull() ?: 1000
    val flowLimitTime = environment.config.tryGetString("flow-limit.limit-time")?.toIntOrNull() ?: 3
    val flowBanTime = environment.config.tryGetString("flow-limit.ban-time")?.toIntOrNull() ?: 60

    install(FlowLimitPlugin) {
        this.limitCount = flowCount
        this.limitTime = flowLimitTime
        this.banTime = flowBanTime
        this.redisService = redisService
    }

    // 10. Configure Routes
    routing {
        authRoutes(authorizationService)
        testRoutes(wsTokenService)
        wsRoutes(wsTokenService)
    }
}
