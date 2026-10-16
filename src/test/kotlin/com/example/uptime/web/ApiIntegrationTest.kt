package com.example.uptime.web

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

const val TEST_USER = "admin"
const val TEST_PASSWORD = "test-password-123"

/**
 * Shared configuration for tests that boot the full application against in-memory H2 with the
 * scheduler off. Using one annotation keeps the Spring context cached across test classes.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:apitest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.scheduler.enabled=false",
        "app.allow-private-targets=true",
        "app.auth.username=admin",
        "app.auth.password=test-password-123",
    ],
)
@AutoConfigureMockMvc
annotation class ApiIntegrationTest
