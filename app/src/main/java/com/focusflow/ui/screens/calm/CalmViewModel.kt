package com.focusflow.ui.screens.calm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.focusflow.data.db.FocusFlowDatabase
import com.focusflow.data.db.entity.MoodEntry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CalmUiState(
    val consistentDays: Int = 0,
    val personalBest: Int = 0,
    val isBreathingActive: Boolean = false,
    val breathPhase: String = "Inhale",
    val breathCounter: Int = 4,
    val selectedMood: Int = 0,
    val energyLevel: Int = 0,
    val journalNote: String = "",
    val stabilityPercent: Int = 0,
    val stabilityTrend: String = "",
    val weeklyData: List<Float> = emptyList()
)

class CalmViewModel(private val database: FocusFlowDatabase) : ViewModel() {
    private val _uiState = MutableStateFlow(CalmUiState())
    val uiState: StateFlow<CalmUiState> = _uiState.asStateFlow()

    private var breathingJob: kotlinx.coroutines.Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        val today = LocalDate.now().toString()
        viewModelScope.launch {
            val existing = database.moodDao().getMoodForDate(today)
            if (existing != null) {
                _uiState.update {
                    it.copy(
                        selectedMood = existing.mood,
                        energyLevel = existing.energyLevel,
                        journalNote = existing.note
                    )
                }
            }
        }

        viewModelScope.launch {
            database.moodDao().getAllMoods().collect { moods ->
                val count = moods.size
                val weeklyPoints = moods.takeLast(7).map { (it.mood.toFloat() / 5f).coerceIn(0.1f, 1f) }
                val stability = if (weeklyPoints.isNotEmpty()) {
                    (weeklyPoints.average() * 100).toInt()
                } else 0

                _uiState.update {
                    it.copy(
                        consistentDays = count,
                        personalBest = count,
                        stabilityPercent = stability,
                        stabilityTrend = if (count > 1) "+5%" else "",
                        weeklyData = weeklyPoints
                    )
                }
            }
        }
    }

    fun selectMood(mood: Int) {
        _uiState.update { it.copy(selectedMood = mood) }
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val existing = database.moodDao().getMoodForDate(today)
            if (existing != null) {
                database.moodDao().updateMood(existing.copy(mood = mood))
            } else {
                database.moodDao().insertMood(MoodEntry(date = today, mood = mood, energyLevel = _uiState.value.energyLevel.coerceAtLeast(1), note = _uiState.value.journalNote))
                com.focusflow.coins.CoinLedgerService.earn(
                    com.focusflow.coins.CoinLedgerService.REWARD_CALM_SESSION,
                    "Daily wellness & mood check-in",
                    "calm_checkin_$today"
                )
            }
        }
    }

    fun selectEnergy(level: Int) {
        _uiState.update { it.copy(energyLevel = level) }
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val existing = database.moodDao().getMoodForDate(today)
            if (existing != null) {
                database.moodDao().updateMood(existing.copy(energyLevel = level))
            } else {
                database.moodDao().insertMood(MoodEntry(date = today, mood = _uiState.value.selectedMood.coerceAtLeast(1), energyLevel = level, note = _uiState.value.journalNote))
            }
        }
    }

    fun saveJournalNote(note: String) {
        _uiState.update { it.copy(journalNote = note) }
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val existing = database.moodDao().getMoodForDate(today)
            if (existing != null) {
                database.moodDao().updateMood(existing.copy(note = note))
            } else {
                database.moodDao().insertMood(MoodEntry(date = today, mood = _uiState.value.selectedMood.coerceAtLeast(3), energyLevel = _uiState.value.energyLevel.coerceAtLeast(3), note = note))
            }
        }
    }

    fun toggleBreathing() {
        val isActive = _uiState.value.isBreathingActive
        if (isActive) {
            breathingJob?.cancel()
            _uiState.update { it.copy(isBreathingActive = false, breathPhase = "Inhale", breathCounter = 4) }
        } else {
            _uiState.update { it.copy(isBreathingActive = true) }
            breathingJob = viewModelScope.launch {
                var cycleTime = 0
                while (true) {
                    val totalLoop = 19
                    cycleTime = cycleTime % totalLoop
                    val (phase, counter) = when {
                        cycleTime < 4 -> "Inhale" to (4 - cycleTime)
                        cycleTime < 11 -> "Hold" to (11 - cycleTime)
                        else -> "Exhale" to (19 - cycleTime)
                    }
                    _uiState.update { it.copy(breathPhase = phase, breathCounter = counter) }
                    kotlinx.coroutines.delay(1000)
                    cycleTime++
                }
            }
        }
    }

    override fun onCleared() {
        breathingJob?.cancel()
        super.onCleared()
    }

    class Factory(private val database: FocusFlowDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CalmViewModel(database) as T
    }
}
