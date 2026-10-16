package com.example.uptime.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

/**
 * Protects the dashboard and API with a single admin login (HTTP Basic).
 *
 * Browsers show a native login prompt on the 401 challenge and then reuse the credentials for the
 * dashboard's API calls. Only the health endpoint is public, so hosting platforms can probe it.
 *
 * Basic auth sends credentials on every request, so only expose this behind HTTPS.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                    .anyRequest().authenticated()
            }
            .httpBasic(Customizer.withDefaults())
            // No cookies or sessions are used: credentials travel with each request. The API only
            // accepts JSON and defines no CORS rules, so cross-site pages cannot forge requests.
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun userDetailsService(props: AppProperties, encoder: PasswordEncoder): UserDetailsService {
        val password = props.auth.password
        // Fail fast: never start up unprotected or with a guessable built-in default.
        check(password.length >= MIN_PASSWORD_LENGTH) {
            "app.auth.password (env var ADMIN_PASSWORD) must be set to at least $MIN_PASSWORD_LENGTH characters"
        }
        val admin = User.withUsername(props.auth.username)
            .password(encoder.encode(password))
            .roles("ADMIN")
            .build()
        return InMemoryUserDetailsManager(admin)
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
