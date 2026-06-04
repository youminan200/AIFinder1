package kr.ac.pcu.aifinder

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class RoomArea(
    val id: Int,
    var name: String
)

data class ItemRecord(
    val id: String,
    val name: String,
    val areaId: Int,
    var areaName: String,
    val timestamp: Long,
    val photoUri: String?,
    val boundingBox: String?, // Format: "left,top,right,bottom"
    var isFavorite: Boolean = false
)

class ItemStorage(context: Context) {

    private val gson = Gson()
    private val itemPrefs: SharedPreferences = context.getSharedPreferences("item_storage_records", Context.MODE_PRIVATE)
    private val areaPrefs: SharedPreferences = context.getSharedPreferences("room_area_records", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ITEMS = "items_list"
        private const val KEY_AREAS = "areas_list"
    }

    // Default 6 Room Areas
    private val defaultAreas = listOf(
        RoomArea(1, "침대 구역"),
        RoomArea(2, "책상 구역"),
        RoomArea(3, "옷장 구역"),
        RoomArea(4, "현관 구역"),
        RoomArea(5, "선반 구역"),
        RoomArea(6, "창가 구역")
    )

    fun getRoomAreas(): List<RoomArea> {
        val json = areaPrefs.getString(KEY_AREAS, null) ?: return defaultAreas.also { saveRoomAreas(it) }
        val type = object : TypeToken<List<RoomArea>>() {}.type
        return try {
            gson.fromJson(json, type) ?: defaultAreas
        } catch (e: Exception) {
            defaultAreas
        }
    }

    fun saveRoomAreas(areas: List<RoomArea>) {
        val json = gson.toJson(areas)
        areaPrefs.edit().putString(KEY_AREAS, json).apply()
    }

    fun renameArea(id: Int, newName: String) {
        val areas = getRoomAreas().map {
            if (it.id == id) it.copy(name = newName) else it
        }
        saveRoomAreas(areas)

        // Cascade rename to existing items
        val items = getItems().map {
            if (it.areaId == id) it.copy(areaName = newName) else it
        }
        saveItems(items)
    }

    fun getItems(): List<ItemRecord> {
        val json = itemPrefs.getString(KEY_ITEMS, null) ?: return emptyList()
        val type = object : TypeToken<List<ItemRecord>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveItems(items: List<ItemRecord>) {
        val json = gson.toJson(items)
        itemPrefs.edit().putString(KEY_ITEMS, json).apply()
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

    // Get statistics for last 7 days: Map of "Area Name" to Count
    fun getRecent7DaysStats(): Map<String, Int> {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val recentItems = getItems().filter { it.timestamp >= sevenDaysAgo }
        
        // Group by area name and count
        return recentItems.groupBy { it.areaName }.mapValues { it.value.size }
    }
}
