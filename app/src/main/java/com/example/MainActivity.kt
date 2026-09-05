package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.core.content.ContextCompat
import com.example.engine.AudioCaptureManager
import com.example.ui.AppLanguage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.engine.ContactSource
import com.example.engine.PhoneContactItem
import com.example.ui.VoiceGuardViewModel
import com.example.ui.components.LiveCallScanningOverlayBar
import com.example.ui.components.TopCyberHeader
import com.example.ui.screens.AttackChainScreen
import com.example.ui.screens.AuthOnboardingScreen
import com.example.ui.screens.CallDashboardScreen
import com.example.ui.screens.DialerContactsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RecordingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShieldScreen
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberBgSecondary
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.VoiceGuardTheme

enum class MainAppTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    DIAL_CONTACTS("Dialer", Icons.Default.Phone),
    VOICE_RECORDINGS("Vault", Icons.Default.RecordVoiceOver),
    SECURITY_SETTINGS("Settings", Icons.Default.Tune),
    PROFILE("Profile", Icons.Default.AccountCircle)
}

class MainActivity : ComponentActivity() {

    private val audioRecordPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        val phoneStateGranted = permissions[Manifest.permission.READ_PHONE_STATE] == true
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        } else true

        if (recordAudioGranted) {
            Log.i("MainActivity", "RECORD_AUDIO permission granted. AudioCaptureManager live monitoring enabled.")
            Toast.makeText(this, "🎙️ Audio recording permission active: Call monitoring enabled", Toast.LENGTH_SHORT).show()
            try {
                com.example.engine.PhoneStateMonitorForegroundService.startService(this)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed starting PhoneStateMonitorForegroundService: ${e.message}")
            }
        } else {
            Log.w("MainActivity", "RECORD_AUDIO permission denied.")
            Toast.makeText(this, "⚠️ Audio recording permission required for live call protection", Toast.LENGTH_LONG).show()
        }
    }

    fun requestCallMonitoringPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            permissions.add(Manifest.permission.READ_PHONE_NUMBERS)
        }
        permissions.add(Manifest.permission.READ_CALL_LOG)

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            audioRecordPermissionLauncher.launch(needed.toTypedArray())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check and request runtime audio recording & call monitoring permissions
        requestCallMonitoringPermissions()

        // Start listening to live phone state and background protection
        try {
            com.example.engine.VoiceGuardNotificationManager.createNotificationChannels(this)
            com.example.engine.PhoneStateMonitor.startListening(this)
            com.example.engine.PhoneStateMonitorForegroundService.startService(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Enable default FLAG_SECURE for mobile screen capture & remote share protection
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContent {
            val viewModel: VoiceGuardViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            VoiceGuardTheme(darkTheme = isDarkMode) {
                VoiceGuardApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            com.example.engine.PhoneStateMonitorForegroundService.startService(this)
        } catch (e: Exception) {
            Log.w("MainActivity", "Service start in onResume: ${e.message}")
        }
    }
}

@Composable
fun VoiceGuardApp(viewModel: VoiceGuardViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    var currentTab by remember { mutableStateOf(MainAppTab.HOME) }
    var isAttackChainOpen by remember { mutableStateOf(false) }

    val isShieldActive by viewModel.isShieldActive.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val authUserState by viewModel.authUserState.collectAsState()
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
    val screenProtected by viewModel.screenProtectionEnabled.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()

    val liveCallState by viewModel.liveCallState.collectAsState()
    val backgroundCallStatus by viewModel.backgroundCallStatus.collectAsState()
    val liveSession by viewModel.liveBackgroundSession.collectAsState()
    val isAudioCapturing by AudioCaptureManager.isCapturing.collectAsState()

    val isCallScanning = liveCallState.isCallActive ||
        backgroundCallStatus.isMonitoring ||
        isAudioCapturing ||
        (liveSession != null && (liveSession?.callState == "RINGING" || liveSession?.callState == "OFFHOOK"))

    val activeCallerName = when {
        liveCallState.isCallActive -> liveCallState.currentContact?.name ?: "Live Caller"
        backgroundCallStatus.isMonitoring -> backgroundCallStatus.callerName
        isAudioCapturing -> AudioCaptureManager.activeCallerName.value
        else -> liveSession?.callerName?.ifEmpty { "Live Caller" } ?: "Live Caller"
    }

    val activeCallerNumber = when {
        liveCallState.isCallActive -> liveCallState.currentContact?.number ?: "+91 (Active)"
        backgroundCallStatus.isMonitoring -> backgroundCallStatus.callerNumber
        isAudioCapturing -> AudioCaptureManager.activeCallerNumber.value
        else -> liveSession?.callerNumber?.ifEmpty { "+91 (Active)" } ?: "+91 (Active)"
    }

    val activeRiskScore = when {
        liveCallState.isCallActive -> liveCallState.currentRiskScore
        backgroundCallStatus.isMonitoring -> backgroundCallStatus.currentRiskScore
        else -> (liveSession?.riskScore ?: 12)
    }

    val activeDuration = when {
        liveCallState.isCallActive -> liveCallState.durationSeconds
        backgroundCallStatus.isMonitoring -> backgroundCallStatus.durationSeconds
        else -> liveSession?.durationSeconds ?: 0
    }

    val activeThreatSummary = when {
        liveCallState.isCallActive -> liveCallState.audioAnomalyFlag ?: "AI Deepfake & Synthesis Scan Active"
        backgroundCallStatus.isMonitoring -> backgroundCallStatus.threatSummary
        isAudioCapturing -> "Live AudioRecord PCM stream active (16kHz AudioCaptureManager)"
        else -> if (activeRiskScore >= 70) "High-risk neural vocoder artifacts detected" else "Acoustic stream authentic & natural (TFLite Scan)"
    }

    // Dynamically enforce FLAG_SECURE based on Screen Security state
    LaunchedEffect(screenProtected) {
        activity?.let { act ->
            if (screenProtected) {
                act.window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
            } else {
                act.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    // Handle incoming navigation intent from High-Priority Notification Banners
    LaunchedEffect(activity?.intent) {
        val navTarget = activity?.intent?.getStringExtra("EXTRA_NAVIGATE_TO")
        if (navTarget == "CALL_DASHBOARD") {
            currentTab = MainAppTab.HOME
        }
    }

    // If user is not authenticated and has not completed onboarding, show Login / Register & Permissions flow
    if (!isOnboardingCompleted && !authUserState.isAuthenticated) {
        AuthOnboardingScreen(
            viewModel = viewModel,
            onOnboardingComplete = {
                currentTab = MainAppTab.HOME
            }
        )
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBg),
            topBar = {
                Column(modifier = Modifier.statusBarsPadding()) {
                    TopCyberHeader(
                        isShieldActive = isShieldActive,
                        onRefreshClick = {
                            viewModel.refreshContactsAndCallLogs()
                            Toast.makeText(context, "Synced device contacts & threat feeds", Toast.LENGTH_SHORT).show()
                        }
                    )

                    val hasAudioRecordPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    AnimatedVisibility(visible = !hasAudioRecordPermission) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            color = Color(0xFF2E1705),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Audio Recording Permission Required",
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Audio recording permission required for live call monitoring",
                                        color = Color(0xFFFDE68A),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        (activity as? MainActivity)?.requestCallMonitoringPermissions()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Grant", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    LiveCallScanningOverlayBar(
                        isScanningActive = isCallScanning,
                        callerName = activeCallerName,
                        callerNumber = activeCallerNumber,
                        riskScore = activeRiskScore,
                        durationSeconds = activeDuration,
                        threatSummary = activeThreatSummary,
                        onSimulateAttack = {
                            val dummyContact = PhoneContactItem(
                                id = "sim_attacker",
                                name = "VIP Deepfake Impersonator",
                                number = activeCallerNumber.ifEmpty { "+91 98765 43210" },
                                source = ContactSource.LIVE_INPUT
                            )
                            viewModel.startLiveCallMonitoring(dummyContact, threatSimulationMode = true)
                            AudioCaptureManager.startCaptureService(
                                context = context,
                                callerName = dummyContact.name,
                                callerNumber = dummyContact.number
                            )
                        },
                        onMarkSafe = {
                            val safeContact = PhoneContactItem(
                                id = "safe_caller",
                                name = activeCallerName,
                                number = activeCallerNumber,
                                source = ContactSource.LIVE_INPUT
                            )
                            viewModel.startLiveCallMonitoring(safeContact, threatSimulationMode = false)
                            Toast.makeText(context, "Caller marked as safe & whitelisted", Toast.LENGTH_SHORT).show()
                        },
                        onDisconnect = {
                            viewModel.endCallMonitoring()
                            com.example.engine.CallMonitorForegroundService.stopCallMonitor(context)
                            AudioCaptureManager.stopCaptureService(context)
                            Toast.makeText(context, "Call terminated by VoiceGuard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            bottomBar = {
                CyberBottomNav(
                    currentTab = currentTab,
                    appLanguage = appLanguage,
                    onTabSelected = {
                        currentTab = it
                        isAttackChainOpen = false
                    }
                )
            },
            containerColor = CyberBg
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isAttackChainOpen) {
                    AttackChainScreen(viewModel = viewModel)
                } else {
                    when (currentTab) {
                        MainAppTab.HOME -> ShieldScreen(viewModel = viewModel)
                        MainAppTab.DIAL_CONTACTS -> DialerContactsScreen(viewModel = viewModel)
                        MainAppTab.VOICE_RECORDINGS -> RecordingsScreen(viewModel = viewModel)
                        MainAppTab.SECURITY_SETTINGS -> SettingsScreen(
                            viewModel = viewModel,
                            onNavigateToAttackChain = { isAttackChainOpen = true }
                        )
                        MainAppTab.PROFILE -> ProfileScreen(
                            viewModel = viewModel,
                            onClose = { currentTab = MainAppTab.HOME }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CyberBottomNav(
    currentTab: MainAppTab,
    appLanguage: AppLanguage = AppLanguage.ENGLISH,
    onTabSelected: (MainAppTab) -> Unit
) {
    Surface(
        color = CyberBgSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainAppTab.values().forEach { tab ->
                val isSelected = currentTab == tab
                val itemColor = if (isSelected) ElectricCyan else CyberTextMuted
                val tabTitle = when (tab) {
                    MainAppTab.HOME -> com.example.ui.util.AppStrings.tabHome(appLanguage)
                    MainAppTab.DIAL_CONTACTS -> com.example.ui.util.AppStrings.tabDialer(appLanguage)
                    MainAppTab.VOICE_RECORDINGS -> com.example.ui.util.AppStrings.tabVault(appLanguage)
                    MainAppTab.SECURITY_SETTINGS -> com.example.ui.util.AppStrings.tabSecurity(appLanguage)
                    MainAppTab.PROFILE -> com.example.ui.util.AppStrings.tabProfile(appLanguage)
                }

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) ElectricCyan.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("nav_tab_${tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tabTitle,
                        tint = itemColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = tabTitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = itemColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.5.sp
                        )
                    )
                }
            }
        }
    }
}
