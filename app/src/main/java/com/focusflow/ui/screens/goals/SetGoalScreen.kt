package com.focusflow.ui.screens.goals

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusflow.coins.CoinLedgerService
import com.focusflow.data.db.FocusFlowDatabase
import com.focusflow.data.db.entity.Goal
import com.focusflow.notifications.AlarmScheduler
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetGoalScreen(
    database: FocusFlowDatabase,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var existingGoal by remember { mutableStateOf<Goal?>(null) }
    var isHoursGoal by remember { mutableStateOf(true) }
    var title by remember { mutableStateOf("") }
    var totalTargetStr by remember { mutableStateOf("50") }
    var dailyTargetStr by remember { mutableStateOf("2") }
    var reminderEnabled by remember { mutableStateOf(true) }
    var reminderTime by remember { mutableStateOf("09:00") }

    LaunchedEffect(Unit) {
        val goals = database.goalDao().getAllGoalsSync()
        val g = goals.firstOrNull()
        if (g != null) {
            existingGoal = g
            title = g.title
            isHoursGoal = g.goalType != "DAYS"
            totalTargetStr = if (isHoursGoal) g.targetHours.toInt().toString() else g.targetDays.toString()
            dailyTargetStr = g.dailyTarget.toInt().toString()
            reminderEnabled = g.reminderEnabled
            reminderTime = g.reminderTime
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existingGoal != null) "Edit Primary Goal" else "Set Primary Goal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (existingGoal != null) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    existingGoal?.let {
                                        database.goalDao().deleteGoal(it)
                                        AlarmScheduler.cancelGoalReminder(context, it.id)
                                    }
                                    Toast.makeText(context, "Goal deleted", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete Goal",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ─── Header Card ───
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.RocketLaunch,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Target Focus Goal",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Track your commitment with live daily check-ins and earn +10 🪙 coins on every log.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ─── Goal Type Selector (Cylindrical Pill Tabs) ───
            Text(
                text = "GOAL TRACKING TYPE",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val types = listOf(true to "⏱ Total Hours", false to "📅 Daily Days")
                    types.forEach { (isHours, label) ->
                        val selected = isHoursGoal == isHours
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .clickable { isHoursGoal = isHours },
                            shape = RoundedCornerShape(50),
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ─── Goal Title & Presets ───
            Text(
                text = "GOAL TITLE",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("e.g. Master Kotlin & Jetpack Compose") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Preset Quick Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf("Deep Work", "Coding", "Reading", "Fitness")
                presets.forEach { preset ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { title = preset }
                    ) {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // ─── Total Target & Daily Target ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isHoursGoal) "TOTAL HOURS" else "TOTAL DAYS",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = totalTargetStr,
                        onValueChange = { totalTargetStr = it.filter { ch -> ch.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isHoursGoal) "DAILY TARGET (H)" else "DAILY TARGET (D)",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = dailyTargetStr,
                        onValueChange = { dailyTargetStr = it.filter { ch -> ch.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Quick Target Increment Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val increments = if (isHoursGoal) listOf("20h", "50h", "100h", "200h") else listOf("14d", "30d", "60d", "90d")
                increments.forEach { chip ->
                    val num = chip.replace("[^0-9]".toRegex(), "")
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { totalTargetStr = num }
                    ) {
                        Text(
                            text = "+$chip",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // ─── Daily Reminder & Live Alarm ───
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Icon(
                                Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Daily Reminder Notification",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it }
                        )
                    }

                    if (reminderEnabled) {
                        OutlinedTextField(
                            value = reminderTime,
                            onValueChange = { reminderTime = it },
                            label = { Text("Reminder Time (HH:mm)") },
                            placeholder = { Text("09:00") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "An exact alarm will wake your device and remind you to log progress even if FocusFlow is closed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ─── Save Button (Cylindrical) ───
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "Please enter a goal title", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val totalTarget = totalTargetStr.toFloatOrNull() ?: 50f
                    val dailyTarget = dailyTargetStr.toFloatOrNull() ?: 2f

                    scope.launch {
                        val current = existingGoal
                        val goalId: Long
                        if (current != null) {
                            val updated = current.copy(
                                title = title.trim(),
                                goalType = if (isHoursGoal) "HOURS" else "DAYS",
                                targetHours = if (isHoursGoal) totalTarget else 0f,
                                targetDays = if (!isHoursGoal) totalTarget.toInt() else 0,
                                dailyTarget = dailyTarget,
                                reminderEnabled = reminderEnabled,
                                reminderTime = reminderTime
                            )
                            database.goalDao().updateGoal(updated)
                            goalId = current.id
                        } else {
                            val newGoal = Goal(
                                title = title.trim(),
                                goalType = if (isHoursGoal) "HOURS" else "DAYS",
                                targetHours = if (isHoursGoal) totalTarget else 0f,
                                loggedHours = 0f,
                                targetDays = if (!isHoursGoal) totalTarget.toInt() else 0,
                                loggedDays = 0,
                                dailyTarget = dailyTarget,
                                icon = "rocket_launch",
                                status = "on_track",
                                reminderEnabled = reminderEnabled,
                                reminderTime = reminderTime
                            )
                            goalId = database.goalDao().insertGoal(newGoal)
                            CoinLedgerService.earn(20L, "Created goal: ${title.trim()}", "create_goal_$goalId")
                        }

                        // Schedule live system alarm
                        if (reminderEnabled) {
                            AlarmScheduler.scheduleGoalReminder(
                                context = context,
                                goalId = goalId,
                                goalTitle = title.trim(),
                                dailyTarget = dailyTarget,
                                loggedHours = current?.loggedHours ?: 0f,
                                timeString = reminderTime
                            )
                        } else {
                            AlarmScheduler.cancelGoalReminder(context, goalId)
                        }

                        Toast.makeText(context, "Goal saved successfully! 🎯", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                },
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Check, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (existingGoal != null) "Save Changes" else "Set Primary Goal (+20 🪙)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

