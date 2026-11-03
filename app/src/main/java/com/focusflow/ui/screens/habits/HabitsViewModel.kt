package com.focusflow.ui.screens.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.focusflow.data.db.FocusFlowDatabase
import com.focusflow.data.db.entity.Habit
import com.focusflow.data.db.entity.HabitCompletion
import com.focusflow.subscription.SubscriptionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class HabitWithStatus(val habit: Habit, val isCompleted: Boolean)

data class HeatmapDayInfo(
    val dateString: String,
    val dayOfMonth: Int,
    val monthNumber: Int,
    val monthName: String,
    val level: Int,
    val count: Int,
    val isToday: Boolean,
    val isFuture: Boolean,
    val isCurrentMonth: Boolean
)

data class HabitsUiState(
    val habits: List<HabitWithStatus> = emptyList(),
    val selectedHabitId: Long? = null, // null = All Habits, or specific habit id
    val selectedHeatmapTab: Int = 0,   // 0 = Month View (Free for current month), 1 = 90 days (Pro), 2 = All Year LeetCode (Pro)
    val selectedMonthValue: Int = LocalDate.now().monthValue,
    val selectedYear: Int = LocalDate.now().year,
    val streakDays: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val monthlyCount: String = "0/0",
    val monthlyRate: String = "0%",
    val totalYearCompletions: Int = 0,
    val totalYearActiveDays: Int = 0,
    val heatmapDaysData: List<HeatmapDayInfo> = emptyList(),
    val yearHeatmapWeeks: List<List<HeatmapDayInfo>> = emptyList(),
    val heatmapData: List<Int> = emptyList(),
    val heatmapDays: Int = 28,
    val showPaywall: Boolean = false
)

class HabitsViewModel(private val database: FocusFlowDatabase) : ViewModel() {
    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

    private val today = LocalDate.now().toString()
    private var cachedCompletions: List<HabitCompletion> = emptyList()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                database.habitDao().getActiveHabits(),
                database.habitDao().getAllCompletions()
            ) { habits, completions ->
                cachedCompletions = completions
                val completedTodayIds = completions.filter { it.date == today }.map { it.habitId }.toSet()
                val habitsWithStatus = habits.map { HabitWithStatus(it, it.id in completedTodayIds) }
                Pair(habitsWithStatus, completions)
            }.collect { (habitsWithStatus, completions) ->
                _uiState.update { it.copy(habits = habitsWithStatus) }
                computeMetricsAndHeatmap(completions)
            }
        }
    }

    fun selectHabitForHeatmap(habitId: Long?) {
        _uiState.update { it.copy(selectedHabitId = habitId) }
        computeMetricsAndHeatmap(cachedCompletions)
    }

    fun selectHeatmapTab(index: Int) {
        // index 0 = Month View, index 1 = 90 days (Pro), index 2 = All Year LeetCode (Pro)
        if ((index == 1 || index == 2) && !SubscriptionManager.isPro.value) {
            _uiState.update { it.copy(showPaywall = true) }
            return
        }
        _uiState.update { it.copy(selectedHeatmapTab = index) }
        computeMetricsAndHeatmap(cachedCompletions)
    }

    fun selectMonthForHeatmap(month: Int) {
        val now = LocalDate.now()
        if (month != now.monthValue && !SubscriptionManager.isPro.value) {
            _uiState.update { it.copy(showPaywall = true) }
            return
        }
        _uiState.update { it.copy(selectedMonthValue = month, selectedHeatmapTab = 0) }
        computeMetricsAndHeatmap(cachedCompletions)
    }

    private fun computeMetricsAndHeatmap(completions: List<HabitCompletion>) {
        val now = LocalDate.now()
        val currentState = _uiState.value
        val selectedHabitId = currentState.selectedHabitId
        val tab = currentState.selectedHeatmapTab
        val targetMonth = currentState.selectedMonthValue

        val relevantCompletions = if (selectedHabitId != null) {
            completions.filter { it.habitId == selectedHabitId }
        } else {
            completions
        }

        val completedDates = relevantCompletions.map { it.date }.toSet()

        // 1. Calculate Current Streak
        var currentStreak = 0
        var checkDate = now
        if (completedDates.contains(checkDate.toString())) {
            while (completedDates.contains(checkDate.toString())) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            }
        } else if (completedDates.contains(checkDate.minusDays(1).toString())) {
            checkDate = checkDate.minusDays(1)
            while (completedDates.contains(checkDate.toString())) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            }
        }

        // 2. Calculate Longest Streak
        val sortedDates = completedDates.mapNotNull {
            try { LocalDate.parse(it) } catch (e: Exception) { null }
        }.sorted()

        var longestStreak = 0
        var tempStreak = 0
        var prevDate: LocalDate? = null
        for (date in sortedDates) {
            if (prevDate == null) {
                tempStreak = 1
            } else if (date == prevDate.plusDays(1)) {
                tempStreak++
            } else if (date != prevDate) {
                tempStreak = 1
            }
            if (tempStreak > longestStreak) {
                longestStreak = tempStreak
            }
            prevDate = date
        }

        // 3. Monthly completions & rate for targetMonth
        val targetMonthPrefix = "${now.year}-${"%02d".format(targetMonth)}"
        val completedInTargetMonth = completedDates.count { it.startsWith(targetMonthPrefix) }
        val startOfTargetMonth = LocalDate.of(now.year, targetMonth, 1)
        val daysInTargetMonth = startOfTargetMonth.lengthOfMonth()
        val daysElapsedInTargetMonth = when {
            targetMonth == now.monthValue -> now.dayOfMonth
            targetMonth < now.monthValue -> daysInTargetMonth
            else -> 0
        }
        val monthlyCount = "$completedInTargetMonth/$daysInTargetMonth"
        val monthlyRate = if (daysElapsedInTargetMonth > 0) "${(completedInTargetMonth * 100) / daysElapsedInTargetMonth}%" else "0%"

        // 4. Yearly stats (LeetCode style)
        val currentYearPrefix = "${now.year}-"
        val yearCompletions = relevantCompletions.filter { it.date.startsWith(currentYearPrefix) }
        val totalYearCompletions = yearCompletions.size
        val totalYearActiveDays = completedDates.count { it.startsWith(currentYearPrefix) }

        // 5. Generate Heatmap Grid Days for selected tab
        val dayList = mutableListOf<HeatmapDayInfo>()
        val completionsByDate = relevantCompletions.groupBy { it.date }

        fun makeDayInfo(d: LocalDate, isCurMonth: Boolean): HeatmapDayInfo {
            val dStr = d.toString()
            val comps = completionsByDate[dStr] ?: emptyList()
            val isComp = comps.isNotEmpty()
            val level = if (selectedHabitId != null) {
                if (isComp) 3 else 0
            } else {
                comps.size.coerceAtMost(3)
            }
            return HeatmapDayInfo(
                dateString = dStr,
                dayOfMonth = d.dayOfMonth,
                monthNumber = d.monthValue,
                monthName = d.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                level = level,
                count = comps.size,
                isToday = (d == now),
                isFuture = d.isAfter(now),
                isCurrentMonth = isCurMonth
            )
        }

        if (tab == 0) {
            // Month View: show the targetMonth calendar aligned to Mon..Sun
            val leadDays = startOfTargetMonth.dayOfWeek.value - 1
            val gridStart = startOfTargetMonth.minusDays(leadDays.toLong())

            val endOfMonth = startOfTargetMonth.plusDays((daysInTargetMonth - 1).toLong())
            val endDayOfWeek = endOfMonth.dayOfWeek.value
            val trailDays = if (endDayOfWeek < 7) 7 - endDayOfWeek else 0
            val totalGridDays = leadDays + daysInTargetMonth + trailDays

            for (i in 0 until totalGridDays) {
                val d = gridStart.plusDays(i.toLong())
                dayList.add(makeDayInfo(d, isCurMonth = (d.monthValue == targetMonth)))
            }
        } else if (tab == 1) {
            // 90 Days view: last 90 days aligned to Mon..Sun
            val pastDate = now.minusDays(89)
            val leadDays = pastDate.dayOfWeek.value - 1
            val gridStart = pastDate.minusDays(leadDays.toLong())

            val trailDays = if (now.dayOfWeek.value < 7) 7 - now.dayOfWeek.value else 0
            val gridEnd = now.plusDays(trailDays.toLong())
            val totalGridDays = java.time.temporal.ChronoUnit.DAYS.between(gridStart, gridEnd).toInt() + 1

            for (i in 0 until totalGridDays) {
                val d = gridStart.plusDays(i.toLong())
                dayList.add(makeDayInfo(d, isCurMonth = (d.monthValue == now.monthValue)))
            }
        }

        // 6. Generate Full Year Weeks (LeetCode 52-week matrix)
        val startOfYear = LocalDate.of(now.year, 1, 1)
        val yearLead = startOfYear.dayOfWeek.value - 1
        val yearGridStart = startOfYear.minusDays(yearLead.toLong())
        val endOfYear = LocalDate.of(now.year, 12, 31)
        val yearTrail = if (endOfYear.dayOfWeek.value < 7) 7 - endOfYear.dayOfWeek.value else 0
        val yearGridEnd = endOfYear.plusDays(yearTrail.toLong())
        val yearTotalDays = java.time.temporal.ChronoUnit.DAYS.between(yearGridStart, yearGridEnd).toInt() + 1

        val allYearDays = (0 until yearTotalDays).map { i ->
            val d = yearGridStart.plusDays(i.toLong())
            makeDayInfo(d, isCurMonth = (d.monthValue == now.monthValue))
        }
        val yearHeatmapWeeks = allYearDays.chunked(7)

        val rawLevels = dayList.map { it.level }

        _uiState.update {
            it.copy(
                streakDays = currentStreak,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                monthlyCount = monthlyCount,
                monthlyRate = monthlyRate,
                totalYearCompletions = totalYearCompletions,
                totalYearActiveDays = totalYearActiveDays,
                heatmapDaysData = dayList,
                yearHeatmapWeeks = yearHeatmapWeeks,
                heatmapData = rawLevels,
                heatmapDays = dayList.size
            )
        }
    }

    fun toggleHabit(habit: Habit, isCompleted: Boolean) {
        viewModelScope.launch {
            if (isCompleted) {
                database.habitDao().deleteCompletion(habit.id, today)
            } else {
                database.habitDao().insertCompletion(HabitCompletion(habitId = habit.id, date = today))
                com.focusflow.coins.CoinLedgerService.earn(
                    com.focusflow.coins.CoinLedgerService.REWARD_HABIT_COMPLETED,
                    "Completed habit: ${habit.title}",
                    "habit_${habit.id}_$today"
                )
            }
        }
    }

    fun dismissPaywall() {
        _uiState.update { it.copy(showPaywall = false) }
    }

    fun addHabit(title: String, category: String, durationMinutes: Int) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val currentCount = _uiState.value.habits.size
            if (!SubscriptionManager.canAddHabit(currentCount)) {
                _uiState.update { it.copy(showPaywall = true) }
                return@launch
            }
            database.habitDao().insertHabit(
                Habit(
                    title = title.trim(),
                    time = "Daily",
                    durationMinutes = durationMinutes,
                    category = category,
                    iconName = "check_circle",
                    isActive = true
                )
            )
        }
    }

    class Factory(private val database: FocusFlowDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HabitsViewModel(database) as T
    }
}
