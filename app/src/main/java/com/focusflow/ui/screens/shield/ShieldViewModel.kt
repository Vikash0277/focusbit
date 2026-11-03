package com.focusflow.ui.screens.shield

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.focusflow.data.db.FocusFlowDatabase
import com.focusflow.data.db.entity.BlockSchedule
import com.focusflow.data.db.entity.BlockedApp
import com.focusflow.shield.InstalledAppInfo
import com.focusflow.shield.InstalledAppUtils
import com.focusflow.shield.ShieldManager
import com.focusflow.subscription.SubscriptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ShieldUiState(
    val masterStartTime: String = "09:00",
    val masterEndTime: String = "22:00",
    val isMasterWindowActive: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val blockedApps: List<BlockedApp> = emptyList(),
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val isLoadingApps: Boolean = false,
    val schedules: List<BlockSchedule> = emptyList(),
    val interceptionsToday: Int = 0,
    val currentTimeMillis: Long = System.currentTimeMillis(),
    val lockedAppsCount: Int = 0,
    val strictLockAlertMessage: String? = null,
    val showPaywall: Boolean = false
)

class ShieldViewModel(
    private val application: Application,
    private val database: FocusFlowDatabase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        ShieldUiState(
            masterStartTime = ShieldManager.getMasterStartTime(application),
            masterEndTime = ShieldManager.getMasterEndTime(application)
        )
    )
    val uiState: StateFlow<ShieldUiState> = _uiState.asStateFlow()

    init {
        ShieldManager.init(application)
        checkAccessibilityPermission()
        loadBlockedApps()
        loadSchedules()
        loadInstalledApps()
        startTimeWindowObserver()
    }

    private fun loadBlockedApps() {
        viewModelScope.launch {
            database.shieldDao().getAllBlockedApps().collect { apps ->
                ShieldManager.syncAppLocks(apps)
                val lockedCount = apps.count { ShieldManager.isAppInStrictLock(it) }
                _uiState.update {
                    it.copy(
                        blockedApps = apps,
                        lockedAppsCount = lockedCount
                    )
                }
            }
        }
    }

    private fun loadSchedules() {
        viewModelScope.launch {
            database.shieldDao().getAllSchedules().collect { schedules ->
                _uiState.update { it.copy(schedules = schedules) }
            }
        }
    }

    fun loadInstalledApps() {
        _uiState.update { it.copy(isLoadingApps = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val apps = InstalledAppUtils.getInstalledLauncherApps(application)
            _uiState.update { it.copy(installedApps = apps, isLoadingApps = false) }
        }
    }

    private fun startTimeWindowObserver() {
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val masterActive = ShieldManager.isTimeWithinWindow(
                    _uiState.value.masterStartTime,
                    _uiState.value.masterEndTime
                )
                val apps = _uiState.value.blockedApps
                val lockedCount = apps.count { ShieldManager.isAppInStrictLock(it) }

                _uiState.update {
                    it.copy(
                        isMasterWindowActive = masterActive,
                        interceptionsToday = ShieldManager.interceptionCount.value,
                        currentTimeMillis = now,
                        lockedAppsCount = lockedCount
                    )
                }
                delay(1000)
            }
        }
    }

    fun checkAccessibilityPermission() {
        val enabled = ShieldManager.isAccessibilityServiceEnabled(application)
        _uiState.update { it.copy(isAccessibilityEnabled = enabled) }
    }

    fun openAccessibilitySettings() {
        ShieldManager.openAccessibilitySettings(application)
    }

    fun clearStrictLockAlert() {
        _uiState.update { it.copy(strictLockAlertMessage = null) }
    }

    fun showStrictLockAlert(message: String) {
        _uiState.update { it.copy(strictLockAlertMessage = message) }
    }

    // --- Time Window & Strict Lock Controls ---

    fun setAppTimeWindow(app: BlockedApp, startTime: String, endTime: String) {
        viewModelScope.launch {
            database.shieldDao().updateAppTimeWindow(app.id, startTime, endTime)
        }
    }

    fun setMasterTimeWindow(startTime: String, endTime: String) {
        ShieldManager.setMasterTimeWindow(application, startTime, endTime)
        viewModelScope.launch {
            database.shieldDao().updateAllTimeWindows(startTime, endTime)
            _uiState.update {
                it.copy(
                    masterStartTime = startTime,
                    masterEndTime = endTime,
                    isMasterWindowActive = ShieldManager.isTimeWithinWindow(startTime, endTime)
                )
            }
        }
    }

    /**
     * Attempts to toggle app. If the app is currently in an active strict-lock window,
     * toggling off is strictly blocked.
     */
    fun toggleAppWithStrictCheck(app: BlockedApp) {
        if (app.isBlocked && ShieldManager.isAppInStrictLock(app)) {
            val endDisplay = ShieldManager.formatTimeAmPm(app.endTime)
            _uiState.update {
                it.copy(
                    strictLockAlertMessage = "Strict Lock Active: ${app.appName} is shielded until $endDisplay and cannot be disabled."
                )
            }
            return
        }

        viewModelScope.launch {
            val newBlocked = !app.isBlocked
            database.shieldDao().toggleAppBlocked(app.id, newBlocked)
            if (newBlocked) {
                ShieldManager.addBlockedPackage(app.packageName)
            } else {
                ShieldManager.removeBlockedPackage(app.packageName)
            }
        }
    }

    /**
     * Attempts to delete app. If the app is currently in an active strict-lock window,
     * deletion is strictly blocked until the window completes.
     */
    fun deleteAppWithStrictCheck(app: BlockedApp) {
        if (app.isBlocked && ShieldManager.isAppInStrictLock(app)) {
            val endDisplay = ShieldManager.formatTimeAmPm(app.endTime)
            _uiState.update {
                it.copy(
                    strictLockAlertMessage = "Strict Lock Active: Cannot remove or delete ${app.appName} until the focus window completes at $endDisplay."
                )
            }
            return
        }

        viewModelScope.launch {
            database.shieldDao().deleteBlockedApp(app)
            ShieldManager.removeBlockedPackage(app.packageName)
        }
    }

    fun clearAllBlockedApps() {
        viewModelScope.launch {
            database.shieldDao().deleteAllBlockedApps()
            ShieldManager.clearAllBlockedPackages()
            ShieldManager.cancelShield(application)
        }
    }

    fun dismissPaywall() {
        _uiState.update { it.copy(showPaywall = false) }
    }

    fun addAppToShield(
        appInfo: InstalledAppInfo,
        startTime: String = _uiState.value.masterStartTime,
        endTime: String = _uiState.value.masterEndTime
    ) {
        if (!SubscriptionManager.canAddProtectedApp(_uiState.value.blockedApps.size)) {
            _uiState.update { it.copy(showPaywall = true) }
            return
        }
        viewModelScope.launch {
            database.shieldDao().insertBlockedApp(
                BlockedApp(
                    appName = appInfo.appName,
                    packageName = appInfo.packageName,
                    category = if (appInfo.isSystemApp) "System App" else "User App",
                    iconName = "smart_display",
                    isBlocked = true,
                    startTime = startTime,
                    endTime = endTime,
                    isStrictLock = true
                )
            )
            ShieldManager.addBlockedPackage(appInfo.packageName)
        }
    }

    fun addMultipleAppsToShield(
        apps: List<InstalledAppInfo>,
        startTime: String = _uiState.value.masterStartTime,
        endTime: String = _uiState.value.masterEndTime
    ) {
        val currentBlockedCount = _uiState.value.blockedApps.size
        val isProSubscriber = SubscriptionManager.isPro.value
        val freeMax = SubscriptionManager.FREE_LIMITS.maxProtectedApps ?: 3

        if (!isProSubscriber && (currentBlockedCount + apps.size) > freeMax) {
            _uiState.update { it.copy(showPaywall = true) }
            return
        }

        viewModelScope.launch {
            val existingPkgs = _uiState.value.blockedApps.map { it.packageName }.toSet()
            val appsToAdd = apps.filter {
                !existingPkgs.contains(it.packageName) &&
                !ShieldManager.isEmergencyPackage(it.packageName)
            }
            if (appsToAdd.isEmpty()) return@launch

            val blockedList = appsToAdd.map { appInfo ->
                BlockedApp(
                    appName = appInfo.appName,
                    packageName = appInfo.packageName,
                    category = if (appInfo.isSystemApp) "System App" else "User App",
                    iconName = "smart_display",
                    isBlocked = true,
                    startTime = startTime,
                    endTime = endTime,
                    isStrictLock = true
                )
            }
            database.shieldDao().insertBlockedApps(blockedList)
            for (app in appsToAdd) {
                ShieldManager.addBlockedPackage(app.packageName)
            }
        }
    }

    fun addAllAppsToShield(
        apps: List<InstalledAppInfo>,
        startTime: String = _uiState.value.masterStartTime,
        endTime: String = _uiState.value.masterEndTime
    ) {
        addMultipleAppsToShield(apps, startTime, endTime)
    }

    // --- Schedules ---

    fun addSchedule(name: String, start: String, end: String, days: String) {
        if (name.isBlank()) return
        if (!SubscriptionManager.canAddSchedule(_uiState.value.schedules.size)) {
            _uiState.update { it.copy(showPaywall = true) }
            return
        }
        viewModelScope.launch {
            database.shieldDao().insertSchedule(
                BlockSchedule(
                    name = name.trim(),
                    startTime = start,
                    endTime = end,
                    daysOfWeek = days,
                    iconName = "psychology",
                    appsCount = _uiState.value.blockedApps.size,
                    isAutomatic = true
                )
            )
        }
    }

    class Factory(
        private val application: Application,
        private val database: FocusFlowDatabase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ShieldViewModel(application, database) as T
        }
    }
}
