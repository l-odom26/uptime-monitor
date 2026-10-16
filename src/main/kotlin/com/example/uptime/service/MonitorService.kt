package com.example.uptime.service

import com.example.uptime.config.AppProperties
import com.example.uptime.domain.CheckResult
import com.example.uptime.domain.Monitor
import com.example.uptime.domain.MonitorStatus
import com.example.uptime.repository.CheckResultRepository
import com.example.uptime.repository.MonitorRepository
import com.example.uptime.web.MonitorRequest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Service
class MonitorService(
    private val monitors: MonitorRepository,
    private val results: CheckResultRepository,
    private val notifiers: List<AlertNotifier>,
    private val validator: TargetValidator,
    private val props: AppProperties,
) {

    @Transactional
    fun create(req: MonitorRequest): Monitor {
        validator.validate(req.url)
        return monitors.save(
            Monitor(
                name = req.name.trim(),
                url = req.url.trim(),
                intervalSeconds = req.intervalSeconds,
                expectedStatus = req.expectedStatus,
                timeoutMs = req.timeoutMs,
                enabled = req.enabled,
            ),
        )
    }

    @Transactional(readOnly = true)
    fun list(): List<Monitor> = monitors.findAll(Sort.by("id"))

    @Transactional(readOnly = true)
    fun findEnabled(): List<Monitor> = monitors.findByEnabledTrue()

    @Transactional(readOnly = true)
    fun get(id: Long): Monitor = monitors.findById(id).orElseThrow { MonitorNotFoundException(id) }

    @Transactional
    fun update(id: Long, req: MonitorRequest): Monitor {
        validator.validate(req.url)
        val monitor = get(id)
        val newUrl = req.url.trim()
        if (newUrl != monitor.url) {
            // Different target: previous state no longer applies, and it should be checked promptly.
            monitor.status = MonitorStatus.UNKNOWN
            monitor.consecutiveFailures = 0
            monitor.lastCheckedAt = null
        }
        monitor.name = req.name.trim()
        monitor.url = newUrl
        monitor.intervalSeconds = req.intervalSeconds
        monitor.expectedStatus = req.expectedStatus
        monitor.timeoutMs = req.timeoutMs
        monitor.enabled = req.enabled
        return monitor
    }

    @Transactional
    fun delete(id: Long) {
        val monitor = get(id)
        results.purgeForMonitor(id)
        monitors.delete(monitor)
    }

    @Transactional(readOnly = true)
    fun recentResults(id: Long, limit: Int): List<CheckResult> {
        get(id)
        return results.findByMonitorIdOrderByCheckedAtDesc(id, PageRequest.of(0, limit))
    }

    @Transactional(readOnly = true)
    fun stats(id: Long, hours: Long): Stats {
        get(id)
        val since = Instant.now().minus(Duration.ofHours(hours))
        return StatsCalculator.compute(results.findByMonitorIdAndCheckedAtAfter(id, since), hours)
    }

    /** Persists a check result, updates the monitor's state machine, and fires alerts on transitions. */
    @Transactional
    fun recordResult(monitorId: Long, result: CheckResult) {
        val monitor = monitors.findById(monitorId).orElse(null) ?: return // deleted mid-check
        results.save(result)

        val evaluation = StatusEvaluator.evaluate(
            current = monitor.status,
            consecutiveFailures = monitor.consecutiveFailures,
            success = result.success,
            threshold = props.alert.failureThreshold,
        )
        monitor.status = evaluation.newStatus
        monitor.consecutiveFailures = evaluation.consecutiveFailures
        monitor.lastCheckedAt = result.checkedAt

        if (evaluation.transition != Transition.NONE) {
            val event = AlertEvent(
                monitorId = monitorId,
                monitorName = monitor.name,
                url = monitor.url,
                transition = evaluation.transition,
                detail = result.error,
                at = result.checkedAt,
            )
            notifiers.forEach { it.send(event) }
        }
    }

    @Transactional
    fun purgeOlderThan(days: Long): Int =
        results.purgeOlderThan(Instant.now().minus(Duration.ofDays(days)))
}
