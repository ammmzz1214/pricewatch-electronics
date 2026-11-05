package com.pricewatch.electronics.core.common

import android.util.Patterns
import java.net.URI

object UrlValidator {

    fun isValidUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isEmpty() || trimmed.length > Constants.MAX_URL_LENGTH) return false
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return false
        if (!Patterns.WEB_URL.matcher(trimmed).matches()) return false
        return try {
            val uri = URI(trimmed)
            val host = uri.host ?: return false
            host.contains(".") && host.length >= 4
        } catch (e: Exception) {
            false
        }
    }

    fun hostOf(url: String): String? = try {
        URI(url.trim()).host?.lowercase()
    } catch (e: Exception) {
        null
    }

    fun matchesSiteDomain(url: String, siteDomain: String): Boolean {
        val host = hostOf(url) ?: return false
        // Site domains may carry a path prefix (e.g. dell.com/en-uk);
        // only the host part is compared.
        val domain = siteDomain.lowercase().substringBefore("/").removePrefix("www.")
        val cleanHost = host.removePrefix("www.")
        return cleanHost == domain || cleanHost.endsWith(".$domain")
    }

    fun normalize(url: String): String = url.trim().removeSuffix("/")
}
