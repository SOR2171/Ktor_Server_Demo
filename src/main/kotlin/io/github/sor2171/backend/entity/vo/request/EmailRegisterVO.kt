package io.github.sor2171.backend.entity.vo.request

import kotlinx.serialization.Serializable

@Serializable
data class EmailRegisterVO(
    val email: String = "",
    val code: String? = null,
    val username: String = "",
    val password: String = "",
) {
    fun validate(): String? {
        if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))) {
            return "Invalid email address format"
        }
        if (code == null || code.length != 6) {
            return "Verification code must be 6 digits"
        }
        if (username.length !in 4..16 || !username.matches(Regex("^[a-zA-Z0-9\\u4e00-\\u9fa5]+$"))) {
            return "Username must be 4-16 characters and contain only letters, numbers, or Chinese characters"
        }
        if (password.length !in 6..20) {
            return "Password must be 6-20 characters long"
        }
        return null
    }
}
