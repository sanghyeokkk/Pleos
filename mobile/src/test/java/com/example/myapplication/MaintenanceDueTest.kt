package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class MaintenanceDueTest {
    @Test fun combinesDistanceAndCalendarDeadlines() {
        val record = MaintenanceRecord(item = "에어컨 필터", date = LocalDate.of(2024, 2, 29), mileageKm = 40000, intervalKm = 10000, intervalMonths = 12, costWon = null)
        val due = maintenanceDue(record, 48000.0)
        assertEquals(2000, due.distanceRemaining)
        assertEquals(LocalDate.of(2025, 2, 28), due.dueDate)
    }
    @Test fun unknownHistoricalMileageNeverUsesCurrentMileage() {
        val record = MaintenanceRecord(item = "와이퍼", date = LocalDate.of(2026, 1, 1), mileageKm = null, intervalKm = 10000, intervalMonths = null, costWon = null)
        assertNull(maintenanceDue(record, 43000.0).distanceRemaining)
    }
    @Test fun marksOverdueDistanceAndDate() {
        val record = MaintenanceRecord(item = "타이어", date = LocalDate.of(2025, 1, 1), mileageKm = 40000, intervalKm = 5000, intervalMonths = 12, costWon = null)
        val label = dueLabel(record, 46000.0, LocalDate.of(2026, 2, 1))
        assertEquals("거리 기준 점검 시기 지남 · 날짜 기준 점검 시기 지남 (2026-01-01)", label)
    }
    @Test fun neverCalculatesDistanceWhenCurrentIsBelowRecordedMileage() {
        val record = MaintenanceRecord(item = "와이퍼", date = LocalDate.of(2026, 1, 1), mileageKm = 40000, intervalKm = 10000, intervalMonths = null, costWon = null)
        assertNull(maintenanceDue(record, 0.0).distanceRemaining)
        assertEquals("현재 거리가 기록 당시 거리보다 작습니다. 입력값을 확인해 주세요.", dueLabel(record, 0.0))
    }
}
