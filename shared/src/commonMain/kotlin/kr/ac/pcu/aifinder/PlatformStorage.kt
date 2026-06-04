package kr.ac.pcu.aifinder

expect class PlatformStorage(context: Any?) {
    fun getString(key: String, defaultValue: String?): String?
    fun putString(key: String, value: String)
}

