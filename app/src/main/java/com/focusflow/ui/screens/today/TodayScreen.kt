package com.focusflow.ui.screens.today

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focusflow.coins.CoinLedgerService
import com.focusflow.data.db.FocusFlowDatabase
import com.focusflow.data.db.entity.Commitment
import com.focusflow.data.db.entity.Note
import com.focusflow.data.db.entity.Todo
import com.focusflow.ui.modals.PaywallModal
import com.focusflow.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TodayScreen(
    database: FocusFlowDatabase,
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory(database)),
    onOpenWallet: () -> Unit = {},
    onNavigateToAddTodo: () -> Unit = {},
    onNavigateToTodoDetail: (Long) -> Unit = {},
    onNavigateToAddNote: () -> Unit = {},
    onNavigateToNoteDetail: (Long) -> Unit = {},
    onNavigateToSetGoal: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val balance by CoinLedgerService.balance.collectAsState()
    val todayEarned by CoinLedgerService.todayEarned.collectAsState()
    val context = LocalContext.current

    // Modal dialog controls
    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var selectedDetailNote by remember { mutableStateOf<Note?>(null) }

    // Form inputs: Goal
    var goalTitleInput by remember { mutableStateOf("") }
    var goalTypeInput by remember { mutableStateOf("HOURS") }
    var goalTargetHoursInput by remember { mutableStateOf("40") }
    var goalTargetDaysInput by remember { mutableStateOf("30") }
    var goalDailyTargetInput by remember { mutableStateOf("2") }
    var goalReminderEnabled by remember { mutableStateOf(true) }
    var goalReminderTime by remember { mutableStateOf("09:00") }

    // Form inputs: Note
    var noteTitleInput by remember { mutableStateOf("") }
    var noteContentInput by remember { mutableStateOf("") }
    var noteEmojiInput by remember { mutableStateOf("✨") }
    var noteCategoryInput by remember { mutableStateOf("High Priority") }
    var noteColorTagInput by remember { mutableStateOf("#EC4899") }

    // Form inputs: Task/Todo
    var todoTitleInput by remember { mutableStateOf("") }
    var todoPriorityInput by remember { mutableStateOf("MEDIUM") }
    var todoDueTimeInput by remember { mutableStateOf("04:00 PM") }
    var todoCategoryInput by remember { mutableStateOf("Work") }

    // Form inputs: Calendar Event
    var calTitleInput by remember { mutableStateOf("") }
    var calStartTimeInput by remember { mutableStateOf("10:00 AM") }
    var calEndTimeInput by remember { mutableStateOf("11:30 AM") }
    var calDurationInput by remember { mutableStateOf("90m") }
    var calLabelInput by remember { mutableStateOf("Work") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. SEGMENTED NAVIGATION TABS (Home, Daily Notes, My Calendar)
            ScreenSegmentedTabs(
                selectedTab = uiState.selectedView,
                noteCount = uiState.notes.size,
                onTabSelect = { viewModel.setSelectedView(it) }
            )

            // 2. MAIN DYNAMIC VIEW
            when (uiState.selectedView) {
                0 -> {
                    // SCREEN 1: HOME / DASHBOARD OVERVIEW
                    HomeScreenContent(
                        uiState = uiState,
                        onOpenCreateNote = onNavigateToAddNote,
                        onOpenCreateTask = onNavigateToAddTodo,
                        onOpenSetGoal = onNavigateToSetGoal,
                        onNavigateToNotes = { viewModel.setSelectedView(1) },
                        onNavigateToCalendar = { viewModel.setSelectedView(2) },
                        onNoteClick = { note -> onNavigateToNoteDetail(note.id) },
                        onTodoClick = { todo -> onNavigateToTodoDetail(todo.id) },
                        onToggleTodo = { todo -> viewModel.toggleTodo(todo) },
                        onDeleteTodo = { todo -> viewModel.deleteTodo(todo) },
                        onSelectDate = { dateStr -> viewModel.setSelectedCalendarDate(dateStr) },
                        onLogGoalTarget = { viewModel.logDailyTarget() },
                        onUpdateGoalReminder = { enabled, time ->
                            uiState.goal?.let { g -> viewModel.updateGoalReminder(context, g.id, enabled, time) }
                        },
                        onTestGoalReminder = {
                            uiState.goal?.let { g -> viewModel.testGoalReminder(context, g) }
                        }
                    )
                }
                1 -> {
                    // SCREEN 2: DAILY NOTES
                    DailyNotesContent(
                        notes = uiState.notes,
                        selectedTag = uiState.selectedFilterTag,
                        onSelectTag = { viewModel.setSelectedCategory(it) },
                        onNoteClick = { note -> onNavigateToNoteDetail(note.id) },
                        onOpenCreateNote = onNavigateToAddNote
                    )
                }
                2 -> {
                    // SCREEN 3: MY CALENDAR
                    MyCalendarContent(
                        commitments = uiState.commitments,
                        selectedDate = uiState.selectedCalendarDate,
                        onSelectDate = { viewModel.setSelectedCalendarDate(it) },
                        onOpenAddEvent = { showAddScheduleDialog = true }
                    )
                }
            }
        }

        // Floating Action Button for Notes or Calendar view
        if (uiState.selectedView == 1) {
            FloatingActionButton(
                onClick = onNavigateToAddNote,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 100.dp)
                    .size(56.dp)
                    .shadow(10.dp, CircleShape)
            ) {
                Icon(Icons.Filled.EditNote, contentDescription = "Add Note", modifier = Modifier.size(28.dp))
            }
        } else if (uiState.selectedView == 2) {
            FloatingActionButton(
                onClick = { showAddScheduleDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 100.dp)
                    .size(56.dp)
                    .shadow(10.dp, CircleShape)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Event", modifier = Modifier.size(28.dp))
            }
        }

        // Toast feedback
        AnimatedVisibility(
            visible = uiState.showToast,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF1E1B2E),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFFA78BFA))
                    Text(
                        uiState.toastMessage,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    // ==========================================
    // DIALOG 1: CREATE NEW NOTE
    // ==========================================
    if (showCreateNoteDialog) {
        Dialog(onDismissRequest = { showCreateNoteDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF3EAFF),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(noteEmojiInput, fontSize = 20.sp)
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Create New Note",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { showCreateNoteDialog = false }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }

                    // Emoji selector row
                    Text("Select Emoji", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val emojis = listOf("✨", "🧠", "📝", "💡", "🎯", "📚", "🎨", "🚀")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        emojis.forEach { emoji ->
                            val isSelected = noteEmojiInput == emoji
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFF3EAFF) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { noteEmojiInput = emoji }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(emoji, fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    // Title
                    OutlinedTextField(
                        value = noteTitleInput,
                        onValueChange = { noteTitleInput = it },
                        label = { Text("Note Title") },
                        placeholder = { Text("e.g. Sketch One UIScreen") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Content
                    OutlinedTextField(
                        value = noteContentInput,
                        onValueChange = { noteContentInput = it },
                        label = { Text("Note Body") },
                        placeholder = { Text("Write your notes, ideas or requirements here...") },
                        minLines = 4,
                        maxLines = 7,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Category Selection
                    Text("Category / Priority", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val categories = listOf(
                        "High Priority" to "#EC4899",
                        "Productive" to "#8B5CF6",
                        "Ideas" to "#F59E0B",
                        "Work" to "#06B6D4"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { (cat, hex) ->
                            val isSelected = noteCategoryInput == cat
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) Color(android.graphics.Color.parseColor(hex)).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (isSelected) BorderStroke(1.5.dp, Color(android.graphics.Color.parseColor(hex))) else null,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        noteCategoryInput = cat
                                        noteColorTagInput = hex
                                    }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(android.graphics.Color.parseColor(hex)) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Save Button
                    Button(
                        onClick = {
                            if (noteTitleInput.isNotBlank()) {
                                viewModel.addNote(
                                    title = noteTitleInput,
                                    content = noteContentInput,
                                    emoji = noteEmojiInput,
                                    category = noteCategoryInput,
                                    colorTag = noteColorTagInput
                                )
                                noteTitleInput = ""
                                noteContentInput = ""
                                showCreateNoteDialog = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save Note (+5 🪙)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG 2: NOTE DETAILS MODAL
    // ==========================================
    selectedDetailNote?.let { note ->
        Dialog(onDismissRequest = { selectedDetailNote = null }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header row with Emoji and Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF3EAFF),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(note.emoji, fontSize = 26.sp)
                            }
                        }
                        IconButton(onClick = { selectedDetailNote = null }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }

                    // Title
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Metadata row: Date pill & Category pill
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFF3EAFF)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.CalendarMonth, null, tint = Color(0xFF6366F1), modifier = Modifier.size(13.dp))
                                Text(note.date, style = MaterialTheme.typography.labelSmall, color = Color(0xFF4338CA), fontWeight = FontWeight.SemiBold)
                            }
                        }

                        val tagColor = try {
                            Color(android.graphics.Color.parseColor(note.colorTag))
                        } catch (e: Exception) {
                            Color(0xFF8B5CF6)
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = tagColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = note.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = tagColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Content Box
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (note.content.isNotBlank()) note.content else "No description provided.",
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    // Actions: Delete & Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteNote(note)
                                selectedDetailNote = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Delete")
                        }

                        Button(
                            onClick = { selectedDetailNote = null },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG 3: CREATE NEW TASK / TODO
    // ==========================================
    if (showCreateTaskDialog) {
        Dialog(onDismissRequest = { showCreateTaskDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFEF3C7),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.TaskAlt, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Create New Task",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { showCreateTaskDialog = false }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }

                    OutlinedTextField(
                        value = todoTitleInput,
                        onValueChange = { todoTitleInput = it },
                        label = { Text("Task Description") },
                        placeholder = { Text("e.g. Sketch One UIScreen for FocusFlow") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = todoDueTimeInput,
                            onValueChange = { todoDueTimeInput = it },
                            label = { Text("Due Time") },
                            placeholder = { Text("04:00 PM") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = todoCategoryInput,
                            onValueChange = { todoCategoryInput = it },
                            label = { Text("Category") },
                            placeholder = { Text("Work") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Priority Level", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("HIGH" to Color(0xFFEF4444), "MEDIUM" to Color(0xFFF59E0B), "LOW" to Color(0xFF10B981)).forEach { (prio, col) ->
                            val isSelected = todoPriorityInput == prio
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) col.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (isSelected) BorderStroke(1.5.dp, col) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .clickable { todoPriorityInput = prio }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text(
                                        prio,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) col else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (todoTitleInput.isNotBlank()) {
                                viewModel.addTodo(
                                    title = todoTitleInput,
                                    priority = todoPriorityInput,
                                    dueTime = todoDueTimeInput,
                                    category = todoCategoryInput
                                )
                                todoTitleInput = ""
                                showCreateTaskDialog = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add Task (+2 🪙)", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG 4: ADD CALENDAR SCHEDULE EVENT
    // ==========================================
    if (showAddScheduleDialog) {
        Dialog(onDismissRequest = { showAddScheduleDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Add Schedule Event",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showAddScheduleDialog = false }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }

                    OutlinedTextField(
                        value = calTitleInput,
                        onValueChange = { calTitleInput = it },
                        label = { Text("Event Title") },
                        placeholder = { Text("e.g. Schedule Client Meeting") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = calStartTimeInput,
                            onValueChange = { calStartTimeInput = it },
                            label = { Text("Start Time") },
                            placeholder = { Text("11:30 AM") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = calEndTimeInput,
                            onValueChange = { calEndTimeInput = it },
                            label = { Text("End Time") },
                            placeholder = { Text("01:00 PM") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = calLabelInput,
                        onValueChange = { calLabelInput = it },
                        label = { Text("Category / Tag") },
                        placeholder = { Text("Work / Health / Design") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (calTitleInput.isNotBlank()) {
                                viewModel.addCommitment(
                                    title = calTitleInput,
                                    startTime = calStartTimeInput,
                                    endTime = calEndTimeInput,
                                    duration = calDurationInput,
                                    label = calLabelInput
                                )
                                calTitleInput = ""
                                showAddScheduleDialog = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Add to Schedule", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG 5: SET PRIMARY GOAL
    // ==========================================
    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.RocketLaunch, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Set Primary Goal", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = goalTitleInput,
                        onValueChange = { goalTitleInput = it },
                        label = { Text("Goal Title") },
                        placeholder = { Text("e.g. Master Kotlin, Launch Product") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Goal Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("HOURS" to "⏱ Hours Goal", "DAYS" to "📅 Days Goal").forEach { (type, label) ->
                            val selected = goalTypeInput == type
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { goalTypeInput = type },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (goalTypeInput == "HOURS") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = goalTargetHoursInput,
                                onValueChange = { goalTargetHoursInput = it },
                                label = { Text("Total Hours") },
                                placeholder = { Text("40") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = goalDailyTargetInput,
                                onValueChange = { goalDailyTargetInput = it },
                                label = { Text("Daily Hours") },
                                placeholder = { Text("2") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = goalTargetDaysInput,
                                onValueChange = { goalTargetDaysInput = it },
                                label = { Text("Total Days") },
                                placeholder = { Text("30") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = goalDailyTargetInput,
                                onValueChange = { goalDailyTargetInput = it },
                                label = { Text("Daily Days") },
                                placeholder = { Text("1") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Daily Reminder", style = MaterialTheme.typography.bodyMedium)
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
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (goalTitleInput.isNotBlank()) {
                            val targetHours = goalTargetHoursInput.toFloatOrNull() ?: 40f
                            val targetDays = goalTargetDaysInput.toIntOrNull() ?: 30
                            val daily = goalDailyTargetInput.toFloatOrNull() ?: 2f
                            viewModel.addGoal(
                                title = goalTitleInput,
                                tag = "Primary",
                                goalType = goalTypeInput,
                                targetHours = targetHours,
                                targetDays = targetDays,
                                dailyTarget = daily,
                                reminderEnabled = goalReminderEnabled,
                                reminderTime = goalReminderTime.ifBlank { "09:00" },
                                context = context
                            )
                            showGoalDialog = false
                            goalTitleInput = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Goal (+10 🪙)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showPaywall) {
        PaywallModal(onDismiss = { viewModel.dismissPaywall() })
    }
}

// ==========================================
// SEGMENTED TABS (Home, Daily Notes, My Calendar) - CYLINDRICAL PILL FORM
// ==========================================
@Composable
private fun ScreenSegmentedTabs(
    selectedTab: Int,
    noteCount: Int,
    onTabSelect: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf(
                Triple(0, "Home", Icons.Filled.Home),
                Triple(1, "Daily Notes", Icons.AutoMirrored.Filled.MenuBook),
                Triple(2, "My Calendar", Icons.Filled.CalendarMonth)
            )

            tabs.forEach { (index, title, icon) ->
                val isSelected = selectedTab == index
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .clickable { onTabSelect(index) },
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shadowElevation = if (isSelected) 3.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (index == 1 && noteCount > 0) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = if (noteCount > 99) "99+" else "$noteCount",
                                        fontSize = 10.sp,
                                        lineHeight = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 1: HOME VIEW CONTENT
// ==========================================
@Composable
private fun HomeScreenContent(
    uiState: TodayUiState,
    onOpenCreateNote: () -> Unit,
    onOpenCreateTask: () -> Unit,
    onOpenSetGoal: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onTodoClick: (Todo) -> Unit,
    onToggleTodo: (Todo) -> Unit,
    onDeleteTodo: (Todo) -> Unit,
    onSelectDate: (String) -> Unit,
    onLogGoalTarget: () -> Unit,
    onUpdateGoalReminder: (Boolean, String) -> Unit,
    onTestGoalReminder: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // App Title & Subtitle ("My FocusBit", "Today <formatted date>")
        Column {
            Text(
                text = "My FocusBit",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            val todayStr = try {
                val now = LocalDate.now()
                "Today ${now.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${now.dayOfMonth}, ${now.year}"
            } catch (e: Exception) {
                "Today"
            }
            Text(
                text = todayStr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Hero Action Cards: Create New Note, Create New Task, Set Goal in the same row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Create New Note (Lavender Gradient)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(138.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onOpenCreateNote() },
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 5.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AppColors.primaryGradientStart, AppColors.primaryGradientEnd)
                            )
                        )
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Add, contentDescription = "Add Note", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        Column {
                            Text(
                                text = "Create",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Normal
                            )
                            Text(
                                text = "New Note",
                                fontSize = 16.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Icon(
                        Icons.Filled.EditNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .size(56.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 8.dp, y = 8.dp)
                    )
                }
            }

            // 2. Create New Task (Amber/Golden Gradient)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(138.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onOpenCreateTask() },
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 5.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AppColors.secondaryGradientStart, AppColors.secondaryGradientEnd)
                            )
                        )
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Add, contentDescription = "Add Task", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        Column {
                            Text(
                                text = "Create",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Normal
                            )
                            Text(
                                text = "New Task",
                                fontSize = 16.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Icon(
                        Icons.Filled.TaskAlt,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .size(56.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 8.dp, y = 8.dp)
                    )
                }
            }

            // 3. Set Primary Goal (Emerald/Teal Gradient)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(138.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onOpenSetGoal() },
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 5.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF0D9488), Color(0xFF10B981))
                            )
                        )
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.RocketLaunch, contentDescription = "Set Goal", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }

                        Column {
                            Text(
                                text = if (uiState.goal != null) "Active" else "Target",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Normal
                            )
                            Text(
                                text = if (uiState.goal != null) "Edit Goal" else "Set Goal",
                                fontSize = 16.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Icon(
                        Icons.Filled.Flag,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .size(56.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 8.dp, y = 8.dp)
                    )
                }
            }
        }

        // Active Goal Card (if a goal is configured)
        if (uiState.goal != null) {
            ActiveGoalCard(
                goal = uiState.goal,
                onLogTarget = onLogGoalTarget,
                onEditGoal = onOpenSetGoal,
                onUpdateReminder = onUpdateGoalReminder,
                onTestReminder = onTestGoalReminder
            )
        }

        // "Complete Your Daily Focus" Progress Bar Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Complete Your Daily Focus",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${uiState.progressPercent}% Completed Today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Vibrant Gradient Progress Bar Capsule
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = (uiState.progressPercent / 100f).coerceIn(0.15f, 1f))
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFF59E0B))
                                )
                            )
                    )
                }
            }
        }

        // Actions Grid: 4 Rounded Pastel Cards (Notebook, Tasks, Audio/Focus, Calendar)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Notebook -> opens Daily Notes tab
                ActionPastelCard(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    label = "Notebook",
                    bgColor = Color(0xFFFDF2F8),
                    iconColor = Color(0xFFEC4899),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToNotes
                )

                // 2. Tasks -> opens Create Task modal
                ActionPastelCard(
                    icon = Icons.Filled.TaskAlt,
                    label = "Tasks",
                    bgColor = Color(0xFFF3E8FF),
                    iconColor = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenCreateTask
                )

                // 3. Audio/Focus -> opens Note with Ideas
                ActionPastelCard(
                    icon = Icons.Filled.Mic,
                    label = "Audio",
                    bgColor = Color(0xFFE0F2FE),
                    iconColor = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f),
                    onClick = onOpenCreateNote
                )

                // 4. Calendar -> opens My Calendar tab
                ActionPastelCard(
                    icon = Icons.Filled.CalendarMonth,
                    label = "Calendar",
                    bgColor = Color(0xFFFEF3C7),
                    iconColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToCalendar
                )
            }
        }

        // Recent Notes Preview Section
        if (uiState.notes.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onNavigateToNotes) {
                    Text("See All", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                uiState.notes.take(2).forEach { note ->
                    NoteCardItem(note = note, onClick = { onNoteClick(note) })
                }
            }
        }

        // Date Picker Strip: Full Month 1..31 for Filtering Tasks
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Schedule & Tasks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val currentMonthLabel = try {
                    val dateObj = if (uiState.selectedCalendarDate.isNotBlank()) LocalDate.parse(uiState.selectedCalendarDate) else LocalDate.now()
                    "${dateObj.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${dateObj.year}"
                } catch (e: Exception) {
                    ""
                }
                Text(
                    text = currentMonthLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalMonthStrip(
                selectedDate = uiState.selectedCalendarDate,
                onSelectDate = onSelectDate
            )
        }

        // Filtered Tasks Section
        val selectedDateObj = try {
            if (uiState.selectedCalendarDate.isNotBlank()) LocalDate.parse(uiState.selectedCalendarDate) else LocalDate.now()
        } catch (e: Exception) {
            LocalDate.now()
        }
        val isDateToday = selectedDateObj == LocalDate.now()
        val tasksHeaderTitle = if (isDateToday) "Today's Tasks" else "Tasks for ${selectedDateObj.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${selectedDateObj.dayOfMonth}"

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$tasksHeaderTitle (${uiState.filteredTodos.count { !it.isCompleted }} Remaining)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            FilledTonalButton(
                onClick = onOpenCreateTask,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add", style = MaterialTheme.typography.labelSmall)
            }
        }

        if (uiState.filteredTodos.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Checklist, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    Text(
                        if (isDateToday) "No tasks for today" else "No tasks on ${selectedDateObj.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${selectedDateObj.dayOfMonth}",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Tap '+ Add' above to schedule a task for this date.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                uiState.filteredTodos.forEach { todo ->
                    TodoItemRow(
                        todo = todo,
                        onToggle = { onToggleTodo(todo) },
                        onClick = { onTodoClick(todo) },
                        onDelete = { onDeleteTodo(todo) }
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 2: DAILY NOTES CONTENT
// ==========================================
@Composable
private fun DailyNotesContent(
    notes: List<Note>,
    selectedTag: String,
    onSelectTag: (String) -> Unit,
    onNoteClick: (Note) -> Unit,
    onTodoClick: (Todo) -> Unit = {},
    onOpenCreateNote: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // Browse Categories Section
        Text(
            text = "Browse",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val browseFilters = listOf(
                Triple("Shortcut", Icons.Filled.Star, Color(0xFFF3E8FF) to Color(0xFF8B5CF6)),
                Triple("Tags", Icons.Filled.LocalOffer, Color(0xFFE0F2FE) to Color(0xFF0284C7)),
                Triple("Recent", Icons.Filled.AccessTime, Color(0xFFFEF3C7) to Color(0xFFF59E0B)),
                Triple("Shared", Icons.Filled.Groups, Color(0xFFFDF2F8) to Color(0xFFEC4899))
            )

            browseFilters.forEach { (label, icon, colors) ->
                val (bgCol, iconCol) = colors
                val isSelected = selectedTag == label
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (isSelected) iconCol.copy(alpha = 0.2f) else bgCol,
                    border = if (isSelected) BorderStroke(1.5.dp, iconCol) else null,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onSelectTag(if (isSelected) "All" else label) }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(icon, null, tint = iconCol, modifier = Modifier.size(20.dp))
                            }
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Daily Notes List Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Daily Notes",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${notes.size} Notes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { onSelectTag("All") }) {
                Text("See All", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        // Notes List
        if (notes.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
                    Text("No notes found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Capture your first note, idea or meeting takeaway.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onOpenCreateNote,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Create New Note")
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                notes.forEach { note ->
                    NoteCardItem(note = note, onClick = { onNoteClick(note) })
                }
            }
        }
    }
}

// ==========================================
// SCREEN 3: MY CALENDAR CONTENT
// ==========================================
@Composable
private fun MyCalendarContent(
    commitments: List<Commitment>,
    selectedDate: String,
    onSelectDate: (String) -> Unit,
    onOpenAddEvent: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // Calendar Title & Subtitle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Calendar",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val formattedHeader = try {
                    val dateObj = if (selectedDate.isNotBlank()) LocalDate.parse(selectedDate) else LocalDate.now()
                    "${dateObj.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${dateObj.dayOfMonth}, ${dateObj.year}"
                } catch (e: Exception) {
                    "Today"
                }
                Text(
                    text = formattedHeader,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Horizontal Month Date Picker Strip (1..31)
        HorizontalMonthStrip(
            selectedDate = selectedDate,
            onSelectDate = onSelectDate
        )

        // Schedule Timeline Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Schedule Timeline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            FilledTonalButton(
                onClick = onOpenAddEvent,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Event", style = MaterialTheme.typography.labelSmall)
            }
        }

        // Timeline Items
        if (commitments.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                    Text("No events scheduled for this day", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Tap 'Add Event' above to schedule focus time or meetings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                commitments.forEachIndexed { index, commitment ->
                    // Insert "Break 1hr" divider between items if there are multiple
                    if (index == 2) {
                        BreakTimeDivider(label = "Break 1hr: 01:00 PM - 02:00 PM")
                    }

                    val stripeColor = when (index % 3) {
                        0 -> Color(0xFFEC4899) // Rose
                        1 -> Color(0xFF06B6D4) // Cyan
                        else -> Color(0xFF8B5CF6) // Violet
                    }

                    ScheduleTimelineRow(
                        commitment = commitment,
                        stripeColor = stripeColor
                    )
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: HORIZONTAL MONTH STRIP (1..31)
// ==========================================
@Composable
private fun HorizontalMonthStrip(
    selectedDate: String,
    onSelectDate: (String) -> Unit
) {
    val today = remember { LocalDate.now() }
    val yearMonth = remember(today) { java.time.YearMonth.from(today) }
    val daysInMonth = remember(yearMonth) { yearMonth.lengthOfMonth() }
    val allDays = remember(daysInMonth) { (1..daysInMonth).map { day -> yearMonth.atDay(day) } }

    val listState = rememberLazyListState()

    // Auto-scroll so current day is visible on launch
    LaunchedEffect(Unit) {
        val targetIndex = (today.dayOfMonth - 3).coerceAtLeast(0)
        listState.scrollToItem(targetIndex)
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
    ) {
        items(allDays) { date ->
            val dateStr = date.toString()
            val isSelected = (selectedDate == dateStr) || (selectedDate.isBlank() && date == today)
            val isToday = date == today
            val dayInitial = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(3)
            val dayNumber = date.dayOfMonth.toString()

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (isSelected) Color(0xFFF3EAFF) else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier
                    .width(54.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onSelectDate(dateStr) }
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = dayInitial,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    // Glowing Circle for selected day - Perfectly Centered
                    if (isSelected) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Transparent,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFFF8A65), Color(0xFFEC4899), MaterialTheme.colorScheme.primary)
                                    )
                                )
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = dayNumber,
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = if (isToday) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else null,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = dayNumber,
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: ACTIVE GOAL CARD
// ==========================================
@Composable
private fun ActiveGoalCard(
    goal: com.focusflow.data.db.entity.Goal,
    onLogTarget: () -> Unit,
    onEditGoal: () -> Unit = {},
    onUpdateReminder: (Boolean, String) -> Unit,
    onTestReminder: () -> Unit
) {
    val isHours = goal.goalType == "HOURS"
    val logged = if (isHours) goal.loggedHours else goal.loggedDays.toFloat()
    val target = if (isHours) goal.targetHours else goal.targetDays.toFloat()
    val progress = if (target > 0) (logged / target).coerceIn(0f, 1f) else 0f
    val percent = (progress * 100).toInt()
    val remaining = (target - logged).coerceAtLeast(0f)
    val pacingDays = if (goal.dailyTarget > 0) (remaining / goal.dailyTarget).toInt() else 0

    var showReminderSettings by remember { mutableStateOf(false) }
    var tempReminderEnabled by remember(goal.reminderEnabled) { mutableStateOf(goal.reminderEnabled) }
    var tempReminderTime by remember(goal.reminderTime) { mutableStateOf(goal.reminderTime) }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Icon + Title + Edit Goal + Reminder Settings Button
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
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.RocketLaunch,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isHours) "⏱ Hours Goal • ${goal.dailyTarget.toInt()}h daily" else "📅 Days Goal • ${goal.dailyTarget.toInt()}d daily",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEditGoal,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Edit Goal",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = { showReminderSettings = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (goal.reminderEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                            contentDescription = "Reminder Settings",
                            tint = if (goal.reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Progress Bar & Counts
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHours) "${logged.toInt()} of ${target.toInt()} hours" else "${logged.toInt()} of ${target.toInt()} days",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round
                )
            }

            // Pacing & Log Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 ~${pacingDays} days remaining",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onLogTarget,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Log Today (+10 🪙)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showReminderSettings) {
        AlertDialog(
            onDismissRequest = { showReminderSettings = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Goal Reminder Settings", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Stay on track with a daily notification for \"${goal.title}\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Daily Reminder", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = tempReminderEnabled,
                            onCheckedChange = { tempReminderEnabled = it }
                        )
                    }

                    if (tempReminderEnabled) {
                        OutlinedTextField(
                            value = tempReminderTime,
                            onValueChange = { tempReminderTime = it },
                            label = { Text("Daily Time (HH:mm)") },
                            placeholder = { Text("09:00") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedButton(
                        onClick = onTestReminder,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Filled.Notifications, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Send Test Reminder", style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateReminder(tempReminderEnabled, tempReminderTime)
                        showReminderSettings = false
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderSettings = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// COMPONENT: SCHEDULE TIMELINE ROW
// ==========================================
@Composable
private fun ScheduleTimelineRow(
    commitment: Commitment,
    stripeColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left Column: Time label
        Text(
            text = commitment.startTime,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(62.dp)
                .padding(top = 14.dp)
        )

        // Right Column: Event Card with vertical colored accent stripe
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 2.dp,
            modifier = Modifier.weight(1f)
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Vertical colored accent bar
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(50))
                            .background(stripeColor)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = commitment.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Time span pill
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = stripeColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${commitment.startTime} - ${commitment.endTime}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = stripeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Attendee avatar group simulation
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-6).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val avatarEmojis = listOf("👩‍💼", "👨‍💻", "🎨")
                    avatarEmojis.forEach { emo ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emo, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: BREAK TIME DIVIDER
// ==========================================
@Composable
private fun BreakTimeDivider(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFFF3E8FF),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Filled.AccessTime, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        )
    }
}

// ==========================================
// COMPONENT: NOTE CARD ITEM
// ==========================================
@Composable
private fun NoteCardItem(
    note: Note,
    onClick: () -> Unit
) {
    val tagColor = try {
        Color(android.graphics.Color.parseColor(note.colorTag))
    } catch (e: Exception) {
        Color(0xFF8B5CF6)
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Emoji + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(note.emoji, fontSize = 20.sp)
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Body excerpt
            if (note.content.isNotBlank()) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.sp
                )
            }

            // Bottom Row: Date Pill, Category Pill, Avatar cluster
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFF3EAFF)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Filled.CalendarMonth, null, tint = Color(0xFF6366F1), modifier = Modifier.size(11.dp))
                            Text(
                                text = note.date,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4338CA),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Category pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = tagColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = note.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = tagColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Small avatar cluster on right matching screenshot
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-6).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("🧑‍💻", "👩‍🎨", "🚀").forEach { iconStr ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(iconStr, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: TODO ITEM ROW
// ==========================================
@Composable
private fun TodoItemRow(
    todo: Todo,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit = onToggle
) {
    val (priorityColor, priorityBg) = when (todo.priority.uppercase()) {
        "HIGH" -> Color(0xFFEF4444) to Color(0xFFEF4444).copy(alpha = 0.15f)
        "MEDIUM" -> Color(0xFFF59E0B) to Color(0xFFF59E0B).copy(alpha = 0.15f)
        else -> Color(0xFF10B981) to Color(0xFF10B981).copy(alpha = 0.15f)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (todo.isCompleted) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = if (todo.isCompleted) 0.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
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
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onToggle, modifier = Modifier.size(28.dp)) {
                    if (todo.isCompleted) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Completed",
                            tint = Color(0xFF10B981),
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

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = todo.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (todo.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                        textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )

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
                                fontSize = 9.sp
                            )
                        }

                        if (todo.dueTime.isNotBlank()) {
                            Text(
                                text = "• ${todo.dueTime}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
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
                            color = Color(0xFFF59E0B),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Delete Todo",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ==========================================
// COMPONENT: ACTION PASTEL CARD
// ==========================================
@Composable
private fun ActionPastelCard(
    icon: ImageVector,
    label: String,
    bgColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = bgColor,
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E1B2E)
            )
        }
    }
}
