package com.stagirite.ledger.reminders

import com.stagirite.ledger.data.Milestones
import com.stagirite.ledger.LedgerApplication
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.stagirite.ledger.MainActivity
import com.stagirite.ledger.R
import com.stagirite.ledger.data.GoalKind
import com.stagirite.ledger.data.Settings
import com.stagirite.ledger.data.plural
import com.stagirite.ledger.widget.EXTRA_OPEN_GOAL
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

    /** Opens a home-screen action: [QUICK_LOG] or [QUICK_NEW]. Used by launcher shortcuts and the reminder. */
    const val EXTRA_QUICK = "quick"
    const val QUICK_LOG = "log"
    const val QUICK_NEW = "new"

    const val ACTION_GOAL = "com.stagirite.ledger.GOAL"
    private const val GOAL_CHANNEL = "goals"

    fun apply(context: Context, settings: Settings) {
        if (settings.reminder) schedule(context, settings.reminderMinutes) else cancel(context)
        if (settings.weekly) scheduleWeekly(context, settings.weeklyDay, settings.weeklyMinutes) else cancelWeekly(context)
        settings.goalReminders.forEach { (id, minutes) -> scheduleGoal(context, id, minutes) }
    }

    /** Sets, moves or (with null) removes the reminder for one goal. */
    fun setGoal(context: Context, settings: Settings, id: String, minutes: Int?) {
        settings.updateGoalReminder(id, minutes)
        if (minutes == null) {
            alarms(context).cancel(goalPending(context, id))
            NotificationManagerCompat.from(context).cancel(goalNotificationId(id))
        } else {
            scheduleGoal(context, id, minutes)
        }
    }

    private fun scheduleGoal(context: Context, id: String, minutes: Int) {
        val now = ZonedDateTime.now()
        var next = ZonedDateTime.of(LocalDate.now(), LocalTime.of(minutes / 60, minutes % 60), now.zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        alarms(context).setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli(),
            AlarmManager.INTERVAL_DAY,
            goalPending(context, id),
        )
    }

    // Alarms are told apart by their data, not their extras, so each goal gets its own address.
    private fun goalUri(id: String) = Uri.parse("stagira://goal/$id")

    private fun goalPending(context: Context, id: String) = PendingIntent.getBroadcast(
        context, 3,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_GOAL).setData(goalUri(id)),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun goalNotificationId(id: String) = 1000 + (id.hashCode() and 0xFFFFF)

    /**
     * One goal's reminder. Skipped on its rest days and once a slip is logged for
     * today. The name shown is the stored one (a private goal's cover name), and
     * none at all while the app lock is on.
     */
    fun showGoal(context: Context, id: String) {
        val app = context.applicationContext as LedgerApplication
        val goal = app.store.find(id)
        if (goal == null) {
            setGoal(context, app.settings, id, null)
            return
        }
        val today = LocalDate.now()
        if (goal.archived || !goal.isScheduled(today) || goal.isSlip(today)) return
        val text = when {
            app.settings.appLock -> "A reminder for one of your goals."
            goal.kind == GoalKind.Virtue -> "Time for ${goal.title}."
            else -> "${goal.title} \u00B7 ${plural(goal.daysKept(today), "day")} kept. Nothing to log unless you slipped."
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(GOAL_CHANNEL, "Goal reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = PendingIntent.getActivity(
            context, 3,
            Intent(context, MainActivity::class.java)
                .setData(goalUri(id))
                .putExtra(EXTRA_OPEN_GOAL, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, GOAL_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Stagira")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(goalNotificationId(id), notification)
        } catch (_: SecurityException) {
            // Notifications are off for the app.
        }
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
            .setContentTitle("Stagira")
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
        val quickLog = PendingIntent.getActivity(
            context, 2,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_QUICK, QUICK_LOG)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Stagira")
            .setContentText(dailyText(context))
            .setContentIntent(open)
            .addAction(0, "Quick log", quickLog)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(1, notification)
        } catch (_: SecurityException) {
            // Notifications were turned off for the app; nothing to do.
        }
    }

    /**
     * Today's line: a milestone if one falls today, otherwise a reminder that
     * nothing needs logging unless something happened. Names stay out of it
     * when the app is locked or the goal is private.
     */
    private fun dailyText(context: Context): String {
        val app = context.applicationContext as LedgerApplication
        val today = LocalDate.now()
        val reached = app.store.goals.filter { Milestones.isMilestone(it.daysKept(today), it.currentStart) }
        return when {
            reached.isEmpty() -> "Anything to log? If not, today counts as kept."
            reached.size > 1 -> "${reached.size} of your goals reach a milestone today."
            app.settings.appLock || reached[0].isPrivate -> "One of your goals reaches a milestone today."
            else -> {
                val days = reached[0].daysKept(today)
                "${reached[0].title} reaches ${Milestones.name(days, reached[0].currentStart) ?: "$days days"} today."
            }
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
        when (intent.action) {
            Reminder.ACTION_WEEKLY -> Reminder.showWeekly(context)
            Reminder.ACTION_GOAL -> intent.data?.lastPathSegment?.let { Reminder.showGoal(context, it) }
            else -> Reminder.show(context)
        }
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
