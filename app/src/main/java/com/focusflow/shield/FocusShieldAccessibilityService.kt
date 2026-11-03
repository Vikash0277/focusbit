package com.focusflow.shield

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.focusflow.FocusFlowApplication
import com.focusflow.scroll.ScrollLimitPopupActivity
import com.focusflow.scroll.ScrollOverlayManager
import com.focusflow.scroll.ScrollTrackerManager
import com.focusflow.subscription.SubscriptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class FocusShieldAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var lastBlockedKey: String? = null
    private var lastBlockedTimestamp: Long = 0L

    private var scrollOverlayManager: ScrollOverlayManager? = null
    private var lastRecordedScrollTime: Long = 0L
    private val appNameCache = mutableMapOf<String, String>()
    private var currentForegroundPackage: String? = null
    private val launcherPackagesCache = mutableSetOf<String>()

    // YouTube Shorts & Reels tracking state
    private var lastShortId: String? = null
    private val lastAutoCloseTimestamp = mutableMapOf<String, Long>()

    companion object {
        private const val TAG = "FocusShield"
        private const val MIN_SCROLL_DEBOUNCE_MS = 400L
        private val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.chrome.canary",
            "com.sec.android.app.sbrowser",
            "com.sec.android.app.sbrowser.beta",
            "com.microsoft.emmx",
            "org.mozilla.firefox",
            "org.mozilla.firefox_beta",
            "org.mozilla.fenix",
            "com.brave.browser",
            "com.opera.browser",
            "com.opera.mini.native",
            "com.opera.gx",
            "com.duckduckgo.mobile.android",
            "com.vivaldi.browser",
            "com.android.browser",
            "com.mi.globalbrowser",
            "org.torproject.torbrowser",
            "com.kiwibrowser.browser",
            "com.ecosia.android",
            "com.heytap.browser"
        )
    }

    override fun onCreate() {
        super.onCreate()
        ShieldManager.init(applicationContext)
        ScrollTrackerManager.init(applicationContext)
        SubscriptionManager.init(applicationContext)
        if (scrollOverlayManager == null) {
            scrollOverlayManager = ScrollOverlayManager(this)
        }
        startDatabaseSync()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        ShieldManager.init(applicationContext)
        ScrollTrackerManager.init(applicationContext)
        SubscriptionManager.init(applicationContext)
        if (scrollOverlayManager == null) {
            scrollOverlayManager = ScrollOverlayManager(this)
        }

        // Dynamically ensure FLAG_INCLUDE_NOT_IMPORTANT_VIEWS is enabled
        // This is critical for YouTube / Shorts Litho & Compose custom views
        try {
            val info = serviceInfo ?: AccessibilityServiceInfo()
            info.flags = info.flags or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            serviceInfo = info
        } catch (e: Exception) {
            Log.e(TAG, "Error updating serviceInfo flags", e)
        }

        // Real-time live synchronization with user settings (color change, enable/disable toggle)
        serviceScope.launch {
            ScrollTrackerManager.statsFlow
                .map { Triple(it.isOverlayEnabled, it.activeColorThemeId, it.warningThreshold) }
                .distinctUntilChanged()
                .collect { (overlayEnabled, _, threshold) ->
                    if (!overlayEnabled) {
                        scrollOverlayManager?.hideImmediate()
                    } else {
                        val currentPkg = currentForegroundPackage
                        val myPackage = applicationContext.packageName
                        if (currentPkg != null && !isLauncherPackage(currentPkg) && currentPkg != myPackage) {
                            val count = ScrollTrackerManager.getAppScrollCount(currentPkg)
                            scrollOverlayManager?.refreshThemeColor(count, threshold)
                        } else {
                            // User is on home screen / launcher or in FocusFlow: keep hidden!
                            scrollOverlayManager?.hideImmediate()
                        }
                    }
                }
        }

        startDatabaseSync()
    }

    override fun onDestroy() {
        super.onDestroy()
        scrollOverlayManager?.destroy()
        scrollOverlayManager = null
    }

    private fun startDatabaseSync() {
        try {
            val app = applicationContext as? FocusFlowApplication ?: return
            serviceScope.launch {
                try {
                    app.database.shieldDao().getAllBlockedApps().collect { apps ->
                        ShieldManager.syncAppLocks(apps, applicationContext)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error collecting blocked apps from DB", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting database sync", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        val myPackage = applicationContext.packageName

        // CRITICAL: Emergency Services Exemption
        // Phone, Dialer, Telecom, 112/SOS, Emergency services are never blocked under any condition
        if (ShieldManager.isEmergencyPackage(packageName)) {
            return
        }

        // Do NOT process temporary system overlays, volume panels, or keyboards
        if (packageName == "com.android.systemui" || packageName == "android") {
            return
        }

        val isLauncher = isLauncherPackage(packageName)
        val isOwnApp = packageName == myPackage

        // Window state changes:
        // - When user goes to Home screen / launcher or FocusFlow: HIDE the counter pod completely!
        // - When user enters and uses any other app: SHOW the counter pod with that app's scroll count.
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val appChanged = currentForegroundPackage != packageName
            currentForegroundPackage = packageName

            if (isLauncher || isOwnApp) {
                scrollOverlayManager?.hideImmediate()
            } else if (ScrollTrackerManager.isOverlayEnabled()) {
                val isCounterAllowed = !ScrollTrackerManager.isCounterSpecificAppsOnly() ||
                    ScrollTrackerManager.isAppSelectedForCounter(packageName)

                if (!isCounterAllowed) {
                    scrollOverlayManager?.hideImmediate()
                } else {
                    val duration = ScrollTrackerManager.getOverlayDisplayDuration()
                    if (appChanged || duration == 0) {
                        val currentCount = ScrollTrackerManager.getAppScrollCount(packageName)
                        val threshold = ScrollTrackerManager.getWarningThreshold()
                        scrollOverlayManager?.showPersistent(currentCount, threshold)
                    }
                }
            }
        }

        // If user is inside FocusFlow or on the launcher, skip app blocking & scroll tracking logic
        if (isLauncher || isOwnApp) {
            return
        }

        // Scroll Tracking: standard scroll events (feeds, recyclers, search)
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            handleScrollEvent(packageName)
        }

        // YouTube Shorts & Reel gesture detection (swiping between shorts emits content changed)
        if (isYouTubePackage(packageName)) {
            checkYouTubeShortsSwipe(event, packageName)
        }

        // Case 1: Direct native application lock check
        if (ShieldManager.isAppLocked(packageName)) {
            triggerBlockScreen(packageName, isWebInterception = false)
            return
        }

        // Case 2: Browser interception (when user opens or browses a shielded site like instagram.com on mobile)
        if (isBrowserPackage(packageName)) {
            checkBrowserForShieldedSites(event, packageName)
        }
    }

    private fun isYouTubePackage(packageName: String): Boolean {
        return packageName == "com.google.android.youtube" ||
            packageName == "com.google.android.apps.youtube.music" ||
            packageName == "com.google.android.apps.youtube.creator"
    }

    private fun checkYouTubeShortsSwipe(event: AccessibilityEvent, packageName: String) {
        // YouTube Shorts can transition via TYPE_WINDOW_CONTENT_CHANGED
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return
        }

        val rootNode = try {
            rootInActiveWindow ?: event.source
        } catch (e: Exception) {
            null
        } ?: return

        try {
            // Well-known YouTube Shorts view IDs
            val shortsTitleIds = listOf(
                "$packageName:id/reel_player_title_view",
                "$packageName:id/reel_player_video_title",
                "$packageName:id/reel_video_title",
                "$packageName:id/reel_player_channel_name",
                "$packageName:id/shorts_container",
                "$packageName:id/reel_recycler_view"
            )

            var detectedIdentifier: String? = null
            for (id in shortsTitleIds) {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
                if (!nodes.isNullOrEmpty()) {
                    for (node in nodes) {
                        val text = node.text?.toString() ?: node.contentDescription?.toString()
                        if (!text.isNullOrBlank()) {
                            detectedIdentifier = text
                            break
                        }
                    }
                }
                if (detectedIdentifier != null) break
            }

            // Fallback: check event texts if reel player/viewpager is active
            if (detectedIdentifier == null && (
                event.className?.contains("Reel", ignoreCase = true) == true ||
                event.className?.contains("ViewPager", ignoreCase = true) == true
            )) {
                for (txt in event.text) {
                    val str = txt?.toString()
                    if (!str.isNullOrBlank() && str.length > 2) {
                        detectedIdentifier = str
                        break
                    }
                }
            }

            if (detectedIdentifier != null && detectedIdentifier != lastShortId) {
                lastShortId = detectedIdentifier
                // Only count as scroll if a scroll wasn't already recorded by TYPE_VIEW_SCROLLED recently
                handleScrollEvent(packageName)
            }
        } catch (e: Exception) {
            // Graceful fallback
        }
    }

    private fun handleScrollEvent(packageName: String) {
        val now = System.currentTimeMillis()
        if (now - lastRecordedScrollTime < MIN_SCROLL_DEBOUNCE_MS) {
            return
        }
        lastRecordedScrollTime = now

        val isCounterAllowed = !ScrollTrackerManager.isCounterSpecificAppsOnly() ||
            ScrollTrackerManager.isAppSelectedForCounter(packageName)

        // If specific apps mode is enabled, only record and show counter for selected apps
        if (ScrollTrackerManager.isCounterSpecificAppsOnly() && !isCounterAllowed) {
            return
        }

        val appName = getAppName(packageName)
        val count = ScrollTrackerManager.recordScroll(packageName, appName)
        val threshold = ScrollTrackerManager.getWarningThreshold()
        if (!isLauncherPackage(packageName) && packageName != applicationContext.packageName && isCounterAllowed) {
            scrollOverlayManager?.updateScroll(count, threshold)
        }

        // Paid feature check: Auto-close app on scroll limit
        checkAndTriggerAutoClose(packageName, appName, count)
    }

    private fun checkAndTriggerAutoClose(packageName: String, appName: String, count: Int) {
        val isPro = SubscriptionManager.isProFlow.value
        val isGlobalEnabled = ScrollTrackerManager.isAutoCloseEnabled()
        val isAppEnabled = ScrollTrackerManager.isAppAutoCloseEnabled(packageName)

        // Enforce app-wise: only trigger if Pro, master switch is ON, AND this specific app is enabled!
        if (isPro && isGlobalEnabled && isAppEnabled) {
            val limit = ScrollTrackerManager.getEffectiveAppAutoCloseLimit(packageName)
            if (limit > 0 && count >= limit) {
                val now = System.currentTimeMillis()
                val lastTrigger = lastAutoCloseTimestamp[packageName] ?: 0L
                if (now - lastTrigger >= 8000L) { // Throttle repeat trigger
                    lastAutoCloseTimestamp[packageName] = now

                    // Launch modal popup directly over the app BEFORE closing, so user clearly sees the notice
                    val popupIntent = Intent(applicationContext, ScrollLimitPopupActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        )
                        putExtra(ScrollLimitPopupActivity.EXTRA_PACKAGE_NAME, packageName)
                        putExtra(ScrollLimitPopupActivity.EXTRA_APP_NAME, appName)
                        putExtra(ScrollLimitPopupActivity.EXTRA_SCROLL_COUNT, count)
                        putExtra(ScrollLimitPopupActivity.EXTRA_SCROLL_LIMIT, limit)
                    }
                    startActivity(popupIntent)
                }
            }
        }
    }

    private fun isLauncherPackage(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        if (launcherPackagesCache.contains(packageName)) return true
        if (packageName.contains("launcher", ignoreCase = true) ||
            packageName.contains("home", ignoreCase = true) ||
            packageName == "com.sec.android.app.launcher" ||
            packageName == "com.miui.home" ||
            packageName == "com.huawei.android.launcher" ||
            packageName == "com.oppo.launcher" ||
            packageName == "com.bbk.launcher2" ||
            packageName == "com.google.android.apps.nexuslauncher" ||
            packageName == "com.google.android.launcher" ||
            packageName == "com.android.launcher3" ||
            packageName == "com.android.launcher" ||
            packageName == "com.oneplus.launcher" ||
            packageName == "com.transsion.launcher" ||
            packageName == "com.motorola.launcher3"
        ) {
            launcherPackagesCache.add(packageName)
            return true
        }
        return try {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfos = packageManager.queryIntentActivities(homeIntent, 0)
            val isHome = resolveInfos.any { it.activityInfo?.packageName == packageName }
            if (isHome) {
                launcherPackagesCache.add(packageName)
            }
            isHome
        } catch (e: Exception) {
            false
        }
    }

    private fun getAppName(packageName: String): String {
        return appNameCache.getOrPut(packageName) {
            try {
                val pm = packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            }
        }
    }

    private fun isBrowserPackage(packageName: String): Boolean {
        return BROWSER_PACKAGES.contains(packageName) ||
                packageName.contains("browser", ignoreCase = true) ||
                packageName.contains("chrome", ignoreCase = true)
    }

    private fun checkBrowserForShieldedSites(event: AccessibilityEvent, browserPackage: String) {
        // Fast check 1: Event text & content description
        for (charSeq in event.text) {
            val text = charSeq?.toString() ?: continue
            val lockedPkg = ShieldManager.findLockedPackageForBrowserText(text)
            if (lockedPkg != null) {
                triggerBlockScreen(lockedPkg, isWebInterception = true, matchedUrl = text)
                return
            }
        }

        event.contentDescription?.let { desc ->
            val text = desc.toString()
            val lockedPkg = ShieldManager.findLockedPackageForBrowserText(text)
            if (lockedPkg != null) {
                triggerBlockScreen(lockedPkg, isWebInterception = true, matchedUrl = text)
                return
            }
        }

        // Fast check 2: Inspect active window root
        val rootNode = try {
            rootInActiveWindow ?: event.source
        } catch (e: Exception) {
            null
        } ?: return

        try {
            // Check well-known URL bar IDs
            val knownUrlIds = listOf(
                "$browserPackage:id/url_bar",
                "$browserPackage:id/location_bar_edit_text",
                "$browserPackage:id/mozac_browser_toolbar_url_view",
                "$browserPackage:id/toolbar_title",
                "$browserPackage:id/display_url",
                "$browserPackage:id/url_field"
            )

            for (id in knownUrlIds) {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
                if (!nodes.isNullOrEmpty()) {
                    for (node in nodes) {
                        val nodeText = node.text?.toString() ?: node.contentDescription?.toString()
                        if (!nodeText.isNullOrBlank()) {
                            val lockedPkg = ShieldManager.findLockedPackageForBrowserText(nodeText)
                            if (lockedPkg != null) {
                                triggerBlockScreen(lockedPkg, isWebInterception = true, matchedUrl = nodeText)
                                return
                            }
                        }
                    }
                }
            }

            // Bounded recursive node inspection (max depth 6, max 40 nodes to preserve UI responsiveness)
            var nodeCount = 0
            fun scanNode(node: AccessibilityNodeInfo?, depth: Int): Boolean {
                if (node == null || depth > 6 || nodeCount > 40) return false
                nodeCount++

                val nodeText = node.text?.toString()
                if (!nodeText.isNullOrBlank()) {
                    val lockedPkg = ShieldManager.findLockedPackageForBrowserText(nodeText)
                    if (lockedPkg != null) {
                        triggerBlockScreen(lockedPkg, isWebInterception = true, matchedUrl = nodeText)
                        return true
                    }
                }

                val desc = node.contentDescription?.toString()
                if (!desc.isNullOrBlank()) {
                    val lockedPkg = ShieldManager.findLockedPackageForBrowserText(desc)
                    if (lockedPkg != null) {
                        triggerBlockScreen(lockedPkg, isWebInterception = true, matchedUrl = desc)
                        return true
                    }
                }

                for (i in 0 until node.childCount) {
                    val child = node.getChild(i) ?: continue
                    if (scanNode(child, depth + 1)) {
                        return true
                    }
                }
                return false
            }

            scanNode(rootNode, 0)
        } catch (e: Exception) {
            // Gracefully ignore window retrieval exceptions
        }
    }

    private fun triggerBlockScreen(
        lockedPackage: String,
        isWebInterception: Boolean = false,
        matchedUrl: String? = null
    ) {
        val throttleKey = if (isWebInterception) "web_$lockedPackage" else lockedPackage
        val now = System.currentTimeMillis()
        if (throttleKey == lastBlockedKey && (now - lastBlockedTimestamp) < 1500L) {
            return
        }
        lastBlockedKey = throttleKey
        lastBlockedTimestamp = now

        // Step 1: Record interception metric
        ShieldManager.recordInterception(applicationContext)

        // Step 2: Launch FocusFlow Blocked Shield Overlay with scheduled time window info
        val startTimeDisplay = ShieldManager.getAppStartTimeDisplay(lockedPackage)
        val endTimeDisplay = ShieldManager.getAppEndTimeDisplay(lockedPackage)
        val remainingSecs = ShieldManager.getAppRemainingSeconds(lockedPackage)

        val blockIntent = Intent(applicationContext, BlockedAppActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(BlockedAppActivity.EXTRA_PACKAGE_NAME, lockedPackage)
            putExtra(BlockedAppActivity.EXTRA_START_TIME, startTimeDisplay)
            putExtra(BlockedAppActivity.EXTRA_END_TIME, endTimeDisplay)
            putExtra(BlockedAppActivity.EXTRA_REMAINING_SECS, remainingSecs)
            putExtra(BlockedAppActivity.EXTRA_IS_BROWSER, isWebInterception)
            if (matchedUrl != null) {
                putExtra(BlockedAppActivity.EXTRA_BROWSER_URL, matchedUrl)
            }
        }
        startActivity(blockIntent)
    }

    override fun onInterrupt() {
        // Required by AccessibilityService interface
    }
}
