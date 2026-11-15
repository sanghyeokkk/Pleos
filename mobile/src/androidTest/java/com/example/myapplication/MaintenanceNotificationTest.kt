package com.example.myapplication

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MaintenanceNotificationTest {
    private val base = ApplicationProvider.getApplicationContext<Context>()
    private val prefix = "maintenance_notification_test_${UUID.randomUUID()}"
    private val isolated = object : ContextWrapper(base) {
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences =
            base.getSharedPreferences("${prefix}_$name", mode)
    }
    private val manager = base.getSystemService(NotificationManager::class.java)
    private val notificationId = generateSequence(900_000_000) { it + 1 }
        .first { candidate -> manager.activeNotifications.none { it.id == candidate } }

    @After fun cleanup() {
        manager.cancel(notificationId)
        base.deleteSharedPreferences("${prefix}_maintenance_records")
        base.deleteSharedPreferences("${prefix}_maintenance_reminders")
    }

    private fun assertNotification(expected: Boolean) {
        val deadline = android.os.SystemClock.elapsedRealtime() + 2000
        while ((manager.activeNotifications.any { it.id == notificationId }) != expected && android.os.SystemClock.elapsedRealtime() < deadline) {
            android.os.SystemClock.sleep(50)
        }
        if (expected) assertTrue(manager.activeNotifications.any { it.id == notificationId })
        else assertFalse(manager.activeNotifications.any { it.id == notificationId })
    }

    @Test fun distanceAndDateNotificationsUseIsolatedRecords() {
        assumeTrue(NotificationManagerCompat.from(base).areNotificationsEnabled())
        if (Build.VERSION.SDK_INT >= 33) assumeTrue(ContextCompat.checkSelfPermission(base, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        isolated.getSharedPreferences("maintenance_reminders", Context.MODE_PRIVATE).edit().putBoolean("enabled", true).commit()
        val record = MaintenanceRecord(item = "시험용", date = LocalDate.now().minusDays(1), mileageKm = 1000, intervalKm = 10000, intervalMonths = null, costWon = null)
        val store = MaintenanceStore(isolated)
        store.save(listOf(record))
        MaintenanceReminders.notifyDue(isolated, 10999.0, notificationId)
        assertNotification(false)
        MaintenanceReminders.notifyDue(isolated, 11000.0, notificationId)
        assertNotification(true)
        MaintenanceReminders.notifyDue(isolated, null, notificationId)
        assertNotification(false)
        store.save(listOf(record.copy(date = LocalDate.now().minusMonths(1), intervalMonths = 1)))
        MaintenanceReminders.notifyDue(isolated, null, notificationId)
        assertNotification(true)
    }
}
