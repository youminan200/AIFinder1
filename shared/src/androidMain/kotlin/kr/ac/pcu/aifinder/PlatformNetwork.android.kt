package kr.ac.pcu.aifinder

import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

actual class PlatformNetwork actual constructor() {
    actual suspend fun post(url: String, jsonBody: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        
        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(jsonBody)
            writer.flush()
        }
        
        val responseCode = connection.responseCode
        if (responseCode in 200..299) {
            return connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw Exception("HTTP Error $responseCode: $errorResponse")
        }
    }

    actual suspend fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.setRequestProperty("Accept", "application/json")
        
        val responseCode = connection.responseCode
        if (responseCode in 200..299) {
            return connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw Exception("HTTP Error $responseCode: $errorResponse")
        }
    }
}
