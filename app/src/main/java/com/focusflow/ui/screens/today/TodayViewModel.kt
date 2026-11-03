package com.focusflow.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.focusflow.coins.CoinLedgerService
import com.focusflow.data.db.FocusFlowDatabase
import com.focusflow.data.db.entity.Commitment
import com.focusflow.data.db.entity.Goal
import com.focusflow.data.db.entity.Todo
import com.focusflow.subscription.SubscriptionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

data class TodayUiState(
    val greeting: String = "",
    val dateLabel: String = "",
    val goal: Goal? = null,
    val commitments: List<Commitment> = emptyList(),
    val todos: List<Todo> = emptyList(),
    val selectedView: Int = 0, // 0: Daily Todos, 1: Goals & Schedule
    val todayLoggedMinutes: Int = 0,
    val todayPlannedMinutes: Int = 0,
    val showToast: Boolean = false,
    val toastMessage: String = "",
    val showPaywall: Boolean = false
)

class TodayViewModel(private val database: FocusFlowDatabase) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val now = LocalDate.now()
        val dayOfWeek = now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        val month = now.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        val dateLabel = "$dayOfWeek, $month ${now.dayOfMonth}"

        val hour = LocalTime.now().hour
        val greeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }

        _uiState.update { it.copy(greeting = greeting, dateLabel = dateLabel) }

        viewModelScope.launch {
            database.goalDao().getAllGoals().collect { goals ->
                val activeGoal = goals.firstOrNull()
                val planned = if (activeGoal != null) (activeGoal.dailyTarget * 60).toInt() else 0
                val logged = if (activeGoal != null) (activeGoal.loggedHours * 60).toInt() else 0
                _uiState.update {
                    it.copy(
                        goal = activeGoal,
                        todayPlannedMinutes = planned,
                        todayLoggedMinutes = logged
                    )
                }
            }
        }

        viewModelScope.launch {
            val today = LocalDate.now().toString()
            database.commitmentDao().getCommitmentsForDate(today).collect { commitments ->
                _uiState.update { it.copy(commitments = commitments) }
            }
        }

        viewModelScope.launch {
            database.todoDao().getAllTodos().collect { todos ->
                _uiState.update { it.copy(todos = todos) }
            }
        }
    }

    fun setSelectedView(viewIndex: Int) {
        _uiState.update { it.copy(selectedView = viewIndex) }
    }

    fun logDailyTarget() {
        viewModelScope.launch {
            val goal = _uiState.value.goal ?: return@launch
            if (goal.goalType == "DAYS") {
                database.goalDao().logDay(goal.id)
                CoinLedgerService.earn(
                    CoinLedgerService.REWARD_GOAL_LOGGED,
                    "Logged daily target for ${goal.title}",
                    "goal_logged_${goal.id}_${System.currentTimeMillis() / 86400000}"
                )
                _uiState.update {
                    it.copy(
                        showToast = true,
                        toastMessage = "+1 day & +${CoinLedgerService.REWARD_GOAL_LOGGED} 🪙 logged to ${goal.title}!"
                    )
                }
            } else {
                database.goalDao().logHours(goal.id, goal.dailyTarget)
                CoinLedgerService.earn(
                    CoinLedgerService.REWARD_GOAL_LOGGED,
                    "Logged daily target for ${goal.title}",
                    "goal_logged_${goal.id}_${System.currentTimeMillis() / 86400000}"
                )
                _uiState.update {
                    it.copy(
                        todayLoggedMinutes = it.todayLoggedMinutes + (goal.dailyTarget * 60).toInt(),
                        showToast = true,
                        toastMessage = "+${goal.dailyTarget.toInt()}h & +${CoinLedgerService.REWARD_GOAL_LOGGED} 🪙 logged to ${goal.title}!"
                    )
                }
            }
            kotlinx.coroutines.delay(3000)
            _uiState.update { it.copy(showToast = false) }
        }
    }


    fun dismissPaywall() {
        _uiState.update { it.copy(showPaywall = false) }
    }

    fun addGoal(
        title: String,
        tag: String,
        goalType: String = "HOURS",
        targetHours: Float,
        targetDays: Int = 0,
        dailyTarget: Float,
        reminderEnabled: Boolean = true,
        reminderTime: String = "09:00",
        context: android.content.Context? = null
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val allGoals = database.goalDao().getAllGoals().firstOrNull() ?: emptyList()
            if (!SubscriptionManager.canAddGoal(allGoals.size)) {
                _uiState.update { it.copy(showPaywall = true) }
                return@launch
            }
            val newGoal = Goal(
                title = title.trim(),
                tag = tag.ifBlank { "Primary" },
                goalType = goalType,
                targetHours = targetHours,
                loggedHours = 0f,
                targetDays = targetDays,
                loggedDays = 0,
                dailyTarget = dailyTarget,
                icon = "rocket_launch",
                status = "on_track",
                reminderEnabled = reminderEnabled,
                reminderTime = reminderTime
            )
            val id = database.goalDao().insertGoal(newGoal)

            if (reminderEnabled && context != null) {
                com.focusflow.notifications.AlarmScheduler.scheduleGoalReminder(
                    context = context,
                    goalId = id,
                    goalTitle = newGoal.title,
                    dailyTarget = newGoal.dailyTarget,
                    loggedHours = 0f,
                    timeString = reminderTime
                )
            }
        }
    }

    fun updateGoalReminder(
        context: android.content.Context,
        goalId: Long,
        enabled: Boolean,
        time: String
    ) {
        viewModelScope.launch {
            database.goalDao().updateGoalReminder(goalId, enabled, time)
            val currentGoal = _uiState.value.goal
            if (enabled && currentGoal != null) {
                com.focusflow.notifications.AlarmScheduler.scheduleGoalReminder(
                    context = context,
                    goalId = goalId,
                    goalTitle = currentGoal.title,
                    dailyTarget = currentGoal.dailyTarget,
                    loggedHours = currentGoal.loggedHours,
                    timeString = time
                )
            } else {
                com.focusflow.notifications.AlarmScheduler.cancelGoalReminder(context, goalId)
            }
            _uiState.update {
                it.copy(
                    showToast = true,
                    toastMessage = if (enabled) "🔔 Reminder set for $time daily" else "🔕 Goal reminder disabled"
                )
            }
            kotlinx.coroutines.delay(2000)
            _uiState.update { it.copy(showToast = false) }
        }
    }

    fun testGoalReminder(context: android.content.Context, goal: Goal) {
        com.focusflow.notifications.NotificationHelper.showGoalReminder(
            context = context,
            goalId = goal.id,
            goalTitle = goal.title,
            dailyTarget = goal.dailyTarget,
            loggedHours = goal.loggedHours
        )
        _uiState.update {
            it.copy(showToast = true, toastMessage = "🔔 Test goal reminder sent!")
        }
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _uiState.update { it.copy(showToast = false) }
        }
    }

    fun addCommitment(title: String, startTime: String, endTime: String, duration: String, label: String) {
        if (title.isBlank()) return
        val today = LocalDate.now().toString()
        viewModelScope.launch {
            database.commitmentDao().insertCommitment(
                Commitment(
                    title = title.trim(),
                    startTime = startTime,
                    endTime = endTime,
                    duration = duration,
                    label = label,
                    date = today,
                    status = "active"
                )
            )
        }
    }

    fun addTodo(title: String, priority: String, dueTime: String, category: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val currentTodos = _uiState.value.todos.size
            if (!SubscriptionManager.canAddTodo(currentTodos)) {
                _uiState.update { it.copy(showPaywall = true) }
                return@launch
            }
            database.todoDao().insertTodo(
                Todo(
                    title = title.trim(),
                    priority = priority,
                    dueTime = dueTime.trim(),
                    category = category.trim().ifBlank { "General" }
                )
            )
        }
    }

    fun toggleTodo(todo: Todo) {
        val newStatus = !todo.isCompleted
        val completedAt = if (newStatus) System.currentTimeMillis() else null
        viewModelScope.launch {
            database.todoDao().updateTodo(todo.copy(isCompleted = newStatus, completedAt = completedAt))
            if (newStatus) {
                CoinLedgerService.earn(
                    CoinLedgerService.REWARD_TODO_COMPLETED,
                    "Completed Todo: ${todo.title}",
                    "todo_done_${todo.id}"
                )
                _uiState.update {
                    it.copy(
                        showToast = true,
                        toastMessage = "+${CoinLedgerService.REWARD_TODO_COMPLETED} 🪙 Todo completed!"
                    )
                }
                kotlinx.coroutines.delay(2500)
                _uiState.update { it.copy(showToast = false) }
            }
        }
    }

    fun deleteTodo(todo: Todo) {
        viewModelScope.launch {
            database.todoDao().deleteTodo(todo)
        }
    }

    class Factory(private val database: FocusFlowDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TodayViewModel(database) as T
        }
    }
}
