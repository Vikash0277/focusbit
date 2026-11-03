package com.focusflow.subscription

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Entitlement {
    BASIC_SHIELD,
    ADVANCED_SHIELD,
    UNLIMITED_GOALS,
    UNLIMITED_HABITS,
    UNLIMITED_TODOS,
    ADVANCED_ANALYTICS,
    GOAL_LINKED_SHIELD,
    COIN_MULTIPLIER,
    NO_ADS,
    SCROLL_LIMIT_AUTOCLOSE
}

data class FeatureLimits(
    val maxGoals: Int?,
    val maxTodos: Int?,
    val maxHabits: Int?,
    val maxProtectedApps: Int?,
    val maxSchedules: Int?,
    val maxDailyUnlocks: Int?,
    val focusBitMultiplier: Double
)

data class SubscriptionPlan(
    val id: String,
    val title: String,
    val priceDisplay: String,
    val billingPeriod: String,
    val savingsBadge: String? = null
)

object SubscriptionManager {
    private const val PREFS_NAME = "focusbit_sub_prefs"
    private const val KEY_IS_PRO = "key_is_pro_subscriber"

    @Volatile
    private var appContext: Context? = null

    private val _isProFlow = MutableStateFlow(false)
    val isProFlow: StateFlow<Boolean> = _isProFlow.asStateFlow()
    val isPro: StateFlow<Boolean> = _isProFlow.asStateFlow()

    val FREE_LIMITS = FeatureLimits(
        maxGoals = 3,
        maxTodos = 30,
        maxHabits = 5,
        maxProtectedApps = 3,
        maxSchedules = 1,
        maxDailyUnlocks = 3,
        focusBitMultiplier = 1.0
    )

    val PRO_LIMITS = FeatureLimits(
        maxGoals = null, // Unlimited
        maxTodos = null, // Unlimited
        maxHabits = null, // Unlimited
        maxProtectedApps = null, // Unlimited
        maxSchedules = null, // Unlimited
        maxDailyUnlocks = 10,
        focusBitMultiplier = 1.5
    )

    fun canAddGoal(currentCount: Int): Boolean {
        val max = getCurrentLimits().maxGoals ?: return true
        return currentCount < max
    }

    fun canAddTodo(currentCount: Int): Boolean {
        val max = getCurrentLimits().maxTodos ?: return true
        return currentCount < max
    }

    fun canAddHabit(currentCount: Int): Boolean {
        val max = getCurrentLimits().maxHabits ?: return true
        return currentCount < max
    }

    fun canAddProtectedApp(currentCount: Int): Boolean {
        val max = getCurrentLimits().maxProtectedApps ?: return true
        return currentCount < max
    }

    fun canAddSchedule(currentCount: Int): Boolean {
        val max = getCurrentLimits().maxSchedules ?: return true
        return currentCount < max
    }

    fun canUnlockApp(todayUnlocks: Int): Boolean {
        val max = getCurrentLimits().maxDailyUnlocks ?: return true
        return todayUnlocks < max
    }

    val AVAILABLE_PLANS = listOf(
        SubscriptionPlan(
            id = "focusbit_annual",
            title = "Annual Membership",
            priceDisplay = "$39.99 / year",
            billingPeriod = "Billed annually ($3.33/mo)",
            savingsBadge = "SAVE 52% · BEST VALUE"
        ),
        SubscriptionPlan(
            id = "focusbit_monthly",
            title = "Monthly Plan",
            priceDisplay = "$6.99 / month",
            billingPeriod = "Billed monthly · Cancel anytime",
            savingsBadge = null
        )
    )

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isProFlow.value = prefs.getBoolean(KEY_IS_PRO, false)
    }

    fun hasEntitlement(entitlement: Entitlement): Boolean {
        if (_isProFlow.value) return true
        return when (entitlement) {
            Entitlement.BASIC_SHIELD -> true
            else -> false
        }
    }

    fun getCurrentLimits(): FeatureLimits {
        return if (_isProFlow.value) PRO_LIMITS else FREE_LIMITS
    }

    fun activatePro(planId: String = "annual") {
        appContext?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_IS_PRO, true).apply()
        }
        _isProFlow.value = true
    }

    fun deactivatePro() {
        appContext?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_IS_PRO, false).apply()
        }
        _isProFlow.value = false
    }

    fun upgradeToPro(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_PRO, true).apply()
        _isProFlow.value = true
    }

    fun restorePurchases(context: Context? = null): Boolean {
        val ctx = context ?: appContext
        val isProActive = if (ctx != null) {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getBoolean(KEY_IS_PRO, false)
        } else {
            _isProFlow.value
        }
        _isProFlow.value = isProActive
        return isProActive
    }
}
