package com.focusflow.shield

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.focusflow.MainActivity
import com.focusflow.coins.CoinLedgerService
import com.focusflow.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BlockedAppActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_END_TIME = "extra_end_time"
        const val EXTRA_REMAINING_SECS = "extra_remaining_secs"
        const val EXTRA_LOCKED_UNTIL = "extra_locked_until"
        const val EXTRA_IS_BROWSER = "extra_is_browser"
        const val EXTRA_BROWSER_URL = "extra_browser_url"
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Prevent pressing Back from bypassing the shield to the blocked app
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                finish()
            }
        })

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: "9:00 AM"
        val endTime = intent.getStringExtra(EXTRA_END_TIME) ?: "10:00 PM"
        val initialRemainingSecs = intent.getLongExtra(EXTRA_REMAINING_SECS, 0L)
        val isBrowser = intent.getBooleanExtra(EXTRA_IS_BROWSER, false)
        val browserUrl = intent.getStringExtra(EXTRA_BROWSER_URL) ?: ""
        val pm = packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            ShieldManager.appSchedules[packageName]?.appName?.takeIf { it.isNotBlank() }
                ?: if (packageName.contains("facebook", ignoreCase = true)) "Facebook"
                else if (packageName.contains("instagram", ignoreCase = true)) "Instagram"
                else if (packageName.contains("youtube", ignoreCase = true)) "YouTube"
                else packageName.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: "App"
        }

        val appIconDrawable = try {
            pm.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
        val appIconBitmap = InstalledAppUtils.drawableToImageBitmap(appIconDrawable)

        val database = (application as? com.focusflow.FocusFlowApplication)?.database

        setContent {
            FocusFlowTheme {
                BlockedScreenContent(
                    packageName = packageName,
                    appName = appName,
                    startTime = startTime,
                    endTime = endTime,
                    initialRemainingSecs = initialRemainingSecs,
                    appIconBitmap = appIconBitmap,
                    isBrowser = isBrowser,
                    browserUrl = browserUrl,
                    database = database,
                    onEmergencyCall = {
                        try {
                            val emergencyIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(emergencyIntent)
                        } catch (e: Exception) {
                            val dialerIntent = Intent(Intent.ACTION_DIAL).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(dialerIntent)
                        }
                    },
                    onResistDistraction = { minutes, cost, reason, note ->
                        lifecycleScope.launch {
                            try {
                                database?.interventionDao()?.insertIntervention(
                                    com.focusflow.data.db.entity.UnlockIntervention(
                                        packageName = packageName,
                                        appName = appName,
                                        reason = reason,
                                        customNote = note,
                                        outcome = "RESISTED",
                                        costCoins = 0,
                                        durationMinutes = minutes
                                    )
                                )
                                val prefs = getSharedPreferences("focusflow_shield_prefs", android.content.Context.MODE_PRIVATE)
                                val lastRewardTime = prefs.getLong("key_last_resist_reward_time", 0L)
                                val now = System.currentTimeMillis()
                                val cooldownMillis = 2 * 60 * 60 * 1000L // 2 hours

                                if (now - lastRewardTime >= cooldownMillis) {
                                    CoinLedgerService.earn(2, "Distraction resisted: $appName")
                                    prefs.edit().putLong("key_last_resist_reward_time", now).apply()
                                    Toast.makeText(
                                        this@BlockedAppActivity,
                                        "💪 Willpower victory! Distraction resisted (+2 🪙)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    val remainingMin = ((cooldownMillis - (now - lastRewardTime)) / 60000L).coerceAtLeast(1)
                                    Toast.makeText(
                                        this@BlockedAppActivity,
                                        "💪 Great job staying focused!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("BlockedApp", "Error recording resisted intervention", e)
                            }
                        }
                    },
                    onUnlockWithCoins = { minutes, cost, reason, note ->
                        lifecycleScope.launch {
                            try {
                                val success = CoinLedgerService.spend(
                                    cost,
                                    "Emergency unlock: $appName for ${minutes}m"
                                )
                                if (success) {
                                    database?.interventionDao()?.insertIntervention(
                                        com.focusflow.data.db.entity.UnlockIntervention(
                                            packageName = packageName,
                                            appName = appName,
                                            reason = reason,
                                            customNote = note,
                                            outcome = "UNLOCKED",
                                            costCoins = cost,
                                            durationMinutes = minutes
                                        )
                                    )
                                    ShieldManager.grantTemporaryUnlock(packageName, minutes * 60L, appName)
                                    Toast.makeText(
                                        this@BlockedAppActivity,
                                        "Unlocked for $minutes minutes! (-$cost 🪙)",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Launch the unlocked app directly
                                    try {
                                        if (packageName.isNotBlank() && !isBrowser) {
                                            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                                            if (launchIntent != null) {
                                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                startActivity(launchIntent)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("BlockedApp", "Could not start app", e)
                                    }

                                    finish()
                                } else {
                                    Toast.makeText(
                                        this@BlockedAppActivity,
                                        "Not enough FocusBits! Complete todos or sessions to earn.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("BlockedApp", "Error during coin unlock", e)
                                Toast.makeText(
                                    this@BlockedAppActivity,
                                    "Unlock error: ${e.localizedMessage}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    onReturnToFocusFlow = {
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        startActivity(mainIntent)
                        finish()
                    },
                    onGoHome = {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(homeIntent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun BlockedScreenContent(
    packageName: String,
    appName: String,
    startTime: String,
    endTime: String,
    initialRemainingSecs: Long,
    appIconBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    isBrowser: Boolean = false,
    browserUrl: String = "",
    database: com.focusflow.data.db.FocusFlowDatabase? = null,
    onEmergencyCall: () -> Unit,
    onResistDistraction: (minutes: Int, cost: Int, reason: String, note: String) -> Unit,
    onUnlockWithCoins: (minutes: Int, cost: Int, reason: String, note: String) -> Unit,
    onReturnToFocusFlow: () -> Unit,
    onGoHome: () -> Unit
) {
    fun getSeconds(): Long {
        val fromManager = ShieldManager.getAppRemainingSeconds(packageName)
        if (fromManager > 0L) return fromManager
        return initialRemainingSecs
    }

    var remainingSeconds by remember { mutableStateOf(getSeconds()) }
    val balance by CoinLedgerService.balance.collectAsState()

    LaunchedEffect(packageName) {
        while (true) {
            remainingSeconds = getSeconds()
            if (remainingSeconds <= 0L) {
                break
            }
            delay(1000)
        }
    }

    var pendingUnlockMinutes by remember { mutableIntStateOf(5) }
    var pendingUnlockCost by remember { mutableIntStateOf(10) }
    var showInterventionModal by remember { mutableStateOf(false) }
    var showLimitModal by remember { mutableStateOf(false) }
    var dailyUnlocksToday by remember { mutableIntStateOf(0) }
    val isPro by com.focusflow.subscription.SubscriptionManager.isPro.collectAsState()
    val maxDailyUnlocks = com.focusflow.subscription.SubscriptionManager.getCurrentLimits().maxDailyUnlocks ?: 10
    val scope = rememberCoroutineScope()

    val handleUnlockClick = { minutes: Int, cost: Int ->
        scope.launch {
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val todayCount = database?.interventionDao()?.getTodayUnlockCountDirect(cal.timeInMillis) ?: 0
            dailyUnlocksToday = todayCount
            if (todayCount >= maxDailyUnlocks) {
                showLimitModal = true
            } else {
                pendingUnlockMinutes = minutes
                pendingUnlockCost = cost
                showInterventionModal = true
            }
        }
    }

    if (showLimitModal) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showLimitModal = false }
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Text(
                        text = "Daily Unlock Limit Reached",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (isPro) {
                            "You have used all $maxDailyUnlocks unlock passes for today. Take a deep breath and protect your focus!"
                        } else {
                            "Free tier allows $maxDailyUnlocks unlocks/day. You've reached your daily limit.\n\nUpgrade to FocusBit Pro for 10 unlocks/day & unlimited shield protection!"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = { showLimitModal = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("I'll Stay Focused")
                    }
                }
            }
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("focusflow_shield_prefs", android.content.Context.MODE_PRIVATE) }
    val lastResistReward = prefs.getLong("key_last_resist_reward_time", 0L)
    val canEarnCoinsOnResist = (System.currentTimeMillis() - lastResistReward) >= (2 * 60 * 60 * 1000L)

    if (showInterventionModal) {
        WhyAreYouUnlockingDialog(
            appName = appName,
            costCoins = pendingUnlockCost,
            durationMinutes = pendingUnlockMinutes,
            canEarnCoins = canEarnCoinsOnResist,
            onDismiss = { showInterventionModal = false },
            onStayFocused = { reason, note ->
                showInterventionModal = false
                onResistDistraction(pendingUnlockMinutes, pendingUnlockCost, reason, note)
            },
            onProceedUnlock = { reason, note ->
                showInterventionModal = false
                onUnlockWithCoins(pendingUnlockMinutes, pendingUnlockCost, reason, note)
            }
        )
    }

    val hours = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val seconds = remainingSeconds % 60
    val timeRemainingText = when {
        hours > 0 -> "%dh %02dm %02ds".format(hours, minutes, seconds)
        minutes > 0 -> "%02dm %02ds".format(minutes, seconds)
        else -> "%02ds".format(seconds)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top badge & Emergency Exemption button
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = TertiaryContainer.copy(alpha = 0.25f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Tertiary))
                        Text(
                            "FOCUSBIT SHIELD",
                            style = MaterialTheme.typography.labelSmall,
                            color = Tertiary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Emergency SOS Button
                FilledTonalButton(
                    onClick = onEmergencyCall,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Error.copy(alpha = 0.2f),
                        contentColor = Error
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Call, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Emergency SOS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            // Center Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                // Shield Glowing Icon
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(PrimaryContainer.copy(alpha = 0.2f))
                    )
                    Surface(
                        shape = CircleShape,
                        color = PrimaryContainer,
                        modifier = Modifier.size(76.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Filled.Shield,
                                contentDescription = "Shield Active",
                                tint = OnPrimaryContainer,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                // App Info & Message
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (appIconBitmap != null) {
                        Image(
                            bitmap = appIconBitmap,
                            contentDescription = appName,
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                        )
                    }

                    Text(
                        if (isBrowser) "$appName is Blocked" else "$appName is Locked",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceContainerHighest
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Lock, null, tint = Tertiary, modifier = Modifier.size(14.dp))
                            Text(
                                if (isBrowser) "Browser & App Shield: $startTime – $endTime" else "Focus Window: $startTime – $endTime",
                                style = MaterialTheme.typography.labelMedium,
                                color = Tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        if (isBrowser) {
                            "FocusBit Shield is active. $appName cannot be accessed via phone browsers or apps until the focus window ends at $endTime."
                        } else {
                            "FocusBit Shield is protecting your time. Unlocks automatically at $endTime."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Timer Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "TIME REMAINING UNTIL UNLOCK",
                            style = MaterialTheme.typography.labelSmall,
                            color = Tertiary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            timeRemainingText,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                        Text(
                            "FocusBit: Small Focus. Big Progress.",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant
                        )
                    }
                }

                // Coin Emergency Unlock Bar
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🪙", fontSize = 16.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Balance: $balance FocusBits",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                "Emergency Pass",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { handleUnlockClick(5, 10) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.LockOpen, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("5m (10 🪙)", style = MaterialTheme.typography.labelSmall)
                            }

                            OutlinedButton(
                                onClick = { handleUnlockClick(15, 25) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.LockOpen, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("15m (25 🪙)", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Bottom Actions
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onReturnToFocusFlow,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = OnPrimary
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Return to FocusBit", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onGoHome,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = OnSurfaceVariant
                    )
                ) {
                    Icon(Icons.Filled.Home, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Go to Home Screen", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun WhyAreYouUnlockingDialog(
    appName: String,
    costCoins: Int,
    durationMinutes: Int,
    canEarnCoins: Boolean = true,
    onDismiss: () -> Unit,
    onStayFocused: (reason: String, note: String) -> Unit,
    onProceedUnlock: (reason: String, note: String) -> Unit
) {
    val reasons = listOf(
        "Just checking",
        "Taking a break",
        "Important message",
        "Work-related",
        "Other"
    )
    var selectedReason by remember { mutableStateOf(reasons[0]) }
    var otherText by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Why are you unlocking?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "What's pulling you away?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    reasons.forEach { reason ->
                        val isSelected = selectedReason == reason
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedReason = reason },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedReason = reason },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = reason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (selectedReason == "Other") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = otherText,
                        onValueChange = { otherText = it },
                        placeholder = { Text("What pulled you away?", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val note = if (selectedReason == "Other") otherText.trim() else ""
                        onStayFocused(selectedReason, note)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        if (canEarnCoins) "I'll Stay Focused 💪 (+2 🪙)" else "I'll Stay Focused 💪",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val note = if (selectedReason == "Other") otherText.trim() else ""
                        onProceedUnlock(selectedReason, note)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        "Proceed to Unlock (${durationMinutes}m · $costCoins 🪙)",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
