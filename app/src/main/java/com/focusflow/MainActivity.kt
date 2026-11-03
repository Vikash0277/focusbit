package com.focusflow

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.focusflow.coins.CoinLedgerService
import com.focusflow.scroll.ScrollTrackerManager
import com.focusflow.subscription.SubscriptionManager
import com.focusflow.ui.modals.CoinWalletModal
import com.focusflow.ui.modals.PaywallModal

import com.focusflow.ui.navigation.NavGraph
import com.focusflow.ui.navigation.Screen
import com.focusflow.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "Notification permission granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val database = (application as FocusFlowApplication).database
        val prefs = getSharedPreferences("focusflow_theme_prefs", Context.MODE_PRIVATE)
        ScrollTrackerManager.init(this)

        setContent {
            var isDarkTheme by remember {
                mutableStateOf(prefs.getBoolean("is_dark_theme", true))
            }

            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            var showWalletModal by remember { mutableStateOf(false) }
            var showPaywallModal by remember { mutableStateOf(false) }


            val balance by CoinLedgerService.balance.collectAsState()
            val isPro by SubscriptionManager.isPro.collectAsState()
            val scrollStats by ScrollTrackerManager.statsFlow.collectAsState()

            FocusFlowTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val currentScreen = Screen.bottomNavItems.find { screen ->
                    currentDestination?.hierarchy?.any { it.route == screen.route } == true
                } ?: Screen.Today

                val isStackScreen = currentDestination?.route == Screen.AppUsage.route

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            modifier = Modifier.width(320.dp),
                            drawerContainerColor = MaterialTheme.colorScheme.surface,
                            drawerTonalElevation = 6.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // App Header
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.app_logo),
                                        contentDescription = "FocusBit Logo",
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    Column {
                                        Text(
                                            text = "FocusBit",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Small Focus. Big Progress.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                // Pro Card - Dark Mode Aware & Sleek
                                val proCardBg = if (isDarkTheme) {
                                    if (isPro) Color(0xFF132A1C) else Color(0xFF231638)
                                } else {
                                    if (isPro) Color(0xFFE8F5E9) else Color(0xFFF3E5F5)
                                }

                                val proCardBorder = if (isDarkTheme) {
                                    if (isPro) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color(0xFFAB47BC).copy(alpha = 0.5f)
                                } else {
                                    if (isPro) Color(0xFF81C784).copy(alpha = 0.7f) else Color(0xFFCE93D8)
                                }

                                val proCardAccent = if (isDarkTheme) {
                                    if (isPro) Color(0xFF81C784) else Color(0xFFCE93D8)
                                } else {
                                    if (isPro) Color(0xFF1B5E20) else Color(0xFF6A1B9A)
                                }

                                val proBadgeBg = if (isDarkTheme) {
                                    if (isPro) Color(0xFF2E7D32) else Color(0xFF8E24AA)
                                } else {
                                    if (isPro) Color(0xFF2E7D32) else Color(0xFF7B1FA2)
                                }

                                val proCardSubtext = if (isDarkTheme) {
                                    if (isPro) Color(0xFFA5D6A7).copy(alpha = 0.85f) else Color(0xFFE1BEE7).copy(alpha = 0.85f)
                                } else {
                                    if (isPro) Color(0xFF2E7D32).copy(alpha = 0.85f) else Color(0xFF4A148C).copy(alpha = 0.8f)
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = proCardBg,
                                    border = BorderStroke(1.dp, proCardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch { drawerState.close() }
                                            showPaywallModal = true
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Filled.Diamond,
                                                    contentDescription = null,
                                                    tint = proCardAccent,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = if (isPro) "FocusBit Pro" else "Go FocusBit Pro",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = proCardAccent
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = proBadgeBg
                                            ) {
                                                Text(
                                                    text = if (isPro) "ACTIVE" else "UPGRADE",
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(5.dp))
                                        Text(
                                            text = if (isPro) "Unlimited shield, 2x coin boost & nuclear lock active." else "Unlock unlimited app shields, strict mode & 2x coins.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = proCardSubtext,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                // Wallet Card
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isDarkTheme) Color(0xFF1E1E28) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDarkTheme) 0.35f else 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch { drawerState.close() }
                                            showWalletModal = true
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 11.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("🪙", fontSize = 22.sp)
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "FocusBits Wallet",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "$balance FocusBits available",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Icon(
                                            Icons.Filled.ChevronRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "PREFERENCES & TOOLS",
                                    style = MaterialTheme.typography.labelSmall,
                                    letterSpacing = 1.1.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Permission Center
                                    NavigationDrawerItem(
                                        modifier = Modifier.height(48.dp),
                                        icon = { Icon(Icons.Filled.Security, null, modifier = Modifier.size(20.dp)) },
                                        label = { Text("Permission Center", style = MaterialTheme.typography.bodyMedium) },
                                        selected = false,
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            try {
                                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                            } catch (e: Exception) {
                                                startActivity(Intent(Settings.ACTION_SETTINGS))
                                            }
                                        }
                                    )

                                    // Phone App Usage
                                    NavigationDrawerItem(
                                        modifier = Modifier.height(48.dp),
                                        icon = { Icon(Icons.Filled.DataUsage, null, modifier = Modifier.size(20.dp)) },
                                        label = { Text("Phone Screen Time & Usage", style = MaterialTheme.typography.bodyMedium) },
                                        selected = false,
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            navController.navigate(Screen.AppUsage.route)
                                        }
                                    )

                                    // Usage Stats Access
                                    NavigationDrawerItem(
                                        modifier = Modifier.height(48.dp),
                                        icon = { Icon(Icons.Filled.Settings, null, modifier = Modifier.size(20.dp)) },
                                        label = { Text("Usage Access Settings", style = MaterialTheme.typography.bodyMedium) },
                                        selected = false,
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            try {
                                                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                            } catch (e: Exception) {
                                                startActivity(Intent(Settings.ACTION_SETTINGS))
                                            }
                                        }
                                    )

                                    // Dark / Light Toggle
                                    NavigationDrawerItem(
                                        modifier = Modifier.height(48.dp),
                                        icon = {
                                            Icon(
                                                if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                                null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        label = { Text(if (isDarkTheme) "Switch to Light Theme" else "Switch to Dark Theme", style = MaterialTheme.typography.bodyMedium) },
                                        selected = false,
                                        onClick = {
                                            val newTheme = !isDarkTheme
                                            isDarkTheme = newTheme
                                            prefs.edit().putBoolean("is_dark_theme", newTheme).apply()
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                // Live Counter Badge Background Color Section
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "BADGE BACKGROUND COLOR",
                                        style = MaterialTheme.typography.labelSmall,
                                        letterSpacing = 1.1.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(
                                        onClick = {
                                            ScrollTrackerManager.resetPositionToTopMiddle()
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Filled.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(13.dp), tint = Primary)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Center Top", fontSize = 11.sp, color = Primary, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDarkTheme) Color(0xFF1E1E28) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDarkTheme) 0.35f else 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        val activeTheme = ScrollTrackerManager.COLOR_THEMES.find { it.id == scrollStats.activeColorThemeId } ?: ScrollTrackerManager.COLOR_THEMES[0]
                                        Text(
                                            text = activeTheme.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            ScrollTrackerManager.COLOR_THEMES.forEach { theme ->
                                                val isSelected = scrollStats.activeColorThemeId == theme.id
                                                val bgColor = try {
                                                    Color(android.graphics.Color.parseColor(theme.bgHex))
                                                } catch (e: Exception) {
                                                    Color(0xFF12111E)
                                                }
                                                val borderColor = try {
                                                    Color(android.graphics.Color.parseColor(theme.borderHex))
                                                } catch (e: Exception) {
                                                    Color(0xFF8B5CF6)
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(bgColor)
                                                        .border(
                                                            width = if (isSelected) 2.dp else 1.dp,
                                                            color = if (isSelected) Color.White else borderColor,
                                                            shape = CircleShape
                                                        )
                                                        .clickable {
                                                            ScrollTrackerManager.setOverlayTheme(theme.id)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            Icons.Filled.Check,
                                                            contentDescription = theme.name,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                // Footer
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "FocusBit v2.0 Native",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Small Focus. Big Progress.",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Surface,
                        topBar = {
                            if (!isStackScreen) {
                                TopAppBar(
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(
                                                Icons.Filled.Menu,
                                                contentDescription = "Open Drawer Menu",
                                                tint = OnSurface
                                            )
                                        }
                                    },
                                    title = {
                                        Text(
                                            text = "FocusBit",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = OnSurface
                                        )
                                    },
                                    actions = {
                                        // Coin Wallet Pill Button
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .clickable { showWalletModal = true }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text("🪙", fontSize = 14.sp)
                                                Text(
                                                    text = "$balance",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }

                                        Spacer(Modifier.width(6.dp))

                                        // Theme toggle
                                        IconButton(
                                            onClick = {
                                                val newTheme = !isDarkTheme
                                                isDarkTheme = newTheme
                                                prefs.edit().putBoolean("is_dark_theme", newTheme).apply()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                                contentDescription = "Toggle Theme",
                                                tint = if (isDarkTheme) Color(0xFFFFD54F) else Primary
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = SurfaceContainer.copy(alpha = 0.95f),
                                        titleContentColor = OnSurface
                                    )
                                )
                            }
                        },
                        bottomBar = {
                            if (!isStackScreen) {
                                NavigationBar(
                                    containerColor = SurfaceContainer.copy(alpha = 0.95f),
                                    contentColor = OnSurfaceVariant,
                                    tonalElevation = 2.dp
                                ) {
                                    Screen.bottomNavItems.forEach { screen ->
                                        val selected = currentDestination?.hierarchy?.any {
                                            it.route == screen.route
                                        } == true

                                        NavigationBarItem(
                                            icon = {
                                                Icon(
                                                    imageVector = screen.icon,
                                                    contentDescription = screen.title
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = screen.title,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            selected = selected,
                                            onClick = {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Primary,
                                                selectedTextColor = Primary,
                                                unselectedIconColor = OnSurfaceVariant,
                                                unselectedTextColor = OnSurfaceVariant,
                                                indicatorColor = PrimaryContainer.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        NavGraph(
                            navController = navController,
                            database = database,
                            innerPadding = innerPadding,
                            onOpenWallet = { showWalletModal = true }
                        )
                    }
                }

                // Global Modals
                if (showWalletModal) {
                    CoinWalletModal(onDismiss = { showWalletModal = false })
                }

                if (showPaywallModal) {
                    PaywallModal(onDismiss = { showPaywallModal = false })
                }

            }
        }
    }
}
