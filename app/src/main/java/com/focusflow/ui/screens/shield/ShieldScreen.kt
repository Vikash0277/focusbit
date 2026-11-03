package com.focusflow.ui.screens.shield

import android.app.Application
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focusflow.data.db.FocusFlowDatabase
import com.focusflow.data.db.entity.BlockedApp
import com.focusflow.shield.InstalledAppInfo
import com.focusflow.shield.InstalledAppUtils
import com.focusflow.shield.ShieldManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.focusflow.subscription.SubscriptionManager
import com.focusflow.ui.modals.PaywallModal
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import com.focusflow.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShieldScreen(
    database: FocusFlowDatabase,
    onNavigateToAppUsage: () -> Unit = {},
    viewModel: ShieldViewModel = viewModel(
        factory = ShieldViewModel.Factory(
            LocalContext.current.applicationContext as Application,
            database
        )
    )
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Re-check accessibility permission when user returns to screen
    LifecycleResumeEffect(Unit) {
        viewModel.checkAccessibilityPermission()
        onPauseOrDispose {}
    }

    var showAppPickerModal by remember { mutableStateOf(false) }
    var appSearchQuery by remember { mutableStateOf("") }
    var appToEditWindow by remember { mutableStateOf<BlockedApp?>(null) }
    var selectedAppToShield by remember { mutableStateOf<InstalledAppInfo?>(null) }

    var showPaywallModal by remember { mutableStateOf(false) }
    val isPro by SubscriptionManager.isPro.collectAsState()

    var selectedPackageNames by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showTimeWindowPickerForShield by remember { mutableStateOf(false) }
    var shouldShieldOnIntervalConfirm by remember { mutableStateOf(false) }
    var showClearAllConfirmationDialog by remember { mutableStateOf(false) }
    var shieldStartTime by remember { mutableStateOf<String?>(null) }
    var shieldEndTime by remember { mutableStateOf<String?>(null) }

    val activeStartTime = shieldStartTime ?: uiState.masterStartTime
    val activeEndTime = shieldEndTime ?: uiState.masterEndTime

    // Strict Lock Rejection Dialog
    if (uiState.strictLockAlertMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearStrictLockAlert() },
            icon = {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Tertiary, modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Strict Shield Active", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    uiState.strictLockAlertMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearStrictLockAlert() },
                    colors = ButtonDefaults.buttonColors(containerColor = Tertiary, contentColor = OnTertiaryContainer)
                ) {
                    Text("Understood")
                }
            }
        )
    }

    // Time Window Picker Dialog when a new app is selected from phone
    if (selectedAppToShield != null) {
        val appInfo = selectedAppToShield!!

        TimeWindowPickerDialog(
            title = "Set Shield Window for ${appInfo.appName}",
            confirmButtonText = "Shield App",
            initialStartTime = activeStartTime,
            initialEndTime = activeEndTime,
            onDismiss = { selectedAppToShield = null },
            onSave = { start, end ->
                val currentBlockedCount = uiState.blockedApps.size
                val freeMax = SubscriptionManager.FREE_LIMITS.maxProtectedApps ?: 3
                if (!isPro && currentBlockedCount >= freeMax) {
                    showPaywallModal = true
                } else {
                    viewModel.addAppToShield(appInfo, start, end)
                    showAppPickerModal = false
                }
                selectedAppToShield = null
            }
        )
    }

    // Time Window Picker Dialog for editing an existing app (when not locked)
    if (appToEditWindow != null) {
        val app = appToEditWindow!!
        TimeWindowPickerDialog(
            title = "Focus Window for ${app.appName}",
            confirmButtonText = "Save Focus Window",
            initialStartTime = app.startTime,
            initialEndTime = app.endTime,
            onDismiss = { appToEditWindow = null },
            onSave = { start, end ->
                viewModel.setAppTimeWindow(app, start, end)
                appToEditWindow = null
            }
        )
    }

    // Time Window Picker Dialog for multiple selected apps / shield time interval
    if (showTimeWindowPickerForShield) {
        val count = selectedPackageNames.size
        TimeWindowPickerDialog(
            title = if (count > 0) {
                "Set Shield Interval ($count ${if (count > 1) "Apps" else "App"})"
            } else {
                "Set Shield Time Interval"
            },
            confirmButtonText = if (count > 0) {
                "Confirm & Shield ($count ${if (count > 1) "Apps" else "App"})"
            } else {
                "Save Interval"
            },
            initialStartTime = activeStartTime,
            initialEndTime = activeEndTime,
            onDismiss = {
                showTimeWindowPickerForShield = false
                shouldShieldOnIntervalConfirm = false
            },
            onSave = { start, end ->
                shieldStartTime = start
                shieldEndTime = end
                showTimeWindowPickerForShield = false
                if (shouldShieldOnIntervalConfirm || selectedPackageNames.isNotEmpty()) {
                    val currentBlockedCount = uiState.blockedApps.size
                    val freeMax = SubscriptionManager.FREE_LIMITS.maxProtectedApps ?: 3
                    if (!isPro && (currentBlockedCount + selectedPackageNames.size) > freeMax) {
                        showPaywallModal = true
                    } else {
                        val appsToAdd = uiState.installedApps.filter {
                            selectedPackageNames.contains(it.packageName)
                        }
                        viewModel.addMultipleAppsToShield(appsToAdd, start, end)
                        showAppPickerModal = false
                        selectedPackageNames = emptySet()
                    }
                    shouldShieldOnIntervalConfirm = false
                }
            }
        )
    }

    // Clear All Shielded Apps Confirmation Dialog
    if (showClearAllConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmationDialog = false },
            icon = {
                Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = Error, modifier = Modifier.size(32.dp))
            },
            title = {
                Text("Clear All Shielded Apps?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will unblock and remove all ${uiState.blockedApps.size} apps from the shield database immediately.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllBlockedApps()
                        showClearAllConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Error, contentColor = OnError)
                ) {
                    Text("Clear All Apps", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Full Screen Modal to pick installed apps from phone
    if (showAppPickerModal) {
        Dialog(
            onDismissRequest = {
                showAppPickerModal = false
                selectedPackageNames = emptySet()
                shouldShieldOnIntervalConfirm = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            val view = LocalView.current
            val density = LocalDensity.current
            val viewBottomInset = remember(view) {
                val insets = ViewCompat.getRootWindowInsets(view)
                val bottomPx = insets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
                with(density) { bottomPx.toDp() }
            }
            val composeBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val safeNavBottom = maxOf(viewBottomInset, composeBottomInset, 48.dp)

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.ime),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(top = 10.dp)
                ) {
                    // Top App Bar
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
                                selectedPackageNames = emptySet()
                                shouldShieldOnIntervalConfirm = false
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close", tint = OnSurface)
                            }
                            Text(
                                "Add Apps to Shield",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        }

                        if (!isPro) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF673AB7).copy(alpha = 0.15f),
                                modifier = Modifier.clickable {
                                    showAppPickerModal = false
                                    showPaywallModal = true
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.Diamond, null, tint = Color(0xFFB388FF), modifier = Modifier.size(12.dp))
                                    Text("Free: ${uiState.blockedApps.size}/3", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD1C4E9), fontWeight = FontWeight.Bold)
                                }
                            }
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

                    Spacer(Modifier.height(10.dp))

                    val existingPackageNames = remember(uiState.blockedApps) {
                        uiState.blockedApps.map { it.packageName }.toSet()
                    }

                    val filteredApps = remember(uiState.installedApps, appSearchQuery) {
                        if (appSearchQuery.isBlank()) {
                            uiState.installedApps
                        } else {
                            uiState.installedApps.filter {
                                it.appName.contains(appSearchQuery, ignoreCase = true) ||
                                it.packageName.contains(appSearchQuery, ignoreCase = true)
                            }
                        }
                    }

                    val selectableFilteredApps = remember(filteredApps, existingPackageNames) {
                        filteredApps.filter {
                            !existingPackageNames.contains(it.packageName) &&
                            !ShieldManager.isEmergencyPackage(it.packageName)
                        }
                    }

                    val allFilteredSelected = selectableFilteredApps.isNotEmpty() &&
                        selectableFilteredApps.all { selectedPackageNames.contains(it.packageName) }

                    val onToggleSelectAll = {
                        if (allFilteredSelected) {
                            val pkgsToRemove = selectableFilteredApps.map { it.packageName }.toSet()
                            selectedPackageNames = selectedPackageNames - pkgsToRemove
                            shouldShieldOnIntervalConfirm = false
                        } else {
                            val pkgsToAdd = selectableFilteredApps.map { it.packageName }.toSet()
                            selectedPackageNames = selectedPackageNames + pkgsToAdd
                            shouldShieldOnIntervalConfirm = true
                            showTimeWindowPickerForShield = true
                        }
                    }

                    // "Select All Apps" Row with Checkbox
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceContainerHigh,
                        border = BorderStroke(1.dp, Color(0xFF9C27B0).copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleSelectAll() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = allFilteredSelected,
                                    onCheckedChange = { onToggleSelectAll() },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF673AB7),
                                        checkmarkColor = Color.White
                                    )
                                )
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            "Select All Apps",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = OnSurface
                                        )
                                        if (selectedPackageNames.isNotEmpty()) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color(0xFF673AB7).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    "${selectedPackageNames.size} selected",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFB388FF),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        if (allFilteredSelected) "Tap to deselect all"
                                        else "Tap to select all and set shield interval",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Shield Time Interval Selector Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceContainerHigh,
                        border = BorderStroke(1.dp, Primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showTimeWindowPickerForShield = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Filled.Schedule,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        "Shield Time Interval",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${ShieldManager.formatTimeAmPm(activeStartTime)} – ${ShieldManager.formatTimeAmPm(activeEndTime)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurface
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = { showTimeWindowPickerForShield = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Primary.copy(alpha = 0.15f),
                                    contentColor = Primary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.Edit, null, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Set Interval", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // App Selection List
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (uiState.isLoadingApps) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Primary)
                            }
                        } else if (filteredApps.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No apps found", color = OnSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(bottom = if (selectedPackageNames.isNotEmpty()) safeNavBottom + 76.dp else safeNavBottom + 16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredApps, key = { it.packageName }) { appInfo ->
                                    val isAlreadyAdded = existingPackageNames.contains(appInfo.packageName)
                                    val isEmergency = ShieldManager.isEmergencyPackage(appInfo.packageName)
                                    val isSelected = selectedPackageNames.contains(appInfo.packageName)
                                    val iconBitmap = remember(appInfo.packageName) {
                                        InstalledAppUtils.drawableToImageBitmap(appInfo.icon)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = when {
                                            isAlreadyAdded -> SurfaceContainerHigh
                                            isSelected -> Color(0xFF673AB7).copy(alpha = 0.12f)
                                            else -> SurfaceContainerLowest
                                        },
                                        border = when {
                                            isSelected -> BorderStroke(1.5.dp, Color(0xFF9C27B0).copy(alpha = 0.6f))
                                            isAlreadyAdded -> null
                                            else -> BorderStroke(0.5.dp, OutlineVariant.copy(alpha = 0.3f))
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !isAlreadyAdded && !isEmergency) {
                                                val currentBlockedCount = uiState.blockedApps.size
                                                val freeMax = SubscriptionManager.FREE_LIMITS.maxProtectedApps ?: 3
                                                if (!isPro && currentBlockedCount >= freeMax) {
                                                    showPaywallModal = true
                                                } else {
                                                    selectedAppToShield = appInfo
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
                                                        contentDescription = appInfo.appName,
                                                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp))
                                                    )
                                                } else {
                                                    Surface(shape = CircleShape, color = PrimaryContainer, modifier = Modifier.size(38.dp)) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(Icons.Filled.Apps, null, tint = OnPrimaryContainer, modifier = Modifier.size(20.dp))
                                                        }
                                                    }
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        appInfo.appName,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                        color = OnSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        appInfo.packageName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = OnSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            if (isAlreadyAdded) {
                                                Surface(shape = RoundedCornerShape(50), color = TertiaryContainer) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Check, null, tint = OnTertiaryContainer, modifier = Modifier.size(12.dp))
                                                        Text("Shielded", style = MaterialTheme.typography.labelSmall, color = OnTertiaryContainer)
                                                    }
                                                }
                                            } else if (isEmergency) {
                                                Surface(shape = RoundedCornerShape(50), color = SurfaceContainerHighest) {
                                                    Text(
                                                        "Exempt",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = OnSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            } else {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    FilledTonalButton(
                                                        onClick = {
                                                            val currentBlockedCount = uiState.blockedApps.size
                                                            val freeMax = SubscriptionManager.FREE_LIMITS.maxProtectedApps ?: 3
                                                            if (!isPro && currentBlockedCount >= freeMax) {
                                                                showPaywallModal = true
                                                            } else {
                                                                selectedAppToShield = appInfo
                                                            }
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.filledTonalButtonColors(
                                                            containerColor = Color(0xFF673AB7).copy(alpha = 0.15f),
                                                            contentColor = Color(0xFFD1C4E9)
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Schedule, null, modifier = Modifier.size(13.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Shield", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                    }

                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = { checked ->
                                                            selectedPackageNames = if (checked) {
                                                                selectedPackageNames + appInfo.packageName
                                                            } else {
                                                                selectedPackageNames - appInfo.packageName
                                                            }
                                                        },
                                                        colors = CheckboxDefaults.colors(
                                                            checkedColor = Color(0xFF673AB7),
                                                            uncheckedColor = OnSurfaceVariant.copy(alpha = 0.6f),
                                                            checkmarkColor = Color.White
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Persistent Bottom Action Bar for Selection & Time Interval
                    if (selectedPackageNames.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceContainerHigh,
                            border = BorderStroke(1.dp, Color(0xFF673AB7).copy(alpha = 0.35f)),
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = safeNavBottom + 12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showTimeWindowPickerForShield = true }
                                ) {
                                    Text(
                                        "${selectedPackageNames.size} app${if (selectedPackageNames.size > 1) "s" else ""} selected",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurface
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Filled.Schedule, null, tint = Primary, modifier = Modifier.size(13.dp))
                                        Text(
                                            "${ShieldManager.formatTimeAmPm(activeStartTime)} – ${ShieldManager.formatTimeAmPm(activeEndTime)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(Icons.Filled.Edit, null, tint = Primary, modifier = Modifier.size(11.dp))
                                    }
                                }

                                Button(
                                    onClick = {
                                        val currentBlockedCount = uiState.blockedApps.size
                                        val freeMax = SubscriptionManager.FREE_LIMITS.maxProtectedApps ?: 3
                                        if (!isPro && (currentBlockedCount + selectedPackageNames.size) > freeMax) {
                                            showPaywallModal = true
                                        } else if (selectedPackageNames.size == 1) {
                                            val singleApp = uiState.installedApps.find { it.packageName == selectedPackageNames.first() }
                                            if (singleApp != null) {
                                                selectedAppToShield = singleApp
                                            } else {
                                                shouldShieldOnIntervalConfirm = true
                                                showTimeWindowPickerForShield = true
                                            }
                                        } else {
                                            shouldShieldOnIntervalConfirm = true
                                            showTimeWindowPickerForShield = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF673AB7),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Icon(Icons.Filled.Shield, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Shield (${selectedPackageNames.size})",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Accessibility Permission Banner (shown only if not granted)
        if (!uiState.isAccessibilityEnabled) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceContainerHighest,
                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = SecondaryContainer.copy(alpha = 0.3f), modifier = Modifier.size(38.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Warning, null, tint = Secondary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Accessibility Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                        Text(
                            "Enable Accessibility for FocusFlow to detect and close blocked apps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = { viewModel.openAccessibilitySettings() },
                            colors = ButtonDefaults.buttonColors(containerColor = Secondary, contentColor = OnSecondary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Enable in Settings", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Phone Screen Time & App Usage Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToAppUsage() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.DataUsage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Phone Usage & Screen Time",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        }
                        Text(
                            "Screen time & unlock insights",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = OnSurfaceVariant
                )
            }
        }

        // Protected Apps Section Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (uiState.blockedApps.isNotEmpty()) "Shielded Apps (${uiState.blockedApps.size})" else "Shielded Apps",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(
                    onClick = {
                        viewModel.loadInstalledApps()
                        selectedPackageNames = emptySet()
                        shieldStartTime = uiState.masterStartTime
                        shieldEndTime = uiState.masterEndTime
                        appSearchQuery = ""
                        showAppPickerModal = true
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = PrimaryContainer, contentColor = OnPrimaryContainer),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add App", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // App-Wise Protected Apps List
        if (uiState.blockedApps.isEmpty()) {
            Surface(shape = RoundedCornerShape(16.dp), color = SurfaceContainer) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Security, null, tint = Primary, modifier = Modifier.size(40.dp))
                    Text("No Apps Shielded Yet", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                    Text("Choose apps to block during your focus hours.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            viewModel.loadInstalledApps()
                            selectedPackageNames = emptySet()
                            shieldStartTime = uiState.masterStartTime
                            shieldEndTime = uiState.masterEndTime
                            appSearchQuery = ""
                            showAppPickerModal = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary)
                    ) {
                        Icon(Icons.Filled.Apps, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Select Apps from Phone")
                    }
                }
            }
        } else {
            Surface(shape = RoundedCornerShape(16.dp), color = SurfaceContainer) {
                Column {
                    uiState.blockedApps.forEachIndexed { index, app ->
                        StrictAppRow(
                            app = app,
                            pm = context.packageManager,
                            onOpenWindowPicker = {
                                if (ShieldManager.isAppInStrictLock(app)) {
                                    val endDisplay = ShieldManager.formatTimeAmPm(app.endTime)
                                    viewModel.showStrictLockAlert("Strict Lock Active: Focus window for ${app.appName} is strictly locked until $endDisplay and cannot be altered.")
                                } else {
                                    appToEditWindow = app
                                }
                            },
                            onToggle = { viewModel.toggleAppWithStrictCheck(app) },
                            onDelete = { viewModel.deleteAppWithStrictCheck(app) }
                        )
                        if (index < uiState.blockedApps.size - 1) {
                            HorizontalDivider(color = SurfaceContainerHigh)
                        }
                    }
                }
            }
        }
    }


    if (showPaywallModal || uiState.showPaywall) {
        PaywallModal(onDismiss = {
            showPaywallModal = false
            viewModel.dismissPaywall()
        })
    }
}

@Composable
private fun StrictAppRow(
    app: BlockedApp,
    pm: PackageManager,
    onOpenWindowPicker: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val appIcon = remember(app.packageName) {
        try {
            val drawable = pm.getApplicationIcon(app.packageName)
            InstalledAppUtils.drawableToImageBitmap(drawable)
        } catch (e: Exception) {
            null
        }
    }

    val isStrictActive = ShieldManager.isAppInStrictLock(app)
    val startDisplay = ShieldManager.formatTimeAmPm(app.startTime)
    val endDisplay = ShieldManager.formatTimeAmPm(app.endTime)
    val remainingSeconds = if (isStrictActive) ShieldManager.getWindowRemainingSeconds(app.startTime, app.endTime) else 0L

    val hours = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val secs = remainingSeconds % 60
    val countdownText = when {
        hours > 0 -> "%dh %02dm left".format(hours, minutes)
        minutes > 0 -> "%dm %02ds left".format(minutes, secs)
        else -> "%ds left".format(secs)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top row: App icon, name, status, switch, delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = app.appName,
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                Surface(shape = CircleShape, color = Primary.copy(alpha = 0.15f), modifier = Modifier.size(38.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.SmartDisplay, null, tint = Primary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // App Name & Lock Status
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(app.appName, style = MaterialTheme.typography.labelLarge, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)

                if (isStrictActive) {
                    Text(
                        "🔒 Locked until $endDisplay ($countdownText)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Tertiary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        if (app.isBlocked) "Window: $startDisplay – $endDisplay" else "Shield Paused",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Switch (Tapping while in strict lock immediately triggers strict lock rejection dialog)
            Box(
                modifier = if (isStrictActive) Modifier.clickable { onToggle() } else Modifier
            ) {
                Switch(
                    checked = app.isBlocked,
                    onCheckedChange = { onToggle() },
                    enabled = !isStrictActive,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Tertiary,
                        checkedThumbColor = OnTertiaryContainer,
                        uncheckedTrackColor = SurfaceContainerHighest,
                        uncheckedThumbColor = Outline,
                        disabledCheckedTrackColor = Tertiary.copy(alpha = 0.6f),
                        disabledCheckedThumbColor = OnTertiaryContainer
                    )
                )
            }

            // Delete or Strict Lock Indicator (Clicking while locked shows rejection dialog)
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                if (isStrictActive) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "Strict locked: Cannot delete until $endDisplay",
                        tint = Tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "Remove App",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bottom row: Window chip (tap to pick focus window; blocked during strict lock)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isStrictActive) TertiaryContainer.copy(alpha = 0.15f) else SurfaceContainerHighest,
            border = ButtonDefaults.outlinedButtonBorder(enabled = true),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onOpenWindowPicker() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    if (isStrictActive) Icons.Filled.Lock else Icons.Filled.Schedule,
                    contentDescription = "Focus window",
                    tint = if (isStrictActive) Tertiary else Primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "$startDisplay – $endDisplay",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isStrictActive) Tertiary else OnSurface
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun calculateDurationDisplay(startTime: String, endTime: String): String {
    return try {
        val startParts = startTime.split(":")
        val endParts = endTime.split(":")
        val startH = startParts[0].toInt()
        val startM = startParts[1].toInt()
        val endH = endParts[0].toInt()
        val endM = endParts[1].toInt()

        val startTotalMin = startH * 60 + startM
        val endTotalMin = endH * 60 + endM

        val diffMin = if (endTotalMin >= startTotalMin) {
            endTotalMin - startTotalMin
        } else {
            (24 * 60 - startTotalMin) + endTotalMin
        }

        val h = diffMin / 60
        val m = diffMin % 60
        when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    } catch (e: Exception) {
        ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeWindowPickerDialog(
    title: String,
    initialStartTime: String,
    initialEndTime: String,
    onDismiss: () -> Unit,
    onSave: (startTime: String, endTime: String) -> Unit,
    confirmButtonText: String = "Save Focus Window"
) {
    var selectedStart by remember { mutableStateOf(initialStartTime) }
    var selectedEnd by remember { mutableStateOf(initialEndTime) }

    // Which time is being edited: null = none, "start" or "end"
    var editingTime by remember { mutableStateOf<String?>(null) }

    val now = Calendar.getInstance()
    val curH = now.get(Calendar.HOUR_OF_DAY)
    val curM = now.get(Calendar.MINUTE)
    val curFormatted = "%02d:%02d".format(curH, curM)
    val plus1h = "%02d:%02d".format((curH + 1) % 24, curM)
    val plus2h = "%02d:%02d".format((curH + 2) % 24, curM)
    val plus4h = "%02d:%02d".format((curH + 4) % 24, curM)

    val presets = remember {
        listOf(
            "Full Day" to ("00:00" to "23:59"),
            "Work (9-5)" to ("09:00" to "17:00"),
            "Evening" to ("18:00" to "22:00"),
            "Night" to ("22:00" to "06:00"),
            "+1 Hour" to (curFormatted to plus1h),
            "+2 Hours" to (curFormatted to plus2h),
            "+4 Hours" to (curFormatted to plus4h)
        )
    }

    val durationText = remember(selectedStart, selectedEnd) {
        calculateDurationDisplay(selectedStart, selectedEnd)
    }

    // Inline Material3 TimePicker clock dial sub-dialog
    if (editingTime != null) {
        val currentTime = if (editingTime == "start") selectedStart else selectedEnd
        val parts = currentTime.split(":")
        val initHour = parts.getOrNull(0)?.toIntOrNull() ?: curH
        val initMinute = parts.getOrNull(1)?.toIntOrNull() ?: curM

        val timePickerState = rememberTimePickerState(
            initialHour = initHour,
            initialMinute = initMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { editingTime = null },
            title = {
                Text(
                    if (editingTime == "start") "Select Start Time" else "Select End Time",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = SurfaceContainerHigh,
                            selectorColor = Primary,
                            containerColor = SurfaceContainer,
                            periodSelectorSelectedContainerColor = PrimaryContainer,
                            periodSelectorSelectedContentColor = OnPrimaryContainer,
                            timeSelectorSelectedContainerColor = PrimaryContainer,
                            timeSelectorSelectedContentColor = OnPrimaryContainer,
                            timeSelectorUnselectedContainerColor = SurfaceContainerHigh,
                            timeSelectorUnselectedContentColor = OnSurface
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val timeString = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
                        if (editingTime == "start") {
                            selectedStart = timeString
                        } else {
                            selectedEnd = timeString
                        }
                        editingTime = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirm", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTime = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Time Range Summary Pill with Duration
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Primary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Schedule, null, tint = Primary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "${ShieldManager.formatTimeAmPm(selectedStart)} – ${ShieldManager.formatTimeAmPm(selectedEnd)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                        if (durationText.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Primary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = durationText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Quick Presets
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Quick Presets",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { (label, times) ->
                            val isSelected = selectedStart == times.first && selectedEnd == times.second
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedStart = times.first
                                    selectedEnd = times.second
                                },
                                label = {
                                    Text(label, style = MaterialTheme.typography.labelSmall)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary.copy(alpha = 0.25f),
                                    selectedLabelColor = Primary
                                )
                            )
                        }
                    }
                }

                // Custom Time Selector Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Start Time Button
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceContainerHigh,
                        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { editingTime = "start" }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Start Time", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, fontWeight = FontWeight.Medium)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Schedule, null, tint = Primary, modifier = Modifier.size(16.dp))
                                Text(
                                    ShieldManager.formatTimeAmPm(selectedStart),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                            }
                        }
                    }

                    // End Time Button
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceContainerHigh,
                        border = BorderStroke(1.dp, OutlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { editingTime = "end" }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("End Time", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, fontWeight = FontWeight.Medium)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Schedule, null, tint = Primary, modifier = Modifier.size(16.dp))
                                Text(
                                    ShieldManager.formatTimeAmPm(selectedEnd),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedStart, selectedEnd) },
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(confirmButtonText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PremiumBadge(
    text: String = "PREMIUM",
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF673AB7).copy(alpha = 0.22f),
        border = BorderStroke(0.8.dp, Color(0xFF9C27B0).copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(Icons.Filled.Diamond, null, tint = Color(0xFFB388FF), modifier = Modifier.size(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = Color(0xFFD1C4E9)
            )
        }
    }
}
