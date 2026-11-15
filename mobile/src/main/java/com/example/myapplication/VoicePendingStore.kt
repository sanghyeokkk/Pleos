package com.example.myapplication

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate

internal data class StoredVoiceDraft(val record: MaintenanceRecord, val createdAt: Long)

internal class VoicePendingStore(context: Context) {
    private val prefs = context.getSharedPreferences("gleo_pending_record", Context.MODE_PRIVATE)

    fun load(now: Long = System.currentTimeMillis()): StoredVoiceDraft? = runCatching {
        val raw = prefs.getString("pending", null) ?: return null
        val o = JSONObject(raw)
        val created = o.getLong("createdAt")
        if (created > now || now - created >= 120_000) { clear(); return null }
        fun optionalInt(key: String): Int? = if (o.has(key) && !o.isNull(key)) o.getInt(key) else null
        StoredVoiceDraft(MaintenanceRecord(
            id = o.getString("id"), item = o.getString("item"), date = LocalDate.parse(o.getString("date")),
            mileageKm = optionalInt("mileageKm"), intervalKm = optionalInt("intervalKm"),
            intervalMonths = optionalInt("intervalMonths"), costWon = optionalInt("costWon")
        ), created)
    }.getOrNull()

    fun save(draft: StoredVoiceDraft) {
        val record = draft.record
        val json = JSONObject().put("id", record.id).put("item", record.item).put("date", record.date.toString())
            .put("mileageKm", record.mileageKm ?: JSONObject.NULL).put("intervalKm", record.intervalKm ?: JSONObject.NULL)
            .put("intervalMonths", record.intervalMonths ?: JSONObject.NULL).put("costWon", record.costWon ?: JSONObject.NULL)
            .put("createdAt", draft.createdAt)
        check(prefs.edit().putString("pending", json.toString()).commit())
    }

    fun clear() { check(prefs.edit().remove("pending").commit()) }
}
