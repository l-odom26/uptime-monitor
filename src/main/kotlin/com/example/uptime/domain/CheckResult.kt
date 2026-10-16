package com.example.uptime.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "check_results",
    indexes = [Index(name = "idx_results_monitor_time", columnList = "monitor_id,checked_at")],
)
class CheckResult(
    @Column(name = "monitor_id", nullable = false)
    val monitorId: Long,

    @Column(name = "checked_at", nullable = false)
    val checkedAt: Instant,

    @Column(nullable = false)
    val success: Boolean,

    val statusCode: Int?,

    @Column(nullable = false)
    val latencyMs: Long,

    @Column(length = 500)
    val error: String?,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
