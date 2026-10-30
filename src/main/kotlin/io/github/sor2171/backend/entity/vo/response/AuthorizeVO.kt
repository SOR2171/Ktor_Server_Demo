package io.github.sor2171.backend.entity.vo.response

import io.github.sor2171.backend.annotation.NoArg
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@NoArg
@Serializable
data class AuthorizeVO(
    val username: String,
    val role: String,
    val token: String,
    val expire: LocalDateTime,
)
