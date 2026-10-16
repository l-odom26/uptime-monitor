package com.example.uptime.web

import com.example.uptime.domain.CheckResult
import com.example.uptime.domain.Monitor
import com.example.uptime.domain.MonitorStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class MonitorRequest(
    @field:NotBlank @field:Size(max = 100)
    val name: String,

    @field:NotBlank @field:Size(max = 2048)
    val url: String,

    @field:Min(10) @field:Max(86_400)
    val intervalSeconds: Int = 60,

    @field:Min(100) @field:Max(599)
    val expectedStatus: Int = 200,

    @field:Min(500) @field:Max(30_000)
    val timeoutMs: Int = 5000,

    val enabled: Boolean = true,
)

data class MonitorResponse(
    val id: Long,
    val name: String,
    val url: String,
    val intervalSeconds: Int,
    val expectedStatus: Int,
    val timeoutMs: Int,
    val enabled: Boolean,
    val status: MonitorStatus,
    val consecutiveFailures: Int,
    val lastCheckedAt: Instant?,
    val createdAt: Instant,
) {
    companion object {
        fun from(m: Monitor) = MonitorResponse(
            id = m.id!!,
            name = m.name,
            url = m.url,
            intervalSeconds = m.intervalSeconds,
            expectedStatus = m.expectedStatus,
            timeoutMs = m.timeoutMs,
            enabled = m.enabled,
            status = m.status,
            consecutiveFailures = m.consecutiveFailures,
            lastCheckedAt = m.lastCheckedAt,
            createdAt = m.createdAt,
        )
    }
}

data class CheckResultResponse(
    val checkedAt: Instant,
    val success: Boolean,
    val statusCode: Int?,
    val latencyMs: Long,
    val error: String?,
) {
    companion object {
        fun from(r: CheckResult) = CheckResultResponse(
            checkedAt = r.checkedAt,
            success = r.success,
            statusCode = r.statusCode,
            latencyMs = r.latencyMs,
            error = r.error,
        )
    }
}
