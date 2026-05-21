package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendWiseApp(
    viewModel: SpendWiseViewModel,
    modifier: Modifier = Modifier
) {
    val isOnboardingComplete by viewModel.isOnboardingCompleted.collectAsState()
    val session by viewModel.userSession.collectAsState()
    
    // Theme options managed locally for instant switching feedback
    var isDarkTheme by remember { mutableStateOf(true) }
    var selectedCurrency by remember { mutableStateOf("$") } // $, ₹, €, £

    MyApplicationTheme(darkTheme = isDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                !isOnboardingComplete -> {
                    OnboardingScreen(
                        onFinished = { viewModel.completeOnboarding() }
                    )
                }
                !session.isLoggedIn -> {
                    AuthScreen(viewModel = viewModel)
                }
                else -> {
                    MainWorkspace(
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        selectedCurrency = selectedCurrency,
                        onChangeCurrency = { selectedCurrency = it }
                    )
                }
            }
        }
    }
}

// ==========================================
// ONBOARDING & SPLASH SCREEN
// ==========================================
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    var step by remember { mutableStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianDark, SlateDeep)
                )
            )
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper Progress indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (index + 1 <= step) TealPrimary else SlateBase
                        )
                )
            }
        }

        // Animated Info Content
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            },
            modifier = Modifier.weight(1f),
            label = "OnboardingContent"
        ) { currentStep ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 40.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Rounded Glow Shield Logo/Icon
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(TealPrimary, ElectricPurple)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (currentStep) {
                            1 -> Icons.Default.Savings
                            2 -> Icons.Default.AutoAwesome
                            else -> Icons.Default.ReceiptLong
                        },
                        contentDescription = "Step Icon",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = when (currentStep) {
                        1 -> "Hyper-Personalized Wealth"
                        2 -> "Next-Generation AI Intelligence"
                        else -> "Receipt Scanning & Shared Wallets"
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (currentStep) {
                        1 -> "SpendWise analyzes your recurring subscriptions, sets flexible budgets, and builds micro-saving streaks optimized for standard lifestyles."
                        2 -> "Speak, chat, or type natural language transactions. Our onboard Gemini coach parses and organizes category budgets instantly."
                        else -> "Automatically capture digital invoices, share common budgeting goals with your partner, family, or flatmates."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = GrayMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Bottom Controls Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 1) {
                TextButton(
                    onClick = { step-- },
                    colors = ButtonDefaults.textButtonColors(contentColor = GrayMuted)
                ) {
                    Text("BACK")
                }
            } else {
                Spacer(modifier = Modifier.width(60.dp))
            }

            Button(
                onClick = {
                    if (step < 3) {
                        step++
                    } else {
                        onFinished()
                    }
                },
                modifier = Modifier
                    .height(56.dp)
                    .testTag("onboarding_next_button"),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (step == 3) "GET STARTED" else "CONTINUE",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Arrow Next",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

// ==========================================
// AUTHENTICATION SCREEN FOR COHESION
// ==========================================
@Composable
fun AuthScreen(
    viewModel: SpendWiseViewModel
) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Student") } // Student, Salaried, Freelancer, Couple, Family
    var isRegisterState by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val roles = listOf("Student", "Salaried", "Freelancer", "Couple", "Family")
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Text(
            text = "SpendWise",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Black,
                color = TealPrimary,
                letterSpacing = (-1.5).sp
            )
        )
        Text(
            text = "AI-POWERED INSIGHTS & WEALTH PLANNER",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = GrayMuted,
                letterSpacing = 1.5.sp
            )
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Card Container
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SlateDeep),
            border = BorderStroke(1.dp, SlateBase)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRegisterState) "Create Fintech Vault" else "Secure Vault Access",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorText = "" },
                    label = { Text("Vault Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input"),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") }
                )

                if (isRegisterState) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorText = "" },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorText = "" },
                    label = { Text("Vault Passphrase") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") }
                )

                if (isRegisterState) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Customize AI Stream Optimization:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MintLight,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Horizontal scroll of roles
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        roles.forEach { role ->
                            val isSelected = selectedRole == role
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedRole = role },
                                label = { Text(role) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                if (errorText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorText,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorText = "Please fill in email and passphrase!"
                            return@Button
                        }
                        errorText = ""
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                if (isRegisterState) {
                                    val finalName = if (name.isBlank()) "Fintech User" else name
                                    val success = viewModel.registerUser(email, finalName, password, selectedRole)
                                    if (success) {
                                        val loginSuccess = viewModel.loginUser(email, password)
                                        if (!loginSuccess) {
                                            errorText = "Registration completed but session build failed."
                                        }
                                    } else {
                                        errorText = "This email is already registered!"
                                    }
                                } else {
                                    val success = viewModel.loginUser(email, password)
                                    if (!success) {
                                        errorText = "Invalid email or vault passphrase!"
                                    }
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("login_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = if (isRegisterState) "INITIALIZE ACCOUNT" else "DECRYPT & ENTER",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { isRegisterState = !isRegisterState; errorText = "" }) {
                    Text(
                        text = if (isRegisterState) "Already have a secure key? Access Vault" else "Need customized AI profiling? Create entry key",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MintLight
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Demo Fast entry bypass option
        TextButton(
            onClick = {
                isLoading = true
                errorText = ""
                coroutineScope.launch {
                    try {
                        val success = viewModel.loginUser("salaried.demo@spendwise.ai", "demo123")
                        if (!success) {
                            errorText = "Demo account secure key mismatch."
                        }
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = "demo", tint = AmberWarning)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Bypass with Pre-Loaded Salaried Demo Account", color = AmberWarning)
            }
        }
    }
}

// ==========================================
// MAIN APP WORKSPACE
// ==========================================
@Composable
fun MainWorkspace(
    viewModel: SpendWiseViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    selectedCurrency: String,
    onChangeCurrency: (String) -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val notificationList by viewModel.notificationsList.collectAsState()
    
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showInsightsDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SpendWiseTopBar(
                viewModel = viewModel,
                onShowNotifications = { showNotificationDialog = true },
                onShowSettings = { showSettingsDialog = true }
            )
        },
        bottomBar = {
            SpendWiseBottomNav(
                currentTab = currentTab,
                onTabSelected = { viewModel.changeTab(it) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExpenseDialog = true },
                containerColor = TealPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense", modifier = Modifier.size(28.dp))
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) { innerPadding ->
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "WorkspaceTab"
            ) { activeTab ->
                when (activeTab) {
                    SpendWiseTab.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        currency = selectedCurrency,
                        onAskAICoach = { query ->
                            viewModel.changeTab(SpendWiseTab.AI_COACH)
                            viewModel.askAICoach(query)
                        }
                    )
                    SpendWiseTab.HISTORY -> HistoryScreen(
                        viewModel = viewModel,
                        currency = selectedCurrency
                    )
                    SpendWiseTab.AI_COACH -> AICoachScreen(
                        viewModel = viewModel
                    )
                    SpendWiseTab.RECURRING -> SubscriptionsScreen(
                        viewModel = viewModel,
                        currency = selectedCurrency
                    )
                    SpendWiseTab.FAMILY -> FamilySharedScreen(
                        viewModel = viewModel,
                        currency = selectedCurrency
                    )
                }
            }
        }
    }

    // Modal Sub dialogue implementations
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            viewModel = viewModel,
            onDismiss = { showAddExpenseDialog = false }
        )
    }

    if (showNotificationDialog) {
        NotificationsDialog(
            list = notificationList,
            onClear = { viewModel.clearNotifications() },
            onDismiss = { showNotificationDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = viewModel,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            selectedCurrency = selectedCurrency,
            onChangeCurrency = onChangeCurrency,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

// ==========================================
// COMPONENT: TOP BAR & STATUS ACTIONS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendWiseTopBar(
    viewModel: SpendWiseViewModel,
    onShowNotifications: () -> Unit,
    onShowSettings: () -> Unit
) {
    val session by viewModel.userSession.collectAsState()
    val notificationList by viewModel.notificationsList.collectAsState()

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Round Emoji avatar
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SlateBase),
                    contentAlignment = Alignment.Center
                ) {
                    Text(session.avatarEmoji, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = session.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Persona Optimization: ${session.role}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GrayMuted
                    )
                }
            }
        },
        actions = {
            // Notification Badge indicator
            Box {
                IconButton(onClick = onShowNotifications) {
                    Icon(
                        imageVector = if (notificationList.isNotEmpty()) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                        contentDescription = "Alerts",
                        tint = if (notificationList.isNotEmpty()) AmberWarning else MaterialTheme.colorScheme.onBackground
                    )
                }
                if (notificationList.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(CrimsonRed)
                            .align(Alignment.TopEnd)
                            .offset(x = (-8).dp, y = 8.dp)
                    )
                }
            }

            IconButton(onClick = onShowSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

// ==========================================
// COMPONENT: MODERN FLOATING BOTTOM NAVIGATION BAR
// ==========================================
@Composable
fun SpendWiseBottomNav(
    currentTab: SpendWiseTab,
    onTabSelected: (SpendWiseTab) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentTab == SpendWiseTab.DASHBOARD,
            onClick = { onTabSelected(SpendWiseTab.DASHBOARD) },
            icon = { Icon(Icons.Default.SpaceDashboard, contentDescription = "Dashboard") },
            label = { Text("Hub") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TealPrimary,
                selectedTextColor = TealPrimary,
                indicatorColor = SlateBase
            )
        )
        NavigationBarItem(
            selected = currentTab == SpendWiseTab.HISTORY,
            onClick = { onTabSelected(SpendWiseTab.HISTORY) },
            icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Ledger") },
            label = { Text("History") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TealPrimary,
                selectedTextColor = TealPrimary,
                indicatorColor = SlateBase
            )
        )
        NavigationBarItem(
            selected = currentTab == SpendWiseTab.AI_COACH,
            onClick = { onTabSelected(SpendWiseTab.AI_COACH) },
            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Coach") },
            label = { Text("AI Coach") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TealPrimary,
                selectedTextColor = TealPrimary,
                indicatorColor = SlateBase
            )
        )
        NavigationBarItem(
            selected = currentTab == SpendWiseTab.RECURRING,
            onClick = { onTabSelected(SpendWiseTab.RECURRING) },
            icon = { Icon(Icons.Default.Update, contentDescription = "Recurring items") },
            label = { Text("Autopays") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TealPrimary,
                selectedTextColor = TealPrimary,
                indicatorColor = SlateBase
            )
        )
        NavigationBarItem(
            selected = currentTab == SpendWiseTab.FAMILY,
            onClick = { onTabSelected(SpendWiseTab.FAMILY) },
            icon = { Icon(Icons.Default.People, contentDescription = "Family wallets") },
            label = { Text("Co-Budget") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TealPrimary,
                selectedTextColor = TealPrimary,
                indicatorColor = SlateBase
            )
        )
    }
}

// ==========================================
// SCREEN: HOME HUB DASHBOARD
// ==========================================
@Composable
fun DashboardScreen(
    viewModel: SpendWiseViewModel,
    currency: String,
    onAskAICoach: (String) -> Unit
) {
    val expenses by viewModel.expenses.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val streak by viewModel.streakCount.collectAsState()
    val rawTip by viewModel.aiInsightTip.collectAsState()

    val totalSpent = expenses.sumOf { it.amount }
    val globalBudgetObj = budgets.firstOrNull { it.category == "Global" }
    val globalLimit = globalBudgetObj?.limitAmount ?: 5000.0
    val remainingBudget = (globalLimit - totalSpent).coerceAtLeast(0.0)
    val budgetProgress = if (globalLimit > 0) (totalSpent / globalLimit).coerceIn(0.0, 1.0) else 0.0

    // Today's spending
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayMs = cal.timeInMillis
    val spentToday = expenses.filter { it.date >= todayMs }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // AI Advice Premium glowing banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAskAICoach("Give me custom tailored savings recommendations based on my expenditures.") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateDeep),
                border = BorderStroke(1.dp, Brush.linearGradient(listOf(TealPrimary, ElectricPurple)))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(MintLight, TealPrimary))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, "Spark", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI INSIGHT OF THE DAY", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MintLight)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(rawTip, style = MaterialTheme.typography.bodyMedium, color = WhiteIce, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // Premium Radial Card / Total balance overview
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SlateDeep),
                border = BorderStroke(1.dp, SlateBase)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left numerical summary
                    Column {
                        Text("TOTAL SPENT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = GrayMuted)
                        Text("$currency${String.format("%,.2f", totalSpent)}", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold), color = Color.White)
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Text("REMAINING REVENUE LIMIT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = GrayMuted)
                        Text("$currency${String.format("%,.2f", remainingBudget)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MintLight)
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Limit: $currency$globalLimit", style = MaterialTheme.typography.labelSmall, color = GrayMuted)
                    }

                    // Right Custom Chart Canvas
                    Box(
                        modifier = Modifier.size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(90.dp)) {
                            // Empty track
                            drawCircle(
                                color = SlateBase,
                                style = Stroke(width = 10.dp.toPx())
                            )
                            // Progress sweep
                            drawArc(
                                color = if (budgetProgress > 0.85) CrimsonRed else TealPrimary,
                                startAngle = -90f,
                                sweepAngle = (budgetProgress * 360f).toFloat(),
                                useCenter = false,
                                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${(budgetProgress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
                            Text("Used", style = MaterialTheme.typography.labelSmall, color = GrayMuted)
                        }
                    }
                }
            }
        }

        // Mid row details: Today spent and streak streaks
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SlateDeep),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Insights, "chart", tint = CyberBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TODAY'S FLOW", style = MaterialTheme.typography.labelSmall, color = GrayMuted)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("$currency${String.format("%.2f", spentToday)}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Recorded 24h", style = MaterialTheme.typography.labelSmall, color = GrayMuted)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SlateDeep),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, "streak", tint = AmberWarning, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SAVING STREAK", style = MaterialTheme.typography.labelSmall, color = GrayMuted)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("$streak Days", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = AmberWarning)
                        Text("+${streak * 20} Financial Badges", style = MaterialTheme.typography.labelSmall, color = GrayMuted)
                    }
                }
            }
        }

        // Category breakdown horizontal quick overview
        item {
            Column {
                Text("BUDGET CATEGORIES METERS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = GrayMuted)
                Spacer(modifier = Modifier.height(10.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateDeep)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val activeCategories = listOf("Food", "Transport", "Shopping", "Entertainment")
                        activeCategories.forEach { cat ->
                            val catBudget = budgets.firstOrNull { it.category == cat }
                            val limitVal = catBudget?.limitAmount ?: 800.0
                            val amtSpent = expenses.filter { it.category == cat }.sumOf { it.amount }
                            val perc = if (limitVal > 0) (amtSpent / limitVal).coerceIn(0.0, 1.0) else 0.0

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(getCategoryColor(cat))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(cat, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Text("$currency${amtSpent.toInt()} / $currency${limitVal.toInt()}", style = MaterialTheme.typography.labelSmall, color = GrayMuted)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { perc.toFloat() },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                    color = if (perc > 0.85) CrimsonRed else getCategoryColor(cat),
                                    trackColor = SlateBase
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent expenses title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("RECENT TRANSACTIONS LEDGER", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = GrayMuted)
            }
        }

        if (expenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateDeep),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No transactions logged yet! Click the '+' button below to append.", color = GrayMuted, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(expenses.take(4)) { item ->
                ExpenseRowItem(
                    expense = item,
                    currency = currency,
                    onDelete = { viewModel.deleteExpense(item) }
                )
            }
        }
    }
}

// ==========================================
// SCREEN: FULL TRANSACTION HISTORY / LEDGER
// ==========================================
@Composable
fun HistoryScreen(
    viewModel: SpendWiseViewModel,
    currency: String
) {
    val expenses by viewModel.expenses.collectAsState()
    var searchPhrase by remember { mutableStateOf("") }
    var filterCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Food", "Transport", "Shopping", "Rent", "Entertainment", "Health", "Bills", "Education", "Travel", "Other")

    val filteredList = expenses.filter {
        val matchesSearch = it.title.contains(searchPhrase, ignoreCase = true)
        val matchesCat = filterCategory == "All" || it.category.equals(filterCategory, ignoreCase = true)
        matchesSearch && matchesCat
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search & Filter Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateDeep),
            border = BorderStroke(1.dp, SlateBase)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = searchPhrase,
                    onValueChange = { searchPhrase = it },
                    placeholder = { Text("Search transactions...", color = GrayMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = GrayMuted) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollablechips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = filterCategory == cat,
                            onClick = { filterCategory = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = SlateBase,
                                labelColor = GrayMuted
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "MATCHED TRANSACTIONS (${filteredList.size})",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = GrayMuted,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.HourglassEmpty, "None", tint = GrayMuted, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No fitting transactions found.", color = GrayMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredList) { item ->
                    ExpenseRowItem(
                        expense = item,
                        currency = currency,
                        onDelete = { viewModel.deleteExpense(item) }
                    )
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: EXPENSE ROW ITEM
// ==========================================
@Composable
fun ExpenseRowItem(
    expense: Expense,
    currency: String,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDeleteConfirm = true },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SlateDeep),
        border = BorderStroke(1.dp, SlateBase)
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
                // Circular icon representation for each category
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(getCategoryColor(expense.category).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(expense.category),
                        contentDescription = expense.category,
                        tint = getCategoryColor(expense.category)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = expense.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = GrayMuted
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatTimestamp(expense.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = GrayMuted
                        )
                    }
                    if (expense.paymentMethod.isNotEmpty() && !expense.paymentMethod.contains("Manual")) {
                        Text(
                            text = "via ${expense.paymentMethod}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MintLight
                        )
                    }
                }
            }

            Text(
                text = "-$currency${String.format("%.2f", expense.amount)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = CrimsonRed
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Entry?") },
            text = { Text("Are you sure you want to permanently remove \"${expense.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("YES, DELETE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("CANCEL")
                }
            },
            containerColor = SlateDeep
        )
    }
}

// ==========================================
// SCREEN: AI FINANCIAL COACH CHATBOT
// ==========================================
@Composable
fun AICoachScreen(
    viewModel: SpendWiseViewModel
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isAILoading by viewModel.isAILoading.collectAsState()
    var userTextPhrase by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    val suggestPrompts = listOf(
        "How can I save more money?",
        "Where am I overspending?",
        "How should I budget as a student?",
        "Can I afford a new laptop?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Quick instruction / Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SPENDWISE WEALTH COACH", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = GrayMuted)
            TextButton(onClick = { viewModel.clearHistory() }) {
                Text("RESET CONVERSATION", color = CrimsonRed, fontSize = 11.sp)
            }
        }

        // Chat bubbles list
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(message = msg)
            }
            if (isAILoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = SlateBase)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TealPrimary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Coach is parsing expenditures...", style = MaterialTheme.typography.bodyMedium, color = GrayMuted)
                            }
                        }
                    }
                }
            }
        }

        // Prompt Suggestions tags
        if (messages.size <= 1) {
            Text("QUICK SPARKS:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = GrayMuted, modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                suggestPrompts.forEach { phrase ->
                    Card(
                        modifier = Modifier.clickable {
                            viewModel.askAICoach(phrase)
                        },
                        colors = CardDefaults.cardColors(containerColor = SlateDeep),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlateBase)
                    ) {
                        Text(phrase, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MintLight, modifier = Modifier.padding(10.dp))
                    }
                }
            }
        }

        // Send workspace bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = userTextPhrase,
                onValueChange = { userTextPhrase = it },
                placeholder = { Text("Consult with SpendWise advisor...", color = GrayMuted) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = SlateBase
                ),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (userTextPhrase.isNotBlank()) {
                                val t = userTextPhrase
                                userTextPhrase = ""
                                viewModel.askAICoach(t)
                            }
                        },
                        enabled = !isAILoading
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = TealPrimary)
                    }
                }
            )
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = if (message.isUser) {
                RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
            } else {
                RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
            },
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) TealPrimary else SlateDeep
            ),
            border = if (message.isUser) null else BorderStroke(1.dp, SlateBase),
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Parsing basic markdown formatting if existing
                val text = message.text
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser) Color.White else WhiteIce
                )
            }
        }
    }
}

// ==========================================
// SCREEN: RECURRING SUBSCRIPTION TRACKER
// ==========================================
@Composable
fun SubscriptionsScreen(
    viewModel: SpendWiseViewModel,
    currency: String
) {
    val subs by viewModel.subscriptions.collectAsState()
    var showAddSubDialog by remember { mutableStateOf(false) }

    val totalMonthlyCharge = subs.filter { it.frequency.equals("Monthly", true) }.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Upper stats ring
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateDeep),
            border = BorderStroke(1.dp, SlateBase)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TOTAL RECURRING CHARGES", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = GrayMuted)
                        Text("$currency${String.format("%.2f", totalMonthlyCharge)}/mo", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold), color = CyberBlue)
                    }
                    Button(
                        onClick = { showAddSubDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateBase)
                    ) {
                        Text("Add Autopay", color = CyanColor)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ACTIVE AUTOMATED AUTOPAYS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = GrayMuted)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (subs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No recurring items logged yet.", color = GrayMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(subs) { sub ->
                    SubscriptionRow(
                        sub = sub,
                        currency = currency,
                        onDelete = { viewModel.removeSubscription(sub) }
                    )
                }
            }
        }
    }

    if (showAddSubDialog) {
        AddSubscriptionDialog(
            onDismiss = { showAddSubDialog = false },
            onSave = { name, amount, category, freq, auto ->
                viewModel.addSubscriptionItem(name, amount, category, freq, auto)
                showAddSubDialog = false
            }
        )
    }
}

@Composable
fun SubscriptionRow(
    sub: Subscription,
    currency: String,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SlateDeep),
        border = BorderStroke(1.dp, SlateBase)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CyberBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CreditCard, "Card", tint = CyberBlue)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(sub.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    Text("Auto-renews: ${if (sub.isAutoRenew) "Yes, Active" else "No"}", style = MaterialTheme.typography.labelSmall, color = GrayMuted)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("$currency${String.format("%.2f", sub.amount)}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold))
                    Text(sub.frequency, style = MaterialTheme.typography.labelSmall, color = GrayMuted)
                }
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Cancel Tracker", tint = CrimsonRed)
                }
            }
        }
    }
}

// ==========================================
// SCREEN: FAMILY/GROUP SHARED WALLET PREVIEW
// ==========================================
@Composable
fun FamilySharedScreen(
    viewModel: SpendWiseViewModel,
    currency: String
) {
    val members by viewModel.familyMembers.collectAsState()
    val goals by viewModel.savingGoals.collectAsState()

    var inviteName by remember { mutableStateOf("") }
    var inviteRole by remember { mutableStateOf("Member") } // Member, Partner

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top stats shared wallet summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateDeep),
            border = BorderStroke(1.dp, SlateBase)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("CO-BUDGET SHARED ESCROW", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = GrayMuted)
                Text("$currency${String.format("%,.2f", 3450.0)}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = ElectricPurple)
                Text("Sum of pooled wallets", style = MaterialTheme.typography.labelSmall, color = GrayMuted)
            }
        }

        // Savings streak/Goals Title
        Text("SAVINGS TARGETS PROGRESS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = GrayMuted)
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateDeep)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                goals.forEach { goal ->
                    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).coerceIn(0.0, 1.0) else 0.0
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(goal.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text("$currency${goal.currentAmount.toInt()} / $currency${goal.targetAmount.toInt()}", style = MaterialTheme.typography.labelSmall, color = GrayMuted)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = ElectricPurple,
                            trackColor = SlateBase
                        )
                    }
                }
            }
        }

        // Shared group invitation controller
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateDeep),
            border = BorderStroke(1.dp, SlateBase)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("INVITE CO-BUDGET PARTNER", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MintLight)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inviteName,
                        onValueChange = { inviteName = it },
                        placeholder = { Text("Partner's Name") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Button(
                        onClick = {
                            if (inviteName.isNotBlank()) {
                                viewModel.inviteFamilyMember(inviteName, inviteRole)
                                inviteName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text("Invite")
                    }
                }
            }
        }

        // List members connected
        Text("GROUP MEMBERS WALLET SYNC", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = GrayMuted)
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(members) { m ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateDeep)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ElectricPurple),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(m.name.take(1))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(m.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                Text(m.role, style = MaterialTheme.typography.labelSmall, color = GrayMuted)
                            }
                        }
                        Text(m.inviteStatus, color = if (m.inviteStatus == "Joined") MintLight else AmberWarning, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// ==========================================
// DIALOG DEFINITIONS
// ==========================================

// Add Expense Drawer dialog with Voice & Image Presets simulation!
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    viewModel: SpendWiseViewModel,
    onDismiss: () -> Unit
) {
    var sourceTab by remember { mutableStateOf(0) } // 0: Manual, 1: AI Prompt, 2: Receipt OCR
    val isAILoading by viewModel.isAILoading.collectAsState()

    // Form inputs
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var paymentMethod by remember { mutableStateOf("Credit Card") }
    
    // AI NLP Prompt input
    var aiNaturalText by remember { mutableStateOf("Spent 250 on burgers") }
    
    val categories = listOf("Food", "Transport", "Shopping", "Rent", "Entertainment", "Health", "Bills", "Education", "Travel", "Other")
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SlateDeep)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Append Entry", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Dismiss")
                    }
                }

                TabRow(
                    selectedTabIndex = sourceTab,
                    containerColor = SlateDeep,
                    contentColor = TealPrimary
                ) {
                    Tab(selected = sourceTab == 0, onClick = { sourceTab = 0 }, text = { Text("Manual") })
                    Tab(selected = sourceTab == 1, onClick = { sourceTab = 1 }, text = { Text("AI NLP Parse") })
                    Tab(selected = sourceTab == 2, onClick = { sourceTab = 2 }, text = { Text("Receipt OCR") })
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when (sourceTab) {
                        0 -> { // MANUAL ENTRY
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = { Text("Transaction Title") },
                                    modifier = Modifier.fillMaxWidth().testTag("add_expense_title"),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = amountText,
                                    onValueChange = { amountText = it },
                                    label = { Text("Amount") },
                                    modifier = Modifier.fillMaxWidth().testTag("add_expense_amount"),
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )

                                Text("Select Category:", style = MaterialTheme.typography.labelSmall, color = GrayMuted)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    categories.forEach { cat ->
                                        FilterChip(
                                            selected = category == cat,
                                            onClick = { category = cat },
                                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary),
                                            label = { Text(cat) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = paymentMethod,
                                    onValueChange = { paymentMethod = it },
                                    label = { Text("Payment Method") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        val amt = amountText.toDoubleOrNull() ?: 0.0
                                        if (amt > 0) {
                                            viewModel.addManualExpense(title, amt, category, paymentMethod)
                                            onDismiss()
                                        } else {
                                            Toast.makeText(context, "Please configure valid amount", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("add_expense_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                                ) {
                                    Text("APPEND TO SYSTEM", color = Color.White)
                                }
                            }
                        }
                        1 -> { // AI VOICE / NLP DIALOG
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    "Explain natural sentence flows, Speakeasy style, or type your command. Our background LLM will break it into a transaction automatically:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GrayMuted
                                )

                                OutlinedTextField(
                                    value = aiNaturalText,
                                    onValueChange = { aiNaturalText = it },
                                    label = { Text("E.g. Spent 120 rupees on snacks") },
                                    modifier = Modifier.fillMaxWidth().height(110.dp),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Micro-glowing prompt helpers
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Spent 45 on subway combo", "Spent 980 for monthly electric bill").forEach { helper ->
                                        InputChip(
                                            selected = false,
                                            onClick = { aiNaturalText = helper },
                                            label = { Text(helper, fontSize = 11.sp, color = GrayMuted) }
                                        )
                                    }
                                }

                                if (isAILoading) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = TealPrimary)
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (aiNaturalText.isNotBlank()) {
                                            viewModel.parseTextExpenseWithAI(aiNaturalText) { success ->
                                                if (success) {
                                                    onDismiss()
                                                } else {
                                                    Toast.makeText(context, "Parsing offline or limits reached. Appending manually.", Toast.LENGTH_LONG).show()
                                                    viewModel.addManualExpense("Command expense", 250.0, "Food", "AI Simulated")
                                                    onDismiss()
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                    enabled = !isAILoading
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.AutoAwesome, "Spark")
                                        Text("PARSE VIA AI SPARK", color = Color.White)
                                    }
                                }
                            }
                        }
                        2 -> { // RECEIPT OCR COMPILER
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    "Simulate receipt scanning with preconfigured actual retail templates. We send high-contrast Base64 data directly to Gemini Vision model:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GrayMuted
                                )

                                val sampleReceipts = listOf("Starbucks Coffee", "Whole Foods Groceries", "Gas Station Diesel")
                                sampleReceipts.forEach { name ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.parseReceiptImageWithAI(
                                                    presetBase64 = "MOCK_BASE64_IMAGE_DATA_VEO_COMPUTED",
                                                    PresetName = name
                                                ) {
                                                    onDismiss()
                                                }
                                            },
                                        colors = CardDefaults.cardColors(containerColor = SlateBase),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, SlateDeep)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.PhotoLibrary, "File", tint = ElectricPurple)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            }
                                            Icon(Icons.Default.ChevronRight, "Scanner")
                                        }
                                    }
                                }

                                if (isAILoading) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                        LinearProgressIndicator(color = ElectricPurple, modifier = Modifier.fillMaxWidth())
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

// Notifications drawer simulation
@Composable
fun NotificationsDialog(
    list: List<String>,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SlateDeep)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SpendWise Alerts Hub", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Dismiss")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (list.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No active notifications. You are budget optimized!", color = GrayMuted, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(list) { alert ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateBase)
                            ) {
                                Text(alert, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClear) {
                        Text("CLEAR ALL", color = CrimsonRed)
                    }
                }
            }
        }
    }
}

// Custom Settings Dialog inside
@Composable
fun SettingsDialog(
    viewModel: SpendWiseViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    selectedCurrency: String,
    onChangeCurrency: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val session by viewModel.userSession.collectAsState()
    var currentDisplayOption by remember { mutableStateOf(session.role) }
    val roles = listOf("Student", "Salaried", "Freelancer", "Couple", "Family")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SlateDeep)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SpendWise Core Settings", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Dismiss")
                    }
                }

                Divider(color = SlateBase)

                // 1. Dark mode Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cosmic Dark Visual Theme", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isDarkTheme, onCheckedChange = { onToggleTheme() })
                }

                // 2. Select Currency Option
                Column {
                    Text("Fintech Base Currency", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("$", "₹", "€", "£").forEach { curr ->
                            val active = selectedCurrency == curr
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onChangeCurrency(curr) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (active) TealPrimary else SlateBase
                                )
                            ) {
                                Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                                    Text(curr, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = if (active) Color.White else WhiteIce)
                                }
                            }
                        }
                    }
                }

                // 3. Select active Persona Model
                Column {
                    Text("Optimized Target Persona", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        roles.forEach { role ->
                            FilterChip(
                                selected = currentDisplayOption == role,
                                onClick = {
                                    currentDisplayOption = role
                                    viewModel.updateRole(role)
                                },
                                label = { Text(role) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary)
                            )
                        }
                    }
                }

                Divider(color = SlateBase)

                // Simulated PDF CSV report triggers
                Button(
                    onClick = {
                        viewModel.addNotification("Financial spreadsheet successfully generated in your /Documents directory.")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SlateBase)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Download, "Download")
                        Text("EXPORT COMPREHENSIVE CSV/PDF")
                    }
                }

                // Sign out simulation
                TextButton(
                    onClick = {
                        viewModel.logout()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Logout, "Exit", tint = CrimsonRed)
                        Text("SIGN OUT OF VAULT", color = CrimsonRed)
                    }
                }
            }
        }
    }
}

// ==========================================
// STATIC HELPER FORMATTERS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscriptionDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Double, category: String, frequency: String, isAutoRenew: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Bills") }
    var frequency by remember { mutableStateOf("Monthly") }
    var isAutoRenew by remember { mutableStateOf(true) }

    val categories = listOf("Bills", "Entertainment", "Health", "Other")
    val frequencies = listOf("Monthly", "Yearly")
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SlateDeep),
            border = BorderStroke(1.dp, SlateBase)
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
                    Text("Track Autopay", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Dismiss")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subscription Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Column {
                    Text("Billing Category:", style = MaterialTheme.typography.labelSmall, color = GrayMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TealPrimary)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Frequency", style = MaterialTheme.typography.bodyMedium)
                        Text(frequency, style = MaterialTheme.typography.labelSmall, color = GrayMuted)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        frequencies.forEach { freq ->
                            ElevatedCard(
                                onClick = { frequency = freq },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (frequency == freq) TealPrimary else SlateBase
                                )
                            ) {
                                Text(
                                    text = freq,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (frequency == freq) Color.White else WhiteIce
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Auto-Renew Subscription", style = MaterialTheme.typography.bodyMedium)
                        Text("Alerts on upcoming cycle", style = MaterialTheme.typography.labelSmall, color = GrayMuted)
                    }
                    Switch(checked = isAutoRenew, onCheckedChange = { isAutoRenew = it })
                }

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank() && amt > 0) {
                            onSave(name, amt, category, frequency, isAutoRenew)
                        } else {
                            Toast.makeText(context, "Please write a name and valid amount", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("MONITOR AUTOPAY", color = Color.White)
                }
            }
        }
    }
}

// ==========================================
// STATIC HELPER FORMATTERS
// ==========================================

fun formatTimestamp(ms: Long): String {
    val date = Date(ms)
    val sdf = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault())
    return sdf.format(date)
}

val CyanColor = Color(0xFF0EA5E9)

fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "food" -> TealPrimary
        "transport" -> CyberBlue
        "shopping" -> ElectricPurple
        "rent" -> Color(0xFFF43F5E)
        "entertainment" -> AmberWarning
        "health" -> Color(0xFF10B981)
        "bills" -> Color(0xFFFF5A5F)
        "education" -> Color(0xFF3B82F6)
        "travel" -> Color(0xFFEC4899)
        else -> GrayMuted
    }
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "food" -> Icons.Default.Restaurant
        "transport" -> Icons.Default.DirectionsCar
        "shopping" -> Icons.Default.LocalMall
        "rent" -> Icons.Default.HomeWork
        "entertainment" -> Icons.Default.LocalPlay
        "health" -> Icons.Default.LocalHospital
        "bills" -> Icons.Default.Receipt
        "education" -> Icons.Default.School
        "travel" -> Icons.Default.Flight
        else -> Icons.Default.Category
    }
}
