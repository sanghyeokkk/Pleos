package com.example.myapplication

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MaintenanceStoreTest {
    private val base = ApplicationProvider.getApplicationContext<Context>()
    private val preferenceName = "maintenance_test_${UUID.randomUUID()}"
    private val isolated = object : ContextWrapper(base) {
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = base.getSharedPreferences(preferenceName, mode)
    }

    @After fun cleanUp() { base.deleteSharedPreferences(preferenceName) }

    @Test fun saveReloadAndUpdateRecords() {
        val original = MaintenanceRecord(item = "와이퍼", date = LocalDate.of(2026, 1, 2), mileageKm = 40000, intervalKm = 10000, intervalMonths = 12, costWon = 25000)
        MaintenanceStore(isolated).save(listOf(original))
        assertEquals(listOf(original), MaintenanceStore(isolated).load())
        val edited = original.copy(costWon = 30000)
        MaintenanceStore(isolated).save(listOf(edited))
        assertEquals(listOf(edited), MaintenanceStore(isolated).load())
    }
}
