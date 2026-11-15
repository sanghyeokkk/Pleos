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
class VoicePendingStoreTest {
    private val base = ApplicationProvider.getApplicationContext<Context>()
    private val prefName = "voice_pending_test_${UUID.randomUUID()}"
    private val isolated = object : ContextWrapper(base) {
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = base.getSharedPreferences(prefName, mode)
    }
    @After fun cleanup() { base.deleteSharedPreferences(prefName) }

    @Test fun persistsDraftAcrossInstancesAndExpiresIt() {
        val record = MaintenanceRecord(item = "와이퍼", date = LocalDate.of(2026, 11, 15), mileageKm = 100, intervalKm = 5000, intervalMonths = null, costWon = 20000)
        VoicePendingStore(isolated).save(StoredVoiceDraft(record, 1000L))
        assertEquals(StoredVoiceDraft(record, 1000L), VoicePendingStore(isolated).load(2000L))
        assertNull(VoicePendingStore(isolated).load(121000L))
        assertNull(VoicePendingStore(isolated).load(2000L))
    }
}
