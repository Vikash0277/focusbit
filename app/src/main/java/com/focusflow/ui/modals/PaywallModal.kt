package com.focusflow.ui.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.focusflow.subscription.SubscriptionManager

@Composable
fun PaywallModal(
    onDismiss: () -> Unit
) {
    val isPro by SubscriptionManager.isPro.collectAsState()
    var selectedPlan by remember { mutableStateOf("annual") }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF673AB7).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "FOCUSBIT PRO",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF673AB7)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Hero Icon
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF673AB7),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Diamond,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Supercharge Your Focus",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Small Focus. Big Progress.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Free vs Pro Feature Comparison Matrix
                Text(
                    text = "Free vs Pro Tier Matrix",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF673AB7).copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Feature",
                                modifier = Modifier.weight(1.3f),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Free",
                                modifier = Modifier.weight(0.9f),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Pro 💎",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF673AB7),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        ComparisonRow("🎯 Goals", "3 active", "Unlimited", isProHighlight = true)
                        ComparisonRow("📋 To-Dos", "30 active", "Unlimited", isProHighlight = true)
                        ComparisonRow("✅ Habits", "5 active", "Unlimited", isProHighlight = true)
                        ComparisonRow("⏱️ Stopwatch", "✅", "✅")
                        ComparisonRow("🍅 Focus Timer", "Basic", "Advanced", isProHighlight = true)
                        ComparisonRow("🛡️ App Shield", "3 apps", "Unlimited", isProHighlight = true)
                        ComparisonRow("🛑 Scroll Auto-Close", "❌ None", "✅ Auto-Exit + Popup", isProHighlight = true)
                        ComparisonRow("🛡️ Shield schedules", "1 schedule", "Unlimited", isProHighlight = true)
                        ComparisonRow("🔓 Coin unlocks", "3/day", "10/day", isProHighlight = true)
                        ComparisonRow("🪙 FocusBits earning", "1×", "1.5×", isProHighlight = true)
                        ComparisonRow("📊 Statistics", "Basic", "Advanced", isProHighlight = true)
                        ComparisonRow("📅 Goal timeline", "✅", "✅")
                        ComparisonRow("🔥 Streaks", "Basic", "Advanced", isProHighlight = true)
                        ComparisonRow("🗓️ Habit heatmap", "This month", "Full history", isProHighlight = true)
                        ComparisonRow("🔄 Reset exercises", "Basic", "Full library", isProHighlight = true)
                        ComparisonRow("🔔 Reminders", "Basic", "Advanced", isProHighlight = true)
                        ComparisonRow("🎯 Goal-linked Shield", "❌", "✅", isProHighlight = true)
                        ComparisonRow("⏰ Scheduled Focus", "❌", "✅", isProHighlight = true)
                        ComparisonRow("📈 Productivity insights", "❌", "✅", isProHighlight = true)
                        ComparisonRow("🧠 Smart recommendations", "❌", "✅", isProHighlight = true)
                        ComparisonRow("🎨 Customization", "Basic", "Advanced", isProHighlight = true)
                        ComparisonRow("☁️ Cloud sync", "❌ initially", "Eventually")
                        ComparisonRow("🚫 Ads", "Maybe", "No ads", isProHighlight = true)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Plan Selectors
                Text(
                    text = "Choose Your Plan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Annual Plan Card
                PlanOptionCard(
                    title = "Annual Membership",
                    badge = "SAVE 52% (Best Value)",
                    price = "$39.99 / year",
                    subtitle = "Just $3.33/month, billed annually",
                    isSelected = selectedPlan == "annual",
                    onClick = { selectedPlan = "annual" }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Monthly Plan Card
                PlanOptionCard(
                    title = "Monthly Flexible",
                    badge = null,
                    price = "$6.99 / month",
                    subtitle = "Cancel anytime from your account",
                    isSelected = selectedPlan == "monthly",
                    onClick = { selectedPlan = "monthly" }
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (isPro) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "FocusBit Pro is Active on this device!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // CTA Button
                Button(
                    onClick = {
                        SubscriptionManager.activatePro(selectedPlan)
                        actionMessage = "🎉 Welcome to FocusBit Pro!"
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF673AB7)
                    )
                ) {
                    Text(
                        if (isPro) "Update Subscription" else "Unlock FocusBit Pro Now",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                actionMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = {
                        SubscriptionManager.restorePurchases()
                        actionMessage = "Purchases Restored."
                    }) {
                        Text("Restore Purchases", style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(onClick = {
                        SubscriptionManager.deactivatePro()
                        actionMessage = "Reverted to Free tier."
                    }) {
                        Text("Reset to Free", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF673AB7),
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlanOptionCard(
    title: String,
    badge: String?,
    price: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF673AB7) else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (isSelected) Color(0xFF673AB7).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(2.dp, borderColor, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    badge?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF673AB7)
                        ) {
                            Text(
                                text = it,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSelected) Color(0xFF673AB7) else MaterialTheme.colorScheme.onSurface
                )
                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF673AB7))
                )
            }
        }
    }
}

@Composable
private fun ComparisonRow(
    feature: String,
    freeVal: String,
    proVal: String,
    isProHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            modifier = Modifier.weight(1.3f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = freeVal,
            modifier = Modifier.weight(0.9f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = proVal,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isProHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isProHighlight) Color(0xFF673AB7) else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    )
}
