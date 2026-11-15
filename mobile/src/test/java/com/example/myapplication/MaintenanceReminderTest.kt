package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MaintenanceReminderTest {
    @Test fun countsDistanceDueWithoutCountingTheSameRecordTwice() {
        val record = MaintenanceRecord(item = "오일", date = LocalDate.of(2025, 1, 1), mileageKm = 40000, intervalKm = 5000, intervalMonths = 12, costWon = null)
        assertEquals(1, dueCount(listOf(record), 45000.0, LocalDate.of(2025, 2, 1)))
        assertEquals(1, dueCount(listOf(record), 45000.0, LocalDate.of(2026, 2, 1)))
        assertEquals(0, dueCount(listOf(record), null, LocalDate.of(2025, 2, 1)))
        assertEquals(0, dueCount(listOf(record), 30000.0, LocalDate.of(2025, 2, 1)))
    }
    @Test fun countsOnlyTheLatestDateDuePerItem() {
        val records = listOf(
            MaintenanceRecord(item = "오일", date = LocalDate.of(2024, 1, 1), mileageKm = 10000, intervalKm = 10000, intervalMonths = 12, costWon = null),
            MaintenanceRecord(item = " 오일 ", date = LocalDate.of(2025, 12, 1), mileageKm = 20000, intervalKm = 10000, intervalMonths = 12, costWon = null),
            MaintenanceRecord(item = "와이퍼", date = LocalDate.of(2025, 1, 1), mileageKm = null, intervalKm = null, intervalMonths = 12, costWon = null),
            MaintenanceRecord(item = "타이어", date = LocalDate.of(2025, 1, 1), mileageKm = 1000, intervalKm = 1000, intervalMonths = null, costWon = null)
        )
        assertEquals(1, dateDueCount(records, LocalDate.of(2026, 1, 1)))
        assertEquals(2, dateDueCount(records, LocalDate.of(2026, 12, 1)))
    }

    @Test fun groupsTheSameItemWhenSpacingDiffers() {
        val records = listOf(
            MaintenanceRecord(item = "엔진 오일", date = LocalDate.of(2024, 1, 1), mileageKm = null, intervalKm = null, intervalMonths = 12, costWon = null),
            MaintenanceRecord(item = "엔진오일", date = LocalDate.of(2026, 1, 1), mileageKm = null, intervalKm = null, intervalMonths = 12, costWon = null)
        )
        assertEquals(0, dateDueCount(records, LocalDate.of(2026, 2, 1)))
    }
}
