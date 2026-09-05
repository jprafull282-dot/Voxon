package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ContactSource
import com.example.engine.PhoneContactItem
import com.example.ui.AppLanguage
import com.example.ui.LiveCallState
import com.example.ui.VoiceGuardViewModel
import com.example.ui.components.ActiveChallengeDialog
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.ForensicMetricRow
import com.example.ui.components.GeminiFraudAnalysisCard
import com.example.ui.components.RiskGauge
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberBgSecondary
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.WarningAmber
import com.example.ui.util.AppStrings

@Composable
fun ShieldScreen(viewModel: VoiceGuardViewModel) {
    val isShieldActive by viewModel.isShieldActive.collectAsState()
    val liveCallState by viewModel.liveCallState.collectAsState()
    val backgroundCallStatus by viewModel.backgroundCallStatus.collectAsState()
    val deviceState by viewModel.deviceState.collectAsState()
    val incidents by viewModel.incidents.collectAsState()
    val geminiResult by viewModel.geminiFraudAnalysisResult.collectAsState()
    val isGeminiAnalyzing by viewModel.isGeminiAnalyzing.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()

    val currentSnippet = liveCallState.scenario?.sampleTranscript
        ?: "URGENT NOTICE: Your bank debit card is suspended due to suspicious cyber activity. Read back your 6-digit OTP immediately to avoid account freeze."

    val infiniteTransition = rememberInfiniteTransition(label = "hero_glow")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    if (liveCallState.isChallengeModalVisible && liveCallState.currentContact != null) {
        ActiveChallengeDialog(
            challengeType = "Zero-Trust Voice Challenge (पासफ़्रेज़ प्रमाणीकरण)",
            challengeStatus = liveCallState.challengeStatus,
            onDismiss = { viewModel.closeChallengeModal() },
            onVerifyVoice = { viewModel.completeChallenge(true) }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. REDESIGNED HERO DEFENSE COCKPIT CARD
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.5.dp,
                        if (isShieldActive) NeonEmerald.copy(alpha = 0.45f) else AlertCrimson.copy(alpha = 0.45f),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Bar with Status Pill & Live Ping
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isShieldActive) NeonEmerald else AlertCrimson)
                            )
                            Text(
                                text = if (isShieldActive) "REAL-TIME PROTECTION 24/7" else "PROTECTION PAUSED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isShieldActive) NeonEmerald else AlertCrimson,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.6.sp
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isShieldActive) NeonEmerald.copy(alpha = 0.12f) else AlertCrimson.copy(alpha = 0.12f))
                                .border(1.dp, if (isShieldActive) NeonEmerald.copy(alpha = 0.35f) else AlertCrimson.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isShieldActive) "🛡️ ACTIVE" else "⚠️ MUTED",
                                color = if (isShieldActive) NeonEmerald else AlertCrimson,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Interactive Master Holographic Shield Button with Dynamic Pulse
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .scale(if (isShieldActive) pulseGlow else 1f)
                            .clip(CircleShape)
                            .background(
                                if (isShieldActive)
                                    Brush.radialGradient(listOf(NeonEmerald.copy(alpha = 0.35f), NeonEmerald.copy(alpha = 0.05f), Color.Transparent))
                                else
                                    Brush.radialGradient(listOf(AlertCrimson.copy(alpha = 0.35f), AlertCrimson.copy(alpha = 0.05f), Color.Transparent))
                            )
                            .border(2.5.dp, if (isShieldActive) NeonEmerald else AlertCrimson, CircleShape)
                            .clickable { viewModel.toggleShield() }
                            .testTag("master_shield_toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isShieldActive) Icons.Default.Shield else Icons.Default.Warning,
                            contentDescription = "Master Shield Toggle",
                            tint = if (isShieldActive) NeonEmerald else AlertCrimson,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    // Title & Action Descriptor
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = AppStrings.homeHeroTitle(appLanguage, isShieldActive),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = CyberTextPrimary,
                                letterSpacing = 0.5.sp,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                        Text(
                            text = AppStrings.homeHeroDesc(appLanguage, isShieldActive),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CyberTextSecondary,
                                fontSize = 11.5.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }

                    // 4 Core Subsystem Tiles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SubsystemChip(
                            icon = Icons.Default.Mic,
                            label = "Vocoder DSP",
                            subLabel = if (isShieldActive) "16kHz Active" else "Offline",
                            isActive = isShieldActive,
                            modifier = Modifier.weight(1f)
                        )
                        SubsystemChip(
                            icon = Icons.Default.GraphicEq,
                            label = "Prosody Drift",
                            subLabel = if (isShieldActive) "Phase Scan" else "Offline",
                            isActive = isShieldActive,
                            modifier = Modifier.weight(1f)
                        )
                        SubsystemChip(
                            icon = Icons.Default.Psychology,
                            label = "Scam Intent",
                            subLabel = if (isShieldActive) "NLP Guard" else "Offline",
                            isActive = isShieldActive,
                            modifier = Modifier.weight(1f)
                        )
                        SubsystemChip(
                            icon = Icons.Outlined.Fingerprint,
                            label = "Zero-Trust",
                            subLabel = if (isShieldActive) "TEE Vault" else "Offline",
                            isActive = isShieldActive,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. LIVE TELEPHONY CALL INTERCEPT BANNER (Triggered during background call)
        if (backgroundCallStatus.isMonitoring) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            2.dp,
                            if (backgroundCallStatus.isThreatDetected) AlertCrimson else ElectricCyan,
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (backgroundCallStatus.isThreatDetected) AlertCrimson.copy(alpha = 0.2f) else ElectricCyan.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = if (backgroundCallStatus.isThreatDetected) AlertCrimson else ElectricCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (appLanguage == AppLanguage.HINDI) "लाइव कॉल टेलीफ़ोनी इंटरसेप्टेड" else "LIVE TELEPHONY HOOK ACTIVE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (backgroundCallStatus.isThreatDetected) AlertCrimson else ElectricCyan,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                    Text(
                                        text = "${backgroundCallStatus.callerName} (${backgroundCallStatus.callerNumber})",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = CyberTextPrimary, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            RiskGauge(score = backgroundCallStatus.currentRiskScore, sizeDp = 64)
                        }

                        Text(
                            text = if (backgroundCallStatus.isThreatDetected)
                                "🚨 ALERT: High-confidence synthetic voice detected! Screen overlay & heads-up warnings dispatched."
                            else
                                "🛡️ Streaming audio telemetry analyzed on-device via TFLite DSP.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (backgroundCallStatus.isThreatDetected) AlertCrimson else CyberTextSecondary,
                                fontSize = 12.sp
                            )
                        )

                        Button(
                            onClick = { viewModel.stopBackgroundCallProtection() },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson),
                            modifier = Modifier.fillMaxWidth().testTag("bg_hangup_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (appLanguage == AppLanguage.HINDI) "कॉल समाप्त करें / हैंग अप" else "Disengage Call Hook / Hang Up", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. LIVE SANDBOX ACTIVE CALL CARD (Interactive Simulation or Mic Feed)
        item {
            AnimatedVisibility(
                visible = liveCallState.isCallActive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                liveCallState.currentContact?.let { contact ->
                    LiveCallActiveCard(
                        contact = contact,
                        liveState = liveCallState,
                        appLanguage = appLanguage,
                        onEndCall = { viewModel.endCallMonitoring() },
                        onChallenge = { viewModel.openChallengeModal() }
                    )
                }
            }
        }

        // 4. RAPID THREAT SIMULATION LAB
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                            Text(
                                text = AppStrings.quickActionsTitle(appLanguage),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CyberTextPrimary,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ElectricCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("1-TAP TEST", color = ElectricCyan, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = if (appLanguage == AppLanguage.HINDI)
                            "वॉक्सन के न्यूरल वोकोडर और इंटेंट एनालिसिस को टेस्ट करने के लिए सिमुलेशन चलाएं।"
                        else
                            "Trigger real-time adversarial scenarios to test Voxen's acoustic vocoder and intent arbitration engine.",
                        style = MaterialTheme.typography.bodySmall.copy(color = CyberTextSecondary, fontSize = 12.sp)
                    )

                    // Scenario Action Buttons Grid (Digital Arrest, Bijli Bill, Exec Clone, Verified Safe)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScenarioButton(
                            title = "🇮🇳 डिजिटल अरेस्ट",
                            subtitle = "Police Extortion Scam",
                            color = AlertCrimson,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val item = PhoneContactItem(
                                    "sim_digital_arrest",
                                    "CBI Inspector Sharma (Delhi)",
                                    "+91 88002 91044",
                                    ContactSource.LIVE_INPUT
                                )
                                viewModel.startLiveCallMonitoring(item, threatSimulationMode = true)
                            }
                        )

                        ScenarioButton(
                            title = "⚡ बिजली बिल फ्रॉड",
                            subtitle = "Hinglish Disconnect Scam",
                            color = WarningAmber,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val item = PhoneContactItem(
                                    "sim_bijli_bill",
                                    "Electricity Dept Officer",
                                    "+91 98765 11223",
                                    ContactSource.LIVE_INPUT
                                )
                                viewModel.startLiveCallMonitoring(item, threatSimulationMode = true)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScenarioButton(
                            title = "💼 Executive Clone",
                            subtitle = "Urgent CEO Wire Fraud",
                            color = AlertCrimson.copy(alpha = 0.85f),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val item = PhoneContactItem(
                                    "sim_exec_wire",
                                    "CEO Managing Director",
                                    "+1 (555) 019-2834",
                                    ContactSource.LIVE_INPUT
                                )
                                viewModel.startLiveCallMonitoring(item, threatSimulationMode = true)
                            }
                        )

                        ScenarioButton(
                            title = "✅ Doctor Recall",
                            subtitle = "Verified Natural Voice",
                            color = NeonEmerald,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val item = PhoneContactItem(
                                    "sim_safe",
                                    "Dr. Priya Sharma (Apollo)",
                                    "+91 94455 66778",
                                    ContactSource.CONTACT_BOOK
                                )
                                viewModel.startLiveCallMonitoring(item, threatSimulationMode = false)
                            }
                        )
                    }
                }
            }
        }

        // 5. GEMINI AI DEEP COGNITIVE FRAUD REASONING CARD
        item {
            GeminiFraudAnalysisCard(
                result = geminiResult,
                isAnalyzing = isGeminiAnalyzing,
                transcriptSnippet = currentSnippet,
                onAnalyzeRequested = {
                    val callerName = liveCallState.currentContact?.name ?: "Unknown Incoming Call"
                    val callerNum = liveCallState.currentContact?.number ?: "+91 98765 11001"
                    viewModel.analyzeCallSnippetWithGemini(
                        transcript = currentSnippet,
                        callerName = callerName,
                        callerNumber = callerNum,
                        durationSeconds = liveCallState.durationSeconds.coerceAtLeast(12)
                    )
                }
            )
        }

        // 6. ZERO-TRUST HARDWARE INTEGRITY & METRICS
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                            Icon(Icons.Default.Security, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                            Text(
                                text = AppStrings.systemHealthTitle(appLanguage),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CyberTextPrimary,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonEmerald.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("100% HEALTH", color = NeonEmerald, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SecurityMatrixCard(
                            icon = Icons.Outlined.Fingerprint,
                            title = "Hardware Keystore",
                            status = "ATTESTED",
                            statusColor = NeonEmerald,
                            detail = "TEE Secure Enclave",
                            modifier = Modifier.weight(1f)
                        )

                        SecurityMatrixCard(
                            icon = Icons.Default.Lock,
                            title = "Screen Protection",
                            status = "ENFORCED",
                            statusColor = NeonEmerald,
                            detail = "FLAG_SECURE Active",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SecurityMatrixCard(
                            icon = Icons.Default.Mic,
                            title = "TFLite DSP Engine",
                            status = "16kHz REALTIME",
                            statusColor = ElectricCyan,
                            detail = "Neural Phase VAD",
                            modifier = Modifier.weight(1f)
                        )

                        SecurityMatrixCard(
                            icon = Icons.Default.Shield,
                            title = "Cloud Threat Feed",
                            status = "SYNCED",
                            statusColor = NeonEmerald,
                            detail = "Zero-Day Rules",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SubsystemChip(
    icon: ImageVector,
    label: String,
    subLabel: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) NeonEmerald.copy(alpha = 0.08f) else CyberSurface)
            .border(
                1.dp,
                if (isActive) NeonEmerald.copy(alpha = 0.35f) else CyberBorder,
                RoundedCornerShape(10.dp)
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) NeonEmerald else CyberTextMuted,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = if (isActive) CyberTextPrimary else CyberTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = subLabel,
                color = if (isActive) NeonEmerald else CyberTextMuted,
                fontSize = 8.5.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ScenarioButton(
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 12.5.sp
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = CyberTextMuted,
                    fontSize = 10.5.sp
                )
            )
        }
    }
}

@Composable
private fun SecurityMatrixCard(
    icon: ImageVector,
    title: String,
    status: String,
    statusColor: Color,
    detail: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        modifier = modifier
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(18.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = status,
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            )

            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = CyberTextMuted,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
fun LiveCallActiveCard(
    contact: PhoneContactItem,
    liveState: LiveCallState,
    appLanguage: AppLanguage = AppLanguage.ENGLISH,
    onEndCall: () -> Unit,
    onChallenge: () -> Unit
) {
    val isCritical = liveState.currentRiskScore >= 80

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, if (isCritical) AlertCrimson else ElectricCyan, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Caller & Status Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isCritical) AlertCrimson.copy(alpha = 0.2f) else ElectricCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = if (isCritical) AlertCrimson else ElectricCyan
                        )
                    }
                    Column {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CyberTextPrimary
                            )
                        )
                        Text(
                            text = "${contact.number} • Active Monitoring (${liveState.durationSeconds}s)",
                            style = MaterialTheme.typography.labelSmall.copy(color = CyberTextSecondary)
                        )
                    }
                }

                RiskGauge(score = liveState.currentRiskScore, sizeDp = 76)
            }

            // Real-Time Audio Waveform & VAD Indicator
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = if (liveState.isVoiceActive) NeonEmerald else CyberTextMuted, modifier = Modifier.size(14.dp))
                        Text(
                            text = if (liveState.isVoiceActive) "VAD: SPEECH ACTIVE" else "VAD: SILENCE / BACKGROUND",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (liveState.isVoiceActive) NeonEmerald else CyberTextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp
                            )
                        )
                    }
                    Text(
                        text = "Energy: ${liveState.decibels.toInt()} dB",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = CyberTextSecondary, fontSize = 9.5.sp)
                    )
                }

                AudioWaveformVisualizer(
                    waveforms = liveState.waveformPoints,
                    isDeepfake = isCritical
                )
            }

            // Forensics Telemetry
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberBgSecondary)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "⚡ TENSORFLOW LITE SPECTRAL & PHASE FORENSICS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp
                    )
                )

                ForensicMetricRow("Phase Inconsistency Index", String.format("%.2f", liveState.phaseInconsistencyScore), isCritical)
                ForensicMetricRow("Prosody & Pitch Jitter Drift", String.format("%.2f", liveState.prosodyAnomalyScore), isCritical)
                ForensicMetricRow("Neural Vocoder Signature", liveState.detectedVocoderSignature)
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onChallenge,
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber, contentColor = Color.Black),
                    modifier = Modifier.weight(1f).testTag("issue_challenge_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (appLanguage == AppLanguage.HINDI) "चुनौती दें" else "Issue Challenge", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onEndCall,
                    colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson),
                    modifier = Modifier.weight(1f).testTag("end_call_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (appLanguage == AppLanguage.HINDI) "कॉल समाप्त करें" else "End Call", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
