package io.github.sor2171.backend.entity.vo.request

import io.github.sor2171.backend.annotation.NoArg
import kotlinx.serialization.Serializable

@NoArg
@Serializable
data class LoginVO(
    val username: String,
    val password: String,
    val remember: Boolean = false,
)
