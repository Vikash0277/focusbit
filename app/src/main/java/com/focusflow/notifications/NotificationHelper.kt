package com.focusflow.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.focusflow.MainActivity
import com.focusflow.R

object NotificationHelper {

    const val CHANNEL_GOAL_REMINDERS = "channel_goal_reminders"
    const val CHANNEL_SHIELD_ALERTS = "channel_shield_alerts"

    private const val NOTIFICATION_ID_GOAL_BASE = 1000
    private const val NOTIFICATION_ID_SHIELD_BASE = 2000

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // 1. Goal Reminders Channel
            val goalChannel = NotificationChannel(
                CHANNEL_GOAL_REMINDERS,
                "Goal Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily check-ins and progress reminders for your primary goals"
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
            }

            // 2. Shield Alerts Channel
            val shieldChannel = NotificationChannel(
                CHANNEL_SHIELD_ALERTS,
                "App Shield Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when timed shields or temporary app unlocks expire"
                enableLights(true)
                lightColor = Color.YELLOW
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(goalChannel)
            notificationManager.createNotificationChannel(shieldChannel)
        }
    }

    fun showGoalReminder(
        context: Context,
        goalId: Long,
        goalTitle: String,
        dailyTarget: Float,
        loggedHours: Float
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "today")
            putExtra("goal_id", goalId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (NOTIFICATION_ID_GOAL_BASE + goalId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remainingHours = (dailyTarget - loggedHours).coerceAtLeast(0f)
        val progressText = if (remainingHours <= 0f) {
            "🎉 You've reached your daily target of ${dailyTarget.toInt()}h! Take a moment to celebrate."
        } else {
            "🎯 ${remainingHours.toInt()}h left to reach today's ${dailyTarget.toInt()}h target. Ready for a focus session?"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_GOAL_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🎯 Goal Check-in: $goalTitle")
            .setContentText(progressText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$progressText\n\nConsistency compounds! Open FocusFlow to log progress or start a timer.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.mipmap.ic_launcher,
                "Open FocusFlow",
                pendingIntent
            )

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify((NOTIFICATION_ID_GOAL_BASE + goalId).toInt(), builder.build())
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "Missing POST_NOTIFICATIONS permission", e)
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Failed to show goal notification", e)
        }
    }

    fun showShieldTimerOver(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = NOTIFICATION_ID_SHIELD_BASE
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "shield")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_SHIELD_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.mipmap.ic_launcher,
                "Open Focus Shield",
                pendingIntent
            )

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "Missing POST_NOTIFICATIONS permission", e)
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Failed to show shield notification", e)
        }
    }
}
