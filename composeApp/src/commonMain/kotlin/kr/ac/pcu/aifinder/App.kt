package kr.ac.pcu.aifinder

import androidx.compose.animation.Crossfade
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(platformContext: Any? = null, refreshTrigger: Int = 0) {
    val platformStorage = remember { PlatformStorage(platformContext) }
    val itemStorage = remember { ItemStorage(platformStorage) }
    val recommender = remember { AiFindRecommender() }

    val coroutineScope = rememberCoroutineScope()

    var currentUser by remember { mutableStateOf(itemStorage.getCurrentUser()) }
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

    LaunchedEffect(refreshTrigger, currentUser) {
        refreshData()
    }


    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF4F46E5),
            secondary = Color(0xFF06B6D4),
            background = Color(0xFFF8FAFC),
            surface = Color(0xFFFFFFFF),
            onPrimary = Color.White
        )
    ) {
        val user = currentUser
        if (user == null) {
            var showRegister by remember { mutableStateOf(false) }
            if (showRegister) {
                RegisterScreen(
                    itemStorage = itemStorage,
                    onBackToLogin = { showRegister = false },
                    onRegisterSuccess = { registeredUser ->
                        currentUser = registeredUser
                    }
                )
            } else {
                LoginScreen(
                    itemStorage = itemStorage,
                    onLoginSuccess = { loggedInUser ->
                        currentUser = loggedInUser
                    },
                    onNavigateToRegister = { showRegister = true }
                )
            }
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFFEEF2FF), shape = RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "AIFinder", 
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1E293B),
                                    fontSize = 20.sp
                                ) 
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White
                        ),
                        actions = {
                            IconButton(onClick = { refreshData() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "새로고침", tint = Color(0xFF64748B))
                            }
                            IconButton(onClick = { 
                                itemStorage.logout()
                                currentUser = null
                            }) {
                                Icon(Icons.Default.ExitToApp, contentDescription = "로그아웃", tint = Color(0xFF64748B))
                            }
                        }
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 8.dp
                    ) {
                        val navItems = listOf(
                            Triple(0, Icons.Default.Search, "검색"),
                            Triple(1, Icons.Default.Home, "방 지도"),
                            Triple(2, Icons.Default.Favorite, "즐겨찾기"),
                            Triple(3, Icons.Default.List, "체크리스트"),
                            Triple(4, Icons.Default.Info, "통계")
                        )
                        navItems.forEach { (index, icon, label) ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF4F46E5),
                                    selectedTextColor = Color(0xFF4F46E5),
                                    indicatorColor = Color(0xFFEEF2FF),
                                    unselectedIconColor = Color(0xFF94A3B8),
                                    unselectedTextColor = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (selectedTab == 0 || selectedTab == 1) {
                        FloatingActionButton(
                            onClick = { 
                                launchObjectDetectionCamera(platformContext) {
                                    refreshData()
                                    coroutineScope.launch { itemStorage.syncItemsRemote() }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = Color.Transparent,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF4F46E5), Color(0xFF06B6D4))
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add, 
                                contentDescription = "물품 인식 카메라 기동", 
                                tint = Color.White,
                                modifier = Modifier.padding(16.dp)
                            )
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
                Crossfade(targetState = selectedTab) { tab ->
                    when (tab) {
                        0 -> SearchTab(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            items = items,
                            areas = areas,
                            recommender = recommender,
                            onToggleFavorite = { id ->
                                itemStorage.toggleFavorite(id)
                                refreshData()
                                coroutineScope.launch { itemStorage.syncItemsRemote() }
                            },
                            onDeleteItem = { id ->
                                itemStorage.deleteItem(id)
                                refreshData()
                                coroutineScope.launch { itemStorage.syncItemsRemote() }
                            }
                        )
                        1 -> RoomMapTab(
                            areas = areas,
                            items = items,
                            itemStorage = itemStorage,
                            onRename = { 
                                refreshData() 
                                coroutineScope.launch { itemStorage.syncItemsRemote() }
                            },
                            onDeleteItem = { id ->
                                itemStorage.deleteItem(id)
                                refreshData()
                                coroutineScope.launch { itemStorage.syncItemsRemote() }
                            }
                        )
                        2 -> FavoritesTab(
                            items = items,
                            onToggleFavorite = { id ->
                                itemStorage.toggleFavorite(id)
                                refreshData()
                                coroutineScope.launch { itemStorage.syncItemsRemote() }
                            },
                            onDeleteItem = { id ->
                                itemStorage.deleteItem(id)
                                refreshData()
                                coroutineScope.launch { itemStorage.syncItemsRemote() }
                            }
                        )
                        3 -> ChecklistTab(platformStorage)
                        4 -> StatsTab(itemStorage)
                    }
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
                                coroutineScope.launch { itemStorage.syncItemsRemote() }
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
}

data class BadgeColor(val background: Color, val tint: Color)

fun getItemBadgeColor(itemName: String): BadgeColor {
    val name = itemName.lowercase()
    return when {
        name.contains("열쇠") || name.contains("key") -> BadgeColor(Color(0xFFFEF3C7), Color(0xFFD97706)) // Amber
        name.contains("지갑") || name.contains("wallet") || name.contains("카드") || name.contains("신분증") -> BadgeColor(Color(0xFFD1FAE5), Color(0xFF059669)) // Emerald
        name.contains("폰") || name.contains("휴대폰") || name.contains("phone") || name.contains("패드") || name.contains("가전") -> BadgeColor(Color(0xFFE0F2FE), Color(0xFF0284C7)) // Sky
        name.contains("책") || name.contains("book") || name.contains("노트") -> BadgeColor(Color(0xFFF3E8FF), Color(0xFF7C3AED)) // Purple
        name.contains("가방") || name.contains("bag") || name.contains("쇼핑") -> BadgeColor(Color(0xFFFFE4E6), Color(0xFFE11D48)) // Rose
        else -> BadgeColor(Color(0xFFF1F5F9), Color(0xFF475569)) // Slate
    }
}

@Composable
fun ItemRecordRow(
    item: ItemRecord,
    onToggleFavorite: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val badgeColor = getItemBadgeColor(item.name)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(badgeColor.background, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getItemIcon(item.name),
                    contentDescription = null,
                    tint = badgeColor.tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Text(
                        item.areaName,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    item.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
            IconButton(onClick = { onToggleFavorite(item.id) }) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "즐겨찾기",
                    tint = if (item.isFavorite) Color(0xFFEF4444) else Color(0xFF94A3B8)
                )
            }
            IconButton(onClick = { onDeleteItem(item.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color(0xFF94A3B8))
            }
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
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF4F46E5)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4F46E5),
                focusedLabelColor = Color(0xFF4F46E5),
                cursorColor = Color(0xFF4F46E5)
            )
        )

        // 1. AI Recommendation Card
        if (query.isNotBlank()) {
            val recommendation = recommender.recommend(query, items, areas)
            if (recommendation != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
                    border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFF4F46E5), Color(0xFF06B6D4)))),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    "★ AI 최적 추천 위치",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                "신뢰도 ${recommendation.confidence}%",
                                color = Color(0xFF4F46E5),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            recommendation.recommendedArea.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            recommendation.matchReason,
                            fontSize = 14.sp,
                            color = Color(0xFF475569),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // 2. Filtered Items list
        Text("등록된 물건 목록", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color(0xFF1E293B))
        val filtered = items.filter {
            query.isBlank() || it.name.contains(query, ignoreCase = true) || it.areaName.contains(query, ignoreCase = true)
        }

        if (filtered.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Text(
                    "검색 결과가 없습니다.",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            filtered.forEach { item ->
                ItemRecordRow(
                    item = item,
                    onToggleFavorite = onToggleFavorite,
                    onDeleteItem = onDeleteItem
                )
            }
        }
    }
}

fun getItemIcon(itemName: String): androidx.compose.ui.graphics.vector.ImageVector {
    val name = itemName.lowercase()
    return when {
        name.contains("열쇠") || name.contains("key") -> Icons.Default.Lock
        name.contains("지갑") || name.contains("wallet") || name.contains("카드") || name.contains("신분증") -> Icons.Default.Person
        name.contains("폰") || name.contains("휴대폰") || name.contains("phone") -> Icons.Default.Info
        name.contains("책") || name.contains("book") || name.contains("노트") -> Icons.Default.List
        name.contains("가방") || name.contains("bag") || name.contains("쇼핑") -> Icons.Default.ShoppingCart
        else -> Icons.Default.Build
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
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                OutlinedButton(
                    onClick = {
                        renameInputText = selectedArea.name
                        showRenameDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF4F46E5)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4F46E5))
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("구역명 수정", fontWeight = FontWeight.Bold)
                }
            }

            if (areaItems.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Text(
                        "이 구역에 등록된 물건이 없습니다.",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                areaItems.forEach { item ->
                    ItemRecordRow(
                        item = item,
                        onToggleFavorite = { id ->
                            itemStorage.toggleFavorite(id)
                            onRename()
                        },
                        onDeleteItem = onDeleteItem
                    )
                }
            }
        }
    }

    if (showRenameDialog && selectedArea != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("구역 이름 변경", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    label = { Text("구역 이름") },
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newName = renameInputText.trim()
                        if (newName.isNotEmpty()) {
                            itemStorage.renameArea(selectedAreaId, newName)
                            onRename()
                            showRenameDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
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
    onToggleFavorite: (String) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    val favorites = items.filter { it.isFavorite }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("즐겨찾기 목록", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))

        if (favorites.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "즐겨찾기가 비어있습니다.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "자주 찾는 물건 카드의 하트 버튼을 눌러 추가하세요.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            favorites.forEach { item ->
                ItemRecordRow(
                    item = item,
                    onToggleFavorite = onToggleFavorite,
                    onDeleteItem = onDeleteItem
                )
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("외출 전 체크리스트", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                placeholder = { Text("준비물 추가...", color = Color(0xFF94A3B8)) },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF1F5F9),
                    unfocusedContainerColor = Color(0xFFF1F5F9),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color(0xFF4F46E5)
                ),
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
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                modifier = Modifier.height(48.dp)
            ) {
                Text("추가", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        items.forEachIndexed { idx, item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF4F46E5),
                            checkmarkColor = Color.White
                        )
                    )
                    Text(
                        text = item.name,
                        modifier = Modifier.weight(1f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (item.checked) Color(0xFF94A3B8) else Color(0xFF1E293B),
                        style = androidx.compose.ui.text.TextStyle(
                            textDecoration = if (item.checked) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                        )
                    )
                    IconButton(onClick = {
                        items = items.toMutableList().apply { removeAt(idx) }
                        save()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color(0xFF94A3B8))
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
    
    // Sort by item count descending for better readability
    val sortedStats = stats.toList().sortedByDescending { it.second }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("최근 7일간 등록 통계", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF4F46E5), Color(0xFF06B6D4))
                        )
                    )
                    .padding(20.dp)
            ) {
                Text("최근 7일간 등록된 물품", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("${totalCount}개", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                if (stats.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "가장 물품 등록이 활발한 구역: ${stats.maxByOrNull { it.value }?.key}",
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text("구역별 등록 비중", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))

        if (stats.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Text(
                    "최근 7일 동안 새로 등록된 소지품 데이터가 없습니다.",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            sortedStats.forEach { (areaName, count) ->
                val ratio = if (totalCount > 0) count.toFloat() / totalCount else 0f
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(areaName, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 15.sp)
                            Text(
                                "${count}개 (${(ratio * 100).toInt()}%)",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4F46E5),
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier.fillMaxWidth().height(10.dp),
                            color = Color(0xFF4F46E5),
                            trackColor = Color(0xFFF1F5F9),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

data class ChecklistItem(val name: String, val checked: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    itemStorage: ItemStorage,
    onLoginSuccess: (User) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    
    // Server API URL state
    var serverUrl by remember { mutableStateOf(itemStorage.getServerUrl()) }
    var showServerConfig by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFEEF2FF), Color(0xFFE0F7FA))
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 로고 그래픽
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .padding(bottom = 8.dp)
                        .background(Color(0xFFEEF2FF), shape = RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "AIFinder",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "언제 어디서나 스마트한 물품 찾기",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // 텍스트 필드 현대화 (둥근 모서리, 부드러운 회색 배경, border 없는 깔끔한 스타일)
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = { Text("아이디", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    enabled = !isLoading,
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = Color(0xFF94A3B8)) },
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF1F5F9),
                        unfocusedContainerColor = Color(0xFFF1F5F9),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFF4F46E5)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("비밀번호", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    enabled = !isLoading,
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF94A3B8)) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF1F5F9),
                        unfocusedContainerColor = Color(0xFFF1F5F9),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFF4F46E5)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // 그라데이션 버튼
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp).padding(bottom = 8.dp),
                        color = Color(0xFF4F46E5)
                    )
                } else {
                    Button(
                        onClick = {
                            if (username.isEmpty() || password.isEmpty()) {
                                errorMessage = "아이디와 비밀번호를 모두 입력하세요."
                                return@Button
                            }
                            
                            // Save URL configuration first
                            itemStorage.saveServerUrl(serverUrl)

                            isLoading = true
                            errorMessage = ""
                            coroutineScope.launch {
                                // Try remote authentication
                                val response = itemStorage.authenticateRemote(username, password)
                                if (response.success && response.user != null) {
                                    val remoteUser = response.user!!
                                    val localUsers = itemStorage.getUsers().toMutableList()
                                    if (!localUsers.any { it.id == remoteUser.id }) {
                                        localUsers.add(remoteUser)
                                        itemStorage.saveUsers(localUsers)
                                    }
                                    
                                    itemStorage.setCurrentUser(remoteUser.id)
                                    
                                    // Load items from remote DB to Local storage
                                    itemStorage.loadItemsRemote()
                                    
                                    isLoading = false
                                    onLoginSuccess(remoteUser)
                                } else {
                                    // Fallback to local authentication in case offline
                                    val localUser = itemStorage.authenticate(username, password)
                                    isLoading = false
                                    if (localUser != null) {
                                        itemStorage.setCurrentUser(localUser.id)
                                        onLoginSuccess(localUser)
                                    } else {
                                        errorMessage = response.message ?: "로그인 정보가 틀렸습니다."
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF4F46E5), Color(0xFF06B6D4))
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Text("로그인", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onNavigateToRegister, enabled = !isLoading) {
                    Text("계정이 없으신가요? 회원가입", color = Color(0xFF4F46E5), fontWeight = FontWeight.SemiBold)
                }

                // 서버 연결 설정 접기/펴기
                TextButton(onClick = { showServerConfig = !showServerConfig }, enabled = !isLoading) {
                    Text(
                        if (showServerConfig) "서버 설정 닫기 ▲" else "서버 설정 열기 ▼",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }

                if (showServerConfig) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        placeholder = { Text("http://192.168.0.X:5000", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color(0xFF4F46E5)
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    itemStorage: ItemStorage,
    onBackToLogin: () -> Unit,
    onRegisterSuccess: (User) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    var serverUrl by remember { mutableStateOf(itemStorage.getServerUrl()) }
    var showServerConfig by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFEEF2FF), Color(0xFFE0F7FA))
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 로고 그래픽
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .padding(bottom = 8.dp)
                        .background(Color(0xFFEEF2FF), shape = RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "회원가입",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "새로운 계정을 생성하여 데이터를 관리하세요",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                TextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = { Text("아이디 (영문/숫자)", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    enabled = !isLoading,
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = Color(0xFF94A3B8)) },
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF1F5F9),
                        unfocusedContainerColor = Color(0xFFF1F5F9),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFF4F46E5)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                TextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("이메일 주소", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    enabled = !isLoading,
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = Color(0xFF94A3B8)) },
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF1F5F9),
                        unfocusedContainerColor = Color(0xFFF1F5F9),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFF4F46E5)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("비밀번호", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    enabled = !isLoading,
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF94A3B8)) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF1F5F9),
                        unfocusedContainerColor = Color(0xFFF1F5F9),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFF4F46E5)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                TextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = { Text("비밀번호 확인", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    enabled = !isLoading,
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF94A3B8)) },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF1F5F9),
                        unfocusedContainerColor = Color(0xFFF1F5F9),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFF4F46E5)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp).padding(bottom = 8.dp),
                        color = Color(0xFF4F46E5)
                    )
                } else {
                    Button(
                        onClick = {
                            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                                errorMessage = "모든 빈칸을 입력해주세요."
                                return@Button
                            }
                            if (password != confirmPassword) {
                                errorMessage = "비밀번호가 일치하지 않습니다."
                                return@Button
                            }

                            itemStorage.saveServerUrl(serverUrl)
                            isLoading = true
                            errorMessage = ""

                            val newUser = User(
                                id = "user_${getCurrentTimeMillis()}",
                                username = username,
                                passwordHash = password,
                                email = email
                            )

                            coroutineScope.launch {
                                // Try remote registration first
                                val response = itemStorage.registerUserRemote(newUser)
                                if (response.success) {
                                    itemStorage.registerUser(newUser)
                                    itemStorage.setCurrentUser(newUser.id)
                                    isLoading = false
                                    onRegisterSuccess(newUser)
                                } else {
                                    // Try fallback local registration
                                    val successLocal = itemStorage.registerUser(newUser)
                                    isLoading = false
                                    if (successLocal) {
                                        itemStorage.setCurrentUser(newUser.id)
                                        onRegisterSuccess(newUser)
                                    } else {
                                        errorMessage = response.message ?: "이미 존재하는 아이디입니다."
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF4F46E5), Color(0xFF06B6D4))
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Text("가입 및 로그인", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onBackToLogin, enabled = !isLoading) {
                    Text("이미 계정이 있으신가요? 로그인", color = Color(0xFF4F46E5), fontWeight = FontWeight.SemiBold)
                }

                // 서버 연결 설정 접기/펴기
                TextButton(onClick = { showServerConfig = !showServerConfig }, enabled = !isLoading) {
                    Text(
                        if (showServerConfig) "서버 설정 닫기 ▲" else "서버 설정 열기 ▼",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }

                if (showServerConfig) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        placeholder = { Text("http://192.168.0.X:5000", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color(0xFF4F46E5)
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                    )
                }
            }
        }
    }
}
