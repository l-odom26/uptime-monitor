package com.example.uptime.web

import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ApiIntegrationTest
class SecurityTest(@Autowired private val mvc: MockMvc) {

    @Test
    fun `the api rejects requests without credentials`() {
        mvc.perform(get("/api/monitors")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `the 401 carries a basic auth challenge so browsers show a login prompt`() {
        mvc.perform(get("/api/monitors"))
            .andExpect(status().isUnauthorized)
            .andExpect(header().string("WWW-Authenticate", startsWith("Basic")))
    }

    @Test
    fun `a wrong password is rejected`() {
        mvc.perform(get("/api/monitors").with(httpBasic(TEST_USER, "not-the-password")))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `a wrong username is rejected`() {
        mvc.perform(get("/api/monitors").with(httpBasic("someone-else", TEST_PASSWORD)))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `correct credentials are accepted`() {
        mvc.perform(get("/api/monitors").with(httpBasic(TEST_USER, TEST_PASSWORD)))
            .andExpect(status().isOk)
    }

    @Test
    fun `an unauthenticated create is rejected and creates nothing`() {
        mvc.perform(
            post("/api/monitors")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Intruder","url":"https://example.com"}"""),
        ).andExpect(status().isUnauthorized)

        val listing = mvc.perform(get("/api/monitors").with(httpBasic(TEST_USER, TEST_PASSWORD)))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        assertFalse(listing.contains("Intruder"))
    }

    @Test
    fun `an unauthenticated delete is rejected`() {
        mvc.perform(delete("/api/monitors/1")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `the dashboard page requires a login too`() {
        mvc.perform(get("/index.html")).andExpect(status().isUnauthorized)
        mvc.perform(get("/index.html").with(httpBasic(TEST_USER, TEST_PASSWORD)))
            .andExpect(status().isOk)
    }

    @Test
    fun `the health endpoint stays public for hosting platform probes`() {
        mvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
    }
}
