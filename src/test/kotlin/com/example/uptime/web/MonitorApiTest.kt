package com.example.uptime.web

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:apitest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.scheduler.enabled=false",
        "app.allow-private-targets=true",
    ],
)
@AutoConfigureMockMvc
class MonitorApiTest(
    @Autowired private val mvc: MockMvc,
    @Autowired private val mapper: ObjectMapper,
) {

    private fun createMonitor(name: String = "Example", url: String = "https://example.com"): Long {
        val body = """{"name":"$name","url":"$url","intervalSeconds":30}"""
        val response = mvc.perform(post("/api/monitors").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated)
            .andReturn().response.contentAsString
        return mapper.readTree(response).get("id").asLong()
    }

    @Test
    fun `creating a monitor returns 201 with defaults applied`() {
        mvc.perform(
            post("/api/monitors")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Docs","url":"https://example.com/docs"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Docs"))
            .andExpect(jsonPath("$.status").value("UNKNOWN"))
            .andExpect(jsonPath("$.intervalSeconds").value(60))
            .andExpect(jsonPath("$.expectedStatus").value(200))
    }

    @Test
    fun `invalid input is rejected with 400`() {
        mvc.perform(
            post("/api/monitors")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"","url":"https://example.com","intervalSeconds":5}"""),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `non-http urls are rejected with 400 and a problem detail`() {
        mvc.perform(
            post("/api/monitors")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Bad","url":"ftp://example.com"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("Only http and https URLs are supported"))
    }

    @Test
    fun `unknown monitor returns 404`() {
        mvc.perform(get("/api/monitors/999999")).andExpect(status().isNotFound)
    }

    @Test
    fun `stats for a monitor with no checks are empty rather than an error`() {
        val id = createMonitor("Fresh")

        mvc.perform(get("/api/monitors/$id/stats"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalChecks").value(0))
            .andExpect(jsonPath("$.uptimePercent").doesNotExist())
    }

    @Test
    fun `deleting a monitor removes it`() {
        val id = createMonitor("Temporary")

        mvc.perform(delete("/api/monitors/$id")).andExpect(status().isNoContent)
        mvc.perform(get("/api/monitors/$id")).andExpect(status().isNotFound)
    }

    @Test
    fun `listing returns created monitors`() {
        val id = createMonitor("Listed")

        val body = mvc.perform(get("/api/monitors")).andExpect(status().isOk)
            .andReturn().response.contentAsString
        val ids = mapper.readTree(body).map { it.get("id").asLong() }

        assertEquals(true, ids.contains(id))
    }
}
