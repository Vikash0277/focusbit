package com.focusflow.ui.screens.today

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focusflow.coins.CoinLedgerService
import com.focusflow.data.db.FocusFlowDatabase
import com.focusflow.data.db.entity.Commitment
import com.focusflow.data.db.entity.Goal
import com.focusflow.data.db.entity.Todo
import com.focusflow.ui.modals.PaywallModal
import com.focusflow.ui.theme.*

@Composable
fun TodayScreen(
    database: FocusFlowDatabase,
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory(database)),
    onOpenWallet: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val balance by CoinLedgerService.balance.collectAsState()
    val todayEarned by CoinLedgerService.todayEarned.collectAsState()
    val context = LocalContext.current

    var showGoalDialog by remember { mutableStateOf(false) }
    var showTodoDialog by remember { mutableStateOf(false) }

    var goalTitleInput by remember { mutableStateOf("") }
    var goalTypeInput by remember { mutableStateOf("HOURS") } // "HOURS" or "DAYS"
    var goalTargetHoursInput by remember { mutableStateOf("40") }
    var goalTargetDaysInput by remember { mutableStateOf("30") }
    var goalDailyTargetInput by remember { mutableStateOf("2") }
    var goalReminderEnabled by remember { mutableStateOf(true) }
    var goalReminderTime by remember { mutableStateOf("09:00") }

    var todoTitleInput by remember { mutableStateOf("") }
    var todoPriorityInput by remember { mutableStateOf("MEDIUM") }
    var todoDueTimeInput by remember { mutableStateOf("") }
    var todoCategoryInput by remember { mutableStateOf("Work") }

    // Goal Dialog
    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Set Primary Goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = goalTitleInput,
                        onValueChange = { goalTitleInput = it },
                        label = { Text("Goal Title") },
                        placeholder = { Text("e.g. Master Kotlin, Launch Product") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Goal type toggle
                    Text("Goal Type", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceContainerHighest)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("HOURS" to "⏱ Hours Goal", "DAYS" to "📅 Days Goal").forEach { (type, label) ->
                            val selected = goalTypeInput == type
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { goalTypeInput = type },
                                color = if (selected) Primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) OnPrimary else OnSurface
                                )
                            }
                        }
                    }

                    // Conditional input fields
                    if (goalTypeInput == "HOURS") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = goalTargetHoursInput,
                                onValueChange = { goalTargetHoursInput = it },
                                label = { Text("Total Target (h)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = goalDailyTargetInput,
                                onValueChange = { goalDailyTargetInput = it },
                                label = { Text("Daily (h)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = goalTargetDaysInput,
                                onValueChange = { goalTargetDaysInput = it },
                                label = { Text("Total Days") },
                                placeholder = { Text("e.g. 30") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = goalDailyTargetInput,
                                onValueChange = { goalDailyTargetInput = it },
                                label = { Text("Hrs/day") },
                                placeholder = { Text("1") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider(color = OutlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.NotificationsActive, null, tint = Primary, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Daily Reminder", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Get a nudge to stay on track", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = goalReminderEnabled,
                            onCheckedChange = { goalReminderEnabled = it }
                        )
                    }

                    if (goalReminderEnabled) {
                        OutlinedTextField(
                            value = goalReminderTime,
                            onValueChange = { goalReminderTime = it },
                            label = { Text("Reminder Time (HH:mm)") },
                            placeholder = { Text("09:00") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (goalTitleInput.isNotBlank()) {
                        val total = goalTargetHoursInput.toFloatOrNull() ?: 40f
                        val days = goalTargetDaysInput.toIntOrNull() ?: 30
                        val daily = goalDailyTargetInput.toFloatOrNull() ?: 2f
                        viewModel.addGoal(
                            title = goalTitleInput,
                            tag = "Primary",
                            goalType = goalTypeInput,
                            targetHours = if (goalTypeInput == "HOURS") total else 0f,
                            targetDays = if (goalTypeInput == "DAYS") days else 0,
                            dailyTarget = daily,
                            reminderEnabled = goalReminderEnabled,
                            reminderTime = goalReminderTime.ifBlank { "09:00" },
                            context = context
                        )
                        goalTitleInput = ""
                        goalTypeInput = "HOURS"
                        showGoalDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Todo Dialog
    if (showTodoDialog) {
        AlertDialog(
            onDismissRequest = { showTodoDialog = false },
            title = { Text("Add Daily Todo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = todoTitleInput,
                        onValueChange = { todoTitleInput = it },
                        label = { Text("Task Description") },
                        placeholder = { Text("e.g. Review pull request, Draft report") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = todoDueTimeInput,
                            onValueChange = { todoDueTimeInput = it },
                            label = { Text("Due Time (e.g. 14:00)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = todoCategoryInput,
                            onValueChange = { todoCategoryInput = it },
                            label = { Text("Category") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Priority", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("HIGH", "MEDIUM", "LOW").forEach { prio ->
                            FilterChip(
                                selected = todoPriorityInput == prio,
                                onClick = { todoPriorityInput = prio },
                                label = { Text(prio) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (todoTitleInput.isNotBlank()) {
                        viewModel.addTodo(
                            title = todoTitleInput,
                            priority = todoPriorityInput,
                            dueTime = todoDueTimeInput,
                            category = todoCategoryInput
                        )
                        todoTitleInput = ""
                        todoDueTimeInput = ""
                        showTodoDialog = false
                    }
                }) {
                    Text("Add Task (+2 🪙)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTodoDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Today's Economy Summary Card
            EconomySummaryCard(
                balance = balance,
                todayEarned = todayEarned,
                onOpenWallet = onOpenWallet
            )

            // View Selector Tabs: Goals & Schedule vs Daily Todos
            SegmentedViewTabs(
                selectedView = uiState.selectedView,
                todoCount = uiState.todos.count { !it.isCompleted },
                onSelectView = { viewModel.setSelectedView(it) }
            )

            // Dynamic Content based on selected tab
            if (uiState.selectedView == 0) {
                // VIEW 0: DAILY TODOS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Todos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val completed = uiState.todos.count { it.isCompleted }
                        Text(
                            text = "$completed / ${uiState.todos.size} completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showTodoDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Todo", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (uiState.todos.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceContainerHigh,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Checklist,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No Todos for Today",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Add daily tasks to keep momentum and earn FocusBits.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = { showTodoDialog = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Create First Todo")
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.todos.forEach { todo ->
                            TodoItemCard(
                                todo = todo,
                                onToggle = { viewModel.toggleTodo(todo) },
                                onDelete = { viewModel.deleteTodo(todo) }
                            )
                        }
                    }
                }
            } else {
                // VIEW 1: GOALS & SCHEDULE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Focus Pacing",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FilledTonalButton(
                        onClick = { showGoalDialog = true },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New Goal", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Today's Target Summary
                FocusSummaryCard(uiState)

                // Active Goal Card or Empty State
                val currentGoal = uiState.goal
                if (currentGoal != null) {
                    ActiveGoalCard(
                        goal = currentGoal,
                        onLogTarget = { viewModel.logDailyTarget() },
                        onUpdateReminder = { enabled, time ->
                            viewModel.updateGoalReminder(context, currentGoal.id, enabled, time)
                        },
                        onTestReminder = {
                            viewModel.testGoalReminder(context, currentGoal)
                        }
                    )
                } else {
                    NoGoalCard(onAddGoal = { showGoalDialog = true })
                }

                // Today's Commitments & Time Horizon (only show if they have values)
                if (uiState.commitments.isNotEmpty()) {
                    CommitmentsSection(uiState.commitments)
                    TimeCanvasSection(uiState.commitments)
                }
            }
        }

        // Toast Feedback
        AnimatedVisibility(
            visible = uiState.showToast,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Tertiary,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = OnTertiaryContainer)
                    Text(
                        uiState.toastMessage,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnTertiaryContainer
                    )
                }
            }
        }
    }

    if (uiState.showPaywall) {
        PaywallModal(onDismiss = { viewModel.dismissPaywall() })
    }
}

@Composable
private fun SegmentedViewTabs(
    selectedView: Int,
    todoCount: Int,
    onSelectView: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Tab 0: Daily Todos
            val tab0Selected = selectedView == 0
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectView(0) },
                shape = RoundedCornerShape(12.dp),
                color = if (tab0Selected) PrimaryContainer else Color.Transparent,
                shadowElevation = if (tab0Selected) 3.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Checklist,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (tab0Selected) OnPrimaryContainer else OnSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Daily Todos",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (tab0Selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (tab0Selected) OnPrimaryContainer else OnSurfaceVariant
                    )
                    if (todoCount > 0) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (tab0Selected) Primary else Tertiary,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$todoCount",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tab0Selected) OnPrimary else OnTertiary
                                )
                            }
                        }
                    }
                }
            }

            // Tab 1: Goals & Schedule
            val tab1Selected = selectedView == 1
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectView(1) },
                shape = RoundedCornerShape(12.dp),
                color = if (tab1Selected) PrimaryContainer else Color.Transparent,
                shadowElevation = if (tab1Selected) 3.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (tab1Selected) OnPrimaryContainer else OnSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Goals & Schedule",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (tab1Selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (tab1Selected) OnPrimaryContainer else OnSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EconomySummaryCard(
    balance: Long,
    todayEarned: Long,
    onOpenWallet: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpenWallet() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🪙", fontSize = 22.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$balance FocusBits",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Tertiary.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "+$todayEarned today",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Tertiary
                            )
                        }
                    }
                }
            }

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Open Wallet",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TodoItemCard(
    todo: Todo,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val (priorityColor, priorityBg) = when (todo.priority.uppercase()) {
        "HIGH" -> Color(0xFFE53935) to Color(0xFFE53935).copy(alpha = 0.15f)
        "MEDIUM" -> Color(0xFFFB8C00) to Color(0xFFFB8C00).copy(alpha = 0.15f)
        else -> Tertiary to Tertiary.copy(alpha = 0.15f)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (todo.isCompleted) SurfaceContainerLow else SurfaceContainerHigh,
        shadowElevation = if (todo.isCompleted) 0.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                    if (todo.isCompleted) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Completed",
                            tint = Tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            Icons.Outlined.RadioButtonUnchecked,
                            contentDescription = "Uncompleted",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column {
                    Text(
                        text = todo.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (todo.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                        textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = priorityBg
                        ) {
                            Text(
                                text = todo.priority.uppercase(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = priorityColor,
                                fontSize = 10.sp
                            )
                        }

                        if (todo.dueTime.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SurfaceContainerHighest
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(10.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        text = todo.dueTime,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        if (todo.category.isNotBlank()) {
                            Text(
                                text = "• ${todo.category}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }

                        Text(
                            text = "+2 🪙",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Tertiary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Delete Todo",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun FocusSummaryCard(uiState: TodayUiState) {
    val targetMinutes = if (uiState.todayPlannedMinutes > 0) {
        uiState.todayPlannedMinutes
    } else {
        (uiState.goal?.dailyTarget?.times(60)?.toInt() ?: 120).coerceAtLeast(60)
    }
    val progress = (uiState.todayLoggedMinutes.toFloat() / targetMinutes).coerceIn(0f, 1f)
    val loggedHours = uiState.todayLoggedMinutes / 60
    val loggedMins = uiState.todayLoggedMinutes % 60
    val plannedHours = targetMinutes / 60
    val plannedMins = targetMinutes % 60

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceContainerHigh,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("TODAY'S TARGET", style = MaterialTheme.typography.labelMedium, color = Tertiary, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${loggedHours}h ${loggedMins}m", style = MaterialTheme.typography.headlineLarge, color = OnSurface, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text("/ ${plannedHours}h ${plannedMins}m", style = MaterialTheme.typography.bodyLarge, color = OnSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = SurfaceContainerHighest,
                        strokeWidth = 6.dp,
                        strokeCap = StrokeCap.Round
                    )
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = Tertiary,
                        strokeWidth = 6.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Tertiary
                    )
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                color = Tertiary,
                trackColor = SurfaceContainerHighest,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun NoGoalCard(onAddGoal: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = SurfaceContainerHigh, shadowElevation = 3.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.Flag, null, tint = Primary, modifier = Modifier.size(36.dp))
            Text("No Active Goal", style = MaterialTheme.typography.titleMedium, color = OnSurface)
            Text("Set your primary focus goal to track pacing and milestones.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onAddGoal,
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Set Goal")
            }
        }
    }
}

@Composable
private fun ActiveGoalCard(
    goal: Goal,
    onLogTarget: () -> Unit,
    onUpdateReminder: (Boolean, String) -> Unit,
    onTestReminder: () -> Unit
) {
    val remaining = (goal.targetHours - goal.loggedHours).coerceAtLeast(0f)
    val percentComplete = if (goal.targetHours > 0) ((goal.loggedHours / goal.targetHours) * 100).toInt() else 0
    val pacingDays = if (goal.dailyTarget > 0) (remaining / goal.dailyTarget).toInt() else 0

    var showReminderDialog by remember { mutableStateOf(false) }
    var tempReminderEnabled by remember(goal.reminderEnabled) { mutableStateOf(goal.reminderEnabled) }
    var tempReminderTime by remember(goal.reminderTime) { mutableStateOf(goal.reminderTime) }

    if (showReminderDialog) {
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.NotificationsActive, null, tint = Primary)
                    Text("Goal Reminder Settings")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "Stay consistent by scheduling a daily check-in notification for \"${goal.title}\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Reminder", fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = tempReminderEnabled,
                            onCheckedChange = { tempReminderEnabled = it }
                        )
                    }

                    if (tempReminderEnabled) {
                        OutlinedTextField(
                            value = tempReminderTime,
                            onValueChange = { tempReminderTime = it },
                            label = { Text("Reminder Time (HH:mm)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Quick Times:", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("08:00", "09:00", "13:00", "19:00", "21:00").forEach { time ->
                                FilterChip(
                                    selected = tempReminderTime == time,
                                    onClick = { tempReminderTime = time },
                                    label = { Text(time, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onTestReminder()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TertiaryContainer, contentColor = OnTertiaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.NotificationImportant, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Send Test Notification Now", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateReminder(tempReminderEnabled, tempReminderTime.ifBlank { "09:00" })
                    showReminderDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(shape = RoundedCornerShape(16.dp), color = SurfaceContainerHigh, shadowElevation = 3.dp) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(50), color = TertiaryContainer.copy(alpha = 0.3f)) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(Tertiary))
                                Text("On Track", style = MaterialTheme.typography.labelSmall, color = Tertiary)
                            }
                        }
                        Text(goal.tag, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    }
                    Text(goal.title, style = MaterialTheme.typography.headlineSmall, color = OnSurface, fontWeight = FontWeight.SemiBold)
                }
                Surface(shape = CircleShape, color = SurfaceContainerHighest, modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Filled.RocketLaunch, null, tint = Primary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Progress Bar towards Goal
            val goalProgress = if (goal.goalType == "DAYS") {
                if (goal.targetDays > 0) (goal.loggedDays.toFloat() / goal.targetDays).coerceIn(0f, 1f) else 0f
            } else {
                if (goal.targetHours > 0) (goal.loggedHours / goal.targetHours).coerceIn(0f, 1f) else 0f
            }
            val displayPct = (goalProgress * 100).toInt()

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Overall Goal Progress", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    Text("$displayPct% achieved", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Primary)
                }
                LinearProgressIndicator(
                    progress = { goalProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                    color = Primary,
                    trackColor = SurfaceContainerHighest,
                    strokeCap = StrokeCap.Round
                )
            }

            // Stats grid
            Surface(shape = RoundedCornerShape(12.dp), color = SurfaceContainerLow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (goal.goalType == "DAYS") {
                        // Days-based stats
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("LOGGED", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                            Text("${goal.loggedDays}d", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = OnSurface)
                            Text("of ${goal.targetDays}d", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        }
                        VerticalDivider(modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp), color = OutlineVariant.copy(alpha = 0.3f))
                        val daysRemaining = (goal.targetDays - goal.loggedDays).coerceAtLeast(0)
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("REMAINING", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                            Text("${daysRemaining}d", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Primary)
                            Text("left", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        }
                        VerticalDivider(modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp), color = OutlineVariant.copy(alpha = 0.3f))
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DAILY", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                            Text("${goal.dailyTarget.toInt()}h", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Tertiary)
                            Text("daily target", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        }
                    } else {
                        // Hours-based stats
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("LOGGED", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                            Text("${goal.loggedHours.toInt()}h", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = OnSurface)
                            Text("of ${goal.targetHours.toInt()}h", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        }
                        VerticalDivider(modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp), color = OutlineVariant.copy(alpha = 0.3f))
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("REMAINING", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                            Text("${remaining.toInt()}h", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Primary)
                            Text("left", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        }
                        VerticalDivider(modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp), color = OutlineVariant.copy(alpha = 0.3f))
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PACING", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                            Text("${pacingDays}d", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Tertiary)
                            Text("${goal.dailyTarget.toInt()}h / day", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        }
                    }
                }
            }

            // Reminder banner row
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (goal.reminderEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                            contentDescription = null,
                            tint = if (goal.reminderEnabled) Primary else OnSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (goal.reminderEnabled) "Daily Reminder: ${goal.reminderTime}" else "Daily Reminder: Off",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (goal.reminderEnabled) OnSurface else OnSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (goal.reminderEnabled) {
                            IconButton(
                                onClick = onTestReminder,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.NotificationImportant,
                                    contentDescription = "Test Notification",
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { showReminderDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Edit", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Log action
            Button(
                onClick = onLogTarget,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary)
            ) {
                Icon(Icons.Filled.AddTask, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                val logLabel = if (goal.goalType == "DAYS")
                    "Log Today (+10 🪙)"
                else
                    "Log Daily Target (+10 🪙)"
                Text(logLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun CommitmentsSection(commitments: List<Commitment>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Today's Commitments", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = OnSurface)
            Text("${commitments.size} sessions", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
        }
        if (commitments.isEmpty()) {
            Surface(shape = RoundedCornerShape(12.dp), color = SurfaceContainer) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No focus commitments scheduled for today.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                }
            }
        } else {
            commitments.forEach { commitment ->
                CommitmentCard(commitment)
            }
        }
    }
}

@Composable
private fun CommitmentCard(commitment: Commitment) {
    val (bgColor, iconColor, statusIcon) = when (commitment.status) {
        "completed" -> Triple(Tertiary.copy(alpha = 0.2f), Tertiary, Icons.Filled.DoneAll)
        "active" -> Triple(Primary.copy(alpha = 0.2f), Primary, Icons.Filled.Bolt)
        else -> Triple(SecondaryContainer.copy(alpha = 0.2f), Secondary, Icons.Filled.Architecture)
    }

    Surface(shape = RoundedCornerShape(16.dp), color = SurfaceContainerHigh, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(shape = CircleShape, color = bgColor, modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(statusIcon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                    }
                }
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(commitment.title, style = MaterialTheme.typography.labelLarge, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Surface(shape = RoundedCornerShape(4.dp), color = if (commitment.status == "completed") Tertiary.copy(alpha = 0.2f) else SurfaceContainerHighest) {
                            Text(commitment.duration, style = MaterialTheme.typography.labelSmall, color = if (commitment.status == "completed") Tertiary else OnSurface, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Text("${commitment.startTime} - ${commitment.endTime} · ${commitment.label}", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                }
            }
            if (commitment.status == "completed") {
                Surface(shape = RoundedCornerShape(50), color = SurfaceContainerHighest) {
                    Text("Finished", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun TimeCanvasSection(commitments: List<Commitment>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.CalendarMonth, null, tint = Primary, modifier = Modifier.size(18.dp))
                Text("Time Horizon", style = MaterialTheme.typography.headlineSmall, color = OnSurface)
            }
        }

        Surface(shape = RoundedCornerShape(16.dp), color = SurfaceContainerLow, shadowElevation = 2.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (commitments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No scheduled time blocks today.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    }
                } else {
                    commitments.forEach { commitment ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(commitment.startTime, style = MaterialTheme.typography.labelSmall, color = Primary, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
                            Surface(shape = RoundedCornerShape(8.dp), color = SurfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(commitment.title, style = MaterialTheme.typography.labelLarge, color = OnSurface)
                                        Text("${commitment.startTime} – ${commitment.endTime} (${commitment.duration})", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
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
