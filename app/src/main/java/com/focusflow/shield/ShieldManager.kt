package com.focusflow.shield

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import android.text.TextUtils
import com.focusflow.data.db.entity.BlockedApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

data class AppScheduleInfo(
    val packageName: String,
    val appName: String = "",
    val startTime: String,
    val endTime: String,
    val isBlocked: Boolean,
    val isStrictLock: Boolean
)

object ShieldManager {
    private const val PREFS_NAME = "focusflow_shield_prefs"
    private const val KEY_SHIELD_ACTIVE = "key_shield_active"
    private const val KEY_SHIELD_END_TIME = "key_shield_end_time"
    private const val KEY_INTERCEPTIONS = "key_interceptions"
    private const val KEY_MASTER_START_TIME = "key_master_start_time"
    private const val KEY_MASTER_END_TIME = "key_master_end_time"

    private const val KEY_SAVED_SCHEDULES = "key_saved_schedules"

    @Volatile
    private var appContext: Context? = null

    private val DOMAIN_PACKAGE_MAP = mapOf(
        "instagram.com" to listOf("com.instagram.android", "com.instagram.lite"),
        "instagr.am" to listOf("com.instagram.android", "com.instagram.lite"),
        "facebook.com" to listOf("com.facebook.katana", "com.facebook.lite"),
        "fb.com" to listOf("com.facebook.katana", "com.facebook.lite"),
        "youtube.com" to listOf("com.google.android.youtube", "com.google.android.apps.youtube.mango"),
        "youtu.be" to listOf("com.google.android.youtube"),
        "twitter.com" to listOf("com.twitter.android"),
        "x.com" to listOf("com.twitter.android"),
        "tiktok.com" to listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
        "reddit.com" to listOf("com.reddit.frontpage"),
        "snapchat.com" to listOf("com.snapchat.android"),
        "linkedin.com" to listOf("com.linkedin.android"),
        "netflix.com" to listOf("com.netflix.mediaclient"),
        "twitch.tv" to listOf("tv.twitch.android.app"),
        "pinterest.com" to listOf("com.pinterest"),
        "threads.net" to listOf("com.instagram.barcelona")
    )

    private val _blockedPackageNames = CopyOnWriteArraySet<String>()
    val blockedPackageNames: Set<String> get() = _blockedPackageNames

    // Per-package schedule configuration: packageName -> AppScheduleInfo
    private val _appSchedules = ConcurrentHashMap<String, AppScheduleInfo>()
    val appSchedules: Map<String, AppScheduleInfo> get() = _appSchedules

    // Per-package lock expiration for minute countdowns (legacy / quick lock)
    private val _appLocks = ConcurrentHashMap<String, Long>()

    // Temporary unlock overrides (e.g., unlocked via coins)
    private val _temporaryUnlocks = ConcurrentHashMap<String, Long>()

    fun grantTemporaryUnlock(packageName: String, durationSeconds: Long, appName: String = "") {
        val expiry = System.currentTimeMillis() + (durationSeconds * 1000)
        _temporaryUnlocks[packageName] = expiry
        if (packageName.contains("instagram", ignoreCase = true)) {
            _temporaryUnlocks["com.instagram.android"] = expiry
            _temporaryUnlocks["com.instagram.lite"] = expiry
            _temporaryUnlocks["instagram.com"] = expiry
        }
        if (packageName.contains("facebook", ignoreCase = true)) {
            _temporaryUnlocks["com.facebook.katana"] = expiry
            _temporaryUnlocks["com.facebook.lite"] = expiry
            _temporaryUnlocks["facebook.com"] = expiry
        }

        appContext?.let { ctx ->
            val minutes = ((durationSeconds + 59) / 60).toInt().coerceAtLeast(1)
            val resolvedName = appName.ifBlank {
                _appSchedules[packageName]?.appName ?: packageName.substringAfterLast('.')
            }
            com.focusflow.notifications.AlarmScheduler.scheduleTemporaryUnlockOver(
                context = ctx,
                packageName = packageName,
                appName = resolvedName,
                durationMinutes = minutes
            )
        }
    }

    fun isTemporarilyUnlocked(packageName: String): Boolean {
        val now = System.currentTimeMillis()
        val exp = _temporaryUnlocks[packageName]
        if (exp != null && exp > now) return true
        for ((pkg, expiry) in _temporaryUnlocks) {
            if (expiry > now) {
                if (pkg.contains("instagram", true) && packageName.contains("instagram", true)) return true
                if (pkg.contains("facebook", true) && packageName.contains("facebook", true)) return true
            }
        }
        return false
    }

    private val _interceptionCount = MutableStateFlow(0)
    val interceptionCount: StateFlow<Int> = _interceptionCount.asStateFlow()

    private val _shieldActiveFlow = MutableStateFlow(false)
    val shieldActiveFlow: StateFlow<Boolean> = _shieldActiveFlow.asStateFlow()

    private val _remainingSecondsFlow = MutableStateFlow(0L)
    val remainingSecondsFlow: StateFlow<Long> = _remainingSecondsFlow.asStateFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = getPrefs(context)
        val active = prefs.getBoolean(KEY_SHIELD_ACTIVE, false)
        val endTime = prefs.getLong(KEY_SHIELD_END_TIME, 0L)
        val now = System.currentTimeMillis()

        if (active && endTime > now) {
            _shieldActiveFlow.value = true
            _remainingSecondsFlow.value = (endTime - now) / 1000
        } else {
            _shieldActiveFlow.value = false
            _remainingSecondsFlow.value = 0L
            if (active) {
                prefs.edit().putBoolean(KEY_SHIELD_ACTIVE, false).putLong(KEY_SHIELD_END_TIME, 0L).apply()
            }
        }
        _interceptionCount.value = prefs.getInt(KEY_INTERCEPTIONS, 0)

        // Synchronously load saved schedules from SharedPreferences
        val savedSchedules = prefs.getStringSet(KEY_SAVED_SCHEDULES, null)
        if (!savedSchedules.isNullOrEmpty()) {
            for (entry in savedSchedules) {
                val parts = entry.split("|||")
                if (parts.size >= 6) {
                    val pkg = parts[0]
                    val appName = parts[1]
                    val start = parts[2]
                    val end = parts[3]
                    val isBlocked = parts[4].toBoolean()
                    val isStrict = parts[5].toBoolean()
                    _appSchedules[pkg] = AppScheduleInfo(
                        packageName = pkg,
                        appName = appName,
                        startTime = start,
                        endTime = end,
                        isBlocked = isBlocked,
                        isStrictLock = isStrict
                    )
                    if (isBlocked) {
                        _blockedPackageNames.add(pkg)
                    }
                }
            }
        }

        // Also query Room in background to guarantee freshness
        syncFromDatabase(context)
    }

    fun syncFromDatabase(context: Context) {
        try {
            val app = context.applicationContext as? com.focusflow.FocusFlowApplication ?: return
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val apps = app.database.shieldDao().getAllBlockedAppsSync()
                    if (apps.isNotEmpty()) {
                        syncAppLocks(apps, context)
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    // --- Emergency Services Exemption ---

    fun isEmergencyPackage(packageName: String): Boolean {
        return InstalledAppUtils.isEmergencyPackage(packageName)
    }

    // --- Time Window Calculations ---

    fun isTimeWithinWindow(startTime: String, endTime: String): Boolean {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startParts = startTime.split(":")
        val endParts = endTime.split(":")
        val startMin = (startParts.getOrNull(0)?.toIntOrNull() ?: 9) * 60 + (startParts.getOrNull(1)?.toIntOrNull() ?: 0)
        val endMin = (endParts.getOrNull(0)?.toIntOrNull() ?: 22) * 60 + (endParts.getOrNull(1)?.toIntOrNull() ?: 0)

        if (startMin == endMin) {
            return true // 24-hour full-day lock
        }

        return if (startMin < endMin) {
            currentMinutes in startMin until endMin
        } else {
            // Window crosses midnight (e.g. 22:00 to 06:00)
            currentMinutes >= startMin || currentMinutes < endMin
        }
    }

    fun getWindowRemainingSeconds(startTime: String, endTime: String): Long {
        if (!isTimeWithinWindow(startTime, endTime)) return 0L
        val now = Calendar.getInstance()
        val currentSeconds = (now.get(Calendar.HOUR_OF_DAY) * 3600) + (now.get(Calendar.MINUTE) * 60) + now.get(Calendar.SECOND)

        val endParts = endTime.split(":")
        val endHour = endParts.getOrNull(0)?.toIntOrNull() ?: 22
        val endMinute = endParts.getOrNull(1)?.toIntOrNull() ?: 0
        var endSeconds = (endHour * 3600) + (endMinute * 60)

        val startParts = startTime.split(":")
        val startHour = startParts.getOrNull(0)?.toIntOrNull() ?: 9
        val startMinute = startParts.getOrNull(1)?.toIntOrNull() ?: 0
        val startSeconds = (startHour * 3600) + (startMinute * 60)

        if (startSeconds == endSeconds) {
            // 24-hour lock: ends in (24h - elapsed today)
            val daySeconds = 24 * 3600
            val elapsedSinceStart = (currentSeconds - startSeconds + daySeconds) % daySeconds
            return (daySeconds - elapsedSinceStart).toLong()
        }

        if (startSeconds > endSeconds) {
            // Window crosses midnight
            if (currentSeconds >= startSeconds) {
                endSeconds += 24 * 3600
            }
        }
        return (endSeconds - currentSeconds).coerceAtLeast(0).toLong()
    }

    fun formatTimeAmPm(timeString: String): String {
        val parts = timeString.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "%d:%02d %s".format(displayHour, minute, amPm)
    }

    // --- Syncing & Locking Logic ---

    fun syncAppLocks(apps: List<BlockedApp>, context: Context? = null) {
        val ctx = context ?: appContext
        _appSchedules.clear()
        _blockedPackageNames.clear()

        val serializedSet = mutableSetOf<String>()
        for (app in apps) {
            _appSchedules[app.packageName] = AppScheduleInfo(
                packageName = app.packageName,
                appName = app.appName,
                startTime = app.startTime,
                endTime = app.endTime,
                isBlocked = app.isBlocked,
                isStrictLock = app.isStrictLock
            )
            if (app.isBlocked) {
                _blockedPackageNames.add(app.packageName)
            }
            serializedSet.add("${app.packageName}|||${app.appName}|||${app.startTime}|||${app.endTime}|||${app.isBlocked}|||${app.isStrictLock}")
        }

        ctx?.let {
            getPrefs(it).edit().putStringSet(KEY_SAVED_SCHEDULES, serializedSet).apply()
        }
    }

    /**
     * Checks if a package is strictly locked and cannot be removed or modified.
     */
    fun isAppInStrictLock(app: BlockedApp): Boolean {
        if (!app.isBlocked || !app.isStrictLock) return false
        return isTimeWithinWindow(app.startTime, app.endTime)
    }

    fun isPackageInStrictLock(packageName: String): Boolean {
        val schedule = _appSchedules[packageName] ?: return false
        if (!schedule.isBlocked || !schedule.isStrictLock) return false
        return isTimeWithinWindow(schedule.startTime, schedule.endTime)
    }

    /**
     * Checks if the foreground application is currently locked.
     * Emergency packages are ALWAYS exempt.
     */
    fun isAppLocked(packageName: String): Boolean {
        if (isEmergencyPackage(packageName)) {
            return false
        }
        if (isTemporarilyUnlocked(packageName)) {
            return false
        }

        // Auto-heal from SharedPreferences if empty
        if (_appSchedules.isEmpty() && appContext != null) {
            init(appContext!!)
        }

        val schedule = _appSchedules[packageName]
        if (schedule != null && schedule.isBlocked) {
            if (isTimeWithinWindow(schedule.startTime, schedule.endTime)) {
                return true
            }
        }

        // Check countdown timer lock
        val countdownExpiry = _appLocks[packageName] ?: 0L
        if (countdownExpiry > System.currentTimeMillis()) {
            return true
        }

        // Check master global shield
        if (_shieldActiveFlow.value && isPackageBlocked(packageName)) {
            return true
        }

        return false
    }

    /**
     * Checks if text/URL from browser corresponds to an app that is currently locked.
     * Returns the locked package name, or null if no locked match is found.
     */
    fun findLockedPackageForBrowserText(text: String): String? {
        val lower = text.lowercase().trim()
        if (lower.isEmpty()) return null

        // 1. Direct domain match
        for ((domain, packages) in DOMAIN_PACKAGE_MAP) {
            if (lower.contains(domain)) {
                for (pkg in packages) {
                    if (isAppLocked(pkg)) {
                        return pkg
                    }
                }
                // Also check if any locked app matches domain keyword (e.g. "instagram")
                val keyword = domain.substringBefore('.')
                for ((pkg, info) in _appSchedules) {
                    if (isAppLocked(pkg)) {
                        if (info.appName.contains(keyword, ignoreCase = true) ||
                            pkg.contains(keyword, ignoreCase = true)
                        ) {
                            return pkg
                        }
                    }
                }
            }
        }

        // 2. Specific checks for popular social networks browsing
        if (lower.contains("facebook") || lower.contains("fb.com") || lower.contains("m.facebook") || lower.contains("fb.me")) {
            if (isAppLocked("com.facebook.katana")) return "com.facebook.katana"
            if (isAppLocked("com.facebook.lite")) return "com.facebook.lite"
            for ((pkg, info) in _appSchedules) {
                if (isAppLocked(pkg) && (info.appName.contains("facebook", ignoreCase = true) || pkg.contains("facebook", ignoreCase = true))) {
                    return pkg
                }
            }
        }

        if (lower.contains("instagram") || lower.contains("instagr.am")) {
            if (isAppLocked("com.instagram.android")) return "com.instagram.android"
            if (isAppLocked("com.instagram.lite")) return "com.instagram.lite"
            for ((pkg, info) in _appSchedules) {
                if (isAppLocked(pkg) && (info.appName.contains("instagram", ignoreCase = true) || pkg.contains("instagram", ignoreCase = true))) {
                    return pkg
                }
            }
        }

        // 3. Check each active locked app schedule
        for ((pkg, info) in _appSchedules) {
            if (isAppLocked(pkg)) {
                val cleanAppName = info.appName.trim().lowercase()
                if (cleanAppName.length >= 4 && lower.contains(cleanAppName)) {
                    val pattern = Regex("(?:^|[./?&=#])${Regex.escape(cleanAppName)}(?:[./?&=#]|$)")
                    if (pattern.containsMatchIn(lower) || lower.contains("$cleanAppName.com")) {
                        return pkg
                    }
                }

                val pkgSegment = pkg.substringAfterLast('.').lowercase()
                if (pkgSegment.length >= 4 && lower.contains(pkgSegment)) {
                    if (lower.contains("$pkgSegment.com") || lower.contains("m.$pkgSegment")) {
                        return pkg
                    }
                }
            }
        }

        return null
    }

    fun getAppEndTimeDisplay(packageName: String): String {
        val schedule = _appSchedules[packageName]
        if (schedule != null) {
            return formatTimeAmPm(schedule.endTime)
        }
        return "10:00 PM"
    }

    fun getAppStartTimeDisplay(packageName: String): String {
        val schedule = _appSchedules[packageName]
        if (schedule != null) {
            return formatTimeAmPm(schedule.startTime)
        }
        return "9:00 AM"
    }

    fun getAppRemainingSeconds(packageName: String): Long {
        val schedule = _appSchedules[packageName]
        if (schedule != null && schedule.isBlocked) {
            val windowRem = getWindowRemainingSeconds(schedule.startTime, schedule.endTime)
            if (windowRem > 0L) return windowRem
        }

        val lockedUntil = _appLocks[packageName] ?: 0L
        val now = System.currentTimeMillis()
        if (lockedUntil > now) {
            return (lockedUntil - now) / 1000L
        }
        if (_shieldActiveFlow.value && isPackageBlocked(packageName)) {
            return _remainingSecondsFlow.value
        }
        return 0L
    }

    // --- Master Time Window Preferences ---

    fun getMasterStartTime(context: Context): String {
        return getPrefs(context).getString(KEY_MASTER_START_TIME, "09:00") ?: "09:00"
    }

    fun getMasterEndTime(context: Context): String {
        return getPrefs(context).getString(KEY_MASTER_END_TIME, "22:00") ?: "22:00"
    }

    fun setMasterTimeWindow(context: Context, startTime: String, endTime: String) {
        getPrefs(context).edit()
            .putString(KEY_MASTER_START_TIME, startTime)
            .putString(KEY_MASTER_END_TIME, endTime)
            .apply()
    }

    fun isMasterWindowActive(context: Context): Boolean {
        val start = getMasterStartTime(context)
        val end = getMasterEndTime(context)
        return isTimeWithinWindow(start, end)
    }

    // --- Legacy / Quick Timed Shield ---

    fun isShieldActive(context: Context): Boolean {
        val prefs = getPrefs(context)
        val active = prefs.getBoolean(KEY_SHIELD_ACTIVE, false)
        if (!active) return false

        val endTime = prefs.getLong(KEY_SHIELD_END_TIME, 0L)
        val now = System.currentTimeMillis()
        if (endTime in 1..now) {
            cancelShield(context)
            return false
        }
        return true
    }

    fun startTimedShield(context: Context, durationMinutes: Int) {
        val now = System.currentTimeMillis()
        val endTime = now + (durationMinutes.toLong() * 60L * 1000L)
        getPrefs(context).edit()
            .putBoolean(KEY_SHIELD_ACTIVE, true)
            .putLong(KEY_SHIELD_END_TIME, endTime)
            .apply()

        _shieldActiveFlow.value = true
        _remainingSecondsFlow.value = durationMinutes.toLong() * 60L
        com.focusflow.notifications.AlarmScheduler.scheduleShieldTimerOver(context, durationMinutes)
    }

    fun cancelShield(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_SHIELD_ACTIVE, false)
            .putLong(KEY_SHIELD_END_TIME, 0L)
            .apply()

        _shieldActiveFlow.value = false
        _remainingSecondsFlow.value = 0L
        com.focusflow.notifications.AlarmScheduler.cancelShieldTimerOver(context)
    }

    fun addBlockedPackage(packageName: String) {
        _blockedPackageNames.add(packageName)
    }

    fun removeBlockedPackage(packageName: String) {
        _blockedPackageNames.remove(packageName)
        _appSchedules.remove(packageName)
        _appLocks.remove(packageName)
    }

    fun clearAllBlockedPackages() {
        _blockedPackageNames.clear()
        _appSchedules.clear()
        _appLocks.clear()
    }

    fun isPackageBlocked(packageName: String): Boolean {
        return _blockedPackageNames.contains(packageName)
    }

    fun recordInterception(context: Context) {
        val prefs = getPrefs(context)
        val count = prefs.getInt(KEY_INTERCEPTIONS, 0) + 1
        prefs.edit().putInt(KEY_INTERCEPTIONS, count).apply()
        _interceptionCount.value = count
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = ComponentName(context, FocusShieldAccessibilityService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent != null && enabledComponent == expectedComponentName) {
                return true
            }
        }
        return false
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
