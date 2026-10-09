package io.github.sor2171.backend.utils

import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import java.util.*

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
