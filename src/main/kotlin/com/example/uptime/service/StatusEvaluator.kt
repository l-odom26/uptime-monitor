package com.example.uptime.service

import com.example.uptime.domain.MonitorStatus

enum class Transition { NONE, WENT_DOWN, RECOVERED }

data class Evaluation(
    val newStatus: MonitorStatus,
    val consecutiveFailures: Int,
    val transition: Transition,
)

/**
 * Pure state-machine logic, kept free of Spring/JPA so it is trivial to unit test.
 *
 * A monitor only flips to DOWN after [threshold] consecutive failures (avoids alerting on a
 * single blip) and only flips back to UP on the first success.
 */
object StatusEvaluator {
    fun evaluate(
        current: MonitorStatus,
        consecutiveFailures: Int,
        success: Boolean,
        threshold: Int,
    ): Evaluation {
        if (success) {
            val transition = if (current == MonitorStatus.DOWN) Transition.RECOVERED else Transition.NONE
            return Evaluation(MonitorStatus.UP, 0, transition)
        }

        val failures = consecutiveFailures + 1
        return if (failures >= threshold) {
            val transition = if (current != MonitorStatus.DOWN) Transition.WENT_DOWN else Transition.NONE
            Evaluation(MonitorStatus.DOWN, failures, transition)
        } else {
            Evaluation(current, failures, Transition.NONE)
        }
    }
}
