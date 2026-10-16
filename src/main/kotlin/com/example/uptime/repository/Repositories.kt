package com.example.uptime.repository

import com.example.uptime.domain.CheckResult
import com.example.uptime.domain.Monitor
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface MonitorRepository : JpaRepository<Monitor, Long> {
    fun findByEnabledTrue(): List<Monitor>
}

interface CheckResultRepository : JpaRepository<CheckResult, Long> {
    fun findByMonitorIdOrderByCheckedAtDesc(monitorId: Long, pageable: Pageable): List<CheckResult>

    fun findByMonitorIdAndCheckedAtAfter(monitorId: Long, since: Instant): List<CheckResult>

    @Modifying
    @Query("delete from CheckResult r where r.monitorId = :monitorId")
    fun purgeForMonitor(@Param("monitorId") monitorId: Long): Int

    @Modifying
    @Query("delete from CheckResult r where r.checkedAt < :cutoff")
    fun purgeOlderThan(@Param("cutoff") cutoff: Instant): Int
}
