package com.focusflow.ui.screens.shield

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.focusflow.data.db.FocusFlowDatabase
import com.focusflow.data.db.dao.ReasonStat
import com.focusflow.data.db.entity.BlockedApp
import com.focusflow.shield.AppDetailAnalytics
import com.focusflow.shield.AppUsageInfo
import com.focusflow.shield.DailyUsagePoint
import com.focusflow.shield.DayUsageSummary
import com.focusflow.shield.HourlyUsage
import com.focusflow.shield.InstalledAppUtils
import com.focusflow.shield.ShieldManager
import com.focusflow.shield.TimeFrameBreakdown
import com.focusflow.shield.UsageStatsHelper
import com.focusflow.subscription.SubscriptionManager
import com.focusflow.ui.modals.PaywallModal
import com.focusflow.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUsageModal(
    database: FocusFlowDatabase,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            AppUsageScreenContent(
                database = database,
                onClose = onDismiss
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUsageScreenContent(
    database: FocusFlowDatabase,
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isPro by SubscriptionManager.isPro.collectAsState()
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(UsageStatsHelper.hasUsageAccessPermission(context)) }
    var selectedDayOffset by remember { mutableIntStateOf(0) } // 0: Today, 1: Yesterday, ...
    var daySummary by remember { mutableStateOf<DayUsageSummary?>(null) }
    var isLoadingStats by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    var selectedAppForDetail by remember { mutableStateOf<AppUsageInfo?>(null) }

    // Interventions stats from database
    val totalInterventions by database.interventionDao().getTotalInterventionsCount().collectAsState(initial = 0)
    val resistedCount by database.interventionDao().getResistedCount().collectAsState(initial = 0)
    val unlockedCount by database.interventionDao().getUnlockedCount().collectAsState(initial = 0)
    val reasonStats by database.interventionDao().getReasonStats().collectAsState(initial = emptyList())

    // Blocked apps for shield status check
    val blockedApps by database.shieldDao().getAllBlockedApps().collectAsState(initial = emptyList())
    val blockedPackageNames = remember(blockedApps) { blockedApps.map { it.packageName }.toSet() }

    // Check permission whenever resumed
    LifecycleResumeEffect(Unit) {
        hasPermission = UsageStatsHelper.hasUsageAccessPermission(context)
        onPauseOrDispose {}
    }

    // Load usage stats when permission is granted or day changes
    LaunchedEffect(hasPermission, selectedDayOffset) {
        if (hasPermission) {
            isLoadingStats = true
            val summary = withContext(Dispatchers.IO) {
                UsageStatsHelper.getDayUsageSummary(context, selectedDayOffset)
            }
            daySummary = summary
            isLoadingStats = false
        }
    }

    if (showPaywall) {
        PaywallModal(onDismiss = { showPaywall = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Phone App Usage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Real-time screen time & distraction analytics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        if (!hasPermission) {
            // Usage Access Permission Card
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.DataUsage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Usage Access Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "To track how much time you spend on distracting apps and help you build digital discipline, FocusBit needs Usage Access permission.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { UsageStatsHelper.openUsageAccessSettings(context) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grant Usage Access in Settings", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Day selector tabs
            val dayLabels = listOf("Today", "Yesterday", "2d ago", "3d ago", "4d ago", "5d ago", "6d ago")
            ScrollableTabRow(
                selectedTabIndex = selectedDayOffset,
                edgePadding = 16.dp,
                divider = {}
            ) {
                dayLabels.forEachIndexed { index, label ->
                    val isTabLocked = index > 0 && !isPro
                    Tab(
                        selected = selectedDayOffset == index,
                        onClick = {
                            if (isTabLocked) {
                                showPaywall = true
                            } else {
                                selectedDayOffset = index
                            }
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = label,
                                    fontWeight = if (selectedDayOffset == index) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isTabLocked) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Filled.Lock,
                                        contentDescription = "Pro",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Free User Pro Banner
                if (!isPro) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF673AB7).copy(alpha = 0.10f),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPaywall = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Filled.WorkspacePremium,
                                        contentDescription = null,
                                        tint = Color(0xFF673AB7),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Unlock 7-Day History & Deep Analytics",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF673AB7)
                                        )
                                        Text(
                                            text = "Upgrade to Pro for full historical breakdown & trends.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                FilledTonalButton(
                                    onClick = { showPaywall = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color(0xFF673AB7),
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Upgrade", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Screen Time Summary Card
                item {
                    val summary = daySummary
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = summary?.dayLabel ?: "Today",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = summary?.dateDisplay ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "⏱ TOTAL SCREEN TIME",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = summary?.totalScreenFormatted ?: "0h 0m",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${summary?.appUsages?.size ?: 0} apps tracked today",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Behavioral Distraction Analytics Card
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🛡️", fontSize = 20.sp)
                                Column {
                                    Text(
                                        text = "Behavioral Distraction Loop",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Trigger → Intervention → Outcome",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Resisted vs Unlocked Counter
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "$resistedCount",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                        Text(
                                            text = "Resisted 💪",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "+${resistedCount * 2} 🪙 Earned",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = Color(0xFF2E7D32).copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFC62828).copy(alpha = 0.12f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "$unlockedCount",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFC62828)
                                        )
                                        Text(
                                            text = "Unlocked 🔓",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFC62828),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Emergency uses",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = Color(0xFFC62828).copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val winRate = if (totalInterventions > 0) {
                                        ((resistedCount * 100) / totalInterventions)
                                    } else 0
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "$winRate%",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Willpower Rate",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "$totalInterventions triggers",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            // Reason Breakdown
                            if (reasonStats.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "WHY YOU ATTEMPTED TO UNLOCK:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val maxReasonCount = reasonStats.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
                                reasonStats.take(5).forEach { stat ->
                                    val progress = stat.count.toFloat() / maxReasonCount.toFloat()
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = stat.reason,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${stat.count} times",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // App Screen Time Breakdown Section Header
                item {
                    Text(
                        text = "App-by-App Screen Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isLoadingStats) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else if (daySummary?.appUsages.isNullOrEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No app usage recorded for this day.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val apps = daySummary!!.appUsages
                    items(apps, key = { it.packageName }) { appInfo ->
                        val isShielded = blockedPackageNames.contains(appInfo.packageName)
                        AppUsageRowItem(
                            info = appInfo,
                            isShielded = isShielded,
                            onClick = {
                                selectedAppForDetail = appInfo
                            },
                            onToggleShield = {
                                scope.launch {
                                    if (isShielded) {
                                        val existing = blockedApps.find { it.packageName == appInfo.packageName }
                                        if (existing != null) {
                                            database.shieldDao().deleteBlockedApp(existing)
                                            ShieldManager.removeBlockedPackage(appInfo.packageName)
                                        }
                                    } else {
                                        // Check subscription limit before adding
                                        if (!SubscriptionManager.canAddProtectedApp(blockedApps.size)) {
                                            showPaywall = true
                                        } else {
                                            val now = java.util.Calendar.getInstance()
                                            val curH = now.get(java.util.Calendar.HOUR_OF_DAY)
                                            val curM = now.get(java.util.Calendar.MINUTE)
                                            val start = "%02d:%02d".format(curH, curM)
                                            val end = "%02d:00".format(if (curH >= 22 || curH < 6) (curH + 2) % 24 else 22)
                                            database.shieldDao().insertBlockedApp(
                                                BlockedApp(
                                                    appName = appInfo.appName,
                                                    packageName = appInfo.packageName,
                                                    category = "App Usage",
                                                    iconName = "smart_display",
                                                    isBlocked = true,
                                                    startTime = start,
                                                    endTime = end,
                                                    isStrictLock = true
                                                )
                                            )
                                            ShieldManager.addBlockedPackage(appInfo.packageName)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        if (selectedAppForDetail != null) {
            val detailApp = selectedAppForDetail!!
            val isAppShielded = blockedPackageNames.contains(detailApp.packageName)
            AppDetailDialog(
                appInfo = detailApp,
                dayOffset = selectedDayOffset,
                isShielded = isAppShielded,
                onDismiss = { selectedAppForDetail = null },
                onToggleShield = {
                    val pkg = detailApp.packageName
                    val name = detailApp.appName
                    val currentShielded = blockedPackageNames.contains(pkg)
                    scope.launch {
                        if (currentShielded) {
                            val existing = blockedApps.find { it.packageName == pkg }
                            if (existing != null) {
                                database.shieldDao().deleteBlockedApp(existing)
                                ShieldManager.removeBlockedPackage(pkg)
                            }
                        } else {
                            if (!SubscriptionManager.canAddProtectedApp(blockedApps.size)) {
                                showPaywall = true
                            } else {
                                val now = java.util.Calendar.getInstance()
                                val curH = now.get(java.util.Calendar.HOUR_OF_DAY)
                                val curM = now.get(java.util.Calendar.MINUTE)
                                val start = "%02d:%02d".format(curH, curM)
                                val end = "%02d:00".format(if (curH >= 22 || curH < 6) (curH + 2) % 24 else 22)
                                database.shieldDao().insertBlockedApp(
                                    BlockedApp(
                                        appName = name,
                                        packageName = pkg,
                                        category = "App Usage",
                                        iconName = "smart_display",
                                        isBlocked = true,
                                        startTime = start,
                                        endTime = end,
                                        isStrictLock = true
                                    )
                                )
                                ShieldManager.addBlockedPackage(pkg)
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun AppUsageRowItem(
    info: AppUsageInfo,
    isShielded: Boolean,
    onClick: () -> Unit,
    onToggleShield: () -> Unit
) {
    val context = LocalContext.current
    val iconBitmap = remember(info.packageName) {
        val drawable = UsageStatsHelper.getAppIcon(context, info.packageName)
        InstalledAppUtils.drawableToImageBitmap(drawable)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = info.appName,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = info.appName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.BarChart,
                                contentDescription = "View time frame graph",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = "${info.formattedTime} (${(info.percentage * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onToggleShield,
                    shape = RoundedCornerShape(50),
                    colors = if (isShielded) {
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Icon(
                        if (isShielded) Icons.Filled.Shield else Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isShielded) "Shielded" else "+ Shield",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Percentage bar
            LinearProgressIndicator(
                progress = { info.percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp)),
                color = if (isShielded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            )
        }
    }
}

@Composable
private fun AppDetailDialog(
    appInfo: AppUsageInfo,
    dayOffset: Int,
    isShielded: Boolean,
    onDismiss: () -> Unit,
    onToggleShield: () -> Unit
) {
    val context = LocalContext.current
    var analytics by remember { mutableStateOf<AppDetailAnalytics?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Hourly Timeline, 1: 7-Day Trend
    var selectedHour by remember { mutableStateOf<HourlyUsage?>(null) }

    val iconBitmap = remember(appInfo.packageName) {
        val drawable = UsageStatsHelper.getAppIcon(context, appInfo.packageName)
        InstalledAppUtils.drawableToImageBitmap(drawable)
    }

    LaunchedEffect(appInfo.packageName, dayOffset) {
        isLoading = true
        val result = withContext(Dispatchers.IO) {
            UsageStatsHelper.getAppDetailAnalytics(context, appInfo.packageName, dayOffset)
        }
        analytics = result
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (iconBitmap != null) {
                            Image(
                                bitmap = iconBitmap,
                                contentDescription = appInfo.appName,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = appInfo.appName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${analytics?.dayLabel ?: "Selected Day"} · ${analytics?.dayTotalFormatted ?: appInfo.formattedTime}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("⏰ 24h Hourly Graph", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("📅 7-Day Trend", fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isLoading || analytics == null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    val data = analytics!!
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (selectedTab == 0) {
                            // TAB 0: HOURLY BREAKDOWN & TIME FRAMES

                            Text(
                                text = "TIME FRAME BREAKDOWN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.8.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TimeFrameCard(
                                    modifier = Modifier.weight(1f),
                                    emoji = "🌅",
                                    title = "Morning",
                                    timeRange = "6 AM – 12 PM",
                                    minutes = data.timeFrameBreakdown.morningMinutes,
                                    totalMinutes = data.dayTotalMinutes
                                )
                                TimeFrameCard(
                                    modifier = Modifier.weight(1f),
                                    emoji = "☀️",
                                    title = "Afternoon",
                                    timeRange = "12 PM – 6 PM",
                                    minutes = data.timeFrameBreakdown.afternoonMinutes,
                                    totalMinutes = data.dayTotalMinutes
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TimeFrameCard(
                                    modifier = Modifier.weight(1f),
                                    emoji = "🌆",
                                    title = "Evening",
                                    timeRange = "6 PM – 10 PM",
                                    minutes = data.timeFrameBreakdown.eveningMinutes,
                                    totalMinutes = data.dayTotalMinutes
                                )
                                TimeFrameCard(
                                    modifier = Modifier.weight(1f),
                                    emoji = "🌙",
                                    title = "Night",
                                    timeRange = "10 PM – 6 AM",
                                    minutes = data.timeFrameBreakdown.nightMinutes,
                                    totalMinutes = data.dayTotalMinutes
                                )
                            }

                            // 24-Hour Bar Graph Card
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Hourly Usage Timeline",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Tap bar to inspect",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    HourlyBarGraph(
                                        hourlyUsages = data.hourlyUsages,
                                        selectedHour = selectedHour,
                                        onSelectHour = { selectedHour = it }
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (selectedHour != null) "🕒" else "🔥",
                                                fontSize = 16.sp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (selectedHour != null) {
                                                    "${selectedHour!!.label}: ${selectedHour!!.minutes}m in active foreground"
                                                } else {
                                                    "Peak Usage Window: ${data.peakHourLabel}"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }

                        } else {
                            // TAB 1: 7-DAY TREND BAR CHART
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "7-Day Usage History",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val totalWeekly = data.past7Days.sumOf { it.minutes }
                                        Text(
                                            text = "Total: ${UsageStatsHelper.formatDuration(totalWeekly * 60000L)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    SevenDayBarGraph(days = data.past7Days)
                                }
                            }

                            // Insight & Tip
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("💡", fontSize = 20.sp)
                                    Column {
                                        Text(
                                            text = "Behavioral Recommendation",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Text(
                                            text = "Shielding ${appInfo.appName} during your peak hours saves an estimated ${(data.dayTotalMinutes * 0.6).toInt()} minutes daily.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        // Shield Action Button in Detail Modal
                        FilledTonalButton(
                            onClick = onToggleShield,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = if (isShielded) {
                                ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        ) {
                            Icon(
                                if (isShielded) Icons.Filled.Shield else Icons.Filled.AddModerator,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isShielded) "Remove from App Shield" else "Add ${appInfo.appName} to Shield",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HourlyBarGraph(
    hourlyUsages: List<HourlyUsage>,
    selectedHour: HourlyUsage?,
    onSelectHour: (HourlyUsage) -> Unit
) {
    val maxMin = remember(hourlyUsages) {
        hourlyUsages.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            hourlyUsages.forEach { hour ->
                val ratio = (hour.minutes.toFloat() / maxMin.toFloat()).coerceIn(0f, 1f)
                val isSelected = selectedHour?.hour == hour.hour
                val hasUsage = hour.minutes > 0
                val barHeight = if (hasUsage) (85.dp * ratio).coerceAtLeast(6.dp) else 3.dp

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelectHour(hour) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .width(7.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(
                                when {
                                    isSelected -> Color(0xFFFFB300)
                                    hasUsage -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                }
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Time Axis Labels (12a, 4a, 8a, 12p, 4p, 8p, 11p)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val keyHours = listOf(0 to "12a", 4 to "4a", 8 to "8a", 12 to "12p", 16 to "4p", 20 to "8p", 23 to "11p")
            keyHours.forEach { (_, label) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SevenDayBarGraph(days: List<DailyUsagePoint>) {
    val maxMin = remember(days) {
        days.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            val ratio = (day.minutes.toFloat() / maxMin.toFloat()).coerceIn(0f, 1f)
            val barHeight = if (day.minutes > 0) (80.dp * ratio).coerceAtLeast(8.dp) else 4.dp

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (day.minutes > 0) "${day.minutes}m" else "-",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (day.minutes > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            if (day.minutes > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = day.dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = day.dateLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TimeFrameCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    timeRange: String,
    minutes: Int,
    totalMinutes: Int
) {
    val percentage = if (totalMinutes > 0) ((minutes * 100) / totalMinutes) else 0

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$emoji $title", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(
                    "$percentage%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(timeRange, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "${minutes}m",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (minutes > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
