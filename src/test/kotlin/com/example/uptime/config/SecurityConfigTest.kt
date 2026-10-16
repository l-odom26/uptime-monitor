package com.example.uptime.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

/** Plain unit tests: no Spring context needed. */
class SecurityConfigTest {
    private val config = SecurityConfig()
    private val encoder = BCryptPasswordEncoder()

    private fun props(password: String, username: String = "admin") =
        AppProperties(auth = AppProperties.Auth(username = username, password = password))

    @Test
    fun `startup fails when no password is configured`() {
        val error = assertThrows(IllegalStateException::class.java) {
            config.userDetailsService(props(""), encoder)
        }
        assertTrue(error.message!!.contains("ADMIN_PASSWORD"))
    }

    @Test
    fun `startup fails when the password is too short`() {
        assertThrows(IllegalStateException::class.java) {
            config.userDetailsService(props("short"), encoder)
        }
    }

    @Test
    fun `a valid password produces an admin user whose password is stored hashed`() {
        val users = config.userDetailsService(props("correct-horse-battery", username = "luke"), encoder)

        val user = users.loadUserByUsername("luke")

        assertEquals("luke", user.username)
        assertNotEquals("correct-horse-battery", user.password)
        assertTrue(encoder.matches("correct-horse-battery", user.password))
    }

    @Test
    fun `the password never appears when properties are printed`() {
        val printed = props("super-secret-value").toString()

        assertTrue(!printed.contains("super-secret-value"))
    }
}
