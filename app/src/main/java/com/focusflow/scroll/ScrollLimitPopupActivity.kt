package com.focusflow.scroll

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusflow.MainActivity
import com.focusflow.ui.theme.FocusFlowTheme
import kotlinx.coroutines.delay
import java.util.Locale

class ScrollLimitPopupActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_SCROLL_COUNT = "extra_scroll_count"
        const val EXTRA_SCROLL_LIMIT = "extra_scroll_limit"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "App"
        val scrollCount = intent.getIntExtra(EXTRA_SCROLL_COUNT, 0)
        val scrollLimit = intent.getIntExtra(EXTRA_SCROLL_LIMIT, 50)

        // Ensure back press returns cleanly to Home
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAndGoHome()
            }
        })

        setContent {
            FocusFlowTheme {
                ScrollLimitPopupContent(
                    packageName = packageName,
                    appName = appName,
                    scrollCount = scrollCount,
                    scrollLimit = scrollLimit,
                    onCloseAndGoHome = { finishAndGoHome() },
                    onOpenFocusBit = {
                        val mainIntent = Intent(this@ScrollLimitPopupActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(mainIntent)
                        finish()
                    },
                    onAllowSnooze = {
                        ScrollTrackerManager.addAutoCloseSnooze(packageName, 25)
                        finish()
                    }
                )
            }
        }
    }

    private fun finishAndGoHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}

@Composable
fun ScrollLimitPopupContent(
    packageName: String,
    appName: String,
    scrollCount: Int,
    scrollLimit: Int,
    onCloseAndGoHome: () -> Unit,
    onOpenFocusBit: () -> Unit,
    onAllowSnooze: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Auto-close countdown timer (6 seconds)
    var countdownSeconds by remember { mutableStateOf(6) }

    LaunchedEffect(Unit) {
        while (countdownSeconds > 0) {
            delay(1000L)
            countdownSeconds--
        }
        onCloseAndGoHome()
    }

    val distanceMeters = scrollCount * 0.15f
    val distanceDisplay = if (distanceMeters >= 1000f) {
        String.format(Locale.US, "%.1f km", distanceMeters / 1000f)
    } else {
        String.format(Locale.US, "%.0f m", distanceMeters)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(onClick = onCloseAndGoHome),
        contentAlignment = Alignment.Center
    ) {
        // Modal Container
        Card(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .wrapContentHeight()
                .clickable(enabled = false) {}, // Prevent dismissing when clicking the card itself
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141322)),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.8.dp,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF8B5CF6))
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glowing Warning Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = 0.18f))
                        .border(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FrontHand,
                        contentDescription = "Stop Doomscroll",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title & Subtitle
                Text(
                    text = "Scroll Limit Reached!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "Auto-Closing $appName",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF8A80)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Description
                Text(
                    text = "You set a scroll limit of $scrollLimit scrolls in $appName. FocusBit is closing the app to protect your attention and break the dopamine loop.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Metric Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E1B33),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF332D55))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$scrollCount",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFF7043)
                            )
                            Text(
                                text = "Scrolls Today",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E1B33),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF332D55))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = distanceDisplay,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF818CF8)
                            )
                            Text(
                                text = "Thumb Distance",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Action: Close Now / Countdown button
                Button(
                    onClick = onCloseAndGoHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF673AB7)
                    )
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (countdownSeconds > 0) "Close App & Go Home (${countdownSeconds}s)" else "Close App & Go Home",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Action: Open FocusBit
                OutlinedButton(
                    onClick = onOpenFocusBit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF4C3B78)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFD1C4E9)
                    )
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open FocusBit Dashboard",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Snooze / Emergency Action
                TextButton(
                    onClick = onAllowSnooze,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Need 1 more minute? Allow +25 scrolls",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
