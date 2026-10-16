package com.example.uptime.web

import com.example.uptime.service.MonitorService
import com.example.uptime.service.Stats
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/monitors")
class MonitorController(private val service: MonitorService) {

    @PostMapping
    fun create(@Valid @RequestBody req: MonitorRequest): ResponseEntity<MonitorResponse> {
        val created = MonitorResponse.from(service.create(req))
        return ResponseEntity.created(URI.create("/api/monitors/${created.id}")).body(created)
    }

    @GetMapping
    fun list(): List<MonitorResponse> = service.list().map { MonitorResponse.from(it) }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): MonitorResponse = MonitorResponse.from(service.get(id))

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody req: MonitorRequest): MonitorResponse =
        MonitorResponse.from(service.update(id, req))

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/results")
    fun results(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "50") limit: Int,
    ): List<CheckResultResponse> =
        service.recentResults(id, limit.coerceIn(1, 500)).map { CheckResultResponse.from(it) }

    @GetMapping("/{id}/stats")
    fun stats(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "24") hours: Long,
    ): Stats = service.stats(id, hours.coerceIn(1, 720))
}
