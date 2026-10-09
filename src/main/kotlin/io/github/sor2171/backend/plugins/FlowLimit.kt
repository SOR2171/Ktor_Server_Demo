package io.github.sor2171.backend.plugins

import io.github.sor2171.backend.entity.ApiResponse
import io.github.sor2171.backend.service.RedisService
import io.github.sor2171.backend.utils.Const
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*

class FlowLimitConfiguration {
    var limitCount: Int = 1000
    var limitTime: Int = 3
    var banTime: Int = 60
    lateinit var redisService: RedisService
}

val FlowLimitPlugin = createApplicationPlugin(
    name = "FlowLimitPlugin",
    createConfiguration = ::FlowLimitConfiguration
) {
    val limitCount = pluginConfig.limitCount
    val limitTime = pluginConfig.limitTime
    val banTime = pluginConfig.banTime
    val redisService = pluginConfig.redisService

    onCall { call ->
        val ip = call.request.origin.remoteHost
        val blockKey = Const.FLOW_LIMIT_BLOCK + ip
        val counterKey = Const.FLOW_LIMIT_COUNTER + ip

        val isBlocked = redisService.hasKey(blockKey)
        if (isBlocked) {
            call.respond(
                HttpStatusCode.Forbidden,
                ApiResponse.forbidden("Too many requests")
            )
            return@onCall
        }

        val count = redisService.increment(counterKey)
        if (count == 1L) {
            redisService.expire(counterKey, limitTime.toLong())
        } else if (count > limitCount) {
            redisService.set(blockKey, "1", banTime.toLong())
            call.respond(
                HttpStatusCode.Forbidden,
                ApiResponse.forbidden("Too many requests")
            )
            return@onCall
        }
    }
}
