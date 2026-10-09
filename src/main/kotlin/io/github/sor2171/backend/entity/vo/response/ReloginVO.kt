package io.github.sor2171.backend.entity.vo.response

import io.github.sor2171.backend.utils.DateUtils.getCurrentDateTime
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class ReloginVO(
    val token: String = "",
    val expire: LocalDateTime = getCurrentDateTime(),
)
