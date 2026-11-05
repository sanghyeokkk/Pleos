package com.example.myapplication

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

data class MaintenanceRecord(
    val id: String = UUID.randomUUID().toString(),
    val item: String,
    val date: LocalDate,
    val mileageKm: Int?,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val costWon: Int?
)

data class MaintenanceDue(val distanceRemaining: Int?, val dueDate: LocalDate?)

fun dateDueCount(records: List<MaintenanceRecord>, today: LocalDate = LocalDate.now()): Int {
    return dueCount(records, null, today)
}

fun dueCount(records: List<MaintenanceRecord>, currentKm: Double?, today: LocalDate = LocalDate.now()): Int {
    val latest = records.groupBy { it.item.trim().lowercase() }.values.map { group ->
        group.maxWith(compareBy<MaintenanceRecord> { it.date }.thenBy { records.indexOf(it) })
    }
    return latest.count { record ->
        val due = maintenanceDue(record, currentKm)
        (due.dueDate?.let { !it.isAfter(today) } == true) || (due.distanceRemaining?.let { it <= 0 } == true)
    }
}

fun maintenanceDue(record: MaintenanceRecord, currentKm: Double?): MaintenanceDue {
    val remaining = if (record.mileageKm != null && record.intervalKm != null && currentKm != null && currentKm >= record.mileageKm)
        kotlin.math.ceil(record.mileageKm.toLong() + record.intervalKm - currentKm).toInt() else null
    return MaintenanceDue(remaining, record.intervalMonths?.let { record.date.plusMonths(it.toLong()) })
}

fun dueLabel(record: MaintenanceRecord, currentKm: Double?, today: LocalDate = LocalDate.now()): String {
    val due = maintenanceDue(record, currentKm)
    val distance = due.distanceRemaining?.let { if (it <= 0) "거리 기준 점검 시기 지남" else "거리 기준 ${"%,d".format(it)}km 남음" }
    val date = due.dueDate?.let { val days = ChronoUnit.DAYS.between(today, it); if (days <= 0) "날짜 기준 점검 시기 지남 ($it)" else "날짜 기준 ${days}일 남음 ($it)" }
    val warning = if (record.mileageKm != null && currentKm != null && currentKm < record.mileageKm) "현재 거리가 기록 당시 거리보다 작습니다. 입력값을 확인해 주세요." else null
    return listOfNotNull(warning, distance, date).joinToString(" · ").ifEmpty { if (record.intervalKm != null && currentKm == null) "현재 주행거리를 확인해 주세요" else "점검 기준을 설정해 주세요" }
}

class MaintenanceStore(context: Context) {
    private val prefs = context.getSharedPreferences("maintenance_records", Context.MODE_PRIVATE)
    fun load(): List<MaintenanceRecord> {
        val json = prefs.getString("records", "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { index ->
                val o = array.getJSONObject(index)
                fun optionalInt(key: String): Int? = if (o.isNull(key) || !o.has(key)) null else o.getInt(key)
                MaintenanceRecord(o.getString("id"), o.getString("item"), LocalDate.parse(o.getString("date")), optionalInt("mileageKm"), optionalInt("intervalKm"), optionalInt("intervalMonths"), optionalInt("costWon"))
            }
        } catch (e: Exception) { throw IllegalStateException("저장된 기록을 읽지 못했습니다. 기존 기록은 보존됩니다.", e) }
    }
    fun save(records: List<MaintenanceRecord>) {
        val array = JSONArray()
        records.forEach { r -> array.put(JSONObject().apply {
            put("id", r.id); put("item", r.item); put("date", r.date.toString())
            put("mileageKm", r.mileageKm ?: JSONObject.NULL); put("intervalKm", r.intervalKm ?: JSONObject.NULL)
            put("intervalMonths", r.intervalMonths ?: JSONObject.NULL); put("costWon", r.costWon ?: JSONObject.NULL)
        }) }
        check(prefs.edit().putString("records", array.toString()).commit()) { "기록을 저장하지 못했습니다." }
    }
}
