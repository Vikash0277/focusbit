package com.focusflow.scroll

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppScrollInfo(
    val packageName: String,
    val appName: String,
    val scrollCount: Int,
    val lastScrolledMs: Long = System.currentTimeMillis()
)

data class AppAutoCloseConfig(
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean = false,
    val limit: Int = 50
)

data class OverlayColorTheme(
    val id: String,
    val name: String,
    val bgHex: String,
    val borderHex: String,
    val textHex: String = "#FFFFFF"
)

data class ScrollDayStats(
    val date: String,
    val totalScrolls: Int,
    val appList: List<AppScrollInfo>,
    val isOverlayEnabled: Boolean = true,
    val warningThreshold: Int = 50,
    val isAutoCloseEnabled: Boolean = false,
    val autoCloseLimit: Int = 50,
    val appAutoCloseConfigs: Map<String, AppAutoCloseConfig> = emptyMap(),
    val activeColorThemeId: String = "obsidian",
    val displayDurationSeconds: Int = 0,
    val isCounterSpecificAppsOnly: Boolean = false,
    val counterSpecificApps: Map<String, String> = emptyMap()
) {
    val distanceMeters: Float
        get() = totalScrolls * 0.15f // ~15 cm per typical phone flick

    fun formatDistance(): String {
        return if (distanceMeters >= 1000f) {
            String.format(Locale.US, "%.1f km", distanceMeters / 1000f)
        } else {
            String.format(Locale.US, "%.0f m", distanceMeters)
        }
    }

    fun getLandmarkComparison(): String {
        return when {
            totalScrolls < 50 -> "🌱 Just getting started — light scrolling today!"
            totalScrolls < 200 -> "⚽ Length of a full football field (~100m)!"
            totalScrolls < 600 -> "🗽 Taller than the Statue of Liberty (~93m)!"
            totalScrolls < 1500 -> "🗼 Taller than the Eiffel Tower (~330m)!"
            totalScrolls < 3000 -> "🏙️ Taller than Burj Khalifa (~828m)!"
            else -> "🚀 Over a kilometer scrolled — doomscroll alert!"
        }
    }
}

object ScrollTrackerManager {

    private const val PREFS_NAME = "focusflow_scroll_tracker"
    private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
    private const val KEY_WARNING_THRESHOLD = "warning_threshold"
    private const val KEY_AUTO_CLOSE_ENABLED = "auto_close_enabled"
    private const val KEY_AUTO_CLOSE_LIMIT = "auto_close_limit"
    private const val KEY_APP_AUTOCLOSE_CONFIGS = "app_autoclose_configs"
    private const val KEY_OVERLAY_THEME_ID = "overlay_theme_id"
    private const val KEY_OVERLAY_DISPLAY_DURATION = "overlay_display_duration"
    private const val KEY_CURRENT_DATE = "current_date"
    private const val KEY_DAILY_APP_DATA = "daily_app_data"
    private const val KEY_COUNTER_SPECIFIC_APPS_ONLY = "counter_specific_apps_only"
    private const val KEY_COUNTER_SPECIFIC_APPS = "counter_specific_apps"

    val COLOR_THEMES = listOf(
        OverlayColorTheme("obsidian", "Obsidian Violet", "#EE12111E", "#8B5CF6"),
        OverlayColorTheme("midnight", "Midnight Blue", "#EE0A0A0E", "#3B82F6"),
        OverlayColorTheme("emerald", "Emerald Green", "#EE062319", "#10B981"),
        OverlayColorTheme("crimson", "Neon Crimson", "#EE2B080D", "#EF4444"),
        OverlayColorTheme("amber", "Sunset Amber", "#EE261603", "#F59E0B"),
        OverlayColorTheme("cyan", "Cyber Cyan", "#EE041C24", "#06B6D4"),
        OverlayColorTheme("magenta", "Electric Pink", "#EE180D2B", "#EC4899"),
        OverlayColorTheme("slate", "Stealth Slate", "#EE1E2235", "#94A3B8")
    )

    private var prefs: SharedPreferences? = null

    // In-memory cache for fast accessibility event increments
    private val appScrollCounts = mutableMapOf<String, AppScrollInfo>()
    private val appAutoCloseSnooze = mutableMapOf<String, Int>()
    private val appAutoCloseConfigs = mutableMapOf<String, AppAutoCloseConfig>()
    private val counterSpecificApps = mutableMapOf<String, String>()
    private var currentDateStr: String = ""

    private val _statsFlow = MutableStateFlow(
        ScrollDayStats(
            date = getTodayDateString(),
            totalScrolls = 0,
            appList = emptyList(),
            isOverlayEnabled = true,
            warningThreshold = 50,
            isAutoCloseEnabled = false,
            autoCloseLimit = 50,
            appAutoCloseConfigs = emptyMap(),
            activeColorThemeId = "obsidian",
            displayDurationSeconds = 0,
            isCounterSpecificAppsOnly = false,
            counterSpecificApps = emptyMap()
        )
    )
    val statsFlow: StateFlow<ScrollDayStats> = _statsFlow.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadFromDisk()
        }
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    @Synchronized
    private fun checkDateRollover() {
        val today = getTodayDateString()
        if (currentDateStr != today) {
            currentDateStr = today
            appScrollCounts.clear()
            appAutoCloseSnooze.clear()
            loadFromDisk()
        }
    }

    @Synchronized
    private fun loadFromDisk() {
        val p = prefs ?: return
        val today = getTodayDateString()
        currentDateStr = today

        val savedDate = p.getString(KEY_CURRENT_DATE, "") ?: ""
        val overlayEnabled = p.getBoolean(KEY_OVERLAY_ENABLED, true)
        val threshold = p.getInt(KEY_WARNING_THRESHOLD, 50)
        val autoCloseEnabled = p.getBoolean(KEY_AUTO_CLOSE_ENABLED, false)
        val autoCloseLimit = p.getInt(KEY_AUTO_CLOSE_LIMIT, 50)
        val activeThemeId = p.getString(KEY_OVERLAY_THEME_ID, "obsidian") ?: "obsidian"

        appScrollCounts.clear()
        appAutoCloseSnooze.clear()
        appAutoCloseConfigs.clear()
        counterSpecificApps.clear()

        // Load specific apps for live counter
        val specificAppsOnly = p.getBoolean(KEY_COUNTER_SPECIFIC_APPS_ONLY, false)
        val specificAppsJsonStr = p.getString(KEY_COUNTER_SPECIFIC_APPS, "{}") ?: "{}"
        try {
            val json = JSONObject(specificAppsJsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val pkg = keys.next()
                val name = json.optString(pkg, pkg)
                counterSpecificApps[pkg] = name
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Load app-wise auto-close configs
        val configsJsonStr = p.getString(KEY_APP_AUTOCLOSE_CONFIGS, "{}") ?: "{}"
        try {
            val json = JSONObject(configsJsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val pkg = keys.next()
                val obj = json.getJSONObject(pkg)
                val appName = obj.optString("name", pkg)
                val enabled = obj.optBoolean("enabled", false)
                val limit = obj.optInt("limit", autoCloseLimit)
                appAutoCloseConfigs[pkg] = AppAutoCloseConfig(pkg, appName, enabled, limit)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (savedDate == today) {
            val jsonStr = p.getString(KEY_DAILY_APP_DATA, "[]") ?: "[]"
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val pkg = obj.getString("pkg")
                    val name = obj.optString("name", pkg)
                    val count = obj.getInt("count")
                    val time = obj.optLong("time", System.currentTimeMillis())
                    appScrollCounts[pkg] = AppScrollInfo(pkg, name, count, time)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // New day: clear previous day counts
            p.edit().putString(KEY_CURRENT_DATE, today).remove(KEY_DAILY_APP_DATA).apply()
        }

        emitUpdatedStats(overlayEnabled, threshold, autoCloseEnabled, autoCloseLimit, activeThemeId)
    }

    @Synchronized
    fun recordScroll(packageName: String, appName: String): Int {
        checkDateRollover()

        val existing = appScrollCounts[packageName]
        val newCount = (existing?.scrollCount ?: 0) + 1
        val resolvedName = if (appName.isNotBlank()) appName else (existing?.appName ?: packageName)

        appScrollCounts[packageName] = AppScrollInfo(
            packageName = packageName,
            appName = resolvedName,
            scrollCount = newCount,
            lastScrolledMs = System.currentTimeMillis()
        )

        emitUpdatedStats(
            overlayEnabled = isOverlayEnabled(),
            threshold = getWarningThreshold(),
            autoCloseEnabled = isAutoCloseEnabled(),
            autoCloseLimit = getAutoCloseLimit(),
            activeThemeId = getOverlayThemeId()
        )
        saveToDiskDebounced()

        return newCount
    }

    fun getAppScrollCount(packageName: String): Int {
        checkDateRollover()
        return appScrollCounts[packageName]?.scrollCount ?: 0
    }

    fun getTotalScrollsToday(): Int {
        checkDateRollover()
        return appScrollCounts.values.sumOf { it.scrollCount }
    }

    // Callbacks to communicate live with active ScrollOverlayManager
    var onResetPosition: (() -> Unit)? = null
    var onRefreshTheme: (() -> Unit)? = null
    var onDurationChanged: ((Int) -> Unit)? = null

    fun resetPositionToTopMiddle() {
        onResetPosition?.invoke()
    }

    fun triggerRefreshTheme() {
        onRefreshTheme?.invoke()
    }

    fun isOverlayEnabled(): Boolean {
        return prefs?.getBoolean(KEY_OVERLAY_ENABLED, true) ?: true
    }

    fun setOverlayEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_OVERLAY_ENABLED, enabled)?.apply()
        emitUpdatedStats(enabled, getWarningThreshold(), isAutoCloseEnabled(), getAutoCloseLimit(), getOverlayThemeId())
        onRefreshTheme?.invoke()
    }

    fun getWarningThreshold(): Int {
        return prefs?.getInt(KEY_WARNING_THRESHOLD, 50) ?: 50
    }

    fun setWarningThreshold(threshold: Int) {
        prefs?.edit()?.putInt(KEY_WARNING_THRESHOLD, threshold)?.apply()
        emitUpdatedStats(isOverlayEnabled(), threshold, isAutoCloseEnabled(), getAutoCloseLimit(), getOverlayThemeId())
    }

    // Master Auto-Close switch
    fun isAutoCloseEnabled(): Boolean {
        return prefs?.getBoolean(KEY_AUTO_CLOSE_ENABLED, false) ?: false
    }

    fun setAutoCloseEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_AUTO_CLOSE_ENABLED, enabled)?.apply()
        emitUpdatedStats(isOverlayEnabled(), getWarningThreshold(), enabled, getAutoCloseLimit(), getOverlayThemeId())
    }

    fun getAutoCloseLimit(): Int {
        return prefs?.getInt(KEY_AUTO_CLOSE_LIMIT, 50) ?: 50
    }

    fun setAutoCloseLimit(limit: Int) {
        prefs?.edit()?.putInt(KEY_AUTO_CLOSE_LIMIT, limit)?.apply()
        emitUpdatedStats(isOverlayEnabled(), getWarningThreshold(), isAutoCloseEnabled(), limit, getOverlayThemeId())
    }

    // App-Wise Auto-Close Configurations
    fun isAppAutoCloseEnabled(packageName: String): Boolean {
        return appAutoCloseConfigs[packageName]?.isEnabled ?: false
    }

    fun setAppAutoCloseEnabled(packageName: String, appName: String, enabled: Boolean) {
        val existing = appAutoCloseConfigs[packageName]
        val limit = existing?.limit ?: getAutoCloseLimit()
        val name = if (appName.isNotBlank()) appName else (existing?.appName ?: packageName)
        appAutoCloseConfigs[packageName] = AppAutoCloseConfig(packageName, name, enabled, limit)
        saveAppAutoCloseConfigsAsync()
        emitUpdatedStats(isOverlayEnabled(), getWarningThreshold(), isAutoCloseEnabled(), getAutoCloseLimit(), getOverlayThemeId())
    }

    fun getAppAutoCloseLimit(packageName: String): Int {
        return appAutoCloseConfigs[packageName]?.limit ?: getAutoCloseLimit()
    }

    fun setAppAutoCloseLimit(packageName: String, appName: String, limit: Int) {
        val existing = appAutoCloseConfigs[packageName]
        val enabled = existing?.isEnabled ?: true
        val name = if (appName.isNotBlank()) appName else (existing?.appName ?: packageName)
        appAutoCloseConfigs[packageName] = AppAutoCloseConfig(packageName, name, enabled, limit)
        saveAppAutoCloseConfigsAsync()
        emitUpdatedStats(isOverlayEnabled(), getWarningThreshold(), isAutoCloseEnabled(), getAutoCloseLimit(), getOverlayThemeId())
    }

    fun addAppAutoCloseConfig(packageName: String, appName: String, limit: Int = getAutoCloseLimit()) {
        appAutoCloseConfigs[packageName] = AppAutoCloseConfig(packageName, appName, isEnabled = true, limit = limit)
        saveAppAutoCloseConfigsAsync()
        emitUpdatedStats(isOverlayEnabled(), getWarningThreshold(), isAutoCloseEnabled(), getAutoCloseLimit(), getOverlayThemeId())
    }

    fun removeAppAutoCloseConfig(packageName: String) {
        appAutoCloseConfigs.remove(packageName)
        appAutoCloseSnooze.remove(packageName)
        saveAppAutoCloseConfigsAsync()
        emitUpdatedStats(isOverlayEnabled(), getWarningThreshold(), isAutoCloseEnabled(), getAutoCloseLimit(), getOverlayThemeId())
    }


    fun getEffectiveAppAutoCloseLimit(packageName: String): Int {
        val baseLimit = getAppAutoCloseLimit(packageName)
        val snooze = appAutoCloseSnooze[packageName] ?: 0
        return baseLimit + snooze
    }

    fun addAutoCloseSnooze(packageName: String, additionalScrolls: Int = 25) {
        val currentSnooze = appAutoCloseSnooze[packageName] ?: 0
        appAutoCloseSnooze[packageName] = currentSnooze + additionalScrolls
    }

    // Color theme customization
    fun getOverlayThemeId(): String {
        return prefs?.getString(KEY_OVERLAY_THEME_ID, "obsidian") ?: "obsidian"
    }

    fun getOverlayTheme(): OverlayColorTheme {
        val id = getOverlayThemeId()
        return COLOR_THEMES.find { it.id == id } ?: COLOR_THEMES[0]
    }

    fun setOverlayTheme(themeId: String) {
        prefs?.edit()?.putString(KEY_OVERLAY_THEME_ID, themeId)?.apply()
        emitUpdatedStats(isOverlayEnabled(), getWarningThreshold(), isAutoCloseEnabled(), getAutoCloseLimit(), themeId)
        onRefreshTheme?.invoke()
    }

    fun getOverlayDisplayDuration(): Int {
        return prefs?.getInt(KEY_OVERLAY_DISPLAY_DURATION, 0) ?: 0
    }

    fun setOverlayDisplayDuration(seconds: Int) {
        prefs?.edit()?.putInt(KEY_OVERLAY_DISPLAY_DURATION, seconds)?.apply()
        emitUpdatedStats(
            overlayEnabled = isOverlayEnabled(),
            threshold = getWarningThreshold(),
            autoCloseEnabled = isAutoCloseEnabled(),
            autoCloseLimit = getAutoCloseLimit(),
            activeThemeId = getOverlayThemeId(),
            displayDuration = seconds
        )
        onDurationChanged?.invoke(seconds)
    }

    // Specific Apps live counter configuration
    fun isCounterSpecificAppsOnly(): Boolean {
        return prefs?.getBoolean(KEY_COUNTER_SPECIFIC_APPS_ONLY, false) ?: false
    }

    fun setCounterSpecificAppsOnly(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_COUNTER_SPECIFIC_APPS_ONLY, enabled)?.apply()
        emitUpdatedStats(
            overlayEnabled = isOverlayEnabled(),
            threshold = getWarningThreshold(),
            autoCloseEnabled = isAutoCloseEnabled(),
            autoCloseLimit = getAutoCloseLimit(),
            activeThemeId = getOverlayThemeId(),
            displayDuration = getOverlayDisplayDuration(),
            isCounterSpecificAppsOnly = enabled
        )
        onRefreshTheme?.invoke()
    }

    fun isAppSelectedForCounter(packageName: String): Boolean {
        if (!isCounterSpecificAppsOnly()) return true
        return counterSpecificApps.containsKey(packageName)
    }

    fun addCounterSpecificApp(packageName: String, appName: String) {
        counterSpecificApps[packageName] = if (appName.isNotBlank()) appName else packageName
        saveCounterSpecificAppsAsync()
        emitUpdatedStats(
            overlayEnabled = isOverlayEnabled(),
            threshold = getWarningThreshold(),
            autoCloseEnabled = isAutoCloseEnabled(),
            autoCloseLimit = getAutoCloseLimit(),
            activeThemeId = getOverlayThemeId(),
            displayDuration = getOverlayDisplayDuration()
        )
        onRefreshTheme?.invoke()
    }

    fun removeCounterSpecificApp(packageName: String) {
        counterSpecificApps.remove(packageName)
        saveCounterSpecificAppsAsync()
        emitUpdatedStats(
            overlayEnabled = isOverlayEnabled(),
            threshold = getWarningThreshold(),
            autoCloseEnabled = isAutoCloseEnabled(),
            autoCloseLimit = getAutoCloseLimit(),
            activeThemeId = getOverlayThemeId(),
            displayDuration = getOverlayDisplayDuration()
        )
        onRefreshTheme?.invoke()
    }

    fun resetToday() {
        appScrollCounts.clear()
        appAutoCloseSnooze.clear()
        prefs?.edit()?.remove(KEY_DAILY_APP_DATA)?.apply()
        emitUpdatedStats(isOverlayEnabled(), getWarningThreshold(), isAutoCloseEnabled(), getAutoCloseLimit(), getOverlayThemeId())
    }

    private fun emitUpdatedStats(
        overlayEnabled: Boolean,
        threshold: Int,
        autoCloseEnabled: Boolean = isAutoCloseEnabled(),
        autoCloseLimit: Int = getAutoCloseLimit(),
        activeThemeId: String = getOverlayThemeId(),
        displayDuration: Int = getOverlayDisplayDuration(),
        isCounterSpecificAppsOnly: Boolean = isCounterSpecificAppsOnly(),
        counterSpecificAppsMap: Map<String, String> = HashMap(counterSpecificApps)
    ) {
        val sortedList = appScrollCounts.values.sortedByDescending { it.scrollCount }
        val total = sortedList.sumOf { it.scrollCount }
        _statsFlow.value = ScrollDayStats(
            date = currentDateStr,
            totalScrolls = total,
            appList = sortedList,
            isOverlayEnabled = overlayEnabled,
            warningThreshold = threshold,
            isAutoCloseEnabled = autoCloseEnabled,
            autoCloseLimit = autoCloseLimit,
            appAutoCloseConfigs = HashMap(appAutoCloseConfigs),
            activeColorThemeId = activeThemeId,
            displayDurationSeconds = displayDuration,
            isCounterSpecificAppsOnly = isCounterSpecificAppsOnly,
            counterSpecificApps = counterSpecificAppsMap
        )
    }

    private fun saveCounterSpecificAppsAsync() {
        val p = prefs ?: return
        try {
            val json = JSONObject()
            counterSpecificApps.forEach { (pkg, name) ->
                json.put(pkg, name)
            }
            p.edit().putString(KEY_COUNTER_SPECIFIC_APPS, json.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveAppAutoCloseConfigsAsync() {
        val p = prefs ?: return
        try {
            val json = JSONObject()
            appAutoCloseConfigs.forEach { (pkg, config) ->
                val obj = JSONObject().apply {
                    put("name", config.appName)
                    put("enabled", config.isEnabled)
                    put("limit", config.limit)
                }
                json.put(pkg, obj)
            }
            p.edit().putString(KEY_APP_AUTOCLOSE_CONFIGS, json.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var lastSaveDiskMs: Long = 0L

    private fun saveToDiskDebounced() {
        val now = System.currentTimeMillis()
        if (now - lastSaveDiskMs >= 1500L) {
            lastSaveDiskMs = now
            saveToDiskAsync()
        }
    }

    private fun saveToDiskAsync() {
        val p = prefs ?: return
        try {
            val array = JSONArray()
            appScrollCounts.values.forEach { info ->
                val obj = JSONObject().apply {
                    put("pkg", info.packageName)
                    put("name", info.appName)
                    put("count", info.scrollCount)
                    put("time", info.lastScrolledMs)
                }
                array.put(obj)
            }
            p.edit()
                .putString(KEY_CURRENT_DATE, currentDateStr)
                .putString(KEY_DAILY_APP_DATA, array.toString())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
