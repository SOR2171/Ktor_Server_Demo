package io.github.sor2171.backend.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.*
import kotlin.time.Clock
import kotlin.time.toKotlinInstant

object DateUtils {
    fun getCurrentDateTime(): LocalDateTime =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    fun dateToLocalDateTime(date: Date): LocalDateTime {
        return date
            .toInstant()
            .toKotlinInstant()
            .toLocalDateTime(TimeZone.currentSystemDefault())
    }
}
