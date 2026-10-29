package com.stagirite.ledger.feedback

import android.content.Context
import com.stagirite.ledger.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

/**
 * Crash reports without tracking. When the app crashes, what went wrong (the
 * error and where in the code) is written to a file on the phone, and nothing
 * else: no goals, no notes. On the next launch the app says so and offers to
 * send it through the usual bug report, where the person sees every word
 * before choosing GitHub or email. Google Play's Android vitals still counts
 * crashes for people who share diagnostics with Google.
 */
object CrashLog {
    private const val MAX_CHARS = 6_000

    private fun file(context: Context) = File(context.filesDir, "last-crash.txt")

    /** Records any uncaught crash, then lets the system handle it as usual. */
    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
                file(app).writeText(
                    "Stagira ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}), ${Instant.now()}, thread ${thread.name}\n\n" +
                        trace.take(MAX_CHARS),
                )
            }
            previous?.uncaughtException(thread, error)
        }
    }

    /** The last crash, if it hasn't been dealt with yet. */
    fun pending(context: Context): String? = runCatching { file(context).takeIf { it.exists() }?.readText() }.getOrNull()

    fun clear(context: Context) {
        file(context).delete()
    }
}
