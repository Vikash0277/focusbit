package com.focusflow.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import java.util.Calendar

object AlarmScheduler {

    const val ACTION_GOAL_REMINDER = "com.focusflow.action.GOAL_REMINDER"
    const val ACTION_TEMP_UNLOCK_OVER = "com.focusflow.action.TEMP_UNLOCK_OVER"
    const val ACTION_SHIELD_TIMER_OVER = "com.focusflow.action.SHIELD_TIMER_OVER"

    const val EXTRA_GOAL_ID = "extra_goal_id"
    const val EXTRA_GOAL_TITLE = "extra_goal_title"
    const val EXTRA_DAILY_TARGET = "extra_daily_target"
    const val EXTRA_LOGGED_HOURS = "extra_logged_hours"
    const val EXTRA_REMINDER_TIME = "extra_reminder_time"

    const val EXTRA_PACKAGE_NAME = "extra_package_name"
    const val EXTRA_APP_NAME = "extra_app_name"
    const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"

    private const val REQUEST_CODE_SHIELD_TIMER = 9999

    /**
     * Schedules a daily recurring reminder for a Goal.
     */
    fun scheduleGoalReminder(
        context: Context,
        goalId: Long,
        goalTitle: String,
        dailyTarget: Float,
        loggedHours: Float,
        timeString: String // "HH:mm"
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val parts = timeString.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, GoalReminderReceiver::class.java).apply {
            action = ACTION_GOAL_REMINDER
            putExtra(EXTRA_GOAL_ID, goalId)
            putExtra(EXTRA_GOAL_TITLE, goalTitle)
            putExtra(EXTRA_DAILY_TARGET, dailyTarget)
            putExtra(EXTRA_LOGGED_HOURS, loggedHours)
            putExtra(EXTRA_REMINDER_TIME, timeString)
        }

        val requestCode = (goalId and 0xFFFF).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            AlarmManagerCompat.setExactAndAllowWhileIdle(
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            android.util.Log.d("AlarmScheduler", "Goal reminder scheduled for $goalTitle at ${calendar.time}")
        } catch (e: SecurityException) {
            // Fallback for devices restricting exact alarms
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            android.util.Log.e("AlarmScheduler", "Failed to schedule goal reminder", e)
        }
    }

    /**
     * Cancels a goal reminder.
     */
    fun cancelGoalReminder(context: Context, goalId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, GoalReminderReceiver::class.java).apply {
            action = ACTION_GOAL_REMINDER
        }
        val requestCode = (goalId and 0xFFFF).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Schedules a notification for when an app's temporary unlock period expires.
     */
    fun scheduleTemporaryUnlockOver(
        context: Context,
        packageName: String,
        appName: String,
        durationMinutes: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAtMillis = System.currentTimeMillis() + (durationMinutes.toLong() * 60L * 1000L)

        val intent = Intent(context, ShieldTimerReceiver::class.java).apply {
            action = ACTION_TEMP_UNLOCK_OVER
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            putExtra(EXTRA_APP_NAME, appName)
            putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
        }

        val requestCode = packageName.hashCode() and 0xFFFF
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            AlarmManagerCompat.setExactAndAllowWhileIdle(
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            android.util.Log.d("AlarmScheduler", "Temporary unlock reminder scheduled for $appName in ${durationMinutes}m")
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            android.util.Log.e("AlarmScheduler", "Failed to schedule temp unlock timer", e)
        }
    }

    /**
     * Schedules a notification for when a timed shield session finishes.
     */
    fun scheduleShieldTimerOver(context: Context, durationMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAtMillis = System.currentTimeMillis() + (durationMinutes.toLong() * 60L * 1000L)

        val intent = Intent(context, ShieldTimerReceiver::class.java).apply {
            action = ACTION_SHIELD_TIMER_OVER
            putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SHIELD_TIMER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            AlarmManagerCompat.setExactAndAllowWhileIdle(
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            android.util.Log.e("AlarmScheduler", "Failed to schedule shield timer", e)
        }
    }

    /**
     * Cancels the timed shield session alarm.
     */
    fun cancelShieldTimerOver(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ShieldTimerReceiver::class.java).apply {
            action = ACTION_SHIELD_TIMER_OVER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SHIELD_TIMER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
