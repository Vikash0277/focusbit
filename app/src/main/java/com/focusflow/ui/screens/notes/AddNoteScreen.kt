package com.focusflow.ui.screens.notes

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.focusflow.coins.CoinLedgerService
import com.focusflow.data.db.FocusFlowDatabase
import com.focusflow.data.db.entity.Note
import com.focusflow.utils.AudioPlayerHelper
import com.focusflow.utils.AudioRecorderHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteScreen(
    database: FocusFlowDatabase,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("✨") }
    var category by remember { mutableStateOf("High Priority") }
    var colorTag by remember { mutableStateOf("#EC4899") }

    // Audio recording state
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }
    var recordedAudioPath by remember { mutableStateOf<String?>(null) }
    var isPlayingPreview by remember { mutableStateOf(false) }

    val audioPlayer = remember { AudioPlayerHelper() }

    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) {
                AudioRecorderHelper.cancelRecording()
            }
            audioPlayer.stop()
        }
    }

    // Timer while recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDurationSeconds = 0
            while (isRecording) {
                delay(1000L)
                recordingDurationSeconds++
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = AudioRecorderHelper.startRecording(context)
            if (file != null) {
                isRecording = true
                emoji = "🎙️"
                if (title.isBlank()) {
                    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    title = "Voice Note $timeStr"
                }
            } else {
                Toast.makeText(context, "Could not start audio recorder", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Audio recording permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                title = {
                    Text(
                        text = "New Note",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Selected Emoji & Note Header Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(emoji, fontSize = 28.sp)
                        }
                    }
                    Column {
                        Text(
                            text = "Capture Thought or Idea",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Record voice notes or write text • Earn +5 🪙",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ─── AUDIO VOICE NOTE RECORDER SECTION ───
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (isRecording) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    1.dp,
                    if (isRecording) Color(0xFFEF4444) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
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
                                if (isRecording) Icons.Filled.Mic else Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = if (isRecording) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isRecording) "Recording Voice Note..." else if (recordedAudioPath != null) "Audio Note Attached" else "Record Voice Note",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isRecording) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (isRecording) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFEF4444).copy(alpha = 0.15f)
                            ) {
                                val mins = recordingDurationSeconds / 60
                                val secs = recordingDurationSeconds % 60
                                Text(
                                    text = String.format("%02d:%02d", mins, secs),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Recording Controls Row
                    if (!isRecording && recordedAudioPath == null) {
                        Button(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    val file = AudioRecorderHelper.startRecording(context)
                                    if (file != null) {
                                        isRecording = true
                                        emoji = "🎙️"
                                        if (title.isBlank()) {
                                            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                                            title = "Voice Note $timeStr"
                                        }
                                    } else {
                                        Toast.makeText(context, "Could not start audio recorder", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Start Voice Recording", fontWeight = FontWeight.Bold)
                        }
                    } else if (isRecording) {
                        Button(
                            onClick = {
                                val path = AudioRecorderHelper.stopRecording()
                                isRecording = false
                                if (path != null) {
                                    recordedAudioPath = path
                                    Toast.makeText(context, "Voice note recorded!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Stop & Attach Recording", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Recorded Audio Preview & Playback Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        recordedAudioPath?.let { path ->
                                            if (isPlayingPreview) {
                                                audioPlayer.stop()
                                                isPlayingPreview = false
                                            } else {
                                                audioPlayer.play(
                                                    filePath = path,
                                                    onFinished = { isPlayingPreview = false },
                                                    onError = {
                                                        isPlayingPreview = false
                                                        Toast.makeText(context, "Error playing audio", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                                isPlayingPreview = true
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(
                                                if (isPlayingPreview) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                contentDescription = if (isPlayingPreview) "Pause" else "Play",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }

                                Column {
                                    Text(
                                        text = if (isPlayingPreview) "Playing preview..." else "Voice recording ready",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val sizeKb = recordedAudioPath?.let { File(it).length() / 1024 } ?: 0
                                    Text(
                                        text = "$sizeKb KB • Audio M4A",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Discard / Re-record button
                            IconButton(
                                onClick = {
                                    audioPlayer.stop()
                                    isPlayingPreview = false
                                    recordedAudioPath?.let { File(it).delete() }
                                    recordedAudioPath = null
                                }
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Discard Audio",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Emoji Selection Row
            Text(
                text = "CHOOSE ICON EMOJI",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val emojis = listOf("✨", "🎙️", "🧠", "📝", "💡", "🎯", "📚", "🎨", "🚀")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                emojis.forEach { e ->
                    val isSelected = emoji == e
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { emoji = e }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(e, fontSize = 18.sp)
                        }
                    }
                }
            }

            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Note Title") },
                placeholder = { Text("e.g. Sketch One UIScreen, Project Roadmap") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Content Multi-line Input
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Note Body (Optional for Voice Notes)") },
                placeholder = { Text("Write your thoughts, ideas, tasks or meeting notes here...") },
                minLines = 4,
                maxLines = 12,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Category Chips
            Text(
                text = "CATEGORY & TAG COLOR",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val categories = listOf(
                "High Priority" to "#EC4899",
                "Productive" to "#8B5CF6",
                "Ideas" to "#F59E0B",
                "Work" to "#06B6D4",
                "Personal" to "#10B981"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { (cat, hex) ->
                    val isSelected = category == cat
                    val tagCol = try {
                        Color(android.graphics.Color.parseColor(hex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) tagCol.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) tagCol else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                category = cat
                                colorTag = hex
                            }
                    ) {
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) tagCol else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Save Note Button (Cylindrical pill shape)
            Button(
                onClick = {
                    val finalTitle = title.ifBlank {
                        if (recordedAudioPath != null) "Voice Note" else "Untitled Note"
                    }
                    val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                    coroutineScope.launch {
                        database.noteDao().insertNote(
                            Note(
                                title = finalTitle.trim(),
                                content = content.trim(),
                                emoji = emoji,
                                category = category,
                                colorTag = colorTag,
                                date = formattedDate,
                                audioPath = recordedAudioPath
                            )
                        )
                        CoinLedgerService.earn(
                            CoinLedgerService.REWARD_TODO_COMPLETED,
                            "Created Note: $finalTitle",
                            "note_created_${System.currentTimeMillis()}"
                        )
                        Toast.makeText(context, "Note saved! (+5 🪙)", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Save Note (+5 🪙)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
