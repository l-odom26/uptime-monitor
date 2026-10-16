package com.example.uptime.service

import com.example.uptime.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/** Deletes check results older than the retention window so the table doesn't grow forever. */
@Component
@ConditionalOnProperty(prefix = "app.scheduler", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class RetentionJob(
    private val service: MonitorService,
    private val props: AppProperties,
) {
    private val log = LoggerFactory.getLogger(RetentionJob::class.java)

    @Scheduled(cron = "0 0 3 * * *")
    fun purge() {
        val deleted = service.purgeOlderThan(props.retention.days)
        log.info("Retention: deleted {} check results older than {} days", deleted, props.retention.days)
    }
}
