package com.example.myapplication

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean

/** Handles read-only requests without requiring an Activity to start in the background. */
internal class GleoBackgroundQuery(
    private val context: Context,
    private val intent: Intent,
    private val pendingResult: BroadcastReceiver.PendingResult
) {
    private val handler = Handler(Looper.getMainLooper())
    private val finished = AtomicBoolean(false)
    private var vehicle: Vehicle? = null

    fun run() {
        handler.postDelayed({ finish(null) }, 1800)
        try {
            val sdk = Vehicle(context)
            vehicle = sdk
            sdk.initialize()
            val odometer = sdk.getOdometer()
            odometer.checkCurrentDistanceDrivenCapabilities(onComplete = { capability ->
                if (capability.accessType != AccessType.READ && capability.accessType != AccessType.READ_WRITE) finish(null)
                else try {
                    odometer.getCurrentDistanceDriven(onComplete = { finish(it?.toDouble()) }, onFailed = { finish(null) })
                } catch (_: Exception) { finish(null) }
            }, onFailed = { finish(null) })
        } catch (_: Exception) { finish(null) }
    }

    private fun finish(current: Double?) {
        if (!finished.compareAndSet(false, true)) return
        handler.post {
            try {
                val action = intent.action?.removePrefix("${context.packageName}.") ?: return@post
                val raw = intent.getStringExtra("ACTION_PARAM")
                val json = runCatching {
                    if (raw.isNullOrBlank()) JSONObject()
                    else if (raw.trim().startsWith("\"")) JSONObject(org.json.JSONTokener(raw).nextValue() as String)
                    else JSONObject(raw)
                }.getOrDefault(JSONObject())
                val item = (json.optString("item").takeIf { it.isNotBlank() } ?: intent.getStringExtra("item").orEmpty()).trim().take(40)
                val result = maintenanceVoiceQuery(context, action, item, current?.takeIf { it.isFinite() && it >= 0 })
                val response = JSONObject().put("functionName", action).put("status", result.status).put("message", result.message)
                result.data?.let { response.put("data", it.toString()) }
                context.sendBroadcast(Intent("ai.pleos.playground.intent.action.APP_ACTION_RESPONSE").apply {
                    setPackage("ai.umos.ambientai")
                    putExtra("packageName", context.packageName)
                    putExtra("response", response.toString())
                })
            } catch (_: Exception) {
                val action = intent.action?.removePrefix("${context.packageName}.")
                if (action != null) runCatching {
                    val response = JSONObject().put("functionName", action).put("status", "fail").put("message", "The maintenance query could not be processed.")
                    context.sendBroadcast(Intent("ai.pleos.playground.intent.action.APP_ACTION_RESPONSE").apply {
                        setPackage("ai.umos.ambientai")
                        putExtra("packageName", context.packageName)
                        putExtra("response", response.toString())
                    })
                }
            } finally {
                runCatching { vehicle?.release() }
                pendingResult.finish()
            }
        }
    }
}

internal data class MaintenanceVoiceQueryResult(val status: String, val message: String, val data: JSONArray? = null)

internal fun maintenanceVoiceQuery(context: Context, action: String, item: String, currentMileage: Double?): MaintenanceVoiceQueryResult {
    return try {
        if (action == "GET_MAINTENANCE_RECORD" && item.isBlank()) return MaintenanceVoiceQueryResult("asking", "A maintenance item is required.")
        val records = MaintenanceStore(context).load()
        val latest = records.groupBy { normalizeMaintenanceItem(it.item) }.values.map { group ->
            group.maxWith(compareBy<MaintenanceRecord> { it.date }.thenBy { records.indexOf(it) })
        }
        val selected = if (action == "GET_MAINTENANCE_RECORD") latest.filter { normalizeMaintenanceItem(it.item) == normalizeMaintenanceItem(item) } else latest
        if (selected.isEmpty()) return MaintenanceVoiceQueryResult("success", if (action == "GET_MAINTENANCE_RECORD") "No maintenance record was found for the requested item." else "There are no saved maintenance records.")
        val ordered = selected.sortedWith(compareBy<MaintenanceRecord> { record ->
            val due = maintenanceDue(record, currentMileage)
            if ((due.distanceRemaining != null && due.distanceRemaining <= 0) || (due.dueDate != null && !due.dueDate.isAfter(LocalDate.now()))) 0 else 1
        }.thenBy { it.item })
        val data = JSONArray()
        ordered.take(5).forEach { record ->
            val row = JSONObject().put("item", record.item).put("lastServiceDate", record.date.toString())
            record.mileageKm?.let { row.put("serviceMileageKm", it) }
            record.costWon?.let { row.put("costWon", it) }
            record.intervalKm?.let { row.put("intervalKm", it) }
            record.intervalMonths?.let { row.put("intervalMonths", it) }
            val due = maintenanceDue(record, currentMileage)
            due.dueDate?.let { row.put("nextDueDate", it.toString()) }
            due.distanceRemaining?.let { row.put("remainingKm", it) }
            if (currentMileage != null && record.mileageKm != null && currentMileage < record.mileageKm) row.put("mileageNeedsReview", true)
            data.put(row)
        }
        val mileageNote = if (currentMileage == null) " Current vehicle mileage is unavailable, so distance-based status cannot be calculated." else ""
        val message = if (action == "GET_MAINTENANCE_RECORD") "The latest saved maintenance record was found." else "Showing ${minOf(5, ordered.size)} of ${ordered.size} saved maintenance items. Do not infer which is closest across distance and time."
        MaintenanceVoiceQueryResult("success", message + mileageNote, data)
    } catch (_: Exception) { MaintenanceVoiceQueryResult("fail", "The saved maintenance records could not be read.") }
}
