package io.github.sor2171.backend.service

import io.github.sor2171.backend.entity.enums.EmailType
import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.Properties

class MailService(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String
) {
    private val logger = LoggerFactory.getLogger(MailService::class.java)

    suspend fun sendVerificationEmail(email: String, type: EmailType, code: String) = withContext(Dispatchers.IO) {
        val (subject, content) = when (type) {
            EmailType.REGISTER -> Pair(
                "Welcome to Our Service",
                "Thank you for registering! Your verification code is: $code\nThis code is valid for 3 minutes."
            )
            EmailType.RESET -> Pair(
                "Password Reset Request",
                "You requested a password reset. Your verification code is: $code\nThis code is valid for 3 minutes."
            )
        }

        try {
            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.ssl.enable", "true")
                put("mail.smtp.host", host)
                put("mail.smtp.port", port.toString())
                put("mail.smtp.timeout", "5000")
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(username, password)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(username))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(email))
                this.subject = subject
                setText(content, "UTF-8")
            }

            Transport.send(message)
            logger.info("Email sent to $email successfully.")
        } catch (e: Exception) {
            logger.error("Failed to send email to $email: ${e.message}", e)
        }
    }
}
