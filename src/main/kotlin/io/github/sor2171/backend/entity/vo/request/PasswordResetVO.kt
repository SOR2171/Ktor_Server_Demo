package io.github.sor2171.backend.entity.vo.request

import kotlinx.serialization.Serializable

@Serializable
data class PasswordResetVO(
    val email: String = "",
    val code: String? = null,
    val password: String = "",
) {
    fun validate(): String? {
        if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))) {
            return "Invalid email address format"
        }
        if (code == null || code.length != 6) {
            return "Verification code must be 6 digits"
        }
        if (password.length !in 6..20) {
            return "Password must be 6-20 characters long"
        }
        return null
    }
}
