package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate

private val Ink = Color(0xFF182D32)
private val Teal = Color(0xFF006D66)
private val Canvas = Color(0xFFF3F6F3)
data class GleoDraft(val item: String, val date: String? = null, val costWon: String? = null, val intervalKm: String? = null, val id: String = java.util.UUID.randomUUID().toString())

@Composable
fun MaintenanceScreen(currentKm: Double?, odometerStatus: String, odometerLoading: Boolean, gleoDraft: GleoDraft?, recordsRevision: Int, parked: Boolean, repairShops: List<RepairShop>, shopStatus: String, onSearchRepairs: () -> Unit, onSearchBluehands: () -> Unit, onNavigateRepair: (RepairShop) -> Unit, onDraftHandled: () -> Unit, onRefresh: () -> Unit, remindersEnabled: Boolean, reminderMessage: String, onRemindersChanged: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { MaintenanceStore(context) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var records by remember { mutableStateOf(try { store.load() } catch (e: Exception) { loadFailed = true; error = e.message; emptyList() }) }
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MaintenanceRecord?>(null) }
    var deleting by remember { mutableStateOf<MaintenanceRecord?>(null) }
    var history by remember { mutableStateOf(false) }
    var showRepairSearch by remember { mutableStateOf(false) }
    var draftItem by remember { mutableStateOf("") }
    var draftDate by remember { mutableStateOf<String?>(null) }
    var draftCost by remember { mutableStateOf<String?>(null) }
    var draftInterval by remember { mutableStateOf<String?>(null) }
    var currentInput by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }
    val manualKm = currentInput.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 && it <= 10_000_000 }
    val effectiveKm = currentKm ?: manualKm
    LaunchedEffect(recordsRevision) {
        if (recordsRevision > 0) try { records = store.load(); loadFailed = false } catch (e: Exception) { loadFailed = true; error = e.message }
    }
    LaunchedEffect(currentKm) { if (currentKm != null) { currentInput = ""; showManualInput = false } }
    val latest = records.groupBy { normalizeMaintenanceItem(it.item) }.values.map { group -> group.maxWith(compareBy<MaintenanceRecord> { it.date }.thenBy { records.indexOf(it) }) }
        .sortedWith(compareBy<MaintenanceRecord> { record ->
            val due = maintenanceDue(record, effectiveKm)
            if ((due.distanceRemaining != null && due.distanceRemaining <= 0) || (due.dueDate != null && due.dueDate <= LocalDate.now())) 0 else 1
        }.thenBy { it.item })
    val dueCount = latest.count { record ->
        val due = maintenanceDue(record, effectiveKm)
        (due.distanceRemaining != null && due.distanceRemaining <= 0) || (due.dueDate != null && due.dueDate <= LocalDate.now())
    }
    LaunchedEffect(gleoDraft?.id) {
        if (gleoDraft != null) {
            if (!loadFailed && parked) { editing = null; draftItem = gleoDraft.item; draftDate = gleoDraft.date; draftCost = gleoDraft.costWon; draftInterval = gleoDraft.intervalKm; showForm = true }
            onDraftHandled()
        }
    }
    LaunchedEffect(parked) { if (!parked) { showForm = false; deleting = null; showRepairSearch = false } }

    Column(modifier.fillMaxSize().background(Canvas).padding(horizontal = 28.dp, vertical = 18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("CAR CARE", color = Teal, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("내 차 관리", color = Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("기록에서 다음 점검까지, 한눈에", color = Color(0xFF607276)) }
            Row {
                OutlinedButton(enabled = parked, onClick = { showRepairSearch = true }) { Text("주변 정비소") }
                Spacer(Modifier.width(8.dp))
                Button(enabled = !loadFailed && parked, onClick = { editing = null; draftItem = ""; draftDate = null; draftCost = null; draftInterval = null; showForm = true }, colors = ButtonDefaults.buttonColors(containerColor = Teal)) { Text("+ 정비 기록") }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("현재 주행거리", color = Color(0xFF607276), style = MaterialTheme.typography.labelLarge)
                    Text(effectiveKm?.let { "%,.0f km".format(it) } ?: "확인 필요", color = Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(if (currentKm == null && manualKm != null) "직접 입력한 임시 값 · 알림에는 사용되지 않습니다" else odometerStatus, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (currentKm == null && !odometerLoading && !showManualInput) {
                            TextButton(enabled = parked, onClick = { showManualInput = true }) { Text("직접 입력") }
                        }
                        TextButton(onClick = onRefresh) { Text("차량 값 새로고침") }
                    }
                    if (currentKm == null && showManualInput) {
                        OutlinedTextField(value = currentInput, onValueChange = { currentInput = it.filter(Char::isDigit).take(8) }, enabled = parked, label = { Text("현재 거리 직접 입력 (km)") }, singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        if (currentInput.isNotEmpty() && manualKm == null) Text("0부터 10,000,000 km까지 입력해 주세요.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFDCEEE9)), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("관리 현황", color = Teal, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("${latest.size}개 항목 · ${records.size}건 기록", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (dueCount > 0) Text("설정한 점검 시기가 된 항목 ${dueCount}개", color = Color(0xFF9B4C20), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("점검 기준은 차량 취급설명서와 정비 이력을 확인해 직접 설정해 주세요.", color = Ink, style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("정비 점검 알림", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Switch(checked = remindersEnabled, enabled = parked, onCheckedChange = onRemindersChanged)
                    }
                    if (reminderMessage.isNotBlank()) Text(reminderMessage, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        if (!parked) Text("기록 입력은 주차 상태와 차량 주행 제한이 확인되면 이용할 수 있습니다.", color = Color(0xFF607276), style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text(if (history) "정비 이력" else "다가오는 관리", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(if (history) "저장한 기록을 확인하고 수정할 수 있습니다." else "각 항목의 가장 최근 기록을 기준으로 계산합니다.", color = Color(0xFF607276), style = MaterialTheme.typography.bodySmall) }
            TextButton(onClick = { history = !history }) { Text(if (history) "점검 현황 보기" else "전체 기록 보기") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (latest.isEmpty()) {
            Card(Modifier.fillMaxWidth().padding(top = 12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(24.dp)) { Text("아직 정비 기록이 없습니다", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("정비 기록을 추가하면 다음 점검 시기를 계산해 드립니다.") }
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp)) {
                items(if (history) records.reversed() else latest, key = { it.id }) { record ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(record.item, color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(dueLabel(record, effectiveKm), color = Teal, style = MaterialTheme.typography.bodyMedium)
                            Text("최근 정비 ${record.date}" + (record.mileageKm?.let { " · %,d km".format(it) } ?: " · 주행거리 미입력") + (record.costWon?.let { " · %,d원".format(it) } ?: ""), color = Color(0xFF607276), style = MaterialTheme.typography.bodySmall)
                            if (!history) Row {
                                TextButton(onClick = { history = true }) { Text("정비 기록 보기") }
                                TextButton(enabled = parked, onClick = { editing = record; showForm = true }) { Text("기준 수정") }
                                TextButton(enabled = parked, onClick = onSearchBluehands) { Text("블루핸즈 찾기") }
                            }
                            if (history) Row { TextButton(enabled = parked, onClick = { editing = record; showForm = true }) { Text("수정") }; TextButton(enabled = parked, onClick = { deleting = record }) { Text("삭제") } }
                        }
                    }
                }
            }
        }
    }
    if (showForm) RecordDialog(currentKm = currentKm, initial = editing, draftItem = draftItem, draftDate = draftDate, draftCost = draftCost, draftInterval = draftInterval, onDismiss = { showForm = false }, onSave = { record ->
        try { val updated = if (editing == null) records + record else records.map { if (it.id == record.id) record else it }; store.save(updated); records = updated; error = null; showForm = false }
        catch (e: Exception) { error = e.message ?: "기록을 저장하지 못했습니다."; showForm = false }
    })
    deleting?.let { record -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("정비 기록 삭제") }, text = { Text("${record.date} ${record.item} 기록을 삭제할까요?") }, confirmButton = { TextButton(onClick = {
        try { val updated = records.filterNot { it.id == record.id }; store.save(updated); records = updated; error = null } catch (e: Exception) { error = e.message }
        deleting = null
    }) { Text("삭제") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("취소") } }) }
    if (showRepairSearch) Dialog(onDismissRequest = { showRepairSearch = false }) {
        Surface(shape = RoundedCornerShape(22.dp), color = Color.White, modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth()) {
            Column(Modifier.padding(24.dp)) {
                Text("주변 정비소", style = MaterialTheme.typography.headlineSmall, color = Ink, fontWeight = FontWeight.Bold)
                Text("블루핸즈는 내비게이션에서 검색하고 지점을 선택해 길 안내를 시작할 수 있습니다.", style = MaterialTheme.typography.bodyMedium)
                Button(enabled = parked, onClick = onSearchBluehands) { Text("내비게이션에서 블루핸즈 찾기") }
                Spacer(Modifier.height(12.dp))
                Text("일반 정비소 검색", style = MaterialTheme.typography.titleSmall)
                Text("검색을 누르면 현재 위치를 OpenStreetMap Overpass 서버(overpass-api.de)에 전송합니다. 위치 권한이 필요합니다.", style = MaterialTheme.typography.bodyMedium)
                Text("정보: © OpenStreetMap 기여자. 운영·취급 항목은 방문 전 확인해 주세요.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF607276))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onSearchRepairs) { Text("현재 위치로 검색") }
                    TextButton(onClick = { showRepairSearch = false }) { Text("닫기") }
                }
                if (shopStatus.isNotBlank()) Text(shopStatus, color = Teal)
                LazyColumn(Modifier.heightIn(max = 350.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(repairShops) { shop ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(shop.name, modifier = Modifier.weight(1f).padding(top = 12.dp))
                            TextButton(enabled = parked, onClick = { onNavigateRepair(shop) }) { Text("길 안내 요청") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordDialog(currentKm: Double?, initial: MaintenanceRecord?, draftItem: String, draftDate: String?, draftCost: String?, draftInterval: String?, onDismiss: () -> Unit, onSave: (MaintenanceRecord) -> Unit) {
    var item by remember { mutableStateOf(initial?.item ?: draftItem) }
    var date by remember { mutableStateOf(initial?.date?.toString() ?: draftDate ?: LocalDate.now().toString()) }
    var mileage by remember { mutableStateOf(initial?.mileageKm?.toString() ?: "") }
    var interval by remember { mutableStateOf(initial?.intervalKm?.toString() ?: draftInterval ?: "") }
    var months by remember { mutableStateOf(initial?.intervalMonths?.toString() ?: "") }
    var cost by remember { mutableStateOf(initial?.costWon?.toString() ?: draftCost ?: "") }
    var error by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(22.dp), color = Color.White, modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth()) {
            Column(Modifier.padding(24.dp)) {
                Text(if (initial == null) "정비 기록 추가" else "정비 기록 수정", style = MaterialTheme.typography.headlineSmall, color = Ink, fontWeight = FontWeight.Bold)
                Text("교체·점검한 날짜와 직접 확인한 기준을 입력해 주세요.", color = Color(0xFF607276))
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("엔진오일", "에어컨 필터", "와이퍼", "타이어", "타이어 위치 교환").forEach { name -> AssistChip(onClick = { item = name }, label = { Text(name) }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(item, { item = it.take(40) }, label = { Text("정비 항목 *") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(date, { date = it.take(10) }, label = { Text("정비 날짜 * (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(mileage, { mileage = it.filter(Char::isDigit).take(8) }, label = { Text("당시 주행거리 (km)") }, singleLine = true, modifier = Modifier.weight(1f))
                    TextButton(enabled = currentKm != null && date == LocalDate.now().toString(), onClick = { mileage = currentKm!!.toInt().toString() }) { Text("오늘 차량 값 사용") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(interval, { interval = it.filter(Char::isDigit).take(8) }, label = { Text("점검 거리 주기 (km)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(months, { months = it.filter(Char::isDigit).take(3) }, label = { Text("점검 기간 주기 (개월)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(cost, { cost = it.filter(Char::isDigit).take(9) }, label = { Text("비용 (원)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Text("차량 취급설명서를 확인하여 주기를 입력하세요. 과거 정비에는 당시 주행거리를 입력합니다.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF607276), modifier = Modifier.padding(top = 8.dp))
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("취소") }
                    Button(onClick = {
                        val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
                        when {
                            item.isBlank() -> error = "정비 항목을 입력해 주세요."
                            parsedDate == null || parsedDate.isAfter(LocalDate.now()) -> error = "오늘 이전의 유효한 정비 날짜를 입력해 주세요."
                            interval.isNotEmpty() && (interval.toIntOrNull() ?: 0) <= 0 -> error = "거리 주기는 1 이상이어야 합니다."
                            months.isNotEmpty() && (months.toIntOrNull() ?: 0) <= 0 -> error = "기간 주기는 1개월 이상이어야 합니다."
                            else -> onSave(MaintenanceRecord(id = initial?.id ?: java.util.UUID.randomUUID().toString(), item = item.trim(), date = parsedDate, mileageKm = mileage.toIntOrNull(), intervalKm = interval.toIntOrNull(), intervalMonths = months.toIntOrNull(), costWon = cost.toIntOrNull()))
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Teal)) { Text("기록 저장") }
                }
            }
        }
    }
}
