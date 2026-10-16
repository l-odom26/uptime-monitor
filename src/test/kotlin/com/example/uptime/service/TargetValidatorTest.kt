package com.example.uptime.service

import com.example.uptime.config.AppProperties
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class TargetValidatorTest {
    private val strict = TargetValidator(AppProperties(allowPrivateTargets = false))
    private val permissive = TargetValidator(AppProperties(allowPrivateTargets = true))

    @Test
    fun `rejects non-http schemes`() {
        assertThrows(InvalidTargetException::class.java) { strict.validate("ftp://example.com/file") }
        assertThrows(InvalidTargetException::class.java) { strict.validate("file:///etc/passwd") }
    }

    @Test
    fun `rejects malformed urls and urls without a host`() {
        assertThrows(InvalidTargetException::class.java) { strict.validate("not a url") }
        assertThrows(InvalidTargetException::class.java) { strict.validate("http://") }
    }

    @Test
    fun `rejects loopback and private addresses by default`() {
        assertThrows(InvalidTargetException::class.java) { strict.validate("http://127.0.0.1:8080/health") }
        assertThrows(InvalidTargetException::class.java) { strict.validate("http://localhost/") }
        assertThrows(InvalidTargetException::class.java) { strict.validate("http://10.0.0.5/") }
        assertThrows(InvalidTargetException::class.java) { strict.validate("http://192.168.1.1/") }
        assertThrows(InvalidTargetException::class.java) { strict.validate("http://169.254.169.254/latest/meta-data") }
    }

    @Test
    fun `accepts a public ip literal`() {
        assertDoesNotThrow { strict.validate("https://93.184.216.34/") }
    }

    @Test
    fun `private targets are allowed when explicitly enabled`() {
        assertDoesNotThrow { permissive.validate("http://127.0.0.1:8080/health") }
    }
}
