package kr.ac.pcu.aifinder

import java.util.prefs.Preferences

actual class PlatformStorage actual constructor(context: Any?) {
    private val prefs: Preferences = Preferences.userRoot().node("kr.ac.pcu.aifinder")

    actual fun getString(key: String, defaultValue: String?): String? {
        return prefs.get(key, defaultValue)
    }

    actual fun putString(key: String, value: String) {
        prefs.put(key, value)
    }
}
