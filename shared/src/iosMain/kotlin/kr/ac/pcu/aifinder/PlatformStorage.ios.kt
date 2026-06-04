package kr.ac.pcu.aifinder

import platform.Foundation.NSUserDefaults

actual class PlatformStorage actual constructor(context: Any?) {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getString(key: String, defaultValue: String?): String? {
        return defaults.stringForKey(key) ?: defaultValue
    }

    actual fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }
}
