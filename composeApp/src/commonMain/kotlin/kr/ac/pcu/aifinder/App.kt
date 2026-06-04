package kr.ac.pcu.aifinder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.Json


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(platformContext: Any? = null, refreshTrigger: Int = 0) {
    val platformStorage = remember { PlatformStorage(platformContext) }
    val itemStorage = remember { ItemStorage(platformStorage) }
    val recommender = remember { AiFindRecommender() }

    var items by remember { mutableStateOf(itemStorage.getItems()) }
    var areas by remember { mutableStateOf(itemStorage.getRoomAreas()) }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 검색, 1: 방지도, 2: 즐겨찾기, 3: 체크리스트, 4: 통계

    var showAddItemDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    var newItemAreaId by remember { mutableIntStateOf(1) }

    fun refreshData() {
        items = itemStorage.getItems()
        areas = itemStorage.getRoomAreas()
    }

    LaunchedEffect(refreshTrigger) {
        refreshData()
    }


    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF2563EB),
            secondary = Color(0xFF0F766E),
            background = Color(0xFFF8FAFC),
            surface = Color(0xFFFFFFFF),
            onPrimary = Color.White
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "AIFinder (iOS & Android)", 
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        ) 
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = { refreshData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Search, "검색") },
                        label = { Text("검색") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Home, "방 지도") },
                        label = { Text("방 지도") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Favorite, "즐겨찾기") },
                        label = { Text("즐겨찾기") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.List, "체크리스트") },
                        label = { Text("체크") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Info, "통계") },
                        label = { Text("통계") }
                    )
                }

            },
            floatingActionButton = {
                if (selectedTab == 0 || selectedTab == 1) {
                    FloatingActionButton(
                        onClick = { 
                            launchObjectDetectionCamera(platformContext) {
                                refreshData()
                            }
                        },
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "물품 등록")
                    }
                }
            }

        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF8FAFC))
            ) {
                when (selectedTab) {
                    0 -> SearchTab(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        items = items,
                        areas = areas,
                        recommender = recommender,
                        onToggleFavorite = { id ->
                            itemStorage.toggleFavorite(id)
                            refreshData()
                        },
                        onDeleteItem = { id ->
                            itemStorage.deleteItem(id)
                            refreshData()
                        }
                    )
                    1 -> RoomMapTab(
                        areas = areas,
                        items = items,
                        itemStorage = itemStorage,
                        onRename = { refreshData() },
                        onDeleteItem = { id ->
                            itemStorage.deleteItem(id)
                            refreshData()
                        }
                    )
                    2 -> FavoritesTab(
                        items = items,
                        onToggleFavorite = { id ->
                            itemStorage.toggleFavorite(id)
                            refreshData()
                        }
                    )
                    3 -> ChecklistTab(platformStorage)
                    4 -> StatsTab(itemStorage)
                }
            }
        }

        // Add Item Dialog (Camera emulation & manual registration)
        if (showAddItemDialog) {
            AlertDialog(
                onDismissRequest = { showAddItemDialog = false },
                title = { Text("물품 등록", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = newItemName,
                            onValueChange = { newItemName = it },
                            label = { Text("물품 이름") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("보관할 구역 선택", fontWeight = FontWeight.Bold)
                        areas.forEach { area ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { newItemAreaId = area.id }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = newItemAreaId == area.id,
                                    onClick = { newItemAreaId = area.id }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(area.name)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val name = newItemName.trim()
                            if (name.isNotEmpty()) {
                                val selectedArea = areas.first { it.id == newItemAreaId }
                                val record = ItemRecord(
                                    id = getCurrentTimeMillis().toString(),
                                    name = name,
                                    areaId = newItemAreaId,
                                    areaName = selectedArea.name,
                                    timestamp = getCurrentTimeMillis(),
                                    photoUri = null,
                                    boundingBox = null
                                )
                                itemStorage.addItem(record)
                                refreshData()
                                newItemName = ""
                                showAddItemDialog = false
                            }
                        }
                    ) {
                        Text("저장")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddItemDialog = false }) {
                        Text("취소")
                    }
                }
            )
        }
    }
}

@Composable
fun SearchTab(
    query: String,
    onQueryChange: (String) -> Unit,
    items: List<ItemRecord>,
    areas: List<RoomArea>,
    recommender: AiFindRecommender,
    onToggleFavorite: (String) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("찾으시는 물건을 입력하세요") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // 1. AI Recommendation Card
        if (query.isNotBlank()) {
            val recommendation = recommender.recommend(query, items, areas)
            if (recommendation != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = BorderStroke(2.dp, Color(0xFF2563EB))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "★ AI 최적 추천 위치",
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "(신뢰도 ${recommendation.confidence}%)",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            recommendation.recommendedArea.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            recommendation.matchReason,
                            fontSize = 14.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }
        }

        // 2. Filtered Items list
        Text("등록된 물건 목록", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        val filtered = items.filter {
            query.isBlank() || it.name.contains(query, ignoreCase = true) || it.areaName.contains(query, ignoreCase = true)
        }

        if (filtered.isEmpty()) {
            Text(
                "검색 결과가 없습니다.",
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        } else {
            filtered.forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.areaName,
                                color = Color(0xFF0F766E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                item.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                        IconButton(onClick = { onToggleFavorite(item.id) }) {
                            Icon(
                                imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "즐겨찾기",
                                tint = if (item.isFavorite) Color.Red else Color.Gray
                            )
                        }
                        IconButton(onClick = { onDeleteItem(item.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoomMapTab(
    areas: List<RoomArea>,
    items: List<ItemRecord>,
    itemStorage: ItemStorage,
    onRename: () -> Unit,
    onDeleteItem: (String) -> Unit
) {
    var selectedAreaId by remember { mutableIntStateOf(1) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInputText by remember { mutableStateOf("") }

    val counts = items.groupBy { it.areaId }.mapValues { it.value.size }
    val selectedArea = areas.firstOrNull { it.id == selectedAreaId }
    val areaItems = items.filter { it.areaId == selectedAreaId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RoomMapView(
            areas = areas,
            itemCounts = counts,
            selectedAreaId = selectedAreaId,
            onAreaClick = { selectedAreaId = it.id }
        )

        if (selectedArea != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${selectedArea.name} 물품 목록",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = {
                    renameInputText = selectedArea.name
                    showRenameDialog = true
                }) {
                    Text("구역명 수정")
                }
            }

            if (areaItems.isEmpty()) {
                Text(
                    "이 구역에 등록된 물건이 없습니다.",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            } else {
                areaItems.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                item.name,
                                modifier = Modifier.weight(1f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { onDeleteItem(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog && selectedArea != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("구역 이름 변경") },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    label = { Text("구역 이름") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val newName = renameInputText.trim()
                    if (newName.isNotEmpty()) {
                        itemStorage.renameArea(selectedAreaId, newName)
                        onRename()
                        showRenameDialog = false
                    }
                }) {
                    Text("저장")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun FavoritesTab(
    items: List<ItemRecord>,
    onToggleFavorite: (String) -> Unit
) {
    val favorites = items.filter { it.isFavorite }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("즐겨찾기 목록", fontSize = 18.sp, fontWeight = FontWeight.Bold)

        if (favorites.isEmpty()) {
            Text(
                "즐겨찾기가 비어있습니다. 자주 찾는 물건 카드의 하트 버튼을 눌러 추가하세요.",
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        } else {
            favorites.forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.areaName, color = Color(0xFF0F766E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(item.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { onToggleFavorite(item.id) }) {
                            Icon(Icons.Default.Favorite, contentDescription = "해제", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChecklistTab(storage: PlatformStorage) {
    val defaultChecklist = listOf("휴대폰", "지갑", "현관 열쇠", "우산", "보조 배터리")
    
    // Save/Load helpers in Compose using platform storage
    var listText by remember { 
        mutableStateOf(storage.getString("checklist_items", "") ?: "") 
    }
    
    var items by remember {
        mutableStateOf(
            if (listText.isBlank()) {
                defaultChecklist.map { ChecklistItem(it, false) }
            } else {
                listText.split("|").filter { it.isNotBlank() }.mapNotNull {
                    val parts = it.split("^")
                    val name = parts.getOrNull(0).orEmpty()
                    if (name.isBlank()) null else ChecklistItem(name, parts.getOrNull(1) == "1")
                }
            }
        )
    }

    fun save() {
        val serialized = items.joinToString("|") { "${it.name}^${if (it.checked) "1" else "0"}" }
        storage.putString("checklist_items", serialized)
        listText = serialized
    }

    var newItemText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("외출 전 체크리스트", fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                label = { Text("준비물 추가") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val name = newItemText.trim()
                    if (name.isNotEmpty()) {
                        items = items + ChecklistItem(name, false)
                        newItemText = ""
                        save()
                    }
                },
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                Text("추가")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        items.forEachIndexed { idx, item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.checked,
                        onCheckedChange = { checked ->
                            items = items.toMutableList().apply {
                                this[idx] = this[idx].copy(checked = checked)
                            }
                            save()
                        }
                    )
                    Text(
                        item.name,
                        modifier = Modifier.weight(1f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = {
                        items = items.toMutableList().apply { removeAt(idx) }
                        save()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun StatsTab(itemStorage: ItemStorage) {
    val stats = itemStorage.getRecent7DaysStats()
    val totalCount = stats.values.sum()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("최근 7일간 등록 통계", fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("최근 7일간 등록된 물품", color = Color.Gray, fontSize = 13.sp)
                Text("${totalCount}개", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                if (stats.isNotEmpty()) {
                    Text(
                        "가장 물품 등록이 활발한 구역: ${stats.maxByOrNull { it.value }?.key}",
                        fontSize = 13.sp,
                        color = Color(0xFF0F766E),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Text("구역별 등록 비중", fontSize = 16.sp, fontWeight = FontWeight.Bold)

        if (stats.isEmpty()) {
            Text(
                "최근 7일 동안 새로 등록된 소지품 데이터가 없습니다.",
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        } else {
            stats.forEach { (areaName, count) ->
                val ratio = if (totalCount > 0) count.toFloat() / totalCount else 0f
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(areaName, fontWeight = FontWeight.Bold)
                            Text("${count}개 (${(ratio * 100).toInt()}%)")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = Color(0xFF2563EB),
                            trackColor = Color(0xFFE2E8F0),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

data class ChecklistItem(val name: String, val checked: Boolean)
