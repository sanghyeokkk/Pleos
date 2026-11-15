package com.example.myapplication

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class VoiceDraftTest {
    private val record = MaintenanceRecord(item = "엔진오일", date = LocalDate.of(2026, 11, 15), mileageKm = 6433, intervalKm = 10000, intervalMonths = null, costWon = null)

    @Test fun correctionPreservesOtherFieldsAndDoesNotMutateOriginal() {
        val corrected = applyVoiceDraftChanges(record, VoiceDraftChanges(intervalKm = 5000))
        assertEquals(5000, corrected.intervalKm)
        assertEquals(6433, corrected.mileageKm)
        assertEquals(10000, record.intervalKm)
        assertEquals(record.id, corrected.id)
    }

    @Test fun changedDetailsAlwaysRequireNewConfirmationBeforeSaving() {
        assertEquals(VoiceDraftAction.STAGE, voiceDraftAction(true, false, false))
        assertEquals(VoiceDraftAction.STAGE, voiceDraftAction(true, true, true))
        assertEquals(VoiceDraftAction.STAGE, voiceDraftAction(false, true, true))
        assertEquals(VoiceDraftAction.SAVE, voiceDraftAction(true, true, false))
        assertEquals(VoiceDraftAction.CANCEL, voiceDraftAction(false, true, false))
    }

    @Test fun changingDateClearsOldMileageUnlessNewMileageIsProvided() {
        val yesterday = record.date.minusDays(1)
        assertNull(applyVoiceDraftChanges(record, VoiceDraftChanges(date = yesterday)).mileageKm)
        assertEquals(4000, applyVoiceDraftChanges(record, VoiceDraftChanges(date = yesterday, mileageKm = 4000)).mileageKm)
    }

    @Test fun matchesSpaceAndPunctuationVariationsWithoutFuzzyMatches() {
        assertEquals(normalizeMaintenanceItem("엔진 오일"), normalizeMaintenanceItem("엔진오일"))
        assertNotEquals(normalizeMaintenanceItem("엔진오일"), normalizeMaintenanceItem("브레이크 오일"))
    }
}
