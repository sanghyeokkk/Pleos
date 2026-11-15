package com.example.myapplication

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MaintenanceFlowTest {
    @get:Rule val compose = createComposeRule()
    private val base = ApplicationProvider.getApplicationContext<Context>()
    private val prefsName = "maintenance_flow_test_${UUID.randomUUID()}"
    private val isolated = object : ContextWrapper(base) {
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = base.getSharedPreferences(prefsName, mode)
    }
    @After fun cleanup() { base.deleteSharedPreferences(prefsName) }

    @Test fun recordDueAndBluehandsAction() {
        val current = mutableStateOf<Double?>(21425.0)
        var navigated = false
        compose.setContent {
            CompositionLocalProvider(LocalContext provides isolated) {
                MaintenanceScreen(currentKm = current.value, odometerStatus = "차량 주행거리 조회 완료", odometerLoading = false,
                    gleoDraft = null, recordsRevision = 0, parked = true, repairShops = emptyList(), shopStatus = "",
                    onSearchRepairs = {}, onSearchBluehands = { navigated = true }, onNavigateRepair = {},
                    onDraftHandled = {}, onRefresh = {}, remindersEnabled = false, reminderMessage = "", onRemindersChanged = {})
            }
        }
        compose.onNodeWithText("+ 정비 기록").performClick()
        compose.onNodeWithText("와이퍼").performClick()
        compose.onNodeWithText("오늘 차량 값 사용").performClick()
        compose.onNodeWithText("점검 거리 주기 (km)").performTextInput("1000")
        compose.onNodeWithText("기록 저장").performClick()
        compose.onNodeWithText("거리 기준 1,000km 남음").assertIsDisplayed()
        compose.runOnIdle {
            val record = MaintenanceStore(isolated).load().single()
            assertEquals("와이퍼", record.item)
            assertEquals(21425, record.mileageKm)
            assertEquals(1000, record.intervalKm)
            current.value = 22425.0
        }
        compose.onNodeWithText("거리 기준 점검 시기 지남").assertIsDisplayed()
        compose.onNodeWithText("블루핸즈 찾기").performClick()
        compose.runOnIdle { assertTrue(navigated) }
        compose.onNodeWithText("정비 기록 보기").performClick()
        compose.onNodeWithText("수정").performClick()
        compose.onNodeWithText("점검 거리 주기 (km)").performTextClearance()
        compose.onNodeWithText("점검 거리 주기 (km)").performTextInput("2000")
        compose.onNodeWithText("비용 (원)").performTextInput("30000")
        compose.onNodeWithText("기록 저장").performClick()
        compose.runOnIdle {
            val record = MaintenanceStore(isolated).load().single()
            assertEquals(2000, record.intervalKm)
            assertEquals(30000, record.costWon)
        }
        compose.onNodeWithText("거리 기준 1,000km 남음").assertIsDisplayed()
        compose.onNodeWithText("삭제").performClick()
        compose.onAllNodesWithText("삭제").onLast().performClick()
        compose.runOnIdle { assertTrue(MaintenanceStore(isolated).load().isEmpty()) }
    }
}
