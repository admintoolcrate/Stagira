package com.stagirite.ledger.beta

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.stagirite.ledger.BuildConfig
import com.stagirite.ledger.feedback.FeedbackDestinations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * For test copies installed from GitHub: notices when a newer beta has been
 * published under Releases. It asks GitHub's public API (no account, nothing
 * sent but the request itself) at most twice a day. Builds from Google Play
 * never check: Play updates them.
 */
class BetaUpdates(context: Context) {
    data class Release(val build: Int, val title: String, val url: String)

    private val prefs = context.getSharedPreferences("beta", Context.MODE_PRIVATE)

    /** True for test builds made by CI, which are the ones published as betas. */
    val enabled: Boolean = BuildConfig.BETA_UPDATES && BuildConfig.BUILD_NUMBER > 0

    val build: Int get() = BuildConfig.BUILD_NUMBER

    /** A newer beta than this one, once found. */
    var available by mutableStateOf<Release?>(null)
        private set

    var checking by mutableStateOf(false)
        private set

    /** Checks unless one ran recently; [force] for the "Check now" button. Returns false if GitHub couldn't be reached. */
    suspend fun check(force: Boolean = false): Boolean {
        if (!enabled || checking) return true
        val now = System.currentTimeMillis()
        if (!force && now - prefs.getLong("lastCheck", 0) < 12 * 3_600_000L) return true
        checking = true
        return try {
            val newest = withContext(Dispatchers.IO) { newestBeta() }
            prefs.edit().putLong("lastCheck", now).apply()
            available = newest?.takeIf { it.build > build }
            true
        } catch (_: Exception) {
            false
        } finally {
            checking = false
        }
    }

    private fun newestBeta(): Release? {
        val connection = URL("https://api.github.com/repos/${FeedbackDestinations.GITHUB_REPO}/releases?per_page=20")
            .openConnection() as HttpURLConnection
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        try {
            if (connection.responseCode != 200) return null
            // A page of releases is a few kilobytes; cap it anyway.
            val body = connection.inputStream.use { it.readBytes().take(512 * 1024).toByteArray() }.toString(Charsets.UTF_8)
            val releases = JSONArray(body)
            return (0 until releases.length()).map { releases.getJSONObject(it) }
                .mapNotNull { r ->
                    val n = r.optString("tag_name").removePrefix("beta-").toIntOrNull() ?: return@mapNotNull null
                    val url = r.optString("html_url")
                    // Only ever send people to this repository's own release pages.
                    if (!url.startsWith("https://github.com/${FeedbackDestinations.GITHUB_REPO}/releases/")) return@mapNotNull null
                    Release(n, r.optString("name").ifBlank { "beta $n" }, url)
                }
                .maxByOrNull { it.build }
        } finally {
            connection.disconnect()
        }
    }
}
