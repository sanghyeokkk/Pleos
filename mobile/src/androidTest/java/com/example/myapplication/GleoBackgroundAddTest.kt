package com.example.myapplication

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GleoBackgroundAddTest {
    private val base = ApplicationProvider.getApplicationContext<Context>()
    private val prefix = "gleo_background_test_${UUID.randomUUID()}"
    private val lastResponse = AtomicReference<JSONObject>()
    private val isolated = object : ContextWrapper(base) {
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = base.getSharedPreferences("${prefix}_$name", mode)
        override fun sendBroadcast(intent: Intent) {
            if (intent.action == "ai.pleos.playground.intent.action.APP_ACTION_RESPONSE") {
                lastResponse.set(JSONObject(intent.getStringExtra("response")!!))
            } else super.sendBroadcast(intent)
        }
    }

    @After fun cleanup() {
        base.deleteSharedPreferences("${prefix}_maintenance_records")
        base.deleteSharedPreferences("${prefix}_gleo_pending_record")
    }

    private fun request(parameters: JSONObject): JSONObject {
        lastResponse.set(null)
        val completed = CountDownLatch(1)
        val intent = Intent("com.example.myapplication.ADD_MAINTENANCE_RECORD").putExtra("ACTION_PARAM", parameters.toString())
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            GleoBackgroundAdd(isolated, intent) { completed.countDown() }.run()
        }
        assertTrue("No response within the Gleo action timeout", completed.await(8, TimeUnit.SECONDS))
        return lastResponse.get() ?: throw AssertionError("No Gleo response was sent")
    }

    @Test fun backgroundRequestsStageCorrectAndSaveOnlyAfterConfirmation() {
        val initial = JSONObject().put("item", "시험용 필터").put("date", LocalDate.now().toString())
            .put("intervalKm", 10000).put("mileageKm", 6000).put("confirmed", false)
        assertEquals("asking", request(initial).getString("status"))
        assertTrue(MaintenanceStore(isolated).load().isEmpty())
        val correction = JSONObject().put("intervalKm", 5000).put("confirmed", false)
        val corrected = request(correction)
        assertEquals("asking", corrected.getString("status"))
        assertTrue(MaintenanceStore(isolated).load().isEmpty())
        assertEquals("success", request(JSONObject().put("confirmed", true)).getString("status"))
        val saved = MaintenanceStore(isolated).load().single()
        assertEquals("시험용 필터", saved.item)
        assertEquals(6000, saved.mileageKm)
        assertEquals(5000, saved.intervalKm)
    }

    @Test fun changedDetailsMustBeConfirmedAgainAndCancelNeverSaves() {
        val initial = JSONObject().put("item", "시험용 와이퍼").put("date", LocalDate.now().toString())
            .put("intervalKm", 10000).put("mileageKm", 2000).put("confirmed", false)
        assertEquals("asking", request(initial).getString("status"))
        assertEquals("asking", request(JSONObject().put("intervalKm", 5000).put("confirmed", true)).getString("status"))
        assertTrue(MaintenanceStore(isolated).load().isEmpty())
        assertEquals("success", request(JSONObject().put("confirmed", false)).getString("status"))
        assertTrue(MaintenanceStore(isolated).load().isEmpty())
        assertEquals("asking", request(JSONObject().put("confirmed", true)).getString("status"))
        assertTrue(MaintenanceStore(isolated).load().isEmpty())
    }
}
