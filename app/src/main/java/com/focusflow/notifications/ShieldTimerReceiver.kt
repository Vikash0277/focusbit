package com.focusflow.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focusflow.shield.ShieldManager

class ShieldTimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AlarmScheduler.ACTION_TEMP_UNLOCK_OVER -> {
                val packageName = intent.getStringExtra(AlarmScheduler.EXTRA_PACKAGE_NAME) ?: ""
                val appName = intent.getStringExtra(AlarmScheduler.EXTRA_APP_NAME) ?: "Distracting App"
                val durationMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_DURATION_MINUTES, 5)

                val notificationId = 2000 + (packageName.hashCode() and 0x7FF)
                NotificationHelper.showShieldTimerOver(
                    context = context,
                    title = "🛡️ Shield Re-locked: $appName",
                    message = "Your ${durationMinutes}m pass for $appName has expired. Focus Shield is active again to protect your attention!",
                    notificationId = notificationId
                )
            }

            AlarmScheduler.ACTION_SHIELD_TIMER_OVER -> {
                val durationMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_DURATION_MINUTES, 25)

                // Ensure shield status updates in ShieldManager
                ShieldManager.cancelShield(context)

                NotificationHelper.showShieldTimerOver(
                    context = context,
                    title = "🎉 Focus Shield Session Complete!",
                    message = "Your ${durationMinutes}-minute focus shield session has finished. Outstanding discipline!",
                    notificationId = 3000
                )
            }
        }
    }
}
