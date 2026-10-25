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

    fun apply(context: Context, settings: Settings) {
        if (settings.reminder) schedule(context, settings.reminderMinutes) else cancel(context)
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
    override fun onReceive(context: Context, intent: Intent) = Reminder.show(context)
}

/** Alarms are cleared on reboot; set the reminder again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) Reminder.apply(context, Settings(context))
    }
}
