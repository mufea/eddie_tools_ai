package com.example.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import okhttp3.Request

object BackendProxyClient {
    const val DEFAULT_PROXY_ROOT = "https://geminiaitools.com"

    @Volatile
    private var lastKnownProxyRoot: String = DEFAULT_PROXY_ROOT

    fun normalizeProxyRoot(baseUrl: String?): String {
        val raw = baseUrl?.trim().orEmpty()
        if (raw.isBlank()) return DEFAULT_PROXY_ROOT

        val parsed = runCatching { Uri.parse(raw) }.getOrNull()
        val scheme = parsed?.scheme?.lowercase()
        val host = parsed?.host?.lowercase()
        if (scheme != "https" || host.isNullOrBlank()) {
            return DEFAULT_PROXY_ROOT
        }

        var clean = raw.trimEnd('/')
        val knownSuffixes = listOf(
            "/v1/chat/completions",
            "/v1/images/generations",
            "/v1/videos",
            "/api/chat",
            "/api/image",
            "/api/video",
            "/api/music",
            "/api/keys",
            "/api/topup-packages"
        )
        for (suffix in knownSuffixes) {
            if (clean.endsWith(suffix, ignoreCase = true)) {
                clean = clean.substring(0, clean.length - suffix.length).trimEnd('/')
            }
        }
        val normalized = clean.ifBlank { DEFAULT_PROXY_ROOT }
        lastKnownProxyRoot = normalized
        return normalized
    }

    fun resolveEndpoint(baseUrl: String?, path: String): String {
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        val raw = baseUrl?.trim().orEmpty()
        if (raw.isNotBlank()) {
            val parsed = runCatching { Uri.parse(raw) }.getOrNull()
            if (parsed?.scheme?.equals("https", ignoreCase = true) == true &&
                !parsed.host.isNullOrBlank() &&
                raw.trimEnd('/').endsWith(normalizedPath, ignoreCase = true)
            ) {
                normalizeProxyRoot(raw)
                return raw.trimEnd('/')
            }
        }
        return "${normalizeProxyRoot(baseUrl)}$normalizedPath"
    }

    fun sanitizeForDisplay(text: String, customProxyUrl: String? = null): String {
        if (text.isBlank()) return text
        var result = text

        val candidateRoots = buildSet {
            add(DEFAULT_PROXY_ROOT)
            add(lastKnownProxyRoot)
            if (!customProxyUrl.isNullOrBlank()) {
                add(normalizeProxyRoot(customProxyUrl))
            }
        }

        val candidateHosts = candidateRoots.mapNotNull { root ->
            runCatching { Uri.parse(root).host?.lowercase()?.takeIf { it.isNotBlank() } }.getOrNull()
        }.toSet()

        for (host in candidateHosts) {
            val escapedHost = Regex.escape(host)
            result = result.replace(
                Regex("https?://[^/\\s\"'\\])>]*$escapedHost(?::\\d+)?(?:/[^\\s\"'\\])>]*)?", RegexOption.IGNORE_CASE),
                "[secure-proxy]"
            )
            result = result.replace(
                Regex(escapedHost, RegexOption.IGNORE_CASE),
                "secure-proxy"
            )
        }

        // Mask any proxy endpoint URL pattern even if host wasn't registered yet
        result = result.replace(
            Regex(
                "https?://[^/\\s\"'\\])>]+/(?:api/(?:chat|image|video|music|keys|topup-packages)|v1/(?:chat/completions|images/generations|videos))(?:/[^\\s\"'\\])>]*)?(?:\\?[^\\s\"'\\])>]*)?",
                RegexOption.IGNORE_CASE
            ),
            "[secure-proxy]"
        )

        return result
    }

    suspend fun getFirebaseIdToken(): String? {
        return runCatching {
            FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    fun Request.Builder.withProxyAuth(
        authScheme: String = "Bearer",
        firebaseIdToken: String? = null
    ): Request.Builder {
        header("Authorization", "$authScheme proxy_active")
        if (!firebaseIdToken.isNullOrBlank()) {
            header("X-Firebase-Auth", firebaseIdToken)
        }
        return this
    }
}
