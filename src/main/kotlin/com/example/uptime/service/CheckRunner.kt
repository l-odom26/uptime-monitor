package com.example.uptime.service

import com.example.uptime.domain.CheckResult
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant

/** Performs a single HTTP check and turns the outcome (or failure) into a [CheckResult]. */
@Component
class CheckRunner {
    private val client: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    fun check(monitorId: Long, url: String, expectedStatus: Int, timeoutMs: Int): CheckResult {
        val startNanos = System.nanoTime()
        return try {
            val request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(timeoutMs.toLong()))
                .header("User-Agent", "uptime-monitor/0.1")
                .GET()
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.discarding())
            val latency = elapsedMs(startNanos)
            val ok = response.statusCode() == expectedStatus
            CheckResult(
                monitorId = monitorId,
                checkedAt = Instant.now(),
                success = ok,
                statusCode = response.statusCode(),
                latencyMs = latency,
                error = if (ok) null else "Expected status $expectedStatus but got ${response.statusCode()}",
            )
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            failure(monitorId, startNanos, "Check interrupted")
        } catch (e: Exception) {
            failure(monitorId, startNanos, (e.message ?: e::class.simpleName ?: "Unknown error").take(500))
        }
    }

    private fun failure(monitorId: Long, startNanos: Long, message: String) = CheckResult(
        monitorId = monitorId,
        checkedAt = Instant.now(),
        success = false,
        statusCode = null,
        latencyMs = elapsedMs(startNanos),
        error = message,
    )

    private fun elapsedMs(startNanos: Long): Long = (System.nanoTime() - startNanos) / 1_000_000
}
