package kr.ac.pcu.aifinder

expect class PlatformNetwork() {
    suspend fun post(url: String, jsonBody: String): String
    suspend fun get(url: String): String
}
