package com.focusflow.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focusflow.FocusFlowApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GoalReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_GOAL_REMINDER) return

        val goalId = intent.getLongExtra(AlarmScheduler.EXTRA_GOAL_ID, -1L)
        val initialTitle = intent.getStringExtra(AlarmScheduler.EXTRA_GOAL_TITLE) ?: "Primary Goal"
        val initialDailyTarget = intent.getFloatExtra(AlarmScheduler.EXTRA_DAILY_TARGET, 2f)
        val initialLoggedHours = intent.getFloatExtra(AlarmScheduler.EXTRA_LOGGED_HOURS, 0f)
        val reminderTime = intent.getStringExtra(AlarmScheduler.EXTRA_REMINDER_TIME) ?: "09:00"

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? FocusFlowApplication
                val db = app?.database
                val goal = if (goalId > 0 && db != null) {
                    db.goalDao().getGoalById(goalId)
                } else null

                // If the goal exists and reminder was disabled, don't show and don't reschedule
                if (goal != null && !goal.reminderEnabled) {
                    return@launch
                }

                val title = goal?.title ?: initialTitle
                val dailyTarget = goal?.dailyTarget ?: initialDailyTarget
                val loggedHours = goal?.loggedHours ?: initialLoggedHours
                val time = goal?.reminderTime ?: reminderTime

                // 1. Post notification
                NotificationHelper.showGoalReminder(
                    context = context,
                    goalId = if (goal != null) goal.id else goalId,
                    goalTitle = title,
                    dailyTarget = dailyTarget,
                    loggedHours = loggedHours
                )

                // 2. Reschedule for tomorrow (daily recurrence)
                if (goal == null || goal.reminderEnabled) {
                    AlarmScheduler.scheduleGoalReminder(
                        context = context,
                        goalId = if (goal != null) goal.id else goalId,
                        goalTitle = title,
                        dailyTarget = dailyTarget,
                        loggedHours = loggedHours,
                        timeString = time
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("GoalReminderReceiver", "Error processing goal reminder", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
