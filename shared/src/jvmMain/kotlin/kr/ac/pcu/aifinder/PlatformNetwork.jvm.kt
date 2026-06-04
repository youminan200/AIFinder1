package kr.ac.pcu.aifinder

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

actual class PlatformNetwork actual constructor() {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    actual suspend fun post(url: String, jsonBody: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .timeout(Duration.ofSeconds(5))
            .build()
            
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() in 200..299) {
            return response.body()
        } else {
            throw Exception("HTTP Error ${response.statusCode()}: ${response.body()}")
        }
    }

    actual suspend fun get(url: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .timeout(Duration.ofSeconds(5))
            .build()
            
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() in 200..299) {
            return response.body()
        } else {
            throw Exception("HTTP Error ${response.statusCode()}: ${response.body()}")
        }
    }
}
