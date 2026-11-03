package com.focusflow.coins

import com.focusflow.data.db.FocusFlowDatabase
import com.focusflow.data.db.entity.CoinTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

object CoinLedgerService {
    private var database: FocusFlowDatabase? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    const val REWARD_GOAL_LOGGED = 10L
    const val REWARD_GOAL_COMPLETED = 10L
    const val REWARD_TODO_COMPLETED = 2L
    const val REWARD_HABIT_COMPLETED = 3L
    const val REWARD_FOCUS_25M = 5L
    const val REWARD_FOCUS_SESSION = 5L
    const val REWARD_CALM_SESSION = 2L
    const val REWARD_DAILY_TARGET = 15L
    const val REWARD_7DAY_STREAK = 50L

    private val _balanceState = MutableStateFlow(0L)
    val balanceState: StateFlow<Long> = _balanceState.asStateFlow()
    val balance: StateFlow<Long> = _balanceState.asStateFlow()

    private val _todayEarningsState = MutableStateFlow(0L)
    val todayEarningsState: StateFlow<Long> = _todayEarningsState.asStateFlow()
    val todayEarned: StateFlow<Long> = _todayEarningsState.asStateFlow()

    private val _todaySpentState = MutableStateFlow(0L)
    val todaySpentState: StateFlow<Long> = _todaySpentState.asStateFlow()
    val todaySpent: StateFlow<Long> = _todaySpentState.asStateFlow()

    private val _recentTransactions = MutableStateFlow<List<CoinTransaction>>(emptyList())
    val recentTransactions: StateFlow<List<CoinTransaction>> = _recentTransactions.asStateFlow()

    fun init(db: FocusFlowDatabase) {
        database = db
        startObservers()
        checkInitialWelcomeBonus()
    }

    private fun getStartOfDayMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun startObservers() {
        val db = database ?: return
        scope.launch {
            db.coinDao().getTotalBalance().collect { bal ->
                _balanceState.value = bal
            }
        }
        scope.launch {
            db.coinDao().getTodayEarnings(getStartOfDayMillis()).collect { earned ->
                _todayEarningsState.value = earned
            }
        }
        scope.launch {
            db.coinDao().getTodaySpent(getStartOfDayMillis()).collect { spent ->
                _todaySpentState.value = kotlin.math.abs(spent)
            }
        }
        scope.launch {
            db.coinDao().getAllTransactions().collect { list ->
                _recentTransactions.value = list
            }
        }
    }

    private fun checkInitialWelcomeBonus() {
        val db = database ?: return
        scope.launch {
            val hasWelcome = db.coinDao().hasTransactionWithKey("welcome_bonus") > 0
            if (!hasWelcome) {
                db.coinDao().insertTransaction(
                    CoinTransaction(
                        amount = 50L,
                        type = "BONUS",
                        source = "Welcome to FocusBit! Starter Bonus",
                        idempotencyKey = "welcome_bonus"
                    )
                )
            }
        }
    }

    suspend fun earn(
        amount: Long,
        source: String,
        idempotencyKey: String? = null
    ): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val db = database ?: return@withContext false
        if (idempotencyKey != null) {
            val exists = db.coinDao().hasTransactionWithKey(idempotencyKey) > 0
            if (exists) return@withContext false
        }
        val multiplier = com.focusflow.subscription.SubscriptionManager.getCurrentLimits().focusBitMultiplier
        val effectiveAmount = kotlin.math.round(amount * multiplier).toLong().coerceAtLeast(1L)
        val note = if (multiplier > 1.0) "$source (${multiplier}x Pro Boost)" else source
        db.coinDao().insertTransaction(
            CoinTransaction(
                amount = effectiveAmount,
                type = "EARNED",
                source = note,
                idempotencyKey = idempotencyKey
            )
        )
        return@withContext true
    }

    suspend fun earn(
        amount: Int,
        source: String,
        idempotencyKey: String? = null
    ): Boolean = earn(amount.toLong(), source, idempotencyKey)

    suspend fun purchaseCoinPack(coins: Long, priceDisplay: String, packName: String): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val db = database ?: return@withContext false
        db.coinDao().insertTransaction(
            CoinTransaction(
                amount = coins,
                type = "PURCHASE",
                source = "Purchased $packName ($priceDisplay)",
                idempotencyKey = "purchase_${System.currentTimeMillis()}"
            )
        )
        return@withContext true
    }

    suspend fun spend(amount: Long, source: String): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val db = database ?: return@withContext false
        val currentBalance = db.coinDao().getTotalBalanceDirect()
        if (currentBalance < amount) {
            return@withContext false // Insufficient balance
        }
        db.coinDao().insertTransaction(
            CoinTransaction(
                amount = -amount,
                type = "SPENT",
                source = source,
                idempotencyKey = null
            )
        )
        return@withContext true
    }

    suspend fun spend(amount: Int, source: String): Boolean = spend(amount.toLong(), source)

    fun getAllTransactionsFlow(): Flow<List<CoinTransaction>> {
        val db = database ?: return flowOf(emptyList())
        return db.coinDao().getAllTransactions()
    }
}
