package com.example.uptime.service

import com.example.uptime.domain.CheckResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class StatsCalculatorTest {

    private fun result(success: Boolean, latencyMs: Long) = CheckResult(
        monitorId = 1,
        checkedAt = Instant.now(),
        success = success,
        statusCode = if (success) 200 else 500,
        latencyMs = latencyMs,
        error = null,
    )

    @Test
    fun `empty window yields null metrics`() {
        val stats = StatsCalculator.compute(emptyList(), 24)

        assertEquals(0, stats.totalChecks)
        assertNull(stats.uptimePercent)
        assertNull(stats.avgLatencyMs)
        assertNull(stats.p95LatencyMs)
    }

    @Test
    fun `uptime is the share of successful checks rounded to two decimals`() {
        val stats = StatsCalculator.compute(
            listOf(result(true, 100), result(true, 200), result(false, 5000)),
            24,
        )

        assertEquals(3, stats.totalChecks)
        assertEquals(2, stats.successfulChecks)
        assertEquals(66.67, stats.uptimePercent)
    }

    @Test
    fun `latency metrics only consider successful checks`() {
        val stats = StatsCalculator.compute(
            listOf(result(true, 100), result(true, 300), result(false, 9999)),
            24,
        )

        assertEquals(200.0, stats.avgLatencyMs)
        assertEquals(300L, stats.p95LatencyMs)
    }

    @Test
    fun `percentile uses nearest rank`() {
        val values = (1L..100L).toList()

        assertEquals(95L, StatsCalculator.percentile(values, 95.0))
        assertEquals(50L, StatsCalculator.percentile(values, 50.0))
        assertEquals(100L, StatsCalculator.percentile(values, 100.0))
        assertEquals(7L, StatsCalculator.percentile(listOf(7L), 95.0))
        assertNull(StatsCalculator.percentile(emptyList(), 95.0))
    }
}
