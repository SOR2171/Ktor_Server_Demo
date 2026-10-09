package io.github.sor2171.backend.entity.dto

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: Int? = null,
    val username: String = "",
    val password: String = "",
    val email: String = "",
    val role: String = "",
    val registerTime: LocalDateTime? = null
)
