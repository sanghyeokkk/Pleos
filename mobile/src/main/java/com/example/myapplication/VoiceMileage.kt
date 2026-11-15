package com.example.myapplication

import java.time.LocalDate

internal fun selectVoiceMileage(
    maintenanceDate: LocalDate,
    today: LocalDate,
    providedMileage: Int?,
    currentVehicleMileage: Double?
): Int? {
    if (providedMileage != null) return providedMileage
    if (maintenanceDate != today) return null
    return currentVehicleMileage
        ?.takeIf { it.isFinite() && it >= 0 && it <= 99_999_999 }
        ?.toInt()
}
