package com.example.myapplication

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.NavigationScreenRequest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType as SttErrorType
import ai.pleos.playground.stt.constant.Mode as SttMode
import ai.pleos.playground.stt.listener.OnInitializeListener as SttInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import ai.pleos.playground.tts.TextToSpeech as PleosTextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import android.os.Bundle
import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Intent
import android.util.Log
import org.json.JSONObject
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var vehicle: Vehicle? = null
    private var requestId = 0
    @Volatile private var odometerRequestId = 0
    private val refreshHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val refreshOdometer = object : Runnable {
        override fun run() {
            readOdometer()
            refreshHandler.postDelayed(this, 60_000L)
        }
    }
    private val reminderEnabled = mutableStateOf(false)
    private val reminderMessage = mutableStateOf("")
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && canEdit()) setReminders(true) else reminderMessage.value = "주차 상태와 알림 권한을 확인한 뒤 다시 설정해 주세요."
    }
    private val doorPosition = mutableStateOf("차량 기능을 확인하는 중입니다.")
    private val odometerKm = mutableStateOf<Double?>(null)
    private val odometerStatus = mutableStateOf("차량 주행거리를 확인하는 중입니다.")
    private val odometerLoading = mutableStateOf(true)
    private val gleoDraft = mutableStateOf<GleoDraft?>(null)
    private val recordsRevision = mutableStateOf(0)
    private val gleoRequestListener: () -> Unit = { drainGleoRequests() }
    private val gleoRecordListener: () -> Unit = { recordsRevision.value++ }
    private data class PendingVoiceRecord(val record: MaintenanceRecord, val createdAt: Long)
    private var pendingVoiceRecord: PendingVoiceRecord? = null
    private val parked = mutableStateOf(false)
    private val uxPermitsEditing = mutableStateOf(false)
    private var uxStateKnown = false
    private var car: Car? = null
    private var uxManager: CarUxRestrictionsManager? = null
    private val uxListener = CarUxRestrictionsManager.OnUxRestrictionsChangedListener { restrictions ->
        runOnUiThread { applyUxRestrictions(restrictions) }
    }
    private fun applyUxRestrictions(restrictions: CarUxRestrictions?) {
        val blockedFlags = CarUxRestrictions.UX_RESTRICTIONS_NO_KEYBOARD or CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP or CarUxRestrictions.UX_RESTRICTIONS_NO_FILTERING
        uxStateKnown = restrictions != null
        uxPermitsEditing.value = restrictions != null && !restrictions.isRequiresDistractionOptimization && restrictions.activeRestrictions and blockedFlags == 0
    }
    private fun canEdit(): Boolean = parked.value && uxPermitsEditing.value

    private fun connectUxRestrictions() {
        try {
            car = Car.createCar(this, android.os.Handler(mainLooper), Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connectedCar, ready ->
                runOnUiThread {
                    if (isDestroyed) return@runOnUiThread
                    if (!ready) { uxStateKnown = false; uxPermitsEditing.value = false; uxManager = null; return@runOnUiThread }
                    try {
                        val manager = connectedCar.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
                        uxManager = manager
                        manager.registerListener(uxListener)
                        applyUxRestrictions(manager.currentCarUxRestrictions)
                    } catch (e: Exception) { uxPermitsEditing.value = false }
                }
            }
        } catch (e: Exception) { uxPermitsEditing.value = false }
    }
    private val shops = mutableStateOf<List<RepairShop>>(emptyList())
    private val shopStatus = mutableStateOf("")
    @Volatile private var repairSearchId = 0
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) searchNearbyRepairShops() else shopStatus.value = "위치 권한이 없어 정비소를 검색할 수 없습니다."
    }
    private var gearListenerRegistered = false
    private val gearListener = object : GearSelectionListener {
        override fun onSelected(value: VehicleGear?) { runOnUiThread { if (!isDestroyed) parked.value = value == VehicleGear.GEAR_PARK } }
        override fun onFailed(error: Exception) { runOnUiThread { if (!isDestroyed) parked.value = false } }
    }
    private var naviHelper: NaviHelper? = null
    private var naviListenerRegistered = false
    private val naviStatus = mutableStateOf("내비게이션 서비스에 연결하는 중입니다.")
    private var textToSpeech: PleosTextToSpeech? = null
    private var ttsListenerRegistered = false
    private val ttsStatus = mutableStateOf("음성 합성 서비스를 준비하는 중입니다.")
    private var speechToText: SpeechToText? = null
    private var fusedLocation: FusedLocation? = null
    private val locationStatus = mutableStateOf("위치 SDK 지원 여부를 확인하는 중입니다.")
    private var sttListenerRegistered = false
    private val sttStatus = mutableStateOf("음성 인식 서비스를 준비하는 중입니다.")
    private val sttListener = object : ResultListener {
        override fun onReady() = Unit
        override fun onStartedRecognition() = Unit
        override fun onUpdated(stt: String, completed: Boolean) = Unit
        override fun onUpdatedEpdData(on: Long, off: Long) = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onEndedRecognition() = Unit
        override fun onError(errorType: SttErrorType) {
            runOnUiThread {
                if (!isDestroyed && speechToText != null) {
                    sttStatus.value = "음성 인식 오류: $errorType"
                }
            }
        }
    }
    private val ttsListener = object : EventListener {
        override fun onReady() = Unit
        override fun onStart() = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onDone() = Unit
        override fun onStop() = Unit
        override fun onError(errorType: ErrorType) {
            runOnUiThread {
                if (!isDestroyed && textToSpeech != null) {
                    ttsStatus.value = "음성 합성 오류: $errorType"
                }
            }
        }
    }
    private val naviListener = object : NaviHelperEventListener {
        override fun onError(errorCode: NaviErrorCode) {
            runOnUiThread {
                if (!isDestroyed && naviHelper != null) {
                    naviStatus.value = when (errorCode) {
                        NaviErrorCode.CAUSE_LOW_BATTERY -> "내비게이션 오류: 배터리가 부족합니다."
                        NaviErrorCode.CAUSE_SEARCH_ROUTE_OVER_DISTANCE -> "내비게이션 오류: 탐색 가능한 거리를 초과했습니다."
                        NaviErrorCode.CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE -> "내비게이션 오류: 목적지가 현재 위치와 너무 가깝습니다."
                        NaviErrorCode.CAUSE_SEARCH_ROUTE -> "내비게이션 오류: 경로 탐색 서버를 확인해 주세요."
                        else -> "내비게이션 오류: $errorCode"
                    }
                    if (shopStatus.value == "길 안내를 요청하는 중입니다.") shopStatus.value = "길 안내 요청 실패: $errorCode"
                    else if (shopStatus.value.startsWith("내비게이션에 블루핸즈 검색을 요청했습니다")) shopStatus.value = "내비게이션 검색 요청 실패: $errorCode"
                }
            }
        }
        override fun onRoutingCompleted() { runOnUiThread { if (!isDestroyed && shopStatus.value == "길 안내를 요청하는 중입니다.") shopStatus.value = "내비게이션 경로 탐색이 완료됐습니다." } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reminderEnabled.value = MaintenanceReminders.enabled(this)
        if (!runCatching { MaintenanceReminders.restoreIfNeeded(this) }.getOrDefault(false)) reminderMessage.value = "점검 알림 예약을 확인하지 못했습니다."
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MaintenanceScreen(
                        currentKm = odometerKm.value,
                        odometerStatus = odometerStatus.value,
                        odometerLoading = odometerLoading.value,
                        gleoDraft = gleoDraft.value,
                        recordsRevision = recordsRevision.value,
                        parked = canEdit(),
                        repairShops = shops.value,
                        shopStatus = shopStatus.value,
                        onSearchRepairs = ::requestRepairSearch,
                        onSearchBluehands = ::searchBluehands,
                        onNavigateRepair = ::navigateToRepair,
                        onDraftHandled = { gleoDraft.value = null },
                        onRefresh = ::readOdometer,
                        remindersEnabled = reminderEnabled.value,
                        reminderMessage = reminderMessage.value,
                        onRemindersChanged = ::requestReminders,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        readDoorPosition()
        connectUxRestrictions()
        observeGear()
        initializeNaviHelper()
        initializeTextToSpeech()
        initializeSpeechToText()
        initializeFusedLocation()
        GleoRequestBridge.attach(gleoRequestListener)
        GleoRequestBridge.attachRecordListener(gleoRecordListener)
        drainGleoRequests()
    }

    override fun onStart() {
        super.onStart()
        refreshHandler.post(refreshOdometer)
    }

    override fun onStop() {
        refreshHandler.removeCallbacks(refreshOdometer)
        super.onStop()
    }

    private fun requestReminders(enabled: Boolean) {
        if (!canEdit()) return
        if (enabled && android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else setReminders(enabled)
    }

    private fun setReminders(enabled: Boolean) {
        val success = runCatching { MaintenanceReminders.setEnabled(this, enabled) }.getOrDefault(false)
        reminderEnabled.value = MaintenanceReminders.enabled(this)
        reminderMessage.value = if (!success) "알림 설정을 저장하지 못했습니다." else if (enabled) "정비 점검 알림이 켜졌습니다." else "점검 알림이 꺼졌습니다."
    }

    private fun requestRepairSearch() {
        if (!canEdit()) { shopStatus.value = "주차 상태와 주행 제한을 확인해 주세요."; return }
        if (ContextCompat.checkSelfPermission(this, "pleos.car.permission.FUSED_LOCATION") == PackageManager.PERMISSION_GRANTED) searchNearbyRepairShops()
        else locationPermission.launch("pleos.car.permission.FUSED_LOCATION")
    }

    private fun searchBluehands() {
        if (!canEdit()) { shopStatus.value = "주차 상태와 주행 제한을 확인해 주세요."; return }
        val helper = naviHelper
        if (helper == null || !naviListenerRegistered) {
            shopStatus.value = "내비게이션 연결을 확인한 뒤 다시 시도해 주세요."
            return
        }
        if (ContextCompat.checkSelfPermission(this, "pleos.car.permission.NAVI_CUSTOM_MAP") != PackageManager.PERMISSION_GRANTED) {
            shopStatus.value = "내비게이션 검색 화면 권한이 없어 요청할 수 없습니다."
            return
        }
        try {
            helper.requestNavigationScreen(NavigationScreenRequest.SearchPlace("블루핸즈"))
            shopStatus.value = "내비게이션에 블루핸즈 검색을 요청했습니다. 검색 화면에서 지점을 선택해 주세요."
        } catch (e: Exception) {
            shopStatus.value = "내비게이션 검색 요청 실패: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    private fun searchNearbyRepairShops() {
        val location = fusedLocation
        val searchId = ++repairSearchId
        shops.value = emptyList()
        if (location == null) { shopStatus.value = "이 환경에서 위치 서비스를 사용할 수 없습니다."; return }
        shopStatus.value = "현재 위치를 확인하는 중입니다."
        android.os.Handler(mainLooper).postDelayed({
            if (!isDestroyed && repairSearchId == searchId && shopStatus.value == "현재 위치를 확인하는 중입니다.") {
                repairSearchId++
                shopStatus.value = "위치 응답 시간이 초과됐습니다. 다시 시도해 주세요."
            }
        }, 10000)
        try {
            location.getLastKnownLocation(object : FusedLocationListener {
                override fun onChanged(value: LocationData?) {
                    if (searchId != repairSearchId) return
                    if (value == null) { runOnUiThread { if (!isDestroyed && searchId == repairSearchId) shopStatus.value = "현재 위치가 없어 검색할 수 없습니다." }; return }
                    runOnUiThread { if (!isDestroyed && searchId == repairSearchId) shopStatus.value = "주변 정비소를 검색하는 중입니다." }
                    Thread {
                        val result = runCatching { searchRepairShops(value.latitude, value.longitude) }
                        runOnUiThread { if (!isDestroyed && searchId == repairSearchId) result.fold(onSuccess = {
                            shops.value = it
                            shopStatus.value = if (it.isEmpty()) "주변 5km에서 등록된 정비소를 찾지 못했습니다." else "주변 5km의 정비소 후보 ${it.size}곳"
                        }, onFailure = { shopStatus.value = "검색 실패: ${it.message ?: it.javaClass.simpleName}" }) }
                    }.start()
                }
                override fun onError(error: Exception) { runOnUiThread { if (!isDestroyed && searchId == repairSearchId) shopStatus.value = "위치 조회 실패: ${error.message ?: error.javaClass.simpleName}" } }
            })
        } catch (e: Exception) { shopStatus.value = "위치 조회 실패: ${e.message ?: e.javaClass.simpleName}" }
    }

    private fun navigateToRepair(shop: RepairShop) {
        if (!canEdit()) { shopStatus.value = "주차 상태와 주행 제한을 확인해 주세요."; return }
        val helper = naviHelper
        if (helper == null || !naviListenerRegistered) { shopStatus.value = "내비게이션 서비스가 아직 연결되지 않았습니다."; return }
        try {
            shopStatus.value = "길 안내를 요청하는 중입니다."
            helper.requestRoute(RouteInfo(latitude = shop.latitude, longitude = shop.longitude, displayLongitude = null, displayLatitude = null, poiName = shop.name, poiId = "", address = "", poiSubId = "0"))
            android.os.Handler(mainLooper).postDelayed({ if (!isDestroyed && shopStatus.value == "길 안내를 요청하는 중입니다.") shopStatus.value = "내비게이션의 응답을 확인하지 못했습니다." }, 15000)
        } catch (e: Exception) { shopStatus.value = "길 안내 요청 실패: ${e.message ?: e.javaClass.simpleName}" }
    }

    private fun observeGear() {
        try {
            val gear = vehicle?.getGear() ?: return
            gear.registerGearSelection(gearListener)
            gearListenerRegistered = true
            gear.getCurrentGearSelectionState(onComplete = { value -> runOnUiThread { if (!isDestroyed) parked.value = value == VehicleGear.GEAR_PARK } }, onFailed = { runOnUiThread { if (!isDestroyed) parked.value = false } })
        } catch (e: Exception) { parked.value = false }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        drainGleoRequests()
    }

    private fun drainGleoRequests() {
        while (true) handleGleoIntent(GleoRequestBridge.take() ?: return)
    }

    private fun handleGleoIntent(intent: Intent) {
        val action = intent.action?.removePrefix("$packageName.") ?: return
        if (action !in setOf("ADD_MAINTENANCE_RECORD", "GET_MAINTENANCE_RECORD", "GET_MAINTENANCE_DUE")) return
        val raw = intent.getStringExtra("ACTION_PARAM")
        val json = runCatching {
            if (raw.isNullOrBlank()) JSONObject()
            else if (raw.trim().startsWith("\"")) JSONObject(org.json.JSONTokener(raw).nextValue() as String)
            else JSONObject(raw)
        }.getOrDefault(JSONObject())
        val item = (json.optString("item").takeIf { it.isNotBlank() } ?: intent.getStringExtra("item").orEmpty()).trim().take(40)
        if (action != "ADD_MAINTENANCE_RECORD") {
            readVoiceOdometer { currentMileage ->
                if (!isDestroyed) {
                    val result = maintenanceVoiceQuery(this, action, item, currentMileage?.takeIf { it.isFinite() && it >= 0 })
                    sendGleoResponse(action, result.status, result.message, result.data)
                }
            }
            return
        }
        val intervalText = if (json.has("intervalKm") && !json.isNull("intervalKm")) json.optString("intervalKm") else intent.extras?.get("intervalKm")?.toString()
        val interval = intervalText?.toIntOrNull()
        if (intervalText != null && (interval == null || interval !in 1..99_999_999)) {
            sendGleoResponse(action, "asking", "Please provide a valid positive maintenance interval in kilometers.")
            return
        }
        val mileageText = if (json.has("mileageKm") && !json.isNull("mileageKm")) json.optString("mileageKm") else intent.extras?.get("mileageKm")?.toString()
        val mileage = mileageText?.toIntOrNull()
        if (mileageText != null && (mileage == null || mileage !in 0..99_999_999)) {
            sendGleoResponse(action, "asking", "Please provide a valid maintenance mileage in kilometers.")
            return
        }
        val confirmation = when {
            json.has("confirmed") && !json.isNull("confirmed") && json.opt("confirmed") is Boolean -> json.getBoolean("confirmed")
            intent.hasExtra("confirmed") && intent.extras?.get("confirmed") is Boolean -> intent.getBooleanExtra("confirmed", false)
            else -> null
        }
        val dateString = (json.optString("date").takeIf { it.isNotBlank() } ?: intent.getStringExtra("date"))
        val costText = if (json.has("costWon") && !json.isNull("costWon")) json.optString("costWon") else intent.extras?.get("costWon")?.toString()
        val date = dateString?.let { parseGleoDate(it) }
        if (dateString != null && (date == null || date.isAfter(java.time.LocalDate.now()))) {
            sendGleoResponse(action, "asking", "Please provide the maintenance date, which must not be in the future.")
            return
        }
        val cost = costText?.toIntOrNull()
        if (costText != null && (cost == null || cost !in 0..999_999_999)) {
            sendGleoResponse(action, "asking", "Please provide a valid whole-number cost in Korean won.")
            return
        }
        val existing = pendingVoiceRecord?.takeIf { android.os.SystemClock.elapsedRealtime() - it.createdAt < 120_000L }
        val changedRecord = existing?.record?.let {
            applyVoiceDraftChanges(it, VoiceDraftChanges(item = item.takeIf(String::isNotBlank), date = date, mileageKm = mileage, intervalKm = interval, costWon = cost))
        }
        val changed = existing != null && changedRecord != existing.record
        val draftAction = voiceDraftAction(confirmation, existing != null, changed)
        if (draftAction == VoiceDraftAction.CANCEL) {
            pendingVoiceRecord = null
            sendGleoResponse(action, "success", "The proposed maintenance record was cancelled without saving.")
            return
        }
        val shouldCommit = draftAction == VoiceDraftAction.SAVE
        if (!shouldCommit) {
            if (changedRecord != null) {
                pendingVoiceRecord = PendingVoiceRecord(changedRecord, android.os.SystemClock.elapsedRealtime())
            } else {
            if (item.isBlank()) { sendGleoResponse(action, "asking", "A maintenance item is required."); return }
            if (date == null) {
                sendGleoResponse(action, "asking", "Please provide the maintenance date, which must not be in the future.")
                return
            }
            pendingVoiceRecord = PendingVoiceRecord(MaintenanceRecord(item = item, date = date, mileageKm = mileage, intervalKm = interval, intervalMonths = null, costWon = cost), android.os.SystemClock.elapsedRealtime())
            }
        }
        val expectedPending = pendingVoiceRecord
        var completed = false
        fun respond(allowed: Boolean) {
            runOnUiThread {
                if (completed || isDestroyed) return@runOnUiThread
                if (allowed && !uxStateKnown) { refreshHandler.postDelayed({ respond(true) }, 100); return@runOnUiThread }
                completed = true
                if (pendingVoiceRecord !== expectedPending) {
                    sendGleoResponse(action, "fail", "The pending record changed. Please start again.")
                    return@runOnUiThread
                }
                parked.value = allowed
                if (!allowed || !uxPermitsEditing.value) {
                    pendingVoiceRecord = null
                    sendGleoResponse(action, "fail", "Records can be added when the vehicle is parked and restrictions allow it.")
                } else if (shouldCommit) {
                    val pending = pendingVoiceRecord
                    pendingVoiceRecord = null
                    if (pending == null || android.os.SystemClock.elapsedRealtime() - pending.createdAt >= 120_000L) {
                        sendGleoResponse(action, "fail", "The confirmation expired. Please request the record again.")
                    } else try {
                        val store = MaintenanceStore(this)
                        store.save(store.load() + pending.record)
                        recordsRevision.value++
                        sendGleoResponse(action, "success", "The confirmed maintenance record was saved.", org.json.JSONArray().put(JSONObject().put("item", pending.record.item).put("date", pending.record.date.toString())))
                    } catch (e: Exception) { sendGleoResponse(action, "fail", "The maintenance record could not be saved.") }
                } else {
                    val record = pendingVoiceRecord!!.record
                    fun askForConfirmation(updated: MaintenanceRecord) {
                        val data = JSONObject().put("item", updated.item).put("date", updated.date.toString())
                        updated.costWon?.let { data.put("costWon", it) }
                        updated.intervalKm?.let { data.put("intervalKm", it) }
                        updated.mileageKm?.let { data.put("mileageKm", it) }
                        val mileageMessage = updated.mileageKm?.let { " The maintenance mileage is $it kilometers." } ?: " The maintenance mileage was not provided."
                        sendGleoResponse(action, "asking", "Ask the user to confirm the maintenance item, date, mileage, and any interval before saving.$mileageMessage Then call this function with confirmed set to true, or false to cancel.", org.json.JSONArray().put(data))
                    }
                    if (record.mileageKm == null && record.date == java.time.LocalDate.now()) {
                        readVoiceOdometer { currentMileage ->
                            if (pendingVoiceRecord !== expectedPending || isDestroyed) return@readVoiceOdometer
                            val mileage = selectVoiceMileage(record.date, java.time.LocalDate.now(), record.mileageKm, currentMileage)
                            if (mileage == null) {
                                pendingVoiceRecord = null
                                sendGleoResponse(action, "asking", "The current vehicle mileage is unavailable. Please provide the maintenance mileage or enter the record in the app.")
                            } else {
                                val updated = record.copy(mileageKm = mileage)
                                pendingVoiceRecord = PendingVoiceRecord(updated, expectedPending!!.createdAt)
                                askForConfirmation(updated)
                            }
                        }
                    } else askForConfirmation(record)
                }
            }
        }
        android.os.Handler(mainLooper).postDelayed({ respond(false) }, 5000)
        try {
            val gear = vehicle?.getGear()
            if (gear == null) respond(false)
            else gear.getCurrentGearSelectionState(onComplete = { respond(it == VehicleGear.GEAR_PARK) }, onFailed = { respond(false) })
        } catch (e: Exception) { respond(false) }
    }

    private fun readVoiceOdometer(onResult: (Double?) -> Unit) {
        var finished = false
        val handler = android.os.Handler(mainLooper)
        fun finish(value: Double?) {
            runOnUiThread {
                if (!finished) {
                    finished = true
                    onResult(value)
                }
            }
        }
        handler.postDelayed({ finish(null) }, 1200)
        try {
            val odometer = vehicle?.getOdometer()
            if (odometer == null) { finish(null); return }
            odometer.checkCurrentDistanceDrivenCapabilities(
                onComplete = { config ->
                    if (config.accessType != AccessType.READ && config.accessType != AccessType.READ_WRITE) finish(null)
                    else try {
                        odometer.getCurrentDistanceDriven(onComplete = { finish(it?.toDouble()) }, onFailed = { finish(null) })
                    } catch (e: Exception) { finish(null) }
                },
                onFailed = { finish(null) }
            )
        } catch (e: Exception) { finish(null) }
    }

    private fun sendGleoResponse(action: String, status: String, message: String, data: org.json.JSONArray? = null) {
        val response = JSONObject().put("functionName", action).put("status", status).put("message", message)
        if (data != null) response.put("data", data.toString())
        sendBroadcast(Intent("ai.pleos.playground.intent.action.APP_ACTION_RESPONSE").apply {
            setPackage("ai.umos.ambientai")
            putExtra("packageName", packageName)
            putExtra("response", response.toString())
        })
    }

    private fun readOdometer() {
        val currentOdometerRequest = ++odometerRequestId
        odometerLoading.value = true
        odometerStatus.value = "차량 주행거리를 확인하는 중입니다."
        try {
            val sdk = vehicle ?: Vehicle(this).also { it.initialize(); vehicle = it }
            sdk.getOdometer().checkCurrentDistanceDrivenCapabilities(
                onComplete = { config ->
                    if (currentOdometerRequest != odometerRequestId || isDestroyed) return@checkCurrentDistanceDrivenCapabilities
                    if (config.accessType != AccessType.READ && config.accessType != AccessType.READ_WRITE) {
                        runOnUiThread { if (currentOdometerRequest == odometerRequestId && !isDestroyed) { odometerKm.value = null; odometerLoading.value = false; odometerStatus.value = "이 차량에서 주행거리 조회를 지원하지 않습니다." } }
                    } else {
                        sdk.getOdometer().getCurrentDistanceDriven(
                            onComplete = { value -> runOnUiThread {
                                if (!isDestroyed && vehicle === sdk && currentOdometerRequest == odometerRequestId) {
                                    odometerKm.value = value?.toDouble()?.takeIf { it.isFinite() && it >= 0 }
                                    odometerLoading.value = false
                                    odometerStatus.value = if (odometerKm.value == null) "주행거리 값을 받지 못했습니다." else "차량 주행거리 조회 완료"
                                }
                            } },
                            onFailed = { error -> runOnUiThread { if (!isDestroyed && currentOdometerRequest == odometerRequestId) { odometerKm.value = null; odometerLoading.value = false; odometerStatus.value = "주행거리 조회 실패: ${error.message ?: error.javaClass.simpleName}" } } }
                        )
                    }
                },
                onFailed = { error -> runOnUiThread { if (!isDestroyed && currentOdometerRequest == odometerRequestId) { odometerKm.value = null; odometerLoading.value = false; odometerStatus.value = "주행거리 지원 확인 실패: ${error.message ?: error.javaClass.simpleName}" } } }
            )
        } catch (error: Exception) {
            odometerKm.value = null
            odometerLoading.value = false
            odometerStatus.value = "주행거리 조회 실패: ${error.message ?: error.javaClass.simpleName}"
        }
    }

    private fun initializeFusedLocation() {
        try {
            val location = FusedLocation(applicationContext)
            if (!location.isSupported()) {
                locationStatus.value = "이 환경은 위치 SDK를 지원하지 않습니다."
                return
            }
            fusedLocation = location
            location.initialize()
            locationStatus.value = "위치 SDK 초기화 호출 완료"
        } catch (error: Exception) {
            locationStatus.value = "위치 SDK 초기화 실패: ${error.message ?: error.javaClass.simpleName}"
        }
    }

    private fun initializeSpeechToText() {
        try {
            val stt = SpeechToText(applicationContext, SttMode.ON_DEVICE)
            speechToText = stt
            stt.initialize(object : SttInitializeListener {
                override fun onSuccess() {
                    runOnUiThread {
                        if (!isDestroyed && speechToText === stt) {
                            try {
                                stt.addListener(sttListener)
                                sttListenerRegistered = true
                                sttStatus.value = "음성 인식 서비스 초기화 성공 (기기 내 모드)"
                            } catch (error: Exception) {
                                sttStatus.value = "음성 인식 리스너 등록 실패: ${error.message ?: error.javaClass.simpleName}"
                            }
                        }
                    }
                }

                override fun onFailure(errorType: SttErrorType) {
                    runOnUiThread {
                        if (!isDestroyed && speechToText === stt) {
                            sttStatus.value = "음성 인식 초기화 실패: $errorType"
                        }
                    }
                }
            })
        } catch (error: Exception) {
            sttStatus.value = "음성 인식 초기화 실패: ${error.message ?: error.javaClass.simpleName}"
        }
    }

    private fun initializeTextToSpeech() {
        try {
            val tts = PleosTextToSpeech(applicationContext, Mode.ON_DEVICE)
            textToSpeech = tts
            tts.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    runOnUiThread {
                        if (!isDestroyed && textToSpeech === tts) {
                            try {
                                tts.addEventListener(ttsListener)
                                ttsListenerRegistered = true
                                ttsStatus.value = "음성 합성 서비스 초기화 성공 (기기 내 모드)"
                            } catch (error: Exception) {
                                ttsStatus.value = "음성 합성 리스너 등록 실패: ${error.message ?: error.javaClass.simpleName}"
                            }
                        }
                    }
                }

                override fun onFailure(errorType: ErrorType) {
                    runOnUiThread {
                        if (!isDestroyed && textToSpeech === tts) {
                            ttsStatus.value = "음성 합성 초기화 실패: $errorType"
                        }
                    }
                }
            })
        } catch (error: Exception) {
            ttsStatus.value = "음성 합성 초기화 실패: ${error.message ?: error.javaClass.simpleName}"
        }
    }

    private fun initializeNaviHelper() {
        try {
            val helper = NaviHelper(applicationContext)
            naviHelper = helper
            helper.initialize(
                onConnected = {
                    runOnUiThread {
                        if (!isDestroyed && naviHelper === helper) {
                            try {
                                helper.addListener(naviListener)
                                naviListenerRegistered = true
                                naviStatus.value = "내비게이션 초기 연결 성공"
                            } catch (error: Exception) {
                                naviStatus.value = "내비게이션 리스너 등록 실패: ${error.message ?: error.javaClass.simpleName}"
                            }
                        }
                    }
                },
                onFailed = { error ->
                    runOnUiThread {
                        if (!isDestroyed && naviHelper === helper) {
                            naviStatus.value = "내비게이션 초기 연결 실패: ${error.message ?: error.javaClass.simpleName}"
                        }
                    }
                }
            )
        } catch (error: Exception) {
            naviStatus.value = "내비게이션 초기화 실패: ${error.message ?: error.javaClass.simpleName}"
        }
    }

    private fun readDoorPosition() {
        val currentRequest = ++requestId
        doorPosition.value = "차량 기능을 확인하는 중입니다."
        try {
            val vehicleSdk = vehicle ?: Vehicle(this).also { sdk ->
                try {
                    sdk.initialize()
                    vehicle = sdk
                } catch (error: Exception) {
                    runCatching { sdk.release() }
                    throw error
                }
            }
            val area = DoorArea(DoorValue.ROW_1_LEFT)
            val door = vehicleSdk.getDoor()
            door.checkDoorPositionCapabilities(
                onComplete = { config, areaMap ->
                    runOnUiThread {
                        if (!isCurrentRequest(currentRequest, vehicleSdk)) return@runOnUiThread
                        when {
                            config.accessType != AccessType.READ &&
                                config.accessType != AccessType.READ_WRITE -> {
                                doorPosition.value = "현재 도어 위치 읽기가 허용되지 않습니다."
                            }
                            areaMap[area] == null -> {
                                doorPosition.value = "왼쪽 앞문 영역의 지원 범위를 확인할 수 없습니다."
                            }
                            else -> {
                                doorPosition.value = "도어 위치를 읽는 중입니다."
                                try {
                                    door.getCurrentDoorPosition(
                                        area = area,
                                        onComplete = { position ->
                                            runOnUiThread {
                                                if (isCurrentRequest(currentRequest, vehicleSdk)) {
                                                    doorPosition.value = position?.let { "왼쪽 앞문 위치: $it" }
                                                        ?: "도어 위치 값을 받지 못했습니다."
                                                }
                                            }
                                        },
                                        onFailed = { error -> showError(currentRequest, vehicleSdk, "도어 위치 조회", error) }
                                    )
                                } catch (error: Exception) {
                                    showError(currentRequest, vehicleSdk, "도어 위치 조회", error)
                                }
                            }
                        }
                    }
                },
                onFailed = { error -> showError(currentRequest, vehicleSdk, "차량 기능 확인", error) }
            )
        } catch (error: Exception) {
            doorPosition.value = errorMessage("차량 서비스 초기화 또는 기능 확인", error)
        }
    }

    private fun isCurrentRequest(id: Int, sdk: Vehicle): Boolean =
        !isDestroyed && requestId == id && vehicle === sdk

    private fun showError(id: Int, sdk: Vehicle, stage: String, error: Exception) {
        runOnUiThread {
            if (isCurrentRequest(id, sdk)) doorPosition.value = errorMessage(stage, error)
        }
    }

    private fun errorMessage(stage: String, error: Exception): String =
        "$stage 실패: ${error.message ?: error.javaClass.simpleName}"

    override fun onDestroy() {
        GleoRequestBridge.detach(gleoRequestListener)
        GleoRequestBridge.detachRecordListener(gleoRecordListener)
        requestId++
        odometerRequestId++
        runCatching { uxManager?.unregisterListener() }
        uxManager = null
        runCatching { car?.disconnect() }
        car = null
        val vehicleSdk = vehicle
        vehicle = null
        if (gearListenerRegistered) runCatching { vehicleSdk?.getGear()?.unregisterGearSelection(gearListener) }
        val helper = naviHelper
        naviHelper = null
        val tts = textToSpeech
        textToSpeech = null
        val stt = speechToText
        speechToText = null
        val location = fusedLocation
        fusedLocation = null
        try {
            try {
                location?.release()
            } catch (error: Exception) {
                Log.w("MainActivity", "FusedLocation release failed", error)
            }
            try {
                if (sttListenerRegistered) stt?.removeListener(sttListener)
            } catch (error: Exception) {
                Log.w("MainActivity", "SpeechToText listener removal failed", error)
            } finally {
                sttListenerRegistered = false
                try {
                    stt?.release()
                } catch (error: Exception) {
                    Log.w("MainActivity", "SpeechToText release failed", error)
                }
            }
            try {
                if (ttsListenerRegistered) tts?.removeEventListener(ttsListener)
            } catch (error: Exception) {
                Log.w("MainActivity", "TextToSpeech listener removal failed", error)
            } finally {
                ttsListenerRegistered = false
                try {
                    tts?.release()
                } catch (error: Exception) {
                    Log.w("MainActivity", "TextToSpeech release failed", error)
                }
            }
            try {
                if (naviListenerRegistered) helper?.removeListener(naviListener)
            } catch (error: Exception) {
                Log.w("MainActivity", "NaviHelper listener removal failed", error)
            } finally {
                naviListenerRegistered = false
                try {
                    helper?.release()
                } catch (error: Exception) {
                    Log.w("MainActivity", "NaviHelper release failed", error)
                }
            }
            vehicleSdk?.release()
        } finally {
            super.onDestroy()
        }
    }
}

@Composable
fun DoorPositionScreen(status: String, naviStatus: String, ttsStatus: String, sttStatus: String, locationStatus: String, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = status)
        Button(onClick = onRefresh) { Text("다시 조회") }
        Text(text = naviStatus)
        Text(text = ttsStatus)
        Text(text = sttStatus)
        Text(text = locationStatus)
    }
}

@Preview(showBackground = true)
@Composable
fun DoorPositionPreview() {
    MyApplicationTheme {
        DoorPositionScreen(status = "왼쪽 앞문 위치: 0", naviStatus = "내비게이션 초기 연결 성공", ttsStatus = "음성 합성 서비스 초기화 성공 (기기 내 모드)", sttStatus = "음성 인식 서비스 초기화 성공 (기기 내 모드)", locationStatus = "위치 SDK 초기화 호출 완료", onRefresh = {})
    }
}
