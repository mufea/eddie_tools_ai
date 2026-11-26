package com.example.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import okhttp3.Request

object BackendProxyClient {
    const val DEFAULT_PROXY_ROOT = "https://geminiaitools.com"

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
        return clean.ifBlank { DEFAULT_PROXY_ROOT }
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
                return raw.trimEnd('/')
            }
        }
        return "${normalizeProxyRoot(baseUrl)}$normalizedPath"
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
