package io.github.sor2171.backend.entity.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EmailType {
    @SerialName("register")
    REGISTER,

    @SerialName("reset")
    RESET,
}
