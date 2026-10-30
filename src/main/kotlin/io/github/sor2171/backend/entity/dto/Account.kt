package io.github.sor2171.backend.entity.dto

import io.github.sor2171.backend.annotation.NoArg
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@NoArg
@Serializable
data class Account(
    val id: Int? = null,
    val username: String,
    val password: String,
    val email: String,
    val role: String,
    val registerTime: LocalDateTime? = null
)
