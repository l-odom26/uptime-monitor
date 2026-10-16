package com.example.uptime.service

import com.example.uptime.domain.MonitorStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StatusEvaluatorTest {

    @Test
    fun `a single failure below the threshold does not flip the status`() {
        val result = StatusEvaluator.evaluate(MonitorStatus.UP, 0, success = false, threshold = 3)

        assertEquals(MonitorStatus.UP, result.newStatus)
        assertEquals(1, result.consecutiveFailures)
        assertEquals(Transition.NONE, result.transition)
    }

    @Test
    fun `reaching the threshold flips to DOWN exactly once`() {
        val first = StatusEvaluator.evaluate(MonitorStatus.UP, 2, success = false, threshold = 3)
        assertEquals(MonitorStatus.DOWN, first.newStatus)
        assertEquals(Transition.WENT_DOWN, first.transition)

        val second = StatusEvaluator.evaluate(first.newStatus, first.consecutiveFailures, success = false, threshold = 3)
        assertEquals(MonitorStatus.DOWN, second.newStatus)
        assertEquals(4, second.consecutiveFailures)
        assertEquals(Transition.NONE, second.transition)
    }

    @Test
    fun `first success after DOWN recovers and resets the failure count`() {
        val result = StatusEvaluator.evaluate(MonitorStatus.DOWN, 5, success = true, threshold = 3)

        assertEquals(MonitorStatus.UP, result.newStatus)
        assertEquals(0, result.consecutiveFailures)
        assertEquals(Transition.RECOVERED, result.transition)
    }

    @Test
    fun `first success on a new monitor goes UP without an alert`() {
        val result = StatusEvaluator.evaluate(MonitorStatus.UNKNOWN, 0, success = true, threshold = 3)

        assertEquals(MonitorStatus.UP, result.newStatus)
        assertEquals(Transition.NONE, result.transition)
    }

    @Test
    fun `a success mid-streak resets failures so earlier blips do not accumulate`() {
        val afterSuccess = StatusEvaluator.evaluate(MonitorStatus.UP, 2, success = true, threshold = 3)
        val nextFailure = StatusEvaluator.evaluate(afterSuccess.newStatus, afterSuccess.consecutiveFailures, success = false, threshold = 3)

        assertEquals(1, nextFailure.consecutiveFailures)
        assertEquals(MonitorStatus.UP, nextFailure.newStatus)
    }
}
