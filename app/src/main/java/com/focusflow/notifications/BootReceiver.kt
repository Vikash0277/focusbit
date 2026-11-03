package com.focusflow.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focusflow.FocusFlowApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reschedules all active goal reminders after device reboot.
 * Alarms are lost on reboot — this receiver restores them.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        android.util.Log.d("BootReceiver", "Device rebooted — rescheduling goal reminders")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? FocusFlowApplication
                val db = app?.database ?: return@launch

                val goals = db.goalDao().getAllGoalsSync()

                goals.filter { it.reminderEnabled }.forEach { goal ->
                    AlarmScheduler.scheduleGoalReminder(
                        context = context,
                        goalId = goal.id,
                        goalTitle = goal.title,
                        dailyTarget = goal.dailyTarget,
                        loggedHours = goal.loggedHours,
                        timeString = goal.reminderTime
                    )
                    android.util.Log.d(
                        "BootReceiver",
                        "Rescheduled reminder for: ${goal.title} at ${goal.reminderTime}"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("BootReceiver", "Failed to reschedule reminders on boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

