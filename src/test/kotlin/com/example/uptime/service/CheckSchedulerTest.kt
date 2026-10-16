package com.example.uptime.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class CheckSchedulerTest {
    private val now = Instant.parse("2026-01-01T12:00:00Z")

    @Test
    fun `a monitor that has never been checked is due`() {
        assertTrue(CheckScheduler.isDue(null, 60, now))
    }

    @Test
    fun `a monitor is not due before its interval has elapsed`() {
        assertFalse(CheckScheduler.isDue(now.minusSeconds(30), 60, now))
    }

    @Test
    fun `a monitor is due exactly when its interval has elapsed`() {
        assertTrue(CheckScheduler.isDue(now.minusSeconds(60), 60, now))
    }

    @Test
    fun `a monitor is due after its interval has passed`() {
        assertTrue(CheckScheduler.isDue(now.minusSeconds(300), 60, now))
    }
}
