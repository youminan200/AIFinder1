package kr.ac.pcu.aifinder

import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

class ItemStorage(private val storage: PlatformStorage) {

    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    companion object {
        private const val KEY_ITEMS = "items_list"
        private const val KEY_AREAS = "areas_list"
    }

    private val defaultAreas = listOf(
        RoomArea(1, "침대 구역"),
        RoomArea(2, "책상 구역"),
        RoomArea(3, "옷장 구역"),
        RoomArea(4, "현관 구역"),
        RoomArea(5, "선반 구역"),
        RoomArea(6, "창가 구역")
    )

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

    fun getItems(): List<ItemRecord> {
        val rawJson = storage.getString(KEY_ITEMS, null) ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(ItemRecord.serializer()), rawJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveItems(items: List<ItemRecord>) {
        val rawJson = json.encodeToString(ListSerializer(ItemRecord.serializer()), items)
        storage.putString(KEY_ITEMS, rawJson)
    }

    fun addItem(item: ItemRecord) {
        val items = getItems().toMutableList()
        items.add(0, item)
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
}
