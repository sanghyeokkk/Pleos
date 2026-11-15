package com.example.myapplication

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceMileageTest {
    private val today = LocalDate.of(2026, 11, 15)

    @Test fun usesCurrentMileageOnlyForTodayWhenNoneWasProvided() {
        assertEquals(6433, selectVoiceMileage(today, today, null, 6433.0))
        assertNull(selectVoiceMileage(today.minusDays(1), today, null, 6433.0))
        assertEquals(3000, selectVoiceMileage(today, today, 3000, 6433.0))
    }

    @Test fun rejectsMissingAndInvalidCurrentMileage() {
        assertNull(selectVoiceMileage(today, today, null, null))
        assertNull(selectVoiceMileage(today, today, null, Double.NaN))
        assertNull(selectVoiceMileage(today, today, null, -1.0))
        assertNull(selectVoiceMileage(today, today, null, 100_000_000.0))
    }
}
