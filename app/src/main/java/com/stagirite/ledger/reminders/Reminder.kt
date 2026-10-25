package com.stagirite.ledger.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.stagirite.ledger.MainActivity
import com.stagirite.ledger.R
import com.stagirite.ledger.data.Settings
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * An optional daily nudge. The notification never names a goal, so nothing
 * private shows on the lock screen.
 */
object Reminder {
    private const val CHANNEL = "daily"
    private const val WEEKLY_CHANNEL = "weekly"
    const val ACTION_WEEKLY = "com.stagirite.ledger.WEEKLY"
    const val EXTRA_OPEN_WEEKLY = "openWeekly"

    fun apply(context: Context, settings: Settings) {
        if (settings.reminder) schedule(context, settings.reminderMinutes) else cancel(context)
        if (settings.weekly) scheduleWeekly(context, settings.weeklyDay, settings.weeklyMinutes) else cancelWeekly(context)
    }

    private fun scheduleWeekly(context: Context, day: Int, minutes: Int) {
        val now = ZonedDateTime.now()
        var next = ZonedDateTime.of(LocalDate.now(), LocalTime.of(minutes / 60, minutes % 60), now.zone)
            .with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.of(day)))
        if (!next.isAfter(now)) next = next.plusWeeks(1)
        alarms(context).setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli(),
            AlarmManager.INTERVAL_DAY * 7,
            weeklyPending(context),
        )
    }

    private fun cancelWeekly(context: Context) = alarms(context).cancel(weeklyPending(context))

    private fun weeklyPending(context: Context) = PendingIntent.getBroadcast(
        context, 1, Intent(context, ReminderReceiver::class.java).setAction(ACTION_WEEKLY),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** "Your weekly review is ready." Never names a goal; tapping it opens the review. */
    fun showWeekly(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(WEEKLY_CHANNEL, "Weekly review", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = PendingIntent.getActivity(
            context, 1,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_WEEKLY, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, WEEKLY_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("The Stagirite\u2019s Ledger")
            .setContentText("Your weekly review is ready.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(2, notification)
        } catch (_: SecurityException) {
            // Notifications are off for the app.
        }
    }

    private fun schedule(context: Context, minutes: Int) {
        val now = ZonedDateTime.now()
        var next = ZonedDateTime.of(LocalDate.now(), LocalTime.of(minutes / 60, minutes % 60), now.zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        alarms(context).setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli(),
            AlarmManager.INTERVAL_DAY,
            pending(context),
        )
    }

    private fun cancel(context: Context) = alarms(context).cancel(pending(context))

    fun show(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("The Stagirite’s Ledger")
            .setContentText("A moment for your ledger.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(1, notification)
        } catch (_: SecurityException) {
            // Notifications were turned off for the app; nothing to do.
        }
    }

    private fun alarms(context: Context) = context.getSystemService(AlarmManager::class.java)

    private fun pending(context: Context) = PendingIntent.getBroadcast(
        context, 0, Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Reminder.ACTION_WEEKLY) Reminder.showWeekly(context) else Reminder.show(context)
    }
}

/** Alarms don't survive a reboot or an app update, and drift with clock changes; set the reminder again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in RESCHEDULE_ON) Reminder.apply(context, Settings(context))
    }

    private companion object {
        val RESCHEDULE_ON = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
