package kr.ac.pcu.aifinder

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.concurrent.Executors

private val networkExecutor = Executors.newCachedThreadPool()

actual class PlatformNetwork actual constructor() {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    actual suspend fun post(url: String, jsonBody: String): String = suspendCoroutine { continuation ->
        networkExecutor.submit {
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(5))
                    .build()
                    
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() in 200..299) {
                    continuation.resume(response.body())
                } else {
                    continuation.resumeWithException(Exception("HTTP Error ${response.statusCode()}: ${response.body()}"))
                }
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    actual suspend fun get(url: String): String = suspendCoroutine { continuation ->
        networkExecutor.submit {
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build()
                    
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() in 200..299) {
                    continuation.resume(response.body())
                } else {
                    continuation.resumeWithException(Exception("HTTP Error ${response.statusCode()}: ${response.body()}"))
                }
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }
}
