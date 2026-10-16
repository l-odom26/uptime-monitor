package com.example.uptime.service

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.Executors

/** Exercises the real HTTP client against a throwaway local server, so no network access is needed. */
class CheckRunnerTest {
    private lateinit var server: HttpServer
    private val runner = CheckRunner()

    @BeforeEach
    fun startServer() {
        server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.executor = Executors.newCachedThreadPool()
        server.createContext("/ok") { ex -> ex.sendResponseHeaders(200, -1); ex.close() }
        server.createContext("/boom") { ex -> ex.sendResponseHeaders(500, -1); ex.close() }
        server.createContext("/slow") { ex -> Thread.sleep(1500); ex.sendResponseHeaders(200, -1); ex.close() }
        server.start()
    }

    @AfterEach
    fun stopServer() {
        server.stop(0)
        (server.executor as java.util.concurrent.ExecutorService).shutdownNow()
    }

    private fun url(path: String) = "http://127.0.0.1:${server.address.port}$path"

    @Test
    fun `matching status code is a success`() {
        val result = runner.check(1, url("/ok"), expectedStatus = 200, timeoutMs = 2000)

        assertTrue(result.success)
        assertEquals(200, result.statusCode)
        assertNull(result.error)
    }

    @Test
    fun `unexpected status code is a failure with a helpful message`() {
        val result = runner.check(1, url("/boom"), expectedStatus = 200, timeoutMs = 2000)

        assertFalse(result.success)
        assertEquals(500, result.statusCode)
        assertEquals("Expected status 200 but got 500", result.error)
    }

    @Test
    fun `expected status can be configured`() {
        val result = runner.check(1, url("/boom"), expectedStatus = 500, timeoutMs = 2000)

        assertTrue(result.success)
    }

    @Test
    fun `a response slower than the timeout is a failure`() {
        val result = runner.check(1, url("/slow"), expectedStatus = 200, timeoutMs = 500)

        assertFalse(result.success)
        assertNull(result.statusCode)
        assertNotNull(result.error)
    }

    @Test
    fun `connection refused is a failure rather than an exception`() {
        // Grab a free port and release it immediately, so nothing is listening there.
        val port = java.net.ServerSocket(0).use { it.localPort }

        val result = runner.check(1, "http://127.0.0.1:$port/ok", expectedStatus = 200, timeoutMs = 2000)

        assertFalse(result.success)
        assertNull(result.statusCode)
        assertNotNull(result.error)
    }
}
