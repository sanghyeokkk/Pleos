package com.example.myapplication

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class OdometerFallbackTest {
    @get:Rule val compose = createComposeRule()

    @Test fun offersManualEntryOnlyAfterFailureAndReturnsToVehicleValueOnRecovery() {
        val current = mutableStateOf<Double?>(1234.0)
        val loading = mutableStateOf(false)
        compose.setContent {
            MaintenanceScreen(
                currentKm = current.value, odometerStatus = "조회 상태", odometerLoading = loading.value,
                gleoDraft = null, recordsRevision = 0, parked = true, repairShops = emptyList(), shopStatus = "",
                onSearchRepairs = {}, onSearchBluehands = {}, onNavigateRepair = {}, onDraftHandled = {},
                onRefresh = {}, remindersEnabled = false, reminderMessage = "", onRemindersChanged = {}
            )
        }
        compose.onNodeWithText("직접 입력").assertDoesNotExist()
        compose.onNodeWithText("현재 거리 직접 입력 (km)").assertDoesNotExist()
        compose.runOnIdle { current.value = null; loading.value = true }
        compose.onNodeWithText("직접 입력").assertDoesNotExist()
        compose.runOnIdle { loading.value = false }
        compose.onNodeWithText("직접 입력").performClick()
        compose.onNodeWithText("현재 거리 직접 입력 (km)").performTextInput("1500")
        compose.onNodeWithText("직접 입력한 임시 값 · 알림에는 사용되지 않습니다").assertIsDisplayed()
        compose.runOnIdle { current.value = 2345.0 }
        compose.onNodeWithText("현재 거리 직접 입력 (km)").assertDoesNotExist()
        compose.onNodeWithText("직접 입력한 임시 값 · 알림에는 사용되지 않습니다").assertDoesNotExist()
    }
}
