package com.focusflow.ui.screens.habits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focusflow.data.db.FocusFlowDatabase
import com.focusflow.subscription.SubscriptionManager
import com.focusflow.ui.modals.PaywallModal
import com.focusflow.ui.theme.*
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HabitsScreen(
    database: FocusFlowDatabase,
    viewModel: HabitsViewModel = viewModel(factory = HabitsViewModel.Factory(database))
) {
    val uiState by viewModel.uiState.collectAsState()
    val completedCount = uiState.habits.count { it.isCompleted }
    val totalCount = uiState.habits.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    var showAddDialog by remember { mutableStateOf(false) }
    var newHabitTitle by remember { mutableStateOf("") }
    var newHabitCategory by remember { mutableStateOf("Productivity") }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Create New Habit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newHabitTitle,
                        onValueChange = { newHabitTitle = it },
                        label = { Text("Habit Name") },
                        placeholder = { Text("e.g. Read 15 mins, Deep Work") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Productivity", "Health", "Mindfulness").forEach { cat ->
                            val isSelected = newHabitCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { newHabitCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newHabitTitle.isNotBlank()) {
                        viewModel.addHabit(newHabitTitle, newHabitCategory, 15)
                        newHabitTitle = ""
                        showAddDialog = false
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Daily Habits", style = MaterialTheme.typography.headlineMedium, color = OnSurface, fontWeight = FontWeight.Bold)

            }
            FilledTonalButton(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = PrimaryContainer, contentColor = OnPrimaryContainer),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Habit", style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }

        // Summary Banner
        Surface(shape = RoundedCornerShape(24.dp), color = SurfaceContainerHigh, shadowElevation = 6.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.LocalFireDepartment, null, tint = Tertiary, modifier = Modifier.size(18.dp))
                            Text("${uiState.streakDays} Day Streak", style = MaterialTheme.typography.labelMedium, color = Tertiary)
                        }
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("$completedCount / $totalCount", style = MaterialTheme.typography.headlineLarge, color = OnSurface)
                            Text("completed", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                        }
                        val remainingText = when {
                            totalCount == 0 -> "Add your first habit to begin tracking."
                            completedCount == totalCount -> "All habits completed today! Outstanding focus! 🎉"
                            else -> "${totalCount - completedCount} habits remaining today."
                        }
                        Text(remainingText, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant.copy(alpha = 0.9f))
                    }
                    // Radial gauge
                    Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = SurfaceContainerHighest, strokeWidth = 6.dp, strokeCap = StrokeCap.Round)
                        CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(), color = Tertiary, strokeWidth = 6.dp, strokeCap = StrokeCap.Round)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp), color = Tertiary)
                            Text("PACE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = OnSurfaceVariant, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }

        // Heatmap (directly below streak)
        HeatmapSection(
            uiState = uiState,
            onTabSelect = { viewModel.selectHeatmapTab(it) },
            onHabitSelect = { viewModel.selectHabitForHeatmap(it) },
            onMonthSelect = { viewModel.selectMonthForHeatmap(it) }
        )

        // Habit Checklist
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Habits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnSurface)
            if (uiState.habits.isEmpty()) {
                Surface(shape = RoundedCornerShape(24.dp), color = SurfaceContainer) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircleOutline, null, tint = Tertiary, modifier = Modifier.size(40.dp))
                        Text("No Habits Yet", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                        Text("Create your first habit to build momentum.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary)
                        ) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add First Habit")
                        }
                    }
                }
            } else {
                uiState.habits.forEach { habitWithStatus ->
                    HabitRow(
                        habitWithStatus = habitWithStatus,
                        isSelected = uiState.selectedHabitId == habitWithStatus.habit.id,
                        onToggle = { viewModel.toggleHabit(habitWithStatus.habit, habitWithStatus.isCompleted) },
                        onSelectForHeatmap = {
                            if (uiState.selectedHabitId == habitWithStatus.habit.id) {
                                viewModel.selectHabitForHeatmap(null)
                            } else {
                                viewModel.selectHabitForHeatmap(habitWithStatus.habit.id)
                            }
                        }
                    )
                }
            }
        }
    }

    if (uiState.showPaywall) {
        PaywallModal(onDismiss = { viewModel.dismissPaywall() })
    }
}

private val habitIconMap = mapOf(
    "eco" to Icons.Filled.Eco,
    "auto_stories" to Icons.Filled.AutoStories,
    "bolt" to Icons.Filled.Bolt,
    "self_improvement" to Icons.Filled.SelfImprovement,
    "water_drop" to Icons.Filled.WaterDrop,
    "edit_note" to Icons.Filled.EditNote
)

@Composable
private fun HabitRow(
    habitWithStatus: HabitWithStatus,
    isSelected: Boolean = false,
    onToggle: () -> Unit,
    onSelectForHeatmap: () -> Unit = {}
) {
    val habit = habitWithStatus.habit
    val isCompleted = habitWithStatus.isCompleted
    val icon = habitIconMap[habit.iconName] ?: Icons.Filled.CheckCircle

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) SurfaceContainerHigh else SurfaceContainer,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Tertiary) else null,
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isCompleted) TertiaryContainer else SurfaceContainerHighest,
                    border = if (!isCompleted) BorderStroke(1.5.dp, OutlineVariant.copy(alpha = 0.6f)) else null,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (isCompleted) Icon(Icons.Filled.Check, null, tint = OnTertiaryContainer, modifier = Modifier.size(18.dp))
                    }
                }
                Column {
                    Text(
                        habit.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (isCompleted) OnSurfaceVariant.copy(alpha = 0.7f) else OnSurface,
                        fontWeight = if (!isCompleted) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(habit.time, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                        if (habit.durationMinutes > 0) {
                            Text("•", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                            Text("${habit.durationMinutes}m", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                        }
                        Text("•", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                        val catColor = when (habit.category) {
                            "Health" -> Tertiary
                            "Mindfulness" -> Secondary
                            "Productivity" -> Primary
                            else -> PrimaryFixedDim
                        }
                        Text(habit.category, style = MaterialTheme.typography.labelSmall, color = catColor, fontWeight = if (habit.category == "Productivity") FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Button to focus heatmap on this habit
                IconButton(
                    onClick = onSelectForHeatmap,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = "View Habit Heatmap",
                        tint = if (isSelected) Tertiary else OnSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Icon(icon, null, tint = if (isCompleted) Tertiary else OutlineVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun HeatmapSection(
    uiState: HabitsUiState,
    onTabSelect: (Int) -> Unit = {},
    onHabitSelect: (Long?) -> Unit = {},
    onMonthSelect: (Int) -> Unit = {}
) {
    val now = LocalDate.now()
    val isPro by SubscriptionManager.isPro.collectAsState()
    val selectedMonth = uiState.selectedMonthValue
    val monthName = java.time.Month.of(selectedMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    val monthShort = java.time.Month.of(selectedMonth).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    val currentMonthNumber = now.monthValue
    val past90Start = now.minusDays(89)
    val past90MonthNumber = past90Start.monthValue

    val tabLabels = listOf(
        monthShort,
        "90 Days",
        "Year"
    )

    val selectedHabit = uiState.habits.firstOrNull { it.habit.id == uiState.selectedHabitId }?.habit
    var selectedDayDetail by remember { mutableStateOf<HeatmapDayInfo?>(null) }
    val yearScrollState = rememberScrollState()

    // Scroll to end of year by default when in LeetCode year tab
    LaunchedEffect(uiState.selectedHeatmapTab) {
        if (uiState.selectedHeatmapTab == 2) {
            yearScrollState.scrollTo(yearScrollState.maxValue)
        }
    }

    Surface(shape = RoundedCornerShape(24.dp), color = SurfaceContainerHigh, shadowElevation = 4.dp) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header with Month Number / LeetCode Title and Habit Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when {
                            selectedHabit != null -> "${selectedHabit.title} Heatmap"
                            uiState.selectedHeatmapTab == 2 -> "Annual Consistency"
                            else -> "Consistency Heatmap"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (uiState.selectedHeatmapTab) {
                            2 -> "${uiState.selectedYear} • ${uiState.totalYearActiveDays} Active Days (${uiState.totalYearCompletions} done)"
                            1 -> "Last 90 Days"
                            else -> "$monthName ${uiState.selectedYear}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary
                    )
                }

                if (selectedHabit != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = TertiaryContainer,
                        modifier = Modifier.clickable { onHabitSelect(null) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Reset", style = MaterialTheme.typography.labelSmall, color = OnTertiaryContainer)
                            Icon(Icons.Filled.Close, contentDescription = "Clear habit filter", tint = OnTertiaryContainer, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            // Tab Switcher (Month View vs 3 Months vs Year)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceContainer, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabLabels.forEachIndexed { index, label ->
                    val selected = uiState.selectedHeatmapTab == index
                    val isProTab = index > 0
                    val locked = isProTab && !isPro
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) PrimaryContainer else Color.Transparent,
                        modifier = Modifier.weight(1f).clickable { onTabSelect(index) }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (locked) {
                                Icon(Icons.Filled.Lock, null, tint = Color(0xFF673AB7), modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                color = when {
                                    selected -> OnPrimaryContainer
                                    locked -> Color(0xFF673AB7)
                                    else -> OnSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }

            // Month Pills (Only shown in Month tab to avoid duplicate navigation)
            if (uiState.selectedHeatmapTab == 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (m in 1..12) {
                        val isSelectedMonth = (uiState.selectedMonthValue == m)
                        val mShort = java.time.Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                        val isCur = (m == currentMonthNumber)
                        val isLocked = !isCur && !isPro

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelectedMonth) TertiaryContainer else SurfaceContainer,
                            modifier = Modifier.clickable { onMonthSelect(m) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isLocked) Icon(Icons.Filled.Lock, null, tint = Color(0xFF673AB7), modifier = Modifier.size(11.dp))
                                Text(
                                    text = mShort,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelectedMonth) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelectedMonth) OnTertiaryContainer else if (isCur) Primary else OnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Habit Filter Chips (Scrollable horizontal row for per-habit selection)
            if (uiState.habits.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val allSelected = uiState.selectedHabitId == null
                    FilterChip(
                        selected = allSelected,
                        onClick = { onHabitSelect(null) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Stars, null, modifier = Modifier.size(14.dp))
                                Text("All Habits", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryContainer,
                            selectedLabelColor = OnPrimaryContainer
                        )
                    )

                    uiState.habits.forEach { habitWithStatus ->
                        val h = habitWithStatus.habit
                        val isSelected = uiState.selectedHabitId == h.id
                        val icon = habitIconMap[h.iconName] ?: Icons.Filled.CheckCircle
                        FilterChip(
                            selected = isSelected,
                            onClick = { onHabitSelect(h.id) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(icon, null, modifier = Modifier.size(14.dp))
                                    Text(h.title, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TertiaryContainer,
                                selectedLabelColor = OnTertiaryContainer
                            )
                        )
                    }
                }
            }

            // Stats row (adapts for LeetCode Year view vs Month view)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.selectedHeatmapTab == 2) {
                    // LeetCode Year Stats
                    Surface(shape = RoundedCornerShape(14.dp), color = SurfaceContainer, modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("ACTIVE DAYS", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                            Text("${uiState.totalYearActiveDays}", style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), color = Tertiary)
                        }
                    }
                    Surface(shape = RoundedCornerShape(14.dp), color = SurfaceContainer, modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("TOTAL DONE", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                            Text("${uiState.totalYearCompletions}", style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), color = Primary)
                        }
                    }
                    Surface(shape = RoundedCornerShape(14.dp), color = SurfaceContainer, modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("LONGEST", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${uiState.longestStreak}", style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), color = OnSurface)
                                Text("d", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                            }
                        }
                    }
                } else {
                    // Month / 90-Day View Stats
                    Surface(shape = RoundedCornerShape(14.dp), color = SurfaceContainer, modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("CURRENT", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${uiState.currentStreak}", style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), color = Tertiary)
                                Text(" days", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceContainer,
                        modifier = Modifier.weight(1f).clickable(enabled = !isPro) { onTabSelect(1) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("LONGEST", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                                if (!isPro) Icon(Icons.Filled.Lock, null, tint = Color(0xFF673AB7), modifier = Modifier.size(11.dp))
                            }
                            if (isPro) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("${uiState.longestStreak}", style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), color = Primary)
                                    Text(" days", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
                                }
                            } else {
                                Text("Locked", style = MaterialTheme.typography.bodySmall, color = Color(0xFF673AB7), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Surface(shape = RoundedCornerShape(14.dp), color = SurfaceContainer, modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(monthShort.uppercase(), style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(uiState.monthlyCount.split("/")[0], style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), color = OnSurface)
                                Text(" days", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
                            }
                        }
                    }
                }
            }

            // === MAIN HEATMAP DISPLAY AREA ===
            if (uiState.selectedHeatmapTab == 2) {
                Surface(shape = RoundedCornerShape(24.dp), color = SurfaceContainerLowest) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Annual Overview • ${uiState.selectedYear}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Surface(shape = RoundedCornerShape(50), color = TertiaryContainer.copy(alpha = 0.3f)) {
                                Text(
                                    text = "${uiState.totalYearActiveDays} Days Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Tertiary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Selected day detail pill
                        selectedDayDetail?.let { day ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SurfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${day.dateString} (${day.monthName} ${day.dayOfMonth}): ${if (day.count > 0) "${day.count} completed" else "No completions"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (day.count > 0) Tertiary else OnSurfaceVariant
                                    )
                                    IconButton(onClick = { selectedDayDetail = null }, modifier = Modifier.size(16.dp)) {
                                        Icon(Icons.Filled.Close, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }

                        // LeetCode Grid Container (Horizontal scroll with 52-53 week columns)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Left day labels (Mon, Wed, Fri)
                            Column(
                                modifier = Modifier.padding(top = 18.dp, end = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, d ->
                                    Box(modifier = Modifier.size(12.dp), contentAlignment = Alignment.Center) {
                                        if (i % 2 == 0) {
                                            Text(
                                                text = d,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = OnSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Horizontally Scrollable 52-week columns with Month headers
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(yearScrollState),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                uiState.yearHeatmapWeeks.forEachIndexed { weekIdx, weekDays ->
                                    val firstDay = weekDays.firstOrNull()
                                    // Show month label if week contains 1st of a month or is the first week
                                    val hasFirstDayOfMonth = weekDays.any { it.dayOfMonth in 1..7 && it.dayOfMonth <= weekDays.size }
                                    val isNewMonth = (weekIdx == 0) || hasFirstDayOfMonth
                                    val monthHeader = if (isNewMonth) {
                                        weekDays.firstOrNull { it.dayOfMonth in 1..7 }?.monthName ?: firstDay?.monthName ?: ""
                                    } else ""

                                    Column(
                                        modifier = Modifier.width(13.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        // Month label above the week
                                        Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.CenterStart) {
                                            if (monthHeader.isNotEmpty()) {
                                                Text(
                                                    text = monthHeader,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    fontWeight = FontWeight.Bold,
                                                    color = Primary,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        // 7 day cells
                                        weekDays.forEach { day ->
                                            val cellColor = when {
                                                day.isFuture -> SurfaceContainerHighest.copy(alpha = 0.2f)
                                                day.level == 3 -> Tertiary
                                                day.level == 2 -> TertiaryContainer
                                                day.level == 1 -> Tertiary.copy(alpha = 0.45f)
                                                else -> SurfaceContainerHighest
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(RoundedCornerShape(2.5.dp))
                                                    .background(cellColor)
                                                    .then(
                                                        if (day.isToday) Modifier.border(1.dp, Primary, RoundedCornerShape(2.5.dp))
                                                        else Modifier
                                                    )
                                                    .clickable {
                                                        selectedDayDetail = day
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Heatmap Legend
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Less",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = OnSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                listOf(SurfaceContainerHighest, Tertiary.copy(alpha = 0.45f), TertiaryContainer, Tertiary).forEach { c ->
                                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(c))
                                }
                            }
                            Text(
                                text = "More",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
            } else if (uiState.selectedHeatmapTab == 1) {
                // === 90-DAY HEATMAP (Horizontal GitHub-style) ===
                Surface(shape = RoundedCornerShape(24.dp), color = SurfaceContainerLowest) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "90-Day Consistency",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurface
                            )
                            Surface(shape = RoundedCornerShape(50), color = TertiaryContainer.copy(alpha = 0.3f)) {
                                Text("${uiState.monthlyRate} Rate", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Tertiary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                        }

                        // Month pills in a row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val monthsList = listOf(
                                past90Start.plusMonths(0),
                                past90Start.plusMonths(1),
                                past90Start.plusMonths(2)
                            )
                            monthsList.forEach { m ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (m.monthValue == currentMonthNumber) PrimaryContainer.copy(alpha = 0.4f) else SurfaceContainer,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = m.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (m.monthValue == currentMonthNumber) Primary else OnSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Horizontal scrollable heatmap grid (like annual view)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Day-of-week labels column
                            Column(
                                modifier = Modifier.padding(top = 18.dp, end = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, d ->
                                    Box(modifier = Modifier.size(12.dp), contentAlignment = Alignment.Center) {
                                        if (i % 2 == 0) {
                                            Text(
                                                text = d,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = OnSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Horizontally scrollable week columns
                            val weeksList90 = uiState.heatmapDaysData.chunked(7)
                            val ninetyScrollState = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(ninetyScrollState),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                weeksList90.forEachIndexed { weekIdx, weekDays ->
                                    val firstDay = weekDays.firstOrNull()
                                    val hasFirstDayOfMonth = weekDays.any { it.dayOfMonth in 1..7 }
                                    val isNewMonth = (weekIdx == 0) || hasFirstDayOfMonth
                                    val monthHeader = if (isNewMonth) {
                                        weekDays.firstOrNull { it.dayOfMonth in 1..7 }?.monthName ?: firstDay?.monthName ?: ""
                                    } else ""

                                    Column(
                                        modifier = Modifier.width(13.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        // Month label above the week column
                                        Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.CenterStart) {
                                            if (monthHeader.isNotEmpty()) {
                                                Text(
                                                    text = monthHeader,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    fontWeight = FontWeight.Bold,
                                                    color = Primary,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }

                                        // 7 day cells in this week column
                                        weekDays.forEach { day ->
                                            val cellColor = when {
                                                day.isFuture -> SurfaceContainerHighest.copy(alpha = 0.2f)
                                                day.level == 3 -> Tertiary
                                                day.level == 2 -> TertiaryContainer
                                                day.level == 1 -> Tertiary.copy(alpha = 0.45f)
                                                else -> SurfaceContainerHighest
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(RoundedCornerShape(2.5.dp))
                                                    .background(cellColor)
                                                    .then(
                                                        if (day.isToday) Modifier.border(1.dp, Primary, RoundedCornerShape(2.5.dp))
                                                        else Modifier
                                                    )
                                                    .clickable {
                                                        selectedDayDetail = day
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Legend
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedHabit != null) "Not Done" else "0 done",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = OnSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (selectedHabit != null) {
                                    Box(Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(SurfaceContainerHighest))
                                    Box(Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(Tertiary))
                                } else {
                                    listOf(SurfaceContainerHighest, Tertiary.copy(alpha = 0.4f), TertiaryContainer, Tertiary).forEach { c ->
                                        Box(Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(c))
                                    }
                                }
                            }
                            Text(
                                text = if (selectedHabit != null) "Completed" else "3+ done",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // === MONTH VIEW CALENDAR HEATMAP ===
                Surface(shape = RoundedCornerShape(24.dp), color = SurfaceContainerLowest) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$monthName Consistency",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurface
                            )
                            Surface(shape = RoundedCornerShape(50), color = TertiaryContainer.copy(alpha = 0.3f)) {
                                Text("${uiState.monthlyRate} Rate", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Tertiary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                        }

                        // Day of Week Header: M, T, W, T, F, S, S
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                        // Dynamic weeks grid from heatmapDaysData
                        val weeksList = uiState.heatmapDaysData.chunked(7)
                        weeksList.forEach { weekDays ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                weekDays.forEach { day ->
                                    val cellColor = when {
                                        day.isFuture -> SurfaceContainerHighest.copy(alpha = 0.25f)
                                        !day.isCurrentMonth -> SurfaceContainerLowest.copy(alpha = 0.4f)
                                        day.level == 3 -> Tertiary
                                        day.level == 2 -> TertiaryContainer
                                        day.level == 1 -> Tertiary.copy(alpha = 0.4f)
                                        else -> SurfaceContainerHighest
                                    }

                                    val textColor = when {
                                        day.isFuture -> OnSurfaceVariant.copy(alpha = 0.25f)
                                        !day.isCurrentMonth -> OnSurfaceVariant.copy(alpha = 0.25f)
                                        day.level == 3 -> OnTertiary
                                        day.level == 2 -> OnTertiaryContainer
                                        day.level == 1 -> OnSurface
                                        else -> OnSurfaceVariant.copy(alpha = 0.75f)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(28.dp)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(cellColor)
                                            .then(
                                                if (day.isToday) Modifier.border(1.5.dp, Primary, RoundedCornerShape(5.dp))
                                                else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${day.dayOfMonth}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            fontWeight = if (day.isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }

                        // Legend
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedHabit != null) "Not Done" else "0 done",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = OnSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (selectedHabit != null) {
                                    Box(Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(SurfaceContainerHighest))
                                    Box(Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(Tertiary))
                                } else {
                                    listOf(SurfaceContainerHighest, Tertiary.copy(alpha = 0.4f), TertiaryContainer, Tertiary).forEach { c ->
                                        Box(Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(c))
                                    }
                                }
                            }
                            Text(
                                text = if (selectedHabit != null) "Completed" else "3+ done",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
