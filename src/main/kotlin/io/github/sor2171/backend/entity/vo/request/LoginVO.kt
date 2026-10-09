package io.github.sor2171.backend.entity.vo.request

import kotlinx.serialization.Serializable

@Serializable
data class LoginVO(
    val username: String = "",
    val password: String = "",
    val remember: Boolean = false,
)
