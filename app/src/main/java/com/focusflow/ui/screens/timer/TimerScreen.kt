package com.focusflow.ui.screens.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focusflow.data.db.FocusFlowDatabase
import com.focusflow.subscription.SubscriptionManager
import com.focusflow.ui.modals.PaywallModal
import com.focusflow.ui.theme.*

@Composable
fun TimerScreen(
    database: FocusFlowDatabase,
    viewModel: TimerViewModel = viewModel(factory = TimerViewModel.Factory(database))
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPro by SubscriptionManager.isPro.collectAsState()
    val progress = if (uiState.targetMs > 0) (uiState.elapsedMs.toFloat() / uiState.targetMs).coerceAtMost(1f) else 0f
    val percentComplete = (progress * 100).toInt()

    // Paywall overlay
    if (uiState.showProPaywall) {
        PaywallModal(onDismiss = { viewModel.dismissProPaywall() })
    }

    val totalSec = uiState.elapsedMs / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    val centis = (uiState.elapsedMs % 1000) / 10
    val timerText = "%02d:%02d:%02d".format(hours, minutes, seconds)

    val targetMin = uiState.targetMs / 60000
    val completedMin = uiState.elapsedMs / 60000
    val remainingMs = (uiState.targetMs - uiState.elapsedMs).coerceAtLeast(0)
    val remMin = remainingMs / 60000
    val remSec = (remainingMs / 1000) % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Circular Stopwatch with Start/Pause Button Inside the Circle
        Surface(shape = RoundedCornerShape(16.dp), color = SurfaceContainerLow, shadowElevation = 8.dp) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(modifier = Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = SurfaceContainerHighest,
                        strokeWidth = 9.dp,
                        strokeCap = StrokeCap.Round
                    )
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = PrimaryContainer,
                        strokeWidth = 10.dp,
                        strokeCap = StrokeCap.Round
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Timelapse, null, tint = Secondary, modifier = Modifier.size(16.dp))
                            Text("DEEP FOCUS", style = MaterialTheme.typography.labelSmall, color = Secondary, letterSpacing = 2.sp)
                        }

                        Spacer(Modifier.height(8.dp))

                        // Digital Timer Readout
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                timerText,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 36.sp),
                                color = Primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                ".%02d".format(centis),
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                color = PrimaryFixedDim.copy(alpha = 0.8f)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // START FOCUS / PAUSE BUTTON (Shifted inside the timer circle)
                        Button(
                            onClick = { viewModel.togglePause() },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isRunning) PrimaryContainer else Tertiary,
                                contentColor = if (uiState.isRunning) OnPrimaryContainer else OnTertiaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isRunning) "Pause" else if (uiState.elapsedMs == 0L) "Start Focus" else "Resume",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Metrics ribbon
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("TARGET", "${targetMin / 60}h %02dm".format(targetMin % 60), Outline),
                        Triple("COMPLETED", "${completedMin / 60}h %02dm".format(completedMin % 60), Primary),
                        Triple("REMAINING", "${remMin}m %02ds".format(remSec), Tertiary)
                    ).forEach { (label, value, color) ->
                        Surface(shape = RoundedCornerShape(8.dp), color = if (label == "COMPLETED") SurfaceContainerHigh else SurfaceContainer, modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(label, style = MaterialTheme.typography.labelSmall, color = color, letterSpacing = 1.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(value, style = MaterialTheme.typography.labelLarge, color = if (label == "COMPLETED") Primary else OnSurface)
                            }
                        }
                    }
                }
            }
        }

        // Target Time Adjuster & Session Controls
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // === Pro Focus Presets ===
            Surface(shape = RoundedCornerShape(14.dp), color = SurfaceContainerLow, shadowElevation = 2.dp) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Focus Presets", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                        if (!isPro) {
                            Surface(shape = RoundedCornerShape(50), color = Color(0xFF673AB7).copy(alpha = 0.12f)) {
                                Text("💎 Pro unlocks more", style = MaterialTheme.typography.labelSmall, color = Color(0xFF673AB7), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PRO_PRESETS_MS.forEach { (label, ms, requiresPro) ->
                            val isActive = uiState.targetMs == ms
                            val locked = requiresPro && !isPro
                            FilterChip(
                                selected = isActive,
                                onClick = { viewModel.applyPreset(ms, requiresPro) },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (locked) Icon(Icons.Filled.Lock, null, tint = Color(0xFF673AB7), modifier = Modifier.size(12.dp))
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (locked) Color(0xFF673AB7).copy(alpha = 0.1f) else PrimaryContainer,
                                    selectedLabelColor = if (locked) Color(0xFF673AB7) else OnPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // === Custom Time Setter ===
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceContainerLow,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Set Focus Duration", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, letterSpacing = 1.sp)
                        if (!isPro) {
                            Surface(shape = RoundedCornerShape(50), color = Color(0xFF673AB7).copy(alpha = 0.12f)) {
                                Text("Max 40m free", style = MaterialTheme.typography.labelSmall, color = Color(0xFF673AB7), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                    }

                    val currentHours = (targetMin / 60).toInt()
                    val currentMinutes = (targetMin % 60).toInt()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hours Column
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("HOURS", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, letterSpacing = 1.sp)
                            FilledIconButton(
                                onClick = { viewModel.adjustTargetMs(3600000L) },
                                shape = RoundedCornerShape(10.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = SurfaceContainerHigh,
                                    contentColor = Primary
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.KeyboardArrowUp, null, modifier = Modifier.size(24.dp))
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PrimaryContainer.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp, 56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "%02d".format(currentHours),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Primary
                                    )
                                }
                            }
                            FilledIconButton(
                                onClick = { viewModel.adjustTargetMs(-3600000L) },
                                shape = RoundedCornerShape(10.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = SurfaceContainerHigh,
                                    contentColor = Tertiary
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.KeyboardArrowDown, null, modifier = Modifier.size(24.dp))
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        // Colon separator
                        Text(
                            ":",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceVariant,
                            modifier = Modifier.padding(top = 20.dp)
                        )

                        Spacer(Modifier.width(10.dp))

                        // Minutes Column
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("MINS", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, letterSpacing = 1.sp)
                            FilledIconButton(
                                onClick = { viewModel.adjustTargetMs(60000L) },
                                shape = RoundedCornerShape(10.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = SurfaceContainerHigh,
                                    contentColor = Primary
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.KeyboardArrowUp, null, modifier = Modifier.size(24.dp))
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PrimaryContainer.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp, 56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "%02d".format(currentMinutes),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Primary
                                    )
                                }
                            }
                            FilledIconButton(
                                onClick = { viewModel.adjustTargetMs(-60000L) },
                                shape = RoundedCornerShape(10.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = SurfaceContainerHigh,
                                    contentColor = Tertiary
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.KeyboardArrowDown, null, modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    // Quick adjust row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        listOf(5, 15, 25, 45, 60).forEach { mins ->
                            val selected = targetMin == mins.toLong()
                            val locked = !isPro && mins > 40
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setCustomTargetMinutes(mins) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        if (locked) Icon(Icons.Filled.Lock, null, tint = Color(0xFF673AB7), modifier = Modifier.size(11.dp))
                                        Text("${mins}m", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (locked) Color(0xFF673AB7).copy(alpha = 0.1f) else PrimaryContainer,
                                    selectedLabelColor = if (locked) Color(0xFF673AB7) else OnPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Lap / Split & Finish Session Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { viewModel.addLap() },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceContainerHigh, contentColor = OnSurface)
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Lap / Split", style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = { viewModel.finishSession() },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TertiaryContainer, contentColor = OnTertiaryContainer)
                ) {
                    Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Finish Session", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }

            // Reset option if paused and elapsed
            if (uiState.elapsedMs > 0L && !uiState.isRunning) {
                TextButton(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.RestartAlt, null, modifier = Modifier.size(16.dp), tint = OnSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("Reset Timer", color = OnSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Laps
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.List, null, tint = Primary, modifier = Modifier.size(18.dp))
                    Text("Splits & Activity", style = MaterialTheme.typography.headlineSmall, color = OnSurface)
                }
                Text("${uiState.laps.size} RECORDED", style = MaterialTheme.typography.labelSmall, color = Outline)
            }
            if (uiState.laps.isEmpty()) {
                Surface(shape = RoundedCornerShape(12.dp), color = SurfaceContainer) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No laps recorded yet. Tap 'Lap / Split' while timer runs.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    }
                }
            } else {
                uiState.laps.forEach { lap ->
                    LapCard(lap, isLatest = lap == uiState.laps.first())
                }
            }
        }

       
    }
}

@Composable
private fun LapCard(lap: LapDisplay, isLatest: Boolean) {
    Surface(shape = RoundedCornerShape(8.dp), color = SurfaceContainer) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(shape = CircleShape, color = if (isLatest) Primary.copy(alpha = 0.2f) else SurfaceContainerHighest, modifier = Modifier.size(32.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("%02d".format(lap.number), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isLatest) Primary else OnSurfaceVariant)
                    }
                }
                Column {
                    Text(lap.label, style = MaterialTheme.typography.labelLarge, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (isLatest) Icon(Icons.AutoMirrored.Filled.ShowChart, null, tint = Tertiary, modifier = Modifier.size(14.dp))
                        Text(lap.note, style = MaterialTheme.typography.bodySmall, color = if (isLatest) Tertiary else OnSurfaceVariant)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(lap.splitTime, style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp), fontWeight = FontWeight.SemiBold, color = if (isLatest) Primary else OnSurface)
                Text("OVERALL ${lap.overallTime}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Outline)
            }
        }
    }
}
