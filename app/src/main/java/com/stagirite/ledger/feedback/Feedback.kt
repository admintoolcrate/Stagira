package com.stagirite.ledger.feedback

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

object Feedback {
    /** App version, Android version and phone model; nothing else. */
    fun deviceSummary(context: Context): String {
        val info = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
        val version = info?.let {
            @Suppress("DEPRECATION")
            val code = if (Build.VERSION.SDK_INT >= 28) it.longVersionCode else it.versionCode.toLong()
            "${it.versionName} ($code)"
        } ?: "unknown"
        return "App $version · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · " +
            "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    /** Opens the pre-filled GitHub issue in the browser. False if nothing can open it. */
    fun openGitHub(context: Context, report: Report): Boolean =
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse(report.githubUrl())))

    /** Opens the reporter's email app with the message filled in. False if there's no email app. */
    fun openEmail(context: Context, report: Report): Boolean {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(report.mailtoUri())).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(FeedbackDestinations.EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, report.subject)
            putExtra(Intent.EXTRA_TEXT, report.body)
        }
        return start(context, intent)
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
