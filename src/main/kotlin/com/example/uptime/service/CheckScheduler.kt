package com.example.uptime.service

import com.example.uptime.config.AppProperties
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * A single "tick" loop finds monitors that are due and hands each check to a worker pool, so a slow
 * or timing-out target never delays the others. The in-flight set prevents the same monitor from
 * being checked twice concurrently if a check outlasts its interval.
 */
@Component
@ConditionalOnProperty(prefix = "app.scheduler", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class CheckScheduler(
    private val service: MonitorService,
    private val runner: CheckRunner,
    props: AppProperties,
) {
    private val log = LoggerFactory.getLogger(CheckScheduler::class.java)
    private val executor: ExecutorService = Executors.newFixedThreadPool(props.scheduler.workerThreads)
    private val inFlight: MutableSet<Long> = ConcurrentHashMap.newKeySet()

    @Scheduled(fixedDelayString = "\${app.scheduler.tick-ms:1000}")
    fun tick() {
        val now = Instant.now()
        for (monitor in service.findEnabled()) {
            val id = monitor.id ?: continue
            if (!isDue(monitor.lastCheckedAt, monitor.intervalSeconds, now)) continue
            if (!inFlight.add(id)) continue

            // Copy what the worker needs; don't touch the entity from another thread.
            val url = monitor.url
            val expectedStatus = monitor.expectedStatus
            val timeoutMs = monitor.timeoutMs

            executor.execute {
                try {
                    service.recordResult(id, runner.check(id, url, expectedStatus, timeoutMs))
                } catch (e: Exception) {
                    log.error("Check pipeline failed for monitor {}", id, e)
                } finally {
                    inFlight.remove(id)
                }
            }
        }
    }

    @PreDestroy
    fun shutdown() {
        executor.shutdown()
    }

    companion object {
        fun isDue(lastCheckedAt: Instant?, intervalSeconds: Int, now: Instant): Boolean =
            lastCheckedAt == null || !lastCheckedAt.plusSeconds(intervalSeconds.toLong()).isAfter(now)
    }
}
