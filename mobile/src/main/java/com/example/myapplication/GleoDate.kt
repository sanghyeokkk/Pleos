package com.example.myapplication

import java.time.LocalDate

internal fun parseGleoDate(value: String, today: LocalDate = LocalDate.now()): LocalDate? {
    val text = value.trim()
    if (text == "오늘") return today
    if (text == "어제") return today.minusDays(1)
    if (text == "그저께") return today.minusDays(2)
    val daysAgo = Regex("^(\\d{1,4})\\s*일\\s*전$").matchEntire(text)
    if (daysAgo != null) {
        val days = daysAgo.groupValues[1].toLong()
        return if (days in 1..3650) today.minusDays(days) else null
    }
    val korean = Regex("^(\\d{4})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일$").matchEntire(text)
    if (korean != null) return runCatching {
        LocalDate.of(korean.groupValues[1].toInt(), korean.groupValues[2].toInt(), korean.groupValues[3].toInt())
    }.getOrNull()
    return runCatching { LocalDate.parse(text) }.getOrNull()
}
