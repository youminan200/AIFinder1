package kr.ac.pcu.aifinder

import android.content.Context
import android.content.SharedPreferences

actual class PlatformStorage actual constructor(context: Any?) {
    private val ctx = context as Context
    private val areaPrefs: SharedPreferences = ctx.getSharedPreferences("room_area_records", Context.MODE_PRIVATE)
    private val itemPrefs: SharedPreferences = ctx.getSharedPreferences("item_storage_records", Context.MODE_PRIVATE)

    actual fun getString(key: String, defaultValue: String?): String? {
        val prefs = getPrefs(key)
        return prefs.getString(key, defaultValue)
    }

    actual fun putString(key: String, value: String) {
        val prefs = getPrefs(key)
        prefs.edit().putString(key, value).apply()
    }

    private fun getPrefs(key: String): SharedPreferences {
        return if (key == "areas_list") {
            areaPrefs
        } else {
            itemPrefs
        }
    }
}
