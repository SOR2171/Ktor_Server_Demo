package io.github.sor2171.backend.entity.dto

import io.github.sor2171.backend.annotation.NoArg
import io.github.sor2171.backend.entity.enums.EmailType
import kotlinx.serialization.Serializable

@NoArg
@Serializable
data class EmailVerificationMsg(
    val email: String,
    val type: EmailType,
    val code: String
)
