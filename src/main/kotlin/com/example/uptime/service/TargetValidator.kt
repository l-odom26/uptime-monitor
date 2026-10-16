package com.example.uptime.service

import com.example.uptime.config.AppProperties
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.net.URI
import java.net.URISyntaxException
import java.net.UnknownHostException

/**
 * Validates monitor target URLs. A service that makes HTTP requests to user-supplied URLs is an
 * SSRF risk, so by default we reject non-HTTP(S) schemes and hosts that resolve to loopback,
 * private, or link-local addresses.
 *
 * Known limitation: this checks DNS at creation time only (see README "Security notes").
 */
@Component
class TargetValidator(private val props: AppProperties) {

    fun validate(url: String) {
        val uri = try {
            URI(url.trim())
        } catch (e: URISyntaxException) {
            throw InvalidTargetException("Malformed URL")
        }

        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            throw InvalidTargetException("Only http and https URLs are supported")
        }
        val host = uri.host ?: throw InvalidTargetException("URL must include a host")

        if (props.allowPrivateTargets) return

        val addresses = try {
            InetAddress.getAllByName(host)
        } catch (e: UnknownHostException) {
            return // can't verify now; the host may simply be down, which is what we monitor
        }
        if (addresses.any { it.isLoopbackAddress || it.isSiteLocalAddress || it.isLinkLocalAddress || it.isAnyLocalAddress }) {
            throw InvalidTargetException("Targets on loopback or private networks are not allowed")
        }
    }
}
