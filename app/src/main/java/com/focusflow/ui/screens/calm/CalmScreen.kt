package com.focusflow.ui.screens.calm

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focusflow.data.db.FocusFlowDatabase
import com.focusflow.subscription.SubscriptionManager
import com.focusflow.ui.modals.PaywallModal
import com.focusflow.ui.theme.*

@Composable
fun CalmScreen(
    database: FocusFlowDatabase,
    viewModel: CalmViewModel = viewModel(factory = CalmViewModel.Factory(database))
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPro by SubscriptionManager.isPro.collectAsState()
    var showProPaywall by remember { mutableStateOf(false) }

    if (showProPaywall) {
        PaywallModal(onDismiss = { showProPaywall = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Streak Hero
        Surface(shape = RoundedCornerShape(16.dp), color = SurfaceContainer, shadowElevation = 4.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${uiState.consistentDays}", style = MaterialTheme.typography.displayLarge, color = Primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("days", style = MaterialTheme.typography.headlineSmall, color = OnSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                }
                Text("Mindfulness Streak", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = OnSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.MilitaryTech, null, tint = Secondary, modifier = Modifier.size(16.dp))
                        Text("Personal Best: ", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        Text("${uiState.personalBest} days", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = OnSurface)
                    }
                }
            }
        }

        // Breathing Section
        Surface(shape = RoundedCornerShape(16.dp), color = SurfaceContainer, shadowElevation = 4.dp) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Secondary.copy(alpha = 0.15f), modifier = Modifier.size(32.dp)) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Air, null, tint = Secondary, modifier = Modifier.size(20.dp)) }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Breathe & Reset", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurface)
                            Text("2-minute cadence", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        }
                    }
                }

                // Breathing Orb
                val orbScale by animateFloatAsState(
                    targetValue = when {
                        !uiState.isBreathingActive -> 1f
                        uiState.breathPhase == "Exhale" -> 0.85f
                        else -> 1.2f
                    },
                    animationSpec = tween(700, easing = EaseInOut), label = "orb"
                )

                Surface(shape = RoundedCornerShape(16.dp), color = SurfaceContainerLowest) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.size((112 * orbScale).dp), contentAlignment = Alignment.Center) {
                            // Gradient orb
                            Box(
                                modifier = Modifier.fillMaxSize().clip(CircleShape).background(
                                    Brush.linearGradient(listOf(PrimaryContainer, SurfaceContainerHigh, TertiaryContainer))
                                )
                            )
                            // Inner circle
                            Box(
                                modifier = Modifier.size((80 * orbScale).dp).clip(CircleShape).background(SurfaceContainerLowest.copy(alpha = 0.8f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(uiState.breathPhase, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = OnSurface)
                                    Text("${uiState.breathCounter}s", style = MaterialTheme.typography.labelSmall, color = Primary)
                                }
                            }
                        }
                        // Cadence pills
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(Triple("Inhale 4s", Primary, "primary"), Triple("Hold 7s", Secondary, "secondary"), Triple("Exhale 8s", Tertiary, "tertiary")).forEach { (label, color, _) ->
                                Surface(shape = RoundedCornerShape(50), color = SurfaceContainerHigh.copy(alpha = 0.9f)) {
                                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
                                        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = OnSurface)
                                    }
                                }
                            }
                        }
                        // Start/Stop button
                        Button(
                            onClick = { viewModel.toggleBreathing() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isBreathingActive) SurfaceContainerHighest else Primary,
                                contentColor = if (uiState.isBreathingActive) OnSurface else OnPrimary
                            )
                        ) {
                            Icon(if (uiState.isBreathingActive) Icons.Filled.Stop else Icons.Filled.PlayArrow, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (uiState.isBreathingActive) "Pause Reset Session" else "Start 4-7-8 Reset", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                // Physical alternative
                Surface(shape = RoundedCornerShape(8.dp), color = SurfaceContainerHigh) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bolt, null, tint = Secondary, modifier = Modifier.size(22.dp))
                        Column {
                            Text("Quick Physical Alternative", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Secondary)
                            Text("Do 10 brisk pushups or drink a cold glass of water right now.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        }
                    }
                }
            }
        }

        // === Pro Reset Library ===
        Surface(shape = RoundedCornerShape(16.dp), color = SurfaceContainer, shadowElevation = 4.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF673AB7).copy(alpha = 0.15f), modifier = Modifier.size(32.dp)) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.AutoAwesome, null, tint = Color(0xFF673AB7), modifier = Modifier.size(18.dp))
                            }
                        }
                        Column {
                            Text("Pro Reset Library", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurface)
                            Text("Advanced breathing protocols", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        }
                    }
                    Surface(shape = RoundedCornerShape(50), color = Color(0xFF673AB7).copy(alpha = 0.12f)) {
                        Text("💎 Pro Only", style = MaterialTheme.typography.labelSmall, color = Color(0xFF673AB7), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }

                // Box Breathing
                ProExerciseCard(
                    emoji = "🟦",
                    title = "Box Breathing",
                    subtitle = "4s inhale → 4s hold → 4s exhale → 4s hold",
                    benefit = "Reduces cortisol & restores calm in 2 minutes",
                    isPro = isPro,
                    onLockedClick = { showProPaywall = true }
                )

                // Resonance Breathing
                ProExerciseCard(
                    emoji = "🌀",
                    title = "Resonance Breathing",
                    subtitle = "5s inhale → 5s exhale at 6 breaths/min",
                    benefit = "Activates parasympathetic recovery response",
                    isPro = isPro,
                    onLockedClick = { showProPaywall = true }
                )

                // Wim Hof Pattern
                ProExerciseCard(
                    emoji = "❄️",
                    title = "Wim Hof Cycle",
                    subtitle = "30× power breaths → hold → recovery exhale",
                    benefit = "Supercharges energy & mental resilience",
                    isPro = isPro,
                    onLockedClick = { showProPaywall = true }
                )
            }
        }

        // Mood Check-in
        Surface(shape = RoundedCornerShape(16.dp), color = SurfaceContainer, shadowElevation = 4.dp) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Tertiary.copy(alpha = 0.15f), modifier = Modifier.size(32.dp)) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Filled.EditNote, null, tint = Tertiary, modifier = Modifier.size(20.dp)) }
                        }
                        Column {
                            Text("Evening Reflection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurface)
                        }
                    }
                    Surface(shape = RoundedCornerShape(50), color = SurfaceContainerHigh) {
                        Text("Today", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }

                // Mood selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Current State / Mood", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = OnSurface)
                        val moodLabels = listOf("", "Very Low", "Low", "Okay", "Fulfilling", "Great")
                        Text(moodLabels.getOrElse(uiState.selectedMood) { "" }, style = MaterialTheme.typography.labelSmall, color = Tertiary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val moods = listOf(
                            1 to Icons.Filled.SentimentVeryDissatisfied to "Very Low",
                            2 to Icons.Filled.SentimentDissatisfied to "Low",
                            3 to Icons.Filled.SentimentNeutral to "Okay",
                            4 to Icons.Filled.SentimentSatisfied to "Good",
                            5 to Icons.Filled.SentimentVerySatisfied to "Great"
                        )
                        moods.forEach { (pair, label) ->
                            val (value, icon) = pair
                            val isSelected = uiState.selectedMood == value
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) PrimaryContainer else SurfaceContainerHigh,
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                onClick = { viewModel.selectMood(value) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(icon, null, tint = if (isSelected) OnPrimaryContainer else OnSurfaceVariant, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = if (isSelected) OnPrimaryContainer else OnSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Energy level
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Energy Reserve", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = OnSurface)
                        val energyLabels = listOf("", "Depleted", "Low", "Moderate", "Vibrant", "Peak")
                        Text(energyLabels.getOrElse(uiState.energyLevel) { "" }, style = MaterialTheme.typography.labelSmall, color = Secondary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..5).forEach { level ->
                            val isSelected = level <= uiState.energyLevel
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Secondary else SurfaceContainerHighest,
                                onClick = { viewModel.selectEnergy(level) },
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("$level", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isSelected) OnSecondary else OnSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Journal note
                var showNoteDialog by remember { mutableStateOf(false) }
                var noteInput by remember { mutableStateOf("") }

                if (showNoteDialog) {
                    AlertDialog(
                        onDismissRequest = { showNoteDialog = false },
                        title = { Text("Today's Reflection Note") },
                        text = {
                            OutlinedTextField(
                                value = noteInput,
                                onValueChange = { noteInput = it },
                                placeholder = { Text("What brought clarity or distraction today?") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.saveJournalNote(noteInput)
                                showNoteDialog = false
                            }) {
                                Text("Save")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showNoteDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Today's Local Note", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = OnSurface)
                        Icon(Icons.Filled.EnhancedEncryption, null, tint = Tertiary, modifier = Modifier.size(14.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceContainerLowest,
                        onClick = {
                            noteInput = uiState.journalNote
                            showNoteDialog = true
                        }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (uiState.journalNote.isNotBlank()) {
                                Text("\"${uiState.journalNote}\"", style = MaterialTheme.typography.bodyMedium, color = OnSurface, fontStyle = FontStyle.Italic, lineHeight = 22.sp)
                            } else {
                                Text("Tap here to write today's reflection note...", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant, fontStyle = FontStyle.Italic)
                            }
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = SurfaceVariant.copy(alpha = 0.3f))
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.Edit, null, tint = Primary, modifier = Modifier.size(14.dp))
                                    Text(if (uiState.journalNote.isBlank()) "Write Note" else "Edit Note", style = MaterialTheme.typography.labelSmall, color = Primary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // Weekly stability sparkline
                Surface(shape = RoundedCornerShape(8.dp), color = SurfaceContainerHigh) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Weekly Stability Index", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = OnSurface)
                                val subtitle = if (uiState.stabilityPercent > 0) "${uiState.stabilityPercent}% Emotional Equanimity" else "No entries yet"
                                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Tertiary)
                            }
                            if (uiState.stabilityTrend.isNotBlank()) {
                                Surface(shape = RoundedCornerShape(50), color = Tertiary.copy(alpha = 0.1f)) {
                                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Filled.ShowChart, null, tint = Tertiary, modifier = Modifier.size(16.dp))
                                        Text(uiState.stabilityTrend, style = MaterialTheme.typography.labelMedium, color = Tertiary)
                                    }
                                }
                            }
                        }
                        // Sparkline
                        val data = uiState.weeklyData
                        if (data.size >= 2) {
                            val tertiaryColor = Tertiary
                            Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                val w = size.width
                                val h = size.height
                                val stepX = w / (data.size - 1)
                                val path = Path()
                                data.forEachIndexed { i, v ->
                                    val x = i * stepX
                                    val y = h - (v * h)
                                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                // Fill
                                val fillPath = Path().apply {
                                    addPath(path)
                                    lineTo(w, h)
                                    lineTo(0f, h)
                                    close()
                                }
                                drawPath(fillPath, Brush.verticalGradient(listOf(tertiaryColor.copy(alpha = 0.35f), tertiaryColor.copy(alpha = 0f))))
                                drawPath(path, tertiaryColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                                // End dot
                                val lastX = (data.size - 1) * stepX
                                val lastY = h - (data.last() * h)
                                drawCircle(tertiaryColor, radius = 4.dp.toPx(), center = Offset(lastX, lastY))
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                                Text("Check in across the week to track stability trend", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Today").forEach { day ->
                                Text(day, style = MaterialTheme.typography.labelSmall, color = if (day == "Today") Tertiary else OnSurfaceVariant, fontWeight = if (day == "Today") FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }

        
    }
}

@Composable
private fun ProExerciseCard(
    emoji: String,
    title: String,
    subtitle: String,
    benefit: String,
    isPro: Boolean,
    onLockedClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isPro) SurfaceContainerHigh else SurfaceContainerHighest.copy(alpha = 0.7f),
        onClick = if (!isPro) onLockedClick else ({})
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                        color = if (isPro) OnSurface else OnSurfaceVariant)
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = if (isPro) OnSurfaceVariant else OnSurfaceVariant.copy(alpha = 0.5f))
                if (isPro) {
                    Spacer(Modifier.height(4.dp))
                    Surface(shape = RoundedCornerShape(50), color = Tertiary.copy(alpha = 0.12f)) {
                        Text(benefit, style = MaterialTheme.typography.labelSmall, color = Tertiary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
            }
            if (!isPro) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF673AB7).copy(alpha = 0.15f)) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = Color(0xFF673AB7),
                        modifier = Modifier.padding(6.dp).size(16.dp)
                    )
                }
            } else {
                Icon(Icons.Filled.PlayArrow, null, tint = Primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}
