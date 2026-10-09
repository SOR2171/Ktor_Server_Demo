package io.github.sor2171.backend.service

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import io.github.sor2171.backend.entity.dto.EmailVerificationMsg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class RabbitService(
    host: String,
    port: Int,
    virtualHost: String = "/",
    username: String,
    password: String,
    private val queueName: String = "mail",
    private val mailService: MailService,
    private val scope: CoroutineScope
) : AutoCloseable {
    private val logger = LoggerFactory.getLogger(RabbitService::class.java)
    private val connection: Connection
    private val channel: Channel

    init {
        val factory = ConnectionFactory().apply {
            this.host = host
            this.port = port
            this.virtualHost = virtualHost
            this.username = username
            this.password = password
        }
        connection = factory.newConnection()
        channel = connection.createChannel()
        channel.queueDeclare(queueName, true, false, false, null)
        startConsumer()
    }

    fun sendMailMessage(msg: EmailVerificationMsg) {
        val json = Json.encodeToString(msg)
        val props = AMQP.BasicProperties.Builder()
            .contentType("application/json")
            .deliveryMode(2)
            .build()
        channel.basicPublish("", queueName, props, json.toByteArray(Charsets.UTF_8))
        logger.info("Sent email verification message for ${msg.email} to RabbitMQ queue '$queueName'")
    }

    private fun startConsumer() {
        channel.basicConsume(queueName, false, { _, delivery ->
            val body = String(delivery.body, Charsets.UTF_8)
            channel.basicAck(delivery.envelope.deliveryTag, false)
            scope.launch(Dispatchers.IO) {
                try {
                    val data = Json.decodeFromString<EmailVerificationMsg>(body)
                    mailService.sendVerificationEmail(data.email, data.type, data.code)
                } catch (e: Exception) {
                    logger.error("Error processing mail queue message: ${e.message}", e)
                }
            }
        }, { consumerTag ->
            logger.warn("Consumer $consumerTag cancelled")
        })
        logger.info("RabbitMQ listener on queue '$queueName' started.")
    }

    override fun close() {
        try {
            channel.close()
            connection.close()
        } catch (e: Exception) {
            logger.error("Error closing RabbitMQ connection: ${e.message}", e)
        }
    }
}
