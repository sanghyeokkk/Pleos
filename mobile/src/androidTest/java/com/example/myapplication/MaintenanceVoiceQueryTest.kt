package com.example.myapplication

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MaintenanceVoiceQueryTest {
    private val base = ApplicationProvider.getApplicationContext<Context>()
    private val prefName = "voice_query_test_${UUID.randomUUID()}"
    private val isolated = object : ContextWrapper(base) {
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = base.getSharedPreferences(prefName, mode)
    }
    @After fun cleanup() { base.deleteSharedPreferences(prefName) }

    @Test fun lookupUsesLatestRecordAndIgnoresSpacing() {
        val records = listOf(
            MaintenanceRecord(item = "엔진 오일", date = LocalDate.of(2025, 1, 1), mileageKm = 1000, intervalKm = 5000, intervalMonths = null, costWon = null),
            MaintenanceRecord(item = "엔진오일", date = LocalDate.of(2026, 1, 1), mileageKm = 4000, intervalKm = 5000, intervalMonths = null, costWon = null)
        )
        MaintenanceStore(isolated).save(records)
        val result = maintenanceVoiceQuery(isolated, "GET_MAINTENANCE_RECORD", "엔진 오일", 5000.0)
        assertEquals("success", result.status)
        assertEquals(1, result.data!!.length())
        assertEquals(4000, result.data.getJSONObject(0).getInt("serviceMileageKm"))
        assertEquals(4000, result.data.getJSONObject(0).getInt("remainingKm"))
    }

    @Test fun dueQueryReportsMissingMileageAndTruncatedResults() {
        val records = (1..6).map { MaintenanceRecord(item = "item$it", date = LocalDate.of(2026, 1, 1), mileageKm = 100, intervalKm = 1000, intervalMonths = null, costWon = null) }
        MaintenanceStore(isolated).save(records)
        val result = maintenanceVoiceQuery(isolated, "GET_MAINTENANCE_DUE", "", null)
        assertEquals(5, result.data!!.length())
        assertTrue(result.message.contains("5 of 6"))
        assertTrue(result.message.contains("unavailable"))
        assertFalse(result.data.getJSONObject(0).has("remainingKm"))
    }
}
