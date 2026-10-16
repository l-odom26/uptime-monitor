package com.example.uptime.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

enum class MonitorStatus { UNKNOWN, UP, DOWN }

@Entity
@Table(name = "monitors")
class Monitor(
    @Column(nullable = false)
    var name: String,

    @Column(nullable = false, length = 2048)
    var url: String,

    @Column(nullable = false)
    var intervalSeconds: Int = 60,

    @Column(nullable = false)
    var expectedStatus: Int = 200,

    @Column(nullable = false)
    var timeoutMs: Int = 5000,

    @Column(nullable = false)
    var enabled: Boolean = true,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: MonitorStatus = MonitorStatus.UNKNOWN

    @Column(nullable = false)
    var consecutiveFailures: Int = 0

    var lastCheckedAt: Instant? = null

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
}
