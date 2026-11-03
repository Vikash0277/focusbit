package com.focusflow.shield

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Process
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.*

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val usageMillis: Long,
    val formattedTime: String,
    val percentage: Float, // 0.0 to 1.0
    val isShielded: Boolean
)

data class DayUsageSummary(
    val dayOffset: Int, // 0: Today, 1: Yesterday, etc.
    val dayLabel: String,
    val dateDisplay: String,
    val totalScreenMillis: Long,
    val totalScreenFormatted: String,
    val appUsages: List<AppUsageInfo>
)

data class HourlyUsage(
    val hour: Int, // 0..23
    val label: String, // "12 AM", "1 AM", ...
    val minutes: Int
)

data class TimeFrameBreakdown(
    val morningMinutes: Int,   // 6 AM - 12 PM
    val afternoonMinutes: Int, // 12 PM - 6 PM
    val eveningMinutes: Int,   // 6 PM - 10 PM
    val nightMinutes: Int      // 10 PM - 6 AM
)

data class DailyUsagePoint(
    val dayLabel: String,   // "Mon", "Tue"
    val dateLabel: String,  // "Sep 12"
    val minutes: Int,
    val formattedTime: String
)

data class AppDetailAnalytics(
    val packageName: String,
    val appName: String,
    val dayLabel: String,
    val dayTotalFormatted: String,
    val dayTotalMinutes: Int,
    val hourlyUsages: List<HourlyUsage>,
    val timeFrameBreakdown: TimeFrameBreakdown,
    val peakHourLabel: String,
    val past7Days: List<DailyUsagePoint>
)

object UsageStatsHelper {

    fun hasUsageAccessPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.noteOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val generalSettings = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(generalSettings)
        }
    }

    fun formatDuration(millis: Long): String {
        val totalSecs = millis / 1000
        val hours = totalSecs / 3600
        val minutes = (totalSecs % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }

    fun isSystemOrNonAppPackage(context: Context, packageName: String): Boolean {
        // Exclude our own app ("yours" - FocusFlow)
        if (packageName == context.packageName) return true

        // Exclude Android OS & System UI
        val lower = packageName.lowercase()
        if (lower == "android" || lower == "com.android.systemui") return true

        // Exclude common keyboards / IMEs
        if (lower.contains("inputmethod") || lower.contains("latin") || lower.contains("swiftkey") || lower.contains("honeyboard")) return true

        // Exclude background Google services & search launcher feed
        if (lower == "com.google.android.gms" || lower == "com.google.android.googlequicksearchbox") return true

        return false
    }

    private fun getLauncherPackages(context: Context): Set<String> {
        val launcherPkgs = mutableSetOf<String>()
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val homeResolvers = context.packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            for (ri in homeResolvers) {
                ri.activityInfo?.packageName?.let { launcherPkgs.add(it) }
            }
        } catch (e: Exception) {
            // ignore
        }
        return launcherPkgs
    }

    fun getDayUsageSummary(context: Context, dayOffset: Int): DayUsageSummary {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -dayOffset)

        // Start of target day (00:00:00.000)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        // End of target day (23:59:59.999 or now if today)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endTime = if (dayOffset == 0) System.currentTimeMillis() else cal.timeInMillis

        // Absolute physical ceiling for this day (at most 24h, or elapsed time today)
        val maxPossibleDayMillis = if (dayOffset == 0) {
            (System.currentTimeMillis() - startTime).coerceAtLeast(0L)
        } else {
            86_400_000L // 24 hours
        }

        val dayLabel = when (dayOffset) {
            0 -> "Today"
            1 -> "Yesterday"
            else -> {
                val sdf = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
                sdf.format(Date(startTime))
            }
        }

        val dateDisplay = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(startTime))

        if (!hasUsageAccessPermission(context)) {
            return DayUsageSummary(
                dayOffset = dayOffset,
                dayLabel = dayLabel,
                dateDisplay = dateDisplay,
                totalScreenMillis = 0L,
                totalScreenFormatted = "0h 0m",
                appUsages = emptyList()
            )
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return DayUsageSummary(dayOffset, dayLabel, dateDisplay, 0L, "0h 0m", emptyList())

        val appUsageMap = mutableMapOf<String, Long>()

        // 1. Primary & accurate: Parse UsageEvents strictly bounded to the target day window
        // This avoids Android's multi-day bucket leakage and foreground service background accumulation
        try {
            val events = usageStatsManager.queryEvents(startTime, endTime)
            val event = android.app.usage.UsageEvents.Event()
            val resumeTimestamps = mutableMapOf<String, Long>()
            var isScreenOn = true

            while (events != null && events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                val time = event.timeStamp
                val eventType = event.eventType

                when (eventType) {
                    android.app.usage.UsageEvents.Event.SCREEN_INTERACTIVE -> {
                        isScreenOn = true
                    }
                    android.app.usage.UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                        isScreenOn = false
                        // Screen locked/off: close all open foreground sessions
                        for ((p, rTime) in resumeTimestamps) {
                            if (rTime > 0L) {
                                val s = maxOf(rTime, startTime)
                                val e = minOf(time, endTime)
                                if (e > s) {
                                    val dur = e - s
                                    appUsageMap[p] = (appUsageMap[p] ?: 0L) + dur
                                }
                            }
                        }
                        resumeTimestamps.clear()
                    }
                    android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED, 1 /* MOVE_TO_FOREGROUND */ -> {
                        if (isScreenOn) {
                            resumeTimestamps[pkg] = time
                        }
                    }
                    android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED,
                    android.app.usage.UsageEvents.Event.ACTIVITY_STOPPED,
                    2 /* MOVE_TO_BACKGROUND */ -> {
                        val rTime = resumeTimestamps.remove(pkg) ?: 0L
                        if (rTime > 0L) {
                            val s = maxOf(rTime, startTime)
                            val e = minOf(time, endTime)
                            if (e > s) {
                                val dur = e - s
                                appUsageMap[pkg] = (appUsageMap[pkg] ?: 0L) + dur
                            }
                        }
                    }
                }
            }

            // Close any app session currently active right now
            val currentNow = if (dayOffset == 0) System.currentTimeMillis() else endTime
            for ((p, rTime) in resumeTimestamps) {
                if (rTime > 0L) {
                    val s = maxOf(rTime, startTime)
                    val e = minOf(currentNow, endTime)
                    if (e > s) {
                        val dur = e - s
                        appUsageMap[p] = (appUsageMap[p] ?: 0L) + dur
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Fallback only if queryEvents yielded no data
        if (appUsageMap.isEmpty()) {
            try {
                val statsList = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    startTime,
                    endTime
                ) ?: emptyList()
                for (stat in statsList) {
                    val pkg = stat.packageName
                    if (stat.lastTimeUsed >= startTime) {
                        val time = stat.totalTimeInForeground.coerceIn(0L, maxPossibleDayMillis)
                        if (time > 1000) {
                            appUsageMap[pkg] = maxOf(appUsageMap[pkg] ?: 0L, time)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val pm = context.packageManager
        val blockedPkgs = ShieldManager.blockedPackageNames
        val launcherPkgs = getLauncherPackages(context)
        val myPackageName = context.packageName

        val userAppList = mutableListOf<AppUsageInfo>()
        var rawTotalMillis = 0L

        for ((pkg, rawTime) in appUsageMap) {
            // Never include FocusFlow ("not yours") - show only user's phone apps
            if (pkg == myPackageName) continue

            // Exclude home launchers and system internals
            if (launcherPkgs.contains(pkg)) continue
            if (pkg.contains("launcher", ignoreCase = true) && !blockedPkgs.contains(pkg)) continue
            if (isSystemOrNonAppPackage(context, pkg)) continue

            // Check if application has launchable intent or is a shielded/known user app
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null || blockedPkgs.contains(pkg)) {
                // Hard clamp every app's time to physical reality (never > 24h)
                val validTime = rawTime.coerceIn(0L, maxPossibleDayMillis)
                rawTotalMillis += validTime

                val appName = try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    pkg.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: pkg
                }

                userAppList.add(
                    AppUsageInfo(
                        packageName = pkg,
                        appName = appName,
                        usageMillis = validTime,
                        formattedTime = formatDuration(validTime),
                        percentage = 0f,
                        isShielded = blockedPkgs.contains(pkg)
                    )
                )
            }
        }

        // Hard clamp total daily screen time so it never exceeds elapsed day duration
        val totalMillis = rawTotalMillis.coerceIn(0L, maxPossibleDayMillis)

        // Sort by longest usage first
        userAppList.sortByDescending { it.usageMillis }

        // Compute percentages relative to user apps
        val updatedList = userAppList.map { info ->
            val ratio = if (totalMillis > 0) (info.usageMillis.toFloat() / totalMillis.toFloat()) else 0f
            info.copy(percentage = ratio.coerceIn(0f, 1f))
        }

        return DayUsageSummary(
            dayOffset = dayOffset,
            dayLabel = dayLabel,
            dateDisplay = dateDisplay,
            totalScreenMillis = totalMillis,
            totalScreenFormatted = formatDuration(totalMillis),
            appUsages = updatedList
        )
    }

    fun getAppIcon(context: Context, packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }

    fun formatHourLabel(hour: Int): String {
        return when (hour) {
            0 -> "12 AM"
            12 -> "12 PM"
            in 1..11 -> "$hour AM"
            else -> "${hour - 12} PM"
        }
    }

    private fun distributeMinutesToHourlyBuckets(sessionStart: Long, sessionEnd: Long, buckets: IntArray) {
        val cal = Calendar.getInstance()
        var current = sessionStart
        while (current < sessionEnd) {
            cal.timeInMillis = current
            val hour = cal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val hourEnd = minOf(cal.timeInMillis + 1, sessionEnd)
            val diffMinutes = ((hourEnd - current) / 60000).toInt()
            if (diffMinutes > 0) {
                buckets[hour] += diffMinutes
            } else if (hourEnd - current >= 15000) {
                buckets[hour] += 1
            }
            current = hourEnd
        }
    }

    fun getAppDetailAnalytics(context: Context, packageName: String, dayOffset: Int = 0): AppDetailAnalytics {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -dayOffset)

        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayStartTime = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val dayEndTime = if (dayOffset == 0) System.currentTimeMillis() else cal.timeInMillis

        val dayLabel = when (dayOffset) {
            0 -> "Today"
            1 -> "Yesterday"
            else -> SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date(dayStartTime))
        }

        val pm = context.packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: packageName
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usageStatsManager == null || !hasUsageAccessPermission(context)) {
            return AppDetailAnalytics(
                packageName = packageName,
                appName = appName,
                dayLabel = dayLabel,
                dayTotalFormatted = "0m",
                dayTotalMinutes = 0,
                hourlyUsages = (0..23).map { HourlyUsage(it, formatHourLabel(it), 0) },
                timeFrameBreakdown = TimeFrameBreakdown(0, 0, 0, 0),
                peakHourLabel = "None",
                past7Days = emptyList()
            )
        }

        // 1. Calculate hourly distribution from UsageEvents
        val hourlyMinutes = IntArray(24)
        var totalForegroundMillis = 0L

        try {
            val events = usageStatsManager.queryEvents(dayStartTime, dayEndTime)
            val event = android.app.usage.UsageEvents.Event()
            var lastResumeTime = 0L

            while (events != null && events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.packageName == packageName) {
                    val type = event.eventType
                    if (type == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED || type == 1 /* MOVE_TO_FOREGROUND */) {
                        lastResumeTime = event.timeStamp
                    } else if ((type == android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED ||
                                type == 2 /* MOVE_TO_BACKGROUND */ ||
                                type == android.app.usage.UsageEvents.Event.ACTIVITY_STOPPED) && lastResumeTime > 0) {
                        val sessionStart = maxOf(lastResumeTime, dayStartTime)
                        val sessionEnd = minOf(event.timeStamp, dayEndTime)
                        if (sessionEnd > sessionStart) {
                            val duration = sessionEnd - sessionStart
                            totalForegroundMillis += duration
                            distributeMinutesToHourlyBuckets(sessionStart, sessionEnd, hourlyMinutes)
                        }
                        lastResumeTime = 0L
                    }
                }
            }

            if (lastResumeTime > 0) {
                val sessionStart = maxOf(lastResumeTime, dayStartTime)
                val sessionEnd = minOf(System.currentTimeMillis(), dayEndTime)
                if (sessionEnd > sessionStart) {
                    val duration = sessionEnd - sessionStart
                    totalForegroundMillis += duration
                    distributeMinutesToHourlyBuckets(sessionStart, sessionEnd, hourlyMinutes)
                }
            }
        } catch (e: Exception) {
            // fallback
        }

        val maxPossibleDayMillis = if (dayOffset == 0) {
            (System.currentTimeMillis() - dayStartTime).coerceAtLeast(0L)
        } else {
            86_400_000L // 24 hours
        }

        // Only fallback if queryEvents recorded zero time
        if (totalForegroundMillis == 0L) {
            try {
                val statsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, dayStartTime, dayEndTime)
                val stat = statsList?.filter { it.packageName == packageName && it.lastTimeUsed >= dayStartTime }
                    ?.maxByOrNull { it.totalTimeInForeground }
                val fallbackTime = (stat?.totalTimeInForeground ?: 0L).coerceIn(0L, maxPossibleDayMillis)
                if (fallbackTime > 1000L) {
                    totalForegroundMillis = fallbackTime
                    val calUsed = Calendar.getInstance()
                    val activeHour = if (stat != null && stat.lastTimeUsed in dayStartTime..dayEndTime) {
                        calUsed.timeInMillis = stat.lastTimeUsed
                        calUsed.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
                    } else {
                        calUsed.timeInMillis = System.currentTimeMillis()
                        calUsed.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
                    }
                    hourlyMinutes[activeHour] = (totalForegroundMillis / 60000).toInt().coerceAtLeast(1)
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        // Ensure total foreground millis never exceeds actual hours passed
        totalForegroundMillis = totalForegroundMillis.coerceIn(0L, maxPossibleDayMillis)

        val totalMinutes = (totalForegroundMillis / 60000).toInt()
        val hourlyList = (0..23).map { h ->
            HourlyUsage(
                hour = h,
                label = formatHourLabel(h),
                minutes = hourlyMinutes[h]
            )
        }

        // Time frame buckets
        var morning = 0
        var afternoon = 0
        var evening = 0
        var night = 0
        for (h in 0..23) {
            val m = hourlyMinutes[h]
            when (h) {
                in 6..11 -> morning += m
                in 12..17 -> afternoon += m
                in 18..21 -> evening += m
                else -> night += m
            }
        }
        val timeFrameBreakdown = TimeFrameBreakdown(morning, afternoon, evening, night)

        // Find peak hour
        var maxMin = -1
        var peakH = -1
        for (h in 0..23) {
            if (hourlyMinutes[h] > maxMin && hourlyMinutes[h] > 0) {
                maxMin = hourlyMinutes[h]
                peakH = h
            }
        }
        val peakHourLabel = if (peakH >= 0) "${formatHourLabel(peakH)} (${maxMin}m)" else "None"

        // 2. 7-Day History strictly bounded per day
        val past7Days = mutableListOf<DailyUsagePoint>()
        for (offset in 6 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -offset)
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            val sTime = c.timeInMillis
            c.set(Calendar.HOUR_OF_DAY, 23)
            c.set(Calendar.MINUTE, 59)
            c.set(Calendar.SECOND, 59)
            c.set(Calendar.MILLISECOND, 999)
            val eTime = if (offset == 0) System.currentTimeMillis() else c.timeInMillis

            val maxOffsetPossible = if (offset == 0) {
                (System.currentTimeMillis() - sTime).coerceAtLeast(0L)
            } else {
                86_400_000L
            }

            var dayMillis = 0L
            try {
                val dEvents = usageStatsManager.queryEvents(sTime, eTime)
                val dEvent = android.app.usage.UsageEvents.Event()
                var resumeT = 0L
                while (dEvents != null && dEvents.hasNextEvent()) {
                    dEvents.getNextEvent(dEvent)
                    if (dEvent.packageName == packageName) {
                        val t = dEvent.eventType
                        if (t == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED || t == 1) {
                            resumeT = dEvent.timeStamp
                        } else if ((t == android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED ||
                                    t == 2 ||
                                    t == android.app.usage.UsageEvents.Event.ACTIVITY_STOPPED) && resumeT > 0L) {
                            val s = maxOf(resumeT, sTime)
                            val e = minOf(dEvent.timeStamp, eTime)
                            if (e > s) dayMillis += (e - s)
                            resumeT = 0L
                        }
                    }
                }
                if (resumeT > 0L) {
                    val s = maxOf(resumeT, sTime)
                    val e = minOf(eTime, if (offset == 0) System.currentTimeMillis() else eTime)
                    if (e > s) dayMillis += (e - s)
                }
            } catch (e: Exception) {
                // fallback
            }

            if (dayMillis == 0L) {
                try {
                    val dStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, sTime, eTime)
                    val stat = dStats?.filter { it.packageName == packageName && it.lastTimeUsed >= sTime }
                        ?.maxByOrNull { it.totalTimeInForeground }
                    dayMillis = (stat?.totalTimeInForeground ?: 0L).coerceIn(0L, maxOffsetPossible)
                } catch (e2: Exception) {
                    dayMillis = 0L
                }
            }

            val timeM = dayMillis.coerceIn(0L, maxOffsetPossible)
            val mins = (timeM / 60000).toInt()
            val dayLbl = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(sTime))
            val dateLbl = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(sTime))
            past7Days.add(DailyUsagePoint(dayLbl, dateLbl, mins, formatDuration(timeM)))
        }

        return AppDetailAnalytics(
            packageName = packageName,
            appName = appName,
            dayLabel = dayLabel,
            dayTotalFormatted = formatDuration(totalForegroundMillis),
            dayTotalMinutes = totalMinutes,
            hourlyUsages = hourlyList,
            timeFrameBreakdown = timeFrameBreakdown,
            peakHourLabel = peakHourLabel,
            past7Days = past7Days
        )
    }
}
