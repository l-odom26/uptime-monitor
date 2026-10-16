package com.example.uptime.service

import com.example.uptime.domain.CheckResult
import kotlin.math.ceil

data class Stats(
    val windowHours: Long,
    val totalChecks: Int,
    val successfulChecks: Int,
    val uptimePercent: Double?,
    val avgLatencyMs: Double?,
    val p95LatencyMs: Long?,
)

object StatsCalculator {
    fun compute(results: List<CheckResult>, windowHours: Long): Stats {
        val total = results.size
        val successes = results.filter { it.success }
        val latencies = successes.map { it.latencyMs }.sorted()

        return Stats(
            windowHours = windowHours,
            totalChecks = total,
            successfulChecks = successes.size,
            uptimePercent = if (total == 0) null else round2(successes.size * 100.0 / total),
            avgLatencyMs = if (latencies.isEmpty()) null else round2(latencies.average()),
            p95LatencyMs = percentile(latencies, 95.0),
        )
    }

    /** Nearest-rank percentile over an already-sorted list. */
    fun percentile(sorted: List<Long>, p: Double): Long? {
        if (sorted.isEmpty()) return null
        val rank = ceil(p / 100.0 * sorted.size).toInt().coerceIn(1, sorted.size)
        return sorted[rank - 1]
    }

    private fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0
}
