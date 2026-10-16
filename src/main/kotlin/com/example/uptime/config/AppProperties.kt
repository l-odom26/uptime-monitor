package com.example.uptime.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val scheduler: Scheduler = Scheduler(),
    val retention: Retention = Retention(),
    val alert: Alert = Alert(),
    val allowPrivateTargets: Boolean = false,
) {
    data class Scheduler(
        val enabled: Boolean = true,
        val tickMs: Long = 1000,
        val workerThreads: Int = 8,
    )

    data class Retention(val days: Long = 30)

    data class Alert(
        val failureThreshold: Int = 3,
        val webhookUrl: String = "",
    )
}
