package kr.ac.pcu.aifinder

import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

class ItemStorage(private val storage: PlatformStorage) {

    private val network = PlatformNetwork()

    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    companion object {
        private const val KEY_ITEMS = "items_list"
        private const val KEY_AREAS = "areas_list"
        private const val KEY_USERS = "users_list"
        private const val KEY_CURRENT_USER = "current_user_session"
    }

    private val defaultAreas = listOf(
        RoomArea(1, "침대 구역"),
        RoomArea(2, "책상 구역"),
        RoomArea(3, "옷장 구역"),
        RoomArea(4, "현관 구역"),
        RoomArea(5, "선반 구역"),
        RoomArea(6, "창가 구역")
    )

    // User authentication & Profile management
    fun getUsers(): List<User> {
        val rawJson = storage.getString(KEY_USERS, null) ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(User.serializer()), rawJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveUsers(users: List<User>) {
        val rawJson = json.encodeToString(ListSerializer(User.serializer()), users)
        storage.putString(KEY_USERS, rawJson)
    }

    fun registerUser(user: User): Boolean {
        val users = getUsers().toMutableList()
        if (users.any { it.username.equals(user.username, ignoreCase = true) }) {
            return false // Username already taken
        }
        users.add(user)
        saveUsers(users)
        return true
    }

    fun saveOrUpdateUser(user: User) {
        val users = getUsers().toMutableList()
        users.removeAll { it.id == user.id || it.username.equals(user.username, ignoreCase = true) }
        users.add(user)
        saveUsers(users)
    }

    fun authenticate(username: String, passwordHash: String): User? {
        return getUsers().firstOrNull { 
            it.username.equals(username, ignoreCase = true) && it.passwordHash == passwordHash 
        }
    }

    fun getCurrentUser(): User? {
        val userId = storage.getString(KEY_CURRENT_USER, null) ?: return null
        if (userId.isEmpty()) return null
        return getUsers().firstOrNull { it.id == userId }
    }

    fun setCurrentUser(userId: String?) {
        storage.putString(KEY_CURRENT_USER, userId ?: "")
    }

    fun logout() {
        setCurrentUser(null)
    }

    fun isAutoLoginEnabled(): Boolean {
        return storage.getString("auto_login_enabled", "true") == "true"
    }

    fun setAutoLoginEnabled(enabled: Boolean) {
        storage.putString("auto_login_enabled", enabled.toString())
    }

    // Room Area management
    fun getRoomAreas(): List<RoomArea> {
        val rawJson = storage.getString(KEY_AREAS, null) ?: return defaultAreas.also { saveRoomAreas(it) }
        return try {
            json.decodeFromString(ListSerializer(RoomArea.serializer()), rawJson)
        } catch (e: Exception) {
            defaultAreas
        }
    }

    fun saveRoomAreas(areas: List<RoomArea>) {
        val rawJson = json.encodeToString(ListSerializer(RoomArea.serializer()), areas)
        storage.putString(KEY_AREAS, rawJson)
    }

    fun renameArea(id: Int, newName: String) {
        val areas = getRoomAreas().map {
            if (it.id == id) it.copy(name = newName) else it
        }
        saveRoomAreas(areas)

        // Cascade rename to items
        val items = getItems().map {
            if (it.areaId == id) it.copy(areaName = newName) else it
        }
        saveItems(items)
    }

    // Item Records segregated by User ID
    fun getItems(): List<ItemRecord> {
        val rawJson = storage.getString(KEY_ITEMS, null) ?: return emptyList()
        val allItems = try {
            json.decodeFromString(ListSerializer(ItemRecord.serializer()), rawJson)
        } catch (e: Exception) {
            emptyList()
        }
        val currentUser = getCurrentUser() ?: return emptyList()
        return allItems.filter { it.userId == currentUser.id }
    }

    fun saveItems(items: List<ItemRecord>) {
        // Load all items in raw storage
        val rawJson = storage.getString(KEY_ITEMS, null) ?: "[]"
        val allItems = try {
            json.decodeFromString(ListSerializer(ItemRecord.serializer()), rawJson)
        } catch (e: Exception) {
            emptyList()
        }

        val currentUser = getCurrentUser()
        val currentUserId = currentUser?.id

        // Keep items belonging to other users, replace items belonging to current user
        val otherUsersItems = allItems.filter { it.userId != currentUserId }
        val combinedItems = items + otherUsersItems

        val newRawJson = json.encodeToString(ListSerializer(ItemRecord.serializer()), combinedItems)
        storage.putString(KEY_ITEMS, newRawJson)
    }

    fun addItem(item: ItemRecord) {
        val currentUser = getCurrentUser()
        val itemWithUser = item.copy(userId = currentUser?.id)

        val items = getItems().toMutableList()
        items.add(0, itemWithUser)
        saveItems(items)
    }

    fun deleteItem(id: String) {
        val items = getItems().filter { it.id != id }
        saveItems(items)
    }

    fun toggleFavorite(id: String): Boolean {
        var newStatus = false
        val items = getItems().map {
            if (it.id == id) {
                newStatus = !it.isFavorite
                it.copy(isFavorite = newStatus)
            } else it
        }
        saveItems(items)
        return newStatus
    }

    fun getItemsInArea(areaId: Int): List<ItemRecord> {
        return getItems().filter { it.areaId == areaId }
    }

    fun getFavorites(): List<ItemRecord> {
        return getItems().filter { it.isFavorite }
    }

    fun getRecent7DaysStats(): Map<String, Int> {
        val sevenDaysAgo = getCurrentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val recentItems = getItems().filter { it.timestamp >= sevenDaysAgo }
        return recentItems.groupBy { it.areaName }.mapValues { it.value.size }
    }

    // API URL configuration
    fun getServerUrl(): String {
        return "https://pcu-aifinder-2026.loca.lt"
    }

    fun saveServerUrl(url: String) {
        var formatted = url.trim()
        if (formatted.isNotEmpty() && !formatted.startsWith("http://") && !formatted.startsWith("https://")) {
            formatted = "http://$formatted"
        }
        storage.putString("server_api_url", formatted)
    }

    // Remote operations (suspend) using Firebase
    suspend fun registerUserRemote(user: User): ServerResponse {
        return FirebaseBackend.registerUser(user)
    }

    suspend fun authenticateRemote(username: String, passwordHash: String): ServerResponse {
        return FirebaseBackend.authenticate(username, passwordHash)
    }

    suspend fun updateUserProfileRemote(user: User): Boolean {
        return FirebaseBackend.updateUserProfile(user)
    }

    suspend fun syncItemsRemote(): Boolean {
        val currentUser = getCurrentUser() ?: return false
        val items = getItems()
        return FirebaseBackend.syncItems(currentUser.id, items)
    }

    suspend fun loadItemsRemote(): Boolean {
        val currentUser = getCurrentUser() ?: return false
        val remoteItems = FirebaseBackend.loadItems(currentUser.id)
        if (remoteItems.isNotEmpty()) {
            saveItemsLocalOnly(remoteItems)
            return true
        }
        return false
    }

    private fun saveItemsLocalOnly(items: List<ItemRecord>) {
        val rawJson = storage.getString(KEY_ITEMS, null) ?: "[]"
        val allItems = try {
            json.decodeFromString(ListSerializer(ItemRecord.serializer()), rawJson)
        } catch (e: Exception) {
            emptyList()
        }

        val currentUser = getCurrentUser()
        val currentUserId = currentUser?.id

        val otherUsersItems = allItems.filter { it.userId != currentUserId }
        val combinedItems = items + otherUsersItems

        val newRawJson = json.encodeToString(ListSerializer(ItemRecord.serializer()), combinedItems)
        storage.putString(KEY_ITEMS, newRawJson)
    }
}
