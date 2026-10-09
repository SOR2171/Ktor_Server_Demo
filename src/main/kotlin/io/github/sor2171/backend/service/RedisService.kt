package io.github.sor2171.backend.service

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class RedisService(redisUrl: String = "redis://localhost:6379") : AutoCloseable {
    private val logger = LoggerFactory.getLogger(RedisService::class.java)
    private val client: RedisClient = RedisClient.create(redisUrl)
    private val connection: StatefulRedisConnection<String, String> = client.connect()
    private val commands: RedisCommands<String, String> = connection.sync()

    suspend fun get(key: String): String? = withContext(Dispatchers.IO) {
        commands.get(key)
    }

    suspend fun set(key: String, value: String, expireSeconds: Long? = null): Unit = withContext(Dispatchers.IO) {
        if (expireSeconds != null && expireSeconds > 0) {
            commands.setex(key, expireSeconds, value)
        } else {
            commands.set(key, value)
        }
    }

    suspend fun delete(key: String): Boolean = withContext(Dispatchers.IO) {
        val deleted = commands.del(key)
        deleted != null && deleted > 0
    }

    suspend fun hasKey(key: String): Boolean = withContext(Dispatchers.IO) {
        val count = commands.exists(key)
        count != null && count > 0
    }

    suspend fun increment(key: String): Long = withContext(Dispatchers.IO) {
        commands.incr(key)
    }

    suspend fun expire(key: String, seconds: Long): Boolean = withContext(Dispatchers.IO) {
        commands.expire(key, seconds) ?: false
    }

    suspend fun limitOnceCheck(key: String, blockTimeSeconds: Long): Boolean = withContext(Dispatchers.IO) {
        val exists = commands.exists(key) ?: 0
        if (exists > 0) {
            false
        } else {
            commands.setex(key, blockTimeSeconds, "")
            true
        }
    }

    override fun close() {
        try {
            logger.info("Closing Redis connection and client...")
            connection.close()
            client.shutdown()
        } catch (e: Exception) {
            logger.error("Error closing Redis client: ${e.message}", e)
        }
    }
}
