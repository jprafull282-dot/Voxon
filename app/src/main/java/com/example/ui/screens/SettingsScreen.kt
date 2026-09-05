package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SecurityFeatureType
import com.example.ui.VoiceGuardViewModel
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberBgSecondary
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberNeonGreen
import com.example.ui.theme.CyberNeonRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonEmerald

/**
 * Security Dashboard & Feature Hub.
 * Lists only REAL, verified security modules.
 * Every feature card is interactive and opens its dedicated in-depth analysis view.
 */
@Composable
fun SettingsScreen(
    viewModel: VoiceGuardViewModel,
    onNavigateToAttackChain: () -> Unit = {}
) {
    var selectedFeatureForAnalysis by remember { mutableStateOf<SecurityFeatureType?>(null) }

    if (selectedFeatureForAnalysis != null) {
        FeatureAnalysisScreen(
            featureType = selectedFeatureForAnalysis!!,
            viewModel = viewModel,
            onBack = { selectedFeatureForAnalysis = null }
        )
        return
    }

    val isShieldActive by viewModel.isShieldActive.collectAsState()
    val autoHangupEnabled by viewModel.autoHangupMaxRiskEnabled.collectAsState()
    val financialSentinelEnabled by viewModel.financialSentinelEnabled.collectAsState()
    val vibrationAlertEnabled by viewModel.vibrationAlertEnabled.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CyberBg
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "SECURITY DASHBOARD",
                        color = ElectricCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Active Protection & Forensic Engines",
                        color = CyberTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Master Shield Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                    border = BorderStroke(1.dp, if (isShieldActive) CyberNeonGreen.copy(alpha = 0.5f) else CyberCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isShieldActive) CyberNeonGreen.copy(alpha = 0.15f) else CyberNeonRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isShieldActive) CyberNeonGreen else CyberNeonRed,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Master Telephony Call Shield",
                                color = CyberTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isShieldActive) "Real-time AI protection armed" else "Protection paused",
                                color = if (isShieldActive) CyberNeonGreen else CyberTextMuted,
                                fontSize = 12.sp
                            )
                        }

                        Switch(
                            checked = isShieldActive,
                            onCheckedChange = { viewModel.toggleShield() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = CyberNeonGreen,
                                uncheckedThumbColor = CyberTextMuted,
                                uncheckedTrackColor = CyberSurface
                            )
                        )
                    }
                }
            }

            item {
                Text(
                    text = "VERIFIED SECURITY ENGINES (TAP FOR FORENSIC TELEMETRY)",
                    color = CyberTextMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Clickable Feature Cards
            item {
                FeatureCard(
                    title = "Voice Authenticity & Anti-Spoofing",
                    subtitle = "Aurigin.ai streaming deepfake detection via secure server proxy",
                    badge = "ACTIVE",
                    icon = Icons.Default.RecordVoiceOver,
                    tint = ElectricCyan,
                    onClick = { selectedFeatureForAnalysis = SecurityFeatureType.VOICE_AUTHENTICITY }
                )
            }

            item {
                FeatureCard(
                    title = "Conversation Intent Analysis",
                    subtitle = "Real-time semantic threat classifier & extortion profiling",
                    badge = "ACTIVE",
                    icon = Icons.Default.Psychology,
                    tint = NeonAmber,
                    onClick = { selectedFeatureForAnalysis = SecurityFeatureType.CONVERSATION_SCAM }
                )
            }

            item {
                FeatureCard(
                    title = "Multi-Signal Rolling Risk Engine",
                    subtitle = "Temporal observation window preventing false alarms from single anomalies",
                    badge = "ACTIVE",
                    icon = Icons.Default.Speed,
                    tint = CyberNeonGreen,
                    onClick = { selectedFeatureForAnalysis = SecurityFeatureType.ROLLING_RISK_ENGINE }
                )
            }

            item {
                FeatureCard(
                    title = "Audio Preprocessing & VAD",
                    subtitle = "16kHz PCM normalization and voice activity gating to filter silence",
                    badge = "ACTIVE",
                    icon = Icons.Default.GraphicEq,
                    tint = ElectricCyan,
                    onClick = { selectedFeatureForAnalysis = SecurityFeatureType.VOICE_ACTIVITY_DETECTOR }
                )
            }

            item {
                FeatureCard(
                    title = "Mobile Screen & Memory Guard",
                    subtitle = "Hardware-enforced FLAG_SECURE preventing screenshot extortion",
                    badge = "ACTIVE",
                    icon = Icons.Default.Shield,
                    tint = CyberNeonGreen,
                    onClick = { selectedFeatureForAnalysis = SecurityFeatureType.SCREEN_CAPTURE_GUARD }
                )
            }

            item {
                FeatureCard(
                    title = "Zero-Audio Forensic Vault",
                    subtitle = "Privacy-first metadata persistence with SHA-256 evidence integrity",
                    badge = "ACTIVE",
                    icon = Icons.Default.Security,
                    tint = ElectricCyan,
                    onClick = { selectedFeatureForAnalysis = SecurityFeatureType.ZERO_STORAGE_VAULT }
                )
            }

            item {
                Text(
                    text = "ADVANCED USER CONTROLS",
                    color = CyberTextMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Auto-Hangup: Explicitly user-configurable, NOT defaulted to true!
            item {
                ConfigSettingCard(
                    title = "Auto-Terminate On Confirmed Threat",
                    subtitle = "Automatically hang up call only after consecutive confirmed high-risk frames",
                    icon = Icons.Default.Block,
                    tint = AlertCrimson,
                    isChecked = autoHangupEnabled,
                    onCheckedChange = { viewModel.toggleAutoHangupMaxRisk(it) }
                )
            }

            item {
                ConfigSettingCard(
                    title = "Financial Extortion Sentinel",
                    subtitle = "Specialized alerts for unauthorized UPI transfer demands",
                    icon = Icons.Default.Warning,
                    tint = NeonAmber,
                    isChecked = financialSentinelEnabled,
                    onCheckedChange = { viewModel.toggleFinancialSentinel(it) }
                )
            }

            item {
                ConfigSettingCard(
                    title = "Tactile Haptic Warning",
                    subtitle = "Discreet vibration sequence when suspicious signals emerge",
                    icon = Icons.Default.Notifications,
                    tint = CyberTextSecondary,
                    isChecked = vibrationAlertEnabled,
                    onCheckedChange = { viewModel.toggleVibrationAlert(it) }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
        border = BorderStroke(1.dp, CyberCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        color = CyberTextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        color = tint.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = badge,
                            color = tint,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = CyberTextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "View Details",
                tint = CyberTextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ConfigSettingCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
        border = BorderStroke(1.dp, CyberCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = CyberTextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = CyberTextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = tint,
                    uncheckedThumbColor = CyberTextMuted,
                    uncheckedTrackColor = CyberSurface
                )
            )
        }
    }
}

private val AlertCrimson = Color(0xFFEF4444)
