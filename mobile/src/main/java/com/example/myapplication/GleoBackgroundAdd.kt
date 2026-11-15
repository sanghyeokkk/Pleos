package com.example.myapplication

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean

private object VoiceRequestSequence {
    private var latest = 0L
    @Synchronized fun next(): Long = ++latest
    @Synchronized fun isCurrent(id: Long): Boolean = id == latest
}

/** Handles Gleo writes without launching an Activity from the background. */
internal class GleoBackgroundAdd(
    private val context: Context,
    private val intent: Intent,
    private val finishRequest: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val finished = AtomicBoolean(false)
    private val sequence = VoiceRequestSequence.next()
    private val pendingStore = VoicePendingStore(context)
    private var vehicle: Vehicle? = null
    private var car: Car? = null
    private var gearOk: Boolean? = null
    private var uxOk: Boolean? = null
    private var started = false

    fun run() {
        handler.postDelayed({ reply("fail", "The vehicle state could not be verified in time.") }, 5500)
        try {
            val sdk = Vehicle(context)
            vehicle = sdk
            sdk.initialize()
            val gear = sdk.getGear()
            if (gear == null) { reply("fail", "The vehicle gear could not be verified."); return }
            gear.getCurrentGearSelectionState(
                onComplete = { value -> handler.post { gearOk = value == VehicleGear.GEAR_PARK; tryProcess() } },
                onFailed = { handler.post { gearOk = false; tryProcess() } }
            )
            car = Car.createCar(context, handler, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connected, ready ->
                if (finished.get()) return@createCar
                if (!ready) { uxOk = false; tryProcess(); return@createCar }
                uxOk = try {
                    val restrictions = (connected.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager).currentCarUxRestrictions
                    val flags = CarUxRestrictions.UX_RESTRICTIONS_NO_KEYBOARD or CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP or CarUxRestrictions.UX_RESTRICTIONS_NO_FILTERING
                    restrictions != null && !restrictions.isRequiresDistractionOptimization && restrictions.activeRestrictions and flags == 0
                } catch (_: Exception) { false }
                tryProcess()
            }
        } catch (_: Exception) { reply("fail", "The vehicle state could not be verified.") }
    }

    private fun tryProcess() {
        if (finished.get() || started) return
        if (gearOk == false || uxOk == false) { reply("fail", "Records can be added when the vehicle is parked and restrictions allow it."); return }
        if (gearOk != true || uxOk != true) return
        started = true
        if (!VoiceRequestSequence.isCurrent(sequence)) { reply("fail", "A newer request replaced this request."); return }
        try { process() } catch (_: Exception) { reply("fail", "The maintenance request could not be processed.") }
    }

    private fun process() {
        val raw = intent.getStringExtra("ACTION_PARAM")
        val json = runCatching {
            if (raw.isNullOrBlank()) JSONObject()
            else if (raw.trim().startsWith("\"")) JSONObject(org.json.JSONTokener(raw).nextValue() as String)
            else JSONObject(raw)
        }.getOrDefault(JSONObject())
        fun text(key: String): String? = if (json.has(key) && !json.isNull(key)) json.optString(key).takeIf { it.isNotBlank() } else intent.extras?.get(key)?.toString()?.takeIf { it.isNotBlank() }
        val item = text("item")?.trim()?.take(40)
        val dateText = text("date")
        val date = dateText?.let { parseGleoDate(it) }
        if (dateText != null && (date == null || date.isAfter(LocalDate.now()))) { reply("asking", "Please provide a valid maintenance date that is not in the future."); return }
        fun number(key: String): Int? = text(key)?.toIntOrNull()
        val mileage = number("mileageKm")
        val interval = number("intervalKm")
        val cost = number("costWon")
        if ((text("mileageKm") != null && (mileage == null || mileage !in 0..99_999_999)) ||
            (text("intervalKm") != null && (interval == null || interval !in 1..99_999_999)) ||
            (text("costWon") != null && (cost == null || cost !in 0..999_999_999))) {
            reply("asking", "Please provide valid whole-number mileage, interval, and cost values."); return
        }
        val confirmed = when {
            json.has("confirmed") && json.opt("confirmed") is Boolean -> json.getBoolean("confirmed")
            intent.hasExtra("confirmed") && intent.extras?.get("confirmed") is Boolean -> intent.getBooleanExtra("confirmed", false)
            else -> null
        }
        val existing = pendingStore.load()
        val updated = existing?.record?.let { applyVoiceDraftChanges(it, VoiceDraftChanges(item, date, mileage, interval, cost)) }
        val changed = existing != null && updated != existing.record
        when (voiceDraftAction(confirmed, existing != null, changed)) {
            VoiceDraftAction.CANCEL -> { pendingStore.clear(); reply("success", "The proposed maintenance record was cancelled without saving."); return }
            VoiceDraftAction.SAVE -> {
                val draft = existing ?: return
                val current = pendingStore.load()
                if (current != draft || !VoiceRequestSequence.isCurrent(sequence)) { reply("asking", "The pending record changed or expired. Please start again."); return }
                val store = MaintenanceStore(context)
                val records = store.load()
                if (records.none { it.id == draft.record.id }) store.save(records + draft.record)
                pendingStore.clear()
                GleoRequestBridge.notifyRecordChanged()
                reply("success", "The confirmed maintenance record was saved.", JSONArray().put(JSONObject().put("item", draft.record.item).put("date", draft.record.date.toString())))
                return
            }
            VoiceDraftAction.STAGE -> Unit
        }
        val record = updated ?: run {
            if (item.isNullOrBlank()) { reply("asking", "A maintenance item is required."); return }
            if (date == null) { reply("asking", "Please provide the maintenance date."); return }
            MaintenanceRecord(item = item, date = date, mileageKm = mileage, intervalKm = interval, intervalMonths = null, costWon = cost)
        }
        if (record.mileageKm == null && record.date == LocalDate.now()) readOdometer { value ->
            if (!VoiceRequestSequence.isCurrent(sequence)) { reply("fail", "A newer request replaced this request."); return@readOdometer }
            val selected = selectVoiceMileage(record.date, LocalDate.now(), null, value)
            if (selected == null) reply("asking", "The current vehicle mileage is unavailable. Please provide the maintenance mileage or enter the record in the app.")
            else stage(record.copy(mileageKm = selected))
        } else stage(record)
    }

    private fun stage(record: MaintenanceRecord) {
        if (finished.get()) return
        pendingStore.save(StoredVoiceDraft(record, System.currentTimeMillis()))
        val data = JSONObject().put("item", record.item).put("date", record.date.toString())
        record.mileageKm?.let { data.put("mileageKm", it) }
        record.intervalKm?.let { data.put("intervalKm", it) }
        record.costWon?.let { data.put("costWon", it) }
        val mileage = record.mileageKm?.let { " The maintenance mileage is $it kilometers." } ?: " The maintenance mileage was not provided."
        reply("asking", "Ask the user to confirm the maintenance item, date, mileage, and any interval before saving.$mileage Then call this function with confirmed set to true, or false to cancel.", JSONArray().put(data))
    }

    private fun readOdometer(done: (Double?) -> Unit) {
        val completed = AtomicBoolean(false)
        fun finish(value: Double?) { handler.post { if (completed.compareAndSet(false, true) && !finished.get()) done(value) } }
        handler.postDelayed({ finish(null) }, 1500)
        try {
            val odometer = vehicle?.getOdometer() ?: run { finish(null); return }
            odometer.checkCurrentDistanceDrivenCapabilities(onComplete = { config ->
                if (config.accessType != AccessType.READ && config.accessType != AccessType.READ_WRITE) finish(null)
                else try { odometer.getCurrentDistanceDriven(onComplete = { finish(it?.toDouble()) }, onFailed = { finish(null) }) } catch (_: Exception) { finish(null) }
            }, onFailed = { finish(null) })
        } catch (_: Exception) { finish(null) }
    }

    private fun reply(status: String, message: String, data: JSONArray? = null) {
        if (!finished.compareAndSet(false, true)) return
        try {
            val response = JSONObject().put("functionName", "ADD_MAINTENANCE_RECORD").put("status", status).put("message", message)
            data?.let { response.put("data", it.toString()) }
            context.sendBroadcast(Intent("ai.pleos.playground.intent.action.APP_ACTION_RESPONSE").apply {
                setPackage("ai.umos.ambientai")
                putExtra("packageName", context.packageName)
                putExtra("response", response.toString())
            })
        } finally {
            runCatching { car?.disconnect() }
            runCatching { vehicle?.release() }
            finishRequest()
        }
    }
}
