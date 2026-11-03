package com.focusflow

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.focusflow.coins.CoinLedgerService
import com.focusflow.data.db.FocusFlowDatabase
import com.focusflow.data.db.entity.Goal
import com.focusflow.data.db.entity.Note
import com.focusflow.data.db.entity.Todo
import com.focusflow.notifications.NotificationHelper
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
        ThemeManager.init(this)
        ScrollTrackerManager.init(this)

        setContent {
            val currentTheme by ThemeManager.currentTheme.collectAsState()
            val isDarkTheme = currentTheme == AppTheme.MIDNIGHT

            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            var showWalletModal by remember { mutableStateOf(false) }
            var showPaywallModal by remember { mutableStateOf(false) }
            var isSearchActive by remember { mutableStateOf(false) }
            var searchQuery by remember { mutableStateOf("") }

            val allTodos by database.todoDao().getAllTodos().collectAsState(initial = emptyList())
            val allNotes by database.noteDao().getAllNotes().collectAsState(initial = emptyList())
            val allGoals by database.goalDao().getAllGoals().collectAsState(initial = emptyList())

            val balance by CoinLedgerService.balance.collectAsState()
            val isPro by SubscriptionManager.isPro.collectAsState()
            val scrollStats by ScrollTrackerManager.statsFlow.collectAsState()

            FocusFlowTheme(appTheme = currentTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val currentScreen = Screen.bottomNavItems.find { screen ->
                    currentDestination?.hierarchy?.any { it.route == screen.route } == true
                } ?: Screen.Today

                val currentRoute = currentDestination?.route ?: ""
                val isStackScreen = currentRoute == Screen.AppUsage.route ||
                        currentRoute == Screen.Appearance.route ||
                        currentRoute == Screen.Notifications.route ||
                        currentRoute == Screen.SetGoal.route ||
                        currentRoute == Screen.AddTodo.route ||
                        currentRoute.startsWith("todo_detail") ||
                        currentRoute == Screen.AddNote.route ||
                        currentRoute.startsWith("note_detail")

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            modifier = Modifier.width(320.dp),
                            drawerContainerColor = MaterialTheme.colorScheme.surface,
                            drawerTonalElevation = 2.dp,
                            drawerShape = RoundedCornerShape(0.dp, 28.dp, 28.dp, 0.dp)
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
                                    shape = RoundedCornerShape(20.dp),
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
                                    shape = RoundedCornerShape(20.dp),
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

                                    // Appearance & Themes
                                    NavigationDrawerItem(
                                        modifier = Modifier.height(48.dp),
                                        icon = {
                                            Icon(
                                                Icons.Filled.Palette,
                                                null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        label = {
                                            Text(
                                                "Appearance & Themes",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        selected = false,
                                        onClick = {
                                            scope.launch { drawerState.close() }
                                            navController.navigate(Screen.Appearance.route)
                                        }
                                    )

                                    // Quick Theme Switcher
                                    NavigationDrawerItem(
                                        modifier = Modifier.height(48.dp),
                                        icon = {
                                            Icon(
                                                if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                                null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                if (isDarkTheme) "Switch to Lavender Dream" else "Switch to Midnight",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        selected = false,
                                        onClick = {
                                            ThemeManager.setTheme(if (isDarkTheme) AppTheme.LAVENDER_DREAM else AppTheme.MIDNIGHT)
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
                        containerColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            if (!isStackScreen) {
                                if (isSearchActive) {
                                    // ACTIVE SEARCH APP BAR
                                    TopAppBar(
                                        navigationIcon = {
                                            IconButton(onClick = {
                                                isSearchActive = false
                                                searchQuery = ""
                                            }) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Close Search",
                                                    tint = OnSurface
                                                )
                                            }
                                        },
                                        title = {
                                            OutlinedTextField(
                                                value = searchQuery,
                                                onValueChange = { searchQuery = it },
                                                placeholder = {
                                                    Text(
                                                        "Search tasks & notes...",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = OnSurfaceVariant
                                                    )
                                                },
                                                singleLine = true,
                                                shape = RoundedCornerShape(50),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color.Transparent,
                                                    unfocusedBorderColor = Color.Transparent,
                                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    cursorColor = Primary
                                                ),
                                                trailingIcon = {
                                                    if (searchQuery.isNotEmpty()) {
                                                        IconButton(onClick = { searchQuery = "" }) {
                                                            Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                            )
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = SurfaceContainer.copy(alpha = 0.98f),
                                            titleContentColor = OnSurface
                                        )
                                    )
                                } else {
                                    // NORMAL APP BAR
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
                                            // 1. Coin Wallet Pill
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
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
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }

                                            // 2. Search Icon Button
                                            IconButton(onClick = { isSearchActive = true }) {
                                                Icon(
                                                    imageVector = Icons.Filled.Search,
                                                    contentDescription = "Search",
                                                    tint = OnSurface
                                                )
                                            }

                                            // 3. Notification Bell Icon with Badge
                                            val pendingCount = allTodos.count { !it.isCompleted }
                                            IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                                                BadgedBox(
                                                    badge = {
                                                        if (pendingCount > 0) {
                                                            Badge(containerColor = Primary) {
                                                                Text(
                                                                    if (pendingCount > 9) "9+" else "$pendingCount",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color.White
                                                                )
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Notifications,
                                                        contentDescription = "Notifications & Reminders",
                                                        tint = OnSurface
                                                    )
                                                }
                                            }

                                            // 4. Appearance & Theme Selector
                                            IconButton(
                                                onClick = {
                                                    navController.navigate(Screen.Appearance.route)
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Palette,
                                                    contentDescription = "Appearance & Themes",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = SurfaceContainer.copy(alpha = 0.95f),
                                            titleContentColor = OnSurface
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        val topPadding = if (isStackScreen) 0.dp else innerPadding.calculateTopPadding()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = topPadding)
                        ) {
                            NavGraph(
                                navController = navController,
                                database = database,
                                innerPadding = PaddingValues(0.dp),
                                allGoals = allGoals,
                                allTodos = allTodos,
                                onOpenWallet = { showWalletModal = true }
                            )

                            // Floating Cylindrical Pill Navigation Bar (Truly Floating with Transparent Background)
                            if (!isStackScreen && !isSearchActive) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 12.dp)
                                        .navigationBarsPadding()
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 1.0f),
                                        tonalElevation = 0.dp,
                                        shadowElevation = 8.dp,
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(68.dp)
                                                .padding(horizontal = 6.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceAround,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Screen.bottomNavItems.forEach { screen ->
                                                val selected = currentDestination?.hierarchy?.any {
                                                    it.route == screen.route
                                                } == true

                                                val indicatorBgColor by animateColorAsState(
                                                    targetValue = if (selected) PrimaryContainer.copy(alpha = 0.65f) else Color.Transparent,
                                                    animationSpec = tween(durationMillis = 200),
                                                    label = "navIndicatorBg"
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .clip(RoundedCornerShape(50))
                                                        .clickable(
                                                            interactionSource = remember { MutableInteractionSource() },
                                                            indication = null
                                                        ) {
                                                            navController.navigate(screen.route) {
                                                                popUpTo(navController.graph.findStartDestination().id) {
                                                                    saveState = true
                                                                }
                                                                launchSingleTop = true
                                                                restoreState = true
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(50))
                                                            .background(
                                                                color = indicatorBgColor,
                                                                shape = RoundedCornerShape(50)
                                                            )
                                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = screen.icon,
                                                                contentDescription = screen.title,
                                                                modifier = Modifier.size(20.dp),
                                                                tint = if (selected) Primary else OnSurfaceVariant
                                                            )
                                                            Text(
                                                                text = screen.title,
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                                color = if (selected) Primary else OnSurfaceVariant,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Interactive Live Search Overlay
                            if (isSearchActive) {
                                SearchOverlayView(
                                    searchQuery = searchQuery,
                                    todos = allTodos,
                                    notes = allNotes,
                                    onSelectTodo = { todoId ->
                                        isSearchActive = false
                                        searchQuery = ""
                                        navController.navigate(Screen.TodoDetail.createRoute(todoId))
                                    },
                                    onSelectNote = { noteId ->
                                        isSearchActive = false
                                        searchQuery = ""
                                        navController.navigate(Screen.NoteDetail.createRoute(noteId))
                                    },
                                    onDismiss = {
                                        isSearchActive = false
                                        searchQuery = ""
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
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

// ==========================================
// SEARCH OVERLAY VIEW
// ==========================================
@Composable
private fun SearchOverlayView(
    searchQuery: String,
    todos: List<Todo>,
    notes: List<Note>,
    onSelectTodo: (Long) -> Unit,
    onSelectNote: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredTodos = remember(todos, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else todos.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredNotes = remember(notes, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else notes.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.content.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                if (searchQuery.isBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Type to search across tasks & notes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Find todo items, notes, ideas and schedule entries instantly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (filteredTodos.isEmpty() && filteredNotes.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "No results for \"$searchQuery\"",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Try searching with different keywords.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (filteredTodos.isNotEmpty()) {
                            item {
                                Text(
                                    text = "TASKS (${filteredTodos.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                )
                            }
                            items(filteredTodos) { todo ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onSelectTodo(todo.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                if (todo.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (todo.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = todo.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                                )
                                                if (todo.dueTime.isNotBlank() || todo.category.isNotBlank()) {
                                                    Text(
                                                        text = listOf(todo.dueTime, todo.category).filter { it.isNotBlank() }.joinToString(" • "),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                        Icon(
                                            Icons.Filled.ChevronRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredNotes.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "NOTES (${filteredNotes.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                )
                            }
                            items(filteredNotes) { note ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onSelectNote(note.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(note.emoji, fontSize = 20.sp)
                                            Column {
                                                Text(
                                                    text = note.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                if (note.content.isNotBlank()) {
                                                    Text(
                                                        text = note.content,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                        Icon(
                                            Icons.Filled.ChevronRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
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
}
