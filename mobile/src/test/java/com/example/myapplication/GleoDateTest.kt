package com.example.myapplication

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GleoDateTest {
    private val today = LocalDate.of(2026, 11, 14)

    @Test fun handlesRelativeDatesUsingDeviceDate() {
        assertEquals(today, parseGleoDate("오늘", today))
        assertEquals(today.minusDays(1), parseGleoDate("어제", today))
        assertEquals(today.minusDays(2), parseGleoDate("그저께", today))
        assertEquals(today.minusDays(3), parseGleoDate("3일 전", today))
        assertNull(parseGleoDate("지난주", today))
    }

    @Test fun acceptsKoreanAndIsoDatesAndRejectsInvalidDates() {
        assertEquals(today, parseGleoDate("2026년 11월 14일", today))
        assertEquals(today, parseGleoDate("2026-11-14", today))
        assertNull(parseGleoDate("2026년 2월 30일", today))
    }
}
