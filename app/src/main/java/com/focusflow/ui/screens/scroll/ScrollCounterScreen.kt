package com.focusflow.ui.screens.scroll

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.focusflow.scroll.AppScrollInfo
import com.focusflow.scroll.ScrollTrackerManager
import com.focusflow.shield.InstalledAppInfo
import com.focusflow.shield.InstalledAppUtils
import com.focusflow.shield.ShieldManager
import com.focusflow.subscription.SubscriptionManager
import com.focusflow.ui.modals.PaywallModal
import com.focusflow.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrollCounterScreen(
    onOpenAccessibilitySettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val stats by ScrollTrackerManager.statsFlow.collectAsState()
    val isPro by SubscriptionManager.isProFlow.collectAsState()
    val isAccessibilityEnabled = remember { mutableStateOf(ShieldManager.isAccessibilityServiceEnabled(context)) }

    val installedApps = remember { mutableStateListOf<InstalledAppInfo>() }
    var isLoadingApps by remember { mutableStateOf(false) }
    var showAppPickerModal by remember { mutableStateOf(false) }
    var appSearchQuery by remember { mutableStateOf("") }
    var showCounterAppPickerModal by remember { mutableStateOf(false) }
    var counterAppSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        isLoadingApps = true
        val apps = withContext(Dispatchers.IO) {
            InstalledAppUtils.getInstalledLauncherApps(context)
        }
        installedApps.clear()
        installedApps.addAll(apps)
        isLoadingApps = false
    }

    // Recheck permission when screen resumes
    DisposableEffect(Unit) {
        isAccessibilityEnabled.value = ShieldManager.isAccessibilityServiceEnabled(context)
        onDispose {}
    }

    var showResetDialog by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }

    if (showPaywall) {
        PaywallModal(onDismiss = { showPaywall = false })
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Filled.RestartAlt, null, tint = Primary) },
            title = { Text("Reset Today's Scrolls?", fontWeight = FontWeight.Bold) },
            text = { Text("This will reset all app scroll counts and distance metrics for today.", color = OnSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        ScrollTrackerManager.resetToday()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAppPickerModal) {
        Dialog(
            onDismissRequest = {
                showAppPickerModal = false
                appSearchQuery = ""
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            val filteredApps = remember(installedApps.toList(), appSearchQuery) {
                if (appSearchQuery.isBlank()) {
                    installedApps
                } else {
                    installedApps.filter {
                        it.appName.contains(appSearchQuery, ignoreCase = true) ||
                        it.packageName.contains(appSearchQuery, ignoreCase = true)
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .windowInsetsPadding(WindowInsets.ime),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(top = 10.dp)
                ) {
                    // Modal Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = {
                                showAppPickerModal = false
                                appSearchQuery = ""
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close", tint = OnSurface)
                            }
                            Column {
                                Text(
                                    "Select Apps for Auto-Close",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                                Text(
                                    "Choose any installed app to monitor",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }

                        TextButton(onClick = {
                            showAppPickerModal = false
                            appSearchQuery = ""
                        }) {
                            Text("Done", fontWeight = FontWeight.Bold, color = Color(0xFF673AB7))
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Search Bar
                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        placeholder = { Text("Search installed apps...") },
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = OnSurfaceVariant) },
                        trailingIcon = {
                            if (appSearchQuery.isNotBlank()) {
                                IconButton(onClick = { appSearchQuery = "" }) {
                                    Icon(Icons.Filled.Close, null)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))

                    if (isLoadingApps) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF673AB7))
                        }
                    } else if (filteredApps.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No matching apps found", color = OnSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                val isAlreadyAdded = stats.appAutoCloseConfigs.containsKey(app.packageName)
                                val iconBitmap = remember(app.packageName) {
                                    InstalledAppUtils.drawableToImageBitmap(app.icon)
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isAlreadyAdded) SurfaceContainerHigh else SurfaceContainerLowest,
                                    border = BorderStroke(
                                        width = if (isAlreadyAdded) 1.2.dp else 0.5.dp,
                                        color = if (isAlreadyAdded) Color(0xFF673AB7).copy(alpha = 0.5f) else OutlineVariant.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isAlreadyAdded) {
                                                ScrollTrackerManager.removeAppAutoCloseConfig(app.packageName)
                                            } else {
                                                ScrollTrackerManager.addAppAutoCloseConfig(app.packageName, app.appName, stats.autoCloseLimit)
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (iconBitmap != null) {
                                                Image(
                                                    bitmap = iconBitmap,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                )
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFF673AB7).copy(alpha = 0.15f),
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            Icons.Filled.Apps,
                                                            contentDescription = null,
                                                            tint = Color(0xFF673AB7),
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Column {
                                                Text(
                                                    app.appName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = OnSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    app.packageName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = OnSurfaceVariant,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        if (isAlreadyAdded) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color(0xFF673AB7).copy(alpha = 0.15f),
                                                border = BorderStroke(1.dp, Color(0xFF673AB7).copy(alpha = 0.4f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = null,
                                                        tint = Color(0xFF673AB7),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        "Added",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF673AB7)
                                                    )
                                                }
                                            }
                                        } else {
                                            FilledTonalButton(
                                                onClick = {
                                                    ScrollTrackerManager.addAppAutoCloseConfig(app.packageName, app.appName, stats.autoCloseLimit)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = Color(0xFF673AB7).copy(alpha = 0.15f),
                                                    contentColor = Color(0xFF673AB7)
                                                ),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Add", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCounterAppPickerModal) {
        Dialog(
            onDismissRequest = {
                showCounterAppPickerModal = false
                counterAppSearchQuery = ""
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            val filteredApps = remember(installedApps.toList(), counterAppSearchQuery) {
                if (counterAppSearchQuery.isBlank()) {
                    installedApps
                } else {
                    installedApps.filter {
                        it.appName.contains(counterAppSearchQuery, ignoreCase = true) ||
                        it.packageName.contains(counterAppSearchQuery, ignoreCase = true)
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .windowInsetsPadding(WindowInsets.ime),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(top = 10.dp)
                ) {
                    // Modal Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = {
                                showCounterAppPickerModal = false
                                counterAppSearchQuery = ""
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close", tint = OnSurface)
                            }
                            Column {
                                Text(
                                    "Select Apps for Live Counter",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                                Text(
                                    "Counter will only track on chosen apps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }

                        TextButton(onClick = {
                            showCounterAppPickerModal = false
                            counterAppSearchQuery = ""
                        }) {
                            Text("Done", fontWeight = FontWeight.Bold, color = Color(0xFF00B0FF))
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Search Bar
                    OutlinedTextField(
                        value = counterAppSearchQuery,
                        onValueChange = { counterAppSearchQuery = it },
                        placeholder = { Text("Search installed apps...") },
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = OnSurfaceVariant) },
                        trailingIcon = {
                            if (counterAppSearchQuery.isNotBlank()) {
                                IconButton(onClick = { counterAppSearchQuery = "" }) {
                                    Icon(Icons.Filled.Close, null)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))

                    if (isLoadingApps) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF00B0FF))
                        }
                    } else if (filteredApps.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No matching apps found", color = OnSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                val isAlreadyAdded = stats.counterSpecificApps.containsKey(app.packageName)
                                val iconBitmap = remember(app.packageName) {
                                    InstalledAppUtils.drawableToImageBitmap(app.icon)
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isAlreadyAdded) SurfaceContainerHigh else SurfaceContainerLowest,
                                    border = BorderStroke(
                                        width = if (isAlreadyAdded) 1.2.dp else 0.5.dp,
                                        color = if (isAlreadyAdded) Color(0xFF00B0FF).copy(alpha = 0.5f) else OutlineVariant.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isAlreadyAdded) {
                                                ScrollTrackerManager.removeCounterSpecificApp(app.packageName)
                                            } else {
                                                ScrollTrackerManager.addCounterSpecificApp(app.packageName, app.appName)
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (iconBitmap != null) {
                                                Image(
                                                    bitmap = iconBitmap,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                )
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFF00B0FF).copy(alpha = 0.15f),
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            Icons.Filled.Apps,
                                                            contentDescription = null,
                                                            tint = Color(0xFF00B0FF),
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Column {
                                                Text(
                                                    app.appName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = OnSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    app.packageName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = OnSurfaceVariant,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        if (isAlreadyAdded) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color(0xFF00B0FF).copy(alpha = 0.15f),
                                                border = BorderStroke(1.dp, Color(0xFF00B0FF).copy(alpha = 0.4f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = null,
                                                        tint = Color(0xFF00B0FF),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        "Tracked",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF00B0FF)
                                                    )
                                                }
                                            }
                                        } else {
                                            FilledTonalButton(
                                                onClick = {
                                                    ScrollTrackerManager.addCounterSpecificApp(app.packageName, app.appName)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = Color(0xFF00B0FF).copy(alpha = 0.15f),
                                                    contentColor = Color(0xFF00B0FF)
                                                ),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Add", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
            // Header Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Doom Scroll Tracker",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )

                    if (stats.totalScrolls > 0) {
                        IconButton(onClick = { showResetDialog = true }) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = "Reset Today", tint = OnSurfaceVariant)
                        }
                    }
                }
            }

            // Accessibility Warning Banner (if disabled)
            if (!isAccessibilityEnabled.value) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF57C00).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFFF57C00).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Filled.WarningAmber, null, tint = Color(0xFFF57C00), modifier = Modifier.size(28.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Accessibility Required", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = OnSurface)
                                Text("FocusBit needs accessibility permission to detect scrolls and display the top counter.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        ShieldManager.openAccessibilitySettings(context)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00), contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Enable in Settings", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Hero Metric Card: Total Scrolls & Distance
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SurfaceContainerHigh,
                    border = BorderStroke(1.dp, Color(0xFF673AB7).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(shape = CircleShape, color = Color(0xFF673AB7).copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.SwapVert, null, tint = Primary, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Text(
                                    "TODAY'S SCROLLS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                            }

                            // Status Pill
                            val statusColor = when {
                                stats.totalScrolls < 200 -> Color(0xFF2E7D32)
                                stats.totalScrolls < 600 -> Color(0xFF1976D2)
                                stats.totalScrolls < 1200 -> Color(0xFFF57C00)
                                else -> Color(0xFFD32F2F)
                            }
                            val statusText = when {
                                stats.totalScrolls < 200 -> "Low"
                                stats.totalScrolls < 600 -> "Moderate"
                                stats.totalScrolls < 1200 -> "High"
                                else -> "Doomscroll!"
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = statusColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    statusText,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }
                        }

                        // Big Numbers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "${stats.totalScrolls}",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Black,
                                    color = OnSurface
                                )
                                Text(
                                    text = "thumb flicks today",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stats.formatDistance(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                                Text(
                                    text = "virtual distance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant
                                )
                            }
                        }

                        // Landmark Comparison Banner
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF673AB7).copy(alpha = 0.08f),
                            border = BorderStroke(0.8.dp, Color(0xFF673AB7).copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    stats.getLandmarkComparison(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurface
                                )
                            }
                        }
                    }
                }
            }

            // Live Screen-Top Counter Overlay Control Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceContainerHigh,
                    border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(shape = CircleShape, color = Color(0xFF00B0FF).copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.TouchApp, null, tint = Color(0xFF00B0FF), modifier = Modifier.size(20.dp))
                                    }
                                }
                                Text("Persistent Live Counter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurface)
                            }

                            Switch(
                                checked = stats.isOverlayEnabled,
                                onCheckedChange = { ScrollTrackerManager.setOverlayEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Primary)
                            )
                        }

                        // Doomscroll Alert Threshold Selector
                        AnimatedVisibility(visible = stats.isOverlayEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                HorizontalDivider(color = OutlineVariant.copy(alpha = 0.2f))

                                // Section 1: Live Counter Tracking Scope
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Tracking Scope",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OnSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    TextButton(
                                        onClick = {
                                            // Reset position to top-most middle live
                                            ScrollTrackerManager.resetPositionToTopMiddle()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Filled.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(14.dp), tint = Primary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Center Top", fontSize = 11.sp, color = Primary, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Scope Options: All Apps vs Specific Apps
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isSpecific = stats.isCounterSpecificAppsOnly

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (!isSpecific) Primary else SurfaceContainerHighest,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { ScrollTrackerManager.setCounterSpecificAppsOnly(false) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Filled.Public,
                                                contentDescription = null,
                                                tint = if (!isSpecific) OnPrimary else OnSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "All Apps",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (!isSpecific) FontWeight.Bold else FontWeight.Medium,
                                                color = if (!isSpecific) OnPrimary else OnSurface
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSpecific) Primary else SurfaceContainerHighest,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { ScrollTrackerManager.setCounterSpecificAppsOnly(true) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Filled.FilterList,
                                                contentDescription = null,
                                                tint = if (isSpecific) OnPrimary else OnSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "Specific Apps",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSpecific) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSpecific) OnPrimary else OnSurface
                                            )
                                        }
                                    }
                                }

                                if (stats.isCounterSpecificAppsOnly) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                if (stats.counterSpecificApps.isNotEmpty()) "Track Live Counter On (${stats.counterSpecificApps.size}):" else "Track Live Counter On:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = OnSurfaceVariant,
                                                fontWeight = FontWeight.SemiBold
                                            )

                                            FilledTonalButton(
                                                onClick = { showCounterAppPickerModal = true },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = Color(0xFF00B0FF).copy(alpha = 0.15f),
                                                    contentColor = Color(0xFF00B0FF)
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Add App", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        if (stats.counterSpecificApps.isEmpty()) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = SurfaceContainerHighest,
                                                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(14.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.TouchApp,
                                                        contentDescription = null,
                                                        tint = Color(0xFF00B0FF),
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                    Text(
                                                        "No Specific Apps Selected",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = OnSurface
                                                    )
                                                    Text(
                                                        "Choose which apps should display the live counter.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = OnSurfaceVariant,
                                                        textAlign = TextAlign.Center
                                                    )
                                                    Spacer(Modifier.height(2.dp))
                                                    Button(
                                                        onClick = { showCounterAppPickerModal = true },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF), contentColor = Color.White),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Select Apps", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                                    }
                                                }
                                            }
                                        } else {
                                            stats.counterSpecificApps.forEach { (pkg, name) ->
                                                val appInfo = remember(pkg, installedApps.size) {
                                                    installedApps.find { it.packageName == pkg }
                                                }
                                                val iconBitmap = remember(appInfo) {
                                                    InstalledAppUtils.drawableToImageBitmap(appInfo?.icon)
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = SurfaceContainerHighest,
                                                    border = BorderStroke(1.dp, Color(0xFF00B0FF).copy(alpha = 0.3f)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            if (iconBitmap != null) {
                                                                Image(
                                                                    bitmap = iconBitmap,
                                                                    contentDescription = null,
                                                                    modifier = Modifier
                                                                        .size(32.dp)
                                                                        .clip(RoundedCornerShape(6.dp))
                                                                )
                                                            } else {
                                                                Surface(
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    color = Color(0xFF00B0FF).copy(alpha = 0.15f),
                                                                    modifier = Modifier.size(32.dp)
                                                                ) {
                                                                    Box(contentAlignment = Alignment.Center) {
                                                                        Icon(
                                                                            Icons.Filled.Apps,
                                                                            contentDescription = null,
                                                                            tint = Color(0xFF00B0FF),
                                                                            modifier = Modifier.size(18.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }

                                                            Column {
                                                                Text(
                                                                    text = name,
                                                                    style = MaterialTheme.typography.titleSmall,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = OnSurface,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                Text(
                                                                    text = pkg,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = OnSurfaceVariant,
                                                                    fontSize = 10.5.sp,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }

                                                        IconButton(
                                                            onClick = { ScrollTrackerManager.removeCounterSpecificApp(pkg) },
                                                            modifier = Modifier.size(30.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Filled.DeleteOutline,
                                                                contentDescription = "Remove",
                                                                tint = OnSurfaceVariant,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF00B0FF).copy(alpha = 0.08f),
                                        border = BorderStroke(0.8.dp, Color(0xFF00B0FF).copy(alpha = 0.2f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Outlined.Info, null, tint = Color(0xFF00B0FF), modifier = Modifier.size(16.dp))
                                            Text(
                                                "Live counter is active and will float across all opened apps.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = OnSurfaceVariant,
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = OutlineVariant.copy(alpha = 0.2f))

                                // Section 2: Alert Warning Glow Threshold
                                Text("Warning Glow Threshold", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val thresholds = listOf(25, 50, 100, 200)
                                    thresholds.forEach { count ->
                                        val isSelected = stats.warningThreshold == count
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) Primary else SurfaceContainerHighest,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { ScrollTrackerManager.setWarningThreshold(count) }
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "$count",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) OnPrimary else OnSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                                HorizontalDivider(color = OutlineVariant.copy(alpha = 0.2f))

                                // Section: Auto-Hide / Display Duration
                                Text(
                                    "Display Duration",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val durationOptions = listOf(
                                        0 to "Always",
                                        3 to "3 sec",
                                        5 to "5 sec",
                                        10 to "10 sec"
                                    )
                                    durationOptions.forEach { (seconds, label) ->
                                        val isSelected = stats.displayDurationSeconds == seconds
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) Primary else SurfaceContainerHighest,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { ScrollTrackerManager.setOverlayDisplayDuration(seconds) }
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    label,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) OnPrimary else OnSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                                HorizontalDivider(color = OutlineVariant.copy(alpha = 0.2f))

                                // Section 3: App-Wise Auto-Close on Scroll Limit (PRO Feature)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF673AB7).copy(alpha = 0.18f),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Filled.Diamond,
                                                    contentDescription = null,
                                                    tint = Color(0xFF9C27B0),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                "App-Wise Auto-Close",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = OnSurface
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = stats.isAutoCloseEnabled && isPro,
                                        onCheckedChange = { enable ->
                                            if (!isPro) {
                                                showPaywall = true
                                            } else {
                                                ScrollTrackerManager.setAutoCloseEnabled(enable)
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF673AB7)
                                        )
                                    )
                                }

                                // App-Wise Configuration Cards
                                if (stats.isAutoCloseEnabled && isPro) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Configure Limits by App:",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = OnSurfaceVariant,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            FilledTonalButton(
                                                onClick = { showAppPickerModal = true },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = Color(0xFF673AB7).copy(alpha = 0.15f),
                                                    contentColor = Color(0xFF673AB7)
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Add App", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        val configuredApps = stats.appAutoCloseConfigs.values.toList()

                                        if (configuredApps.isEmpty()) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = SurfaceContainerHighest,
                                                border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.3f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(16.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.TouchApp,
                                                        contentDescription = null,
                                                        tint = Color(0xFF673AB7),
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                    Text(
                                                        "No Apps Configured Yet",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = OnSurface
                                                    )
                                                    Spacer(Modifier.height(2.dp))
                                                    Button(
                                                        onClick = { showAppPickerModal = true },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                                                        shape = RoundedCornerShape(10.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(6.dp))
                                                        Text("Select Apps", fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        } else {
                                            configuredApps.forEach { config ->
                                                val pkg = config.packageName
                                                val name = config.appName
                                                val isAppEnabled = config.isEnabled
                                                val appLimit = config.limit
                                                val appInfo = remember(pkg, installedApps.size) {
                                                    installedApps.find { it.packageName == pkg }
                                                }
                                                val iconBitmap = remember(appInfo) {
                                                    InstalledAppUtils.drawableToImageBitmap(appInfo?.icon)
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = SurfaceContainerHighest,
                                                    border = BorderStroke(
                                                        width = if (isAppEnabled) 1.2.dp else 0.5.dp,
                                                        color = if (isAppEnabled) Color(0xFF673AB7) else OutlineVariant.copy(alpha = 0.3f)
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.weight(1f),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                            ) {
                                                                if (iconBitmap != null) {
                                                                    Image(
                                                                        bitmap = iconBitmap,
                                                                        contentDescription = null,
                                                                        modifier = Modifier
                                                                            .size(36.dp)
                                                                            .clip(RoundedCornerShape(8.dp))
                                                                    )
                                                                } else {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(8.dp),
                                                                        color = Color(0xFF673AB7).copy(alpha = 0.15f),
                                                                        modifier = Modifier.size(36.dp)
                                                                    ) {
                                                                        Box(contentAlignment = Alignment.Center) {
                                                                            Icon(
                                                                                Icons.Filled.Apps,
                                                                                contentDescription = null,
                                                                                tint = Color(0xFF673AB7),
                                                                                modifier = Modifier.size(20.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }

                                                                Column {
                                                                    Text(
                                                                        text = name,
                                                                        style = MaterialTheme.typography.titleSmall,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = OnSurface,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                    Text(
                                                                        text = if (isAppEnabled) "$appLimit scrolls limit" else "Disabled",
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = if (isAppEnabled) Color(0xFFFF8A80) else OnSurfaceVariant,
                                                                        fontSize = 11.5.sp
                                                                    )
                                                                }
                                                            }

                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Switch(
                                                                    checked = isAppEnabled,
                                                                    onCheckedChange = { enable ->
                                                                        ScrollTrackerManager.setAppAutoCloseEnabled(pkg, name, enable)
                                                                    },
                                                                    colors = SwitchDefaults.colors(
                                                                        checkedThumbColor = Color.White,
                                                                        checkedTrackColor = Color(0xFF673AB7)
                                                                    )
                                                                )
                                                                IconButton(
                                                                    onClick = { ScrollTrackerManager.removeAppAutoCloseConfig(pkg) },
                                                                    modifier = Modifier.size(32.dp)
                                                                ) {
                                                                    Icon(
                                                                        Icons.Filled.DeleteOutline,
                                                                        contentDescription = "Remove app",
                                                                        tint = OnSurfaceVariant,
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        if (isAppEnabled) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                            ) {
                                                                val limits = listOf(25, 50, 100, 150, 250, 500)
                                                                limits.forEach { limit ->
                                                                    val isSelected = appLimit == limit
                                                                    Surface(
                                                                        shape = RoundedCornerShape(8.dp),
                                                                        color = if (isSelected) Color(0xFF673AB7) else Surface,
                                                                        modifier = Modifier
                                                                            .weight(1f)
                                                                            .clip(RoundedCornerShape(8.dp))
                                                                            .clickable {
                                                                                ScrollTrackerManager.setAppAutoCloseLimit(pkg, name, limit)
                                                                            }
                                                                    ) {
                                                                        Box(
                                                                            modifier = Modifier.padding(vertical = 6.dp),
                                                                            contentAlignment = Alignment.Center
                                                                        ) {
                                                                            Text(
                                                                                "$limit",
                                                                                style = MaterialTheme.typography.labelSmall,
                                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                                color = if (isSelected) Color.White else OnSurface
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = { showAppPickerModal = true },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF673AB7)),
                                                border = BorderStroke(1.dp, Color(0xFF673AB7).copy(alpha = 0.5f))
                                            ) {
                                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Add Another App", fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                } else if (!isPro) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF673AB7).copy(alpha = 0.08f),
                                        border = BorderStroke(1.dp, Color(0xFF673AB7).copy(alpha = 0.3f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showPaywall = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Enforce App-Wise Scroll Limits",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF673AB7),
                                                modifier = Modifier.weight(1f)
                                            )
                                            Button(
                                                onClick = { showPaywall = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Unlock", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // App-Wise Scroll Breakdown Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (stats.appList.isNotEmpty()) "App Breakdown (${stats.appList.size})" else "App Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Text(
                        "${stats.totalScrolls} Total",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnSurfaceVariant
                    )
                }
            }

            // Empty State
            if (stats.appList.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.SwapVert, null, tint = Primary, modifier = Modifier.size(40.dp))
                            Text("No Scrolling Detected Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurface)
                        }
                    }
                }
            } else {
                // List of apps
                items(stats.appList, key = { it.packageName }) { appInfo ->
                    AppScrollRow(appInfo = appInfo, totalScrolls = stats.totalScrolls, threshold = stats.warningThreshold)
                }
            }
    }
}

@Composable
private fun AppScrollRow(
    appInfo: AppScrollInfo,
    totalScrolls: Int,
    threshold: Int
) {
    val context = LocalContext.current
    val appIcon = remember(appInfo.packageName) {
        try {
            val pm = context.packageManager
            pm.getApplicationIcon(appInfo.packageName)
        } catch (e: Exception) {
            null
        }
    }

    val fraction = if (totalScrolls > 0) appInfo.scrollCount.toFloat() / totalScrolls.toFloat() else 0f
    val animatedProgress by animateFloatAsState(targetValue = fraction, label = "progress")

    val isOverThreshold = appInfo.scrollCount >= threshold
    val distanceMeters = appInfo.scrollCount * 0.15f
    val distanceDisplay = if (distanceMeters >= 1000f) {
        String.format(java.util.Locale.US, "%.1f km", distanceMeters / 1000f)
    } else {
        String.format(java.util.Locale.US, "%.0f m", distanceMeters)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceContainerHigh,
        border = BorderStroke(1.dp, if (isOverThreshold) Color(0xFFD32F2F).copy(alpha = 0.4f) else OutlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (appIcon != null) {
                        Image(
                            bitmap = appIcon.toBitmap().asImageBitmap(),
                            contentDescription = appInfo.appName,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Surface(shape = CircleShape, color = PrimaryContainer, modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Apps, null, tint = OnPrimaryContainer, modifier = Modifier.size(22.dp))
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            appInfo.appName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                distanceDisplay,
                                style = MaterialTheme.typography.bodySmall,
                                color = Primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("•", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                            Text(
                                "${(fraction * 100).toInt()}% of today",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }

                // Scroll Count Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isOverThreshold) Color(0xFFD32F2F).copy(alpha = 0.15f) else Color(0xFF673AB7).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, if (isOverThreshold) Color(0xFFD32F2F).copy(alpha = 0.5f) else Color(0xFF673AB7).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isOverThreshold) {
                            Icon(Icons.Filled.Warning, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(13.dp))
                        }
                        Text(
                            "${appInfo.scrollCount} scrolls",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverThreshold) Color(0xFFD32F2F) else Primary
                        )
                    }
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (isOverThreshold) Color(0xFFD32F2F) else Primary,
                trackColor = SurfaceContainerHighest
            )
        }
    }
}
