package com.example.myapplication

import java.time.LocalDate

internal data class VoiceDraftChanges(
    val item: String? = null,
    val date: LocalDate? = null,
    val mileageKm: Int? = null,
    val intervalKm: Int? = null,
    val costWon: Int? = null
)

internal enum class VoiceDraftAction { STAGE, SAVE, CANCEL }

internal fun voiceDraftAction(confirmed: Boolean?, hasPending: Boolean, changed: Boolean): VoiceDraftAction = when {
    hasPending && !changed && confirmed == true -> VoiceDraftAction.SAVE
    hasPending && !changed && confirmed == false -> VoiceDraftAction.CANCEL
    else -> VoiceDraftAction.STAGE
}

internal fun applyVoiceDraftChanges(record: MaintenanceRecord, changes: VoiceDraftChanges): MaintenanceRecord {
    val date = changes.date ?: record.date
    return record.copy(
        item = changes.item ?: record.item,
        date = date,
        mileageKm = changes.mileageKm ?: if (date != record.date) null else record.mileageKm,
        intervalKm = changes.intervalKm ?: record.intervalKm,
        costWon = changes.costWon ?: record.costWon
    )
}

internal fun normalizeMaintenanceItem(value: String): String =
    value.lowercase(java.util.Locale.ROOT).filter { it.isLetterOrDigit() }
