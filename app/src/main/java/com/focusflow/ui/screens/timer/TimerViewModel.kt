package com.focusflow.ui.screens.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.focusflow.data.db.FocusFlowDatabase
import com.focusflow.data.db.entity.FocusSession
import com.focusflow.data.db.entity.Lap
import com.focusflow.subscription.SubscriptionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TimerUiState(
    val isRunning: Boolean = false,
    val elapsedMs: Long = 0L,
    val targetMs: Long = 1500000L, // 25m default focus block (Free baseline)
    val sessionNumber: Int = 1,
    val goalTitle: String = "Focus Session",
    val laps: List<LapDisplay> = emptyList(),
    val showProPaywall: Boolean = false
)

/** Pro-only preset durations (ms) available beyond the free 25m baseline */
val PRO_PRESETS_MS = listOf(
    Triple("🎯 25m Pomodoro", 1500000L, false),   // Free
    Triple("⚡ 45m Deep Work", 2700000L, true),    // Pro
    Triple("🔥 90m Flow State", 5400000L, true),   // Pro
    Triple("🌙 2h Marathon",   7200000L, true)      // Pro
)

data class LapDisplay(
    val number: Int,
    val label: String,
    val note: String,
    val splitTime: String,
    val overallTime: String
)

class TimerViewModel(private val database: FocusFlowDatabase) : ViewModel() {
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            database.sessionDao().getAllSessions().collect { sessions ->
                _uiState.update { it.copy(sessionNumber = sessions.size + 1) }
            }
        }
        viewModelScope.launch {
            database.goalDao().getAllGoals().collect { goals ->
                val activeGoal = goals.firstOrNull()
                if (activeGoal != null) {
                    _uiState.update { it.copy(goalTitle = activeGoal.title) }
                }
            }
        }
    }

    fun togglePause() {
        val nextRunning = !_uiState.value.isRunning
        _uiState.update { it.copy(isRunning = nextRunning) }
        if (nextRunning) {
            startTimer()
        } else {
            timerJob?.cancel()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRunning) {
                delay(100)
                _uiState.update { it.copy(elapsedMs = it.elapsedMs + 100) }
            }
        }
    }

    /**
     * +15m boost — Pro-only after first boost to encourage upgrade.
     * Free users get one free boost (up to 40m total), then paywall.
     */
    fun addBoost() {
        val current = _uiState.value
        val isPro = SubscriptionManager.isPro.value
        // Free users capped at 40m target (2 boosts from 25m default)
        val freeCap = 2400000L // 40m
        if (!isPro && current.targetMs >= freeCap) {
            _uiState.update { it.copy(showProPaywall = true) }
            return
        }
        _uiState.update { it.copy(targetMs = it.targetMs + 900000L) } // +15m
    }

    fun reduceTarget() {
        _uiState.update {
            val newTarget = (it.targetMs - 900000L).coerceAtLeast(60000L) // -15m, min 1m
            it.copy(targetMs = newTarget)
        }
    }

    fun setCustomTargetMinutes(minutes: Int) {
        val ms = minutes.toLong() * 60000L
        val isPro = SubscriptionManager.isPro.value
        val freeCap = 2400000L // 40m
        if (!isPro && ms > freeCap) {
            _uiState.update { it.copy(showProPaywall = true) }
            return
        }
        _uiState.update { it.copy(targetMs = ms.coerceAtLeast(60000L)) }
    }

    fun adjustTargetMs(deltaMs: Long) {
        val current = _uiState.value
        val newTarget = (current.targetMs + deltaMs).coerceAtLeast(60000L) // min 1m
        val isPro = SubscriptionManager.isPro.value
        val freeCap = 2400000L // 40m
        if (!isPro && deltaMs > 0 && newTarget > freeCap) {
            _uiState.update { it.copy(showProPaywall = true) }
            return
        }
        _uiState.update { it.copy(targetMs = newTarget) }
    }

    /** Apply a preset duration; Pro-only presets show paywall if user is Free */
    fun applyPreset(durationMs: Long, requiresPro: Boolean) {
        if (requiresPro && !SubscriptionManager.isPro.value) {
            _uiState.update { it.copy(showProPaywall = true) }
            return
        }
        _uiState.update { it.copy(targetMs = durationMs) }
    }

    fun dismissProPaywall() {
        _uiState.update { it.copy(showProPaywall = false) }
    }

    fun reset() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                isRunning = false,
                elapsedMs = 0L,
                laps = emptyList()
            )
        }
    }

    fun addLap() {
        val current = _uiState.value
        if (current.elapsedMs == 0L) return
        val newLapNumber = current.laps.size + 1
        val elapsed = current.elapsedMs
        val lastOverall = if (current.laps.isNotEmpty()) parseTime(current.laps.first().overallTime) else 0L
        val split = (elapsed - lastOverall).coerceAtLeast(0L)

        val newLap = LapDisplay(
            number = newLapNumber,
            label = "Lap $newLapNumber",
            note = "Split at ${formatTimeMsShort(elapsed)}",
            splitTime = formatTimeMs(split),
            overallTime = formatTimeMsShort(elapsed)
        )
        _uiState.update { it.copy(laps = listOf(newLap) + it.laps) }

        viewModelScope.launch {
            database.sessionDao().insertLap(
                Lap(
                    sessionId = current.sessionNumber.toLong(),
                    lapNumber = newLapNumber,
                    label = newLap.label,
                    splitTimeMs = split,
                    overallTimeMs = elapsed
                )
            )
        }
    }

    fun finishSession() {
        timerJob?.cancel()
        val current = _uiState.value
        _uiState.update { it.copy(isRunning = false) }
        if (current.elapsedMs > 0L) {
            viewModelScope.launch {
                database.sessionDao().insertSession(
                    FocusSession(
                        goalTitle = current.goalTitle,
                        durationMs = current.elapsedMs,
                        targetDurationMs = current.targetMs,
                        status = "completed"
                    )
                )
                // Log hours to goal if matched
                val hours = current.elapsedMs / 3600000f
                if (hours > 0f) {
                    val goal = database.goalDao().getAllGoals().firstOrNull()?.firstOrNull()
                    if (goal != null) {
                        database.goalDao().logHours(goal.id, hours)
                    }
                }
                val earnedCoins = ((current.elapsedMs / (5 * 60000L)) * 2).coerceIn(2L, 30L).toInt()
                com.focusflow.coins.CoinLedgerService.earn(
                    earnedCoins,
                    "Completed focus session (${(current.elapsedMs / 60000).coerceAtLeast(1)}m)",
                    "session_${System.currentTimeMillis()}"
                )
                reset()
            }
        }
    }

    private fun parseTime(t: String): Long {
        val parts = t.split(":")
        return if (parts.size == 3) {
            (parts[0].toLongOrNull() ?: 0) * 3600000 + (parts[1].toLongOrNull() ?: 0) * 60000 + ((parts[2].toDoubleOrNull() ?: 0.0) * 1000).toLong()
        } else 0L
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }

    class Factory(private val database: FocusFlowDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TimerViewModel(database) as T
    }
}

fun formatTimeMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val centis = (ms % 1000) / 10
    return "%02d:%02d.%02d".format(minutes, seconds, centis)
}

fun formatTimeMsShort(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
