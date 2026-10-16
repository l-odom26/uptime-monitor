package com.example.uptime.service

import com.example.uptime.config.AppProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant

data class AlertEvent(
    val monitorId: Long,
    val monitorName: String,
    val url: String,
    val transition: Transition,
    val detail: String?,
    val at: Instant,
) {
    fun message(): String = when (transition) {
        Transition.WENT_DOWN -> "[DOWN] $monitorName ($url)" + (detail?.let { " - $it" } ?: "")
        Transition.RECOVERED -> "[RECOVERED] $monitorName ($url)"
        Transition.NONE -> "$monitorName: no change"
    }
}

interface AlertNotifier {
    fun send(event: AlertEvent)
}

@Component
class LoggingAlertNotifier : AlertNotifier {
    private val log = LoggerFactory.getLogger(LoggingAlertNotifier::class.java)

    override fun send(event: AlertEvent) {
        log.warn("ALERT {}", event.message())
    }
}

/** Posts alerts to a webhook (Slack/Discord-compatible payload). No-op when no URL is configured. */
@Component
class WebhookAlertNotifier(
    private val props: AppProperties,
    private val mapper: ObjectMapper,
) : AlertNotifier {
    private val log = LoggerFactory.getLogger(WebhookAlertNotifier::class.java)
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

    override fun send(event: AlertEvent) {
        val webhookUrl = props.alert.webhookUrl
        if (webhookUrl.isBlank()) return

        try {
            val text = event.message()
            val body = mapper.writeValueAsString(
                mapOf(
                    "text" to text,      // Slack
                    "content" to text,   // Discord
                    "monitorId" to event.monitorId,
                    "type" to event.transition.name,
                    "at" to event.at.toString(),
                ),
            )
            val request = HttpRequest.newBuilder(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.discarding())
            if (response.statusCode() !in 200..299) {
                log.warn("Webhook returned HTTP {}", response.statusCode())
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (e: Exception) {
            // An alerting failure must never break the check pipeline.
            log.warn("Webhook alert failed: {}", e.message)
        }
    }
}
