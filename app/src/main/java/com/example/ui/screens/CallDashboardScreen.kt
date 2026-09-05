package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockedCallerEntity
import com.example.engine.AudioCaptureManager
import com.example.engine.ContactSource
import com.example.engine.PhoneContactItem
import com.example.engine.ThreatLevel
import com.example.ui.TranscriptLine
import com.example.ui.VoiceGuardViewModel
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.AlertRed
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
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallDashboardScreen(
    viewModel: VoiceGuardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val liveCallState by viewModel.liveCallState.collectAsState()
    val isShieldActive by viewModel.isShieldActive.collectAsState()
    val isAudioCapturing by AudioCaptureManager.isCapturing.collectAsState()
    val userFraudLogs by viewModel.userFraudLogs.collectAsState()
    val firestoreLogStatus by viewModel.firestoreLogStatus.collectAsState()
    val currentSensitivity by viewModel.aiSensitivity.collectAsState()

    val totalProtectedMinutes by viewModel.totalProtectedMinutes.collectAsState()
    val blockedFraudAttempts by viewModel.blockedFraudAttempts.collectAsState()
    val blockedCallers by viewModel.blockedCallers.collectAsState()

    val isCallActive = liveCallState.isCallActive || isAudioCapturing
    val callerName = when {
        liveCallState.isCallActive -> liveCallState.currentContact?.name ?: "Unknown Caller"
        isAudioCapturing -> AudioCaptureManager.activeCallerName.value
        else -> "Standby (No Active Call)"
    }
    val callerNumber = when {
        liveCallState.isCallActive -> liveCallState.currentContact?.number ?: "+91 98765 43210"
        isAudioCapturing -> AudioCaptureManager.activeCallerNumber.value
        else -> "+91 98765 43210"
    }

    val riskScore = if (isCallActive) liveCallState.currentRiskScore else 8
    val durationSeconds = liveCallState.durationSeconds

    val listState = rememberLazyListState()
    LaunchedEffect(liveCallState.liveTranscript.size) {
        if (liveCallState.liveTranscript.isNotEmpty()) {
            listState.animateScrollToItem(liveCallState.liveTranscript.size - 1)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("call_dashboard_screen"),
        color = CyberBg
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            // 1. Live Call Status Header
            item {
                CallStatusHeaderCard(
                    isCallActive = isCallActive,
                    isShieldActive = isShieldActive,
                    isAudioCapturing = isAudioCapturing,
                    callerName = callerName,
                    callerNumber = callerNumber,
                    durationSeconds = durationSeconds,
                    riskScore = riskScore,
                    onSimulateThreat = {
                        val contact = PhoneContactItem(
                            id = "sim_threat_${System.currentTimeMillis()}",
                            name = "Suspected AI Voice Clone",
                            number = "+91 98765 43210",
                            source = ContactSource.LIVE_INPUT
                        )
                        viewModel.startLiveCallMonitoring(contact, threatSimulationMode = true)
                        AudioCaptureManager.startCaptureService(context, contact.name, contact.number)
                        Toast.makeText(context, "🚨 Live threat call simulation started", Toast.LENGTH_SHORT).show()
                    },
                    onSimulateSafe = {
                        val contact = PhoneContactItem(
                            id = "sim_safe_${System.currentTimeMillis()}",
                            name = "Verified Work Colleague",
                            number = "+91 94440 12345",
                            source = ContactSource.LIVE_INPUT
                        )
                        viewModel.startLiveCallMonitoring(contact, threatSimulationMode = false)
                        AudioCaptureManager.startCaptureService(context, contact.name, contact.number)
                        Toast.makeText(context, "✅ Verified safe call started", Toast.LENGTH_SHORT).show()
                    },
                    onDisconnect = {
                        viewModel.endCallMonitoring()
                        AudioCaptureManager.stopCaptureService(context)
                        Toast.makeText(context, "Call terminated safely", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 2. Real-Time Threat Level Indicators & Waveform Canvas
            item {
                ThreatLevelIndicatorsCard(
                    riskScore = riskScore,
                    liveCallState = liveCallState,
                    isCallActive = isCallActive,
                    onFireDeepfakeBanner = {
                        viewModel.triggerHighPriorityBanner(isDeepfake = true)
                        Toast.makeText(context, "🚨 Dispatched Deepfake Heads-Up Banner", Toast.LENGTH_SHORT).show()
                    },
                    onFireFraudBanner = {
                        viewModel.triggerHighPriorityBanner(isDeepfake = false)
                        Toast.makeText(context, "⚠️ Dispatched Fraud Intent Heads-Up Banner", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 3. Live Call Transcript Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isCallActive) NeonEmerald else CyberTextMuted)
                        )
                        Text(
                            text = "LIVE SPEECH TRANSCRIPT STREAM",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CyberTextPrimary,
                                letterSpacing = 0.8.sp
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElectricCyan.copy(alpha = 0.15f))
                            .border(1.dp, ElectricCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PII REDACTED • MASKED",
                            color = ElectricCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Empty Transcript Placeholder
            if (liveCallState.liveTranscript.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = CyberTextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = if (isCallActive) "Listening for speech on AudioRecord stream..." else "No active call transcript. Start a simulated or live call.",
                                style = MaterialTheme.typography.bodySmall.copy(color = CyberTextMuted)
                            )
                        }
                    }
                }
            } else {
                items(liveCallState.liveTranscript, key = { it.id }) { transcriptItem ->
                    TranscriptLineCard(transcript = transcriptItem)
                }
            }

            // 4. Gemini AI & Firestore Fraud Intent Log Actions
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberBgSecondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(20.dp))
                                Text(
                                    text = "FIRESTORE FRAUD INTENT VAULT",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CyberTextPrimary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                            Text(
                                text = "${userFraudLogs.size} Logged Incidents",
                                style = MaterialTheme.typography.labelSmall.copy(color = CyberTextMuted)
                            )
                        }

                        Text(
                            text = "Detected fraud metadata is securely logged in Firestore with masked phone numbers (+91 98*** **210) and SHA-256 correlation hashes for user review.",
                            style = MaterialTheme.typography.bodySmall.copy(color = CyberTextSecondary, fontSize = 11.sp)
                        )

                        if (!firestoreLogStatus.isNullOrEmpty()) {
                            Text(
                                text = firestoreLogStatus!!,
                                style = MaterialTheme.typography.labelSmall.copy(color = NeonEmerald, fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val fullTranscript = liveCallState.liveTranscript.joinToString("\n") { "${it.speaker}: ${it.text}" }
                                    viewModel.logFraudMetadataToFirestore(
                                        callerName = callerName,
                                        callerNumber = callerNumber,
                                        threatCategory = if (riskScore >= 70) "Deepfake & Coercive Extortion" else "Verified Monitored Stream",
                                        fraudRiskScore = riskScore,
                                        detectedTactics = listOf("Authority Impersonation", "Artificial Urgency", "OTP Demand"),
                                        transcript = fullTranscript.ifEmpty { "Real-time speech stream inspected by VoiceGuard." },
                                        acousticProbability = if (riskScore >= 70) 0.94f else 0.12f,
                                        onComplete = { success, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("log_to_firestore_button")
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log to Firestore", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.clearAllFraudLogs()
                                    Toast.makeText(context, "Local fraud logs cleared", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(0.7f)
                            ) {
                                Text("Clear Logs", color = CyberTextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // 5. Recent Logged Incidents in Firestore
            if (userFraudLogs.isNotEmpty()) {
                item {
                    Text(
                        text = "STORED FRAUD INTENT INCIDENTS (SECURE REVIEW)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberTextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    )
                }

                items(userFraudLogs.take(5), key = { it.id }) { log ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
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
                                Text(
                                    text = log.threatCategory,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = if (log.fraudRiskScore >= 70) AlertCrimson else NeonAmber,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (log.fraudRiskScore >= 70) AlertCrimson.copy(alpha = 0.2f) else NeonEmerald.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${log.fraudRiskScore}% RISK",
                                        color = if (log.fraudRiskScore >= 70) AlertCrimson else NeonEmerald,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Text(
                                text = "Caller: ${log.callerNumberMasked} • Language: ${log.languageDetected}",
                                style = MaterialTheme.typography.bodySmall.copy(color = CyberTextSecondary, fontSize = 11.sp)
                            )

                            Text(
                                text = "Transcript snippet: \"${log.transcriptSnippetMasked}\"",
                                style = MaterialTheme.typography.bodySmall.copy(color = CyberTextPrimary, fontSize = 11.5.sp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Hash: ${log.callerNumberHash.take(12)}...",
                                    style = MaterialTheme.typography.labelSmall.copy(color = CyberTextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                )
                                val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(log.timestamp))
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.labelSmall.copy(color = CyberTextMuted, fontSize = 10.sp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                SecurityProofSummaryCard(
                    totalProtectedMinutes = totalProtectedMinutes,
                    blockedFraudAttempts = blockedFraudAttempts,
                    blockedCallers = blockedCallers,
                    onUnblockCaller = { viewModel.unblockCaller(it) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(84.dp))
            }
        }

        // Floating 'End Call & Block' Button
        ExtendedFloatingActionButton(
            onClick = {
                viewModel.endCallAndBlockCaller(
                    callerName = callerName,
                    callerNumber = callerNumber,
                    reason = if (riskScore >= 60) "High-Risk Deepfake Attack Detected" else "Manual Block from Call Dashboard"
                )
                Toast.makeText(
                    context,
                    "🚨 Call Terminated & $callerNumber Added to Room Local Blocklist",
                    Toast.LENGTH_LONG
                ).show()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "End Call and Block Caller",
                    tint = Color.White
                )
            },
            text = {
                Text(
                    text = if (isCallActive) "END CALL & BLOCK" else "BLOCK CURRENT CALLER",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = Color.White
                )
            },
            containerColor = AlertCrimson,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp, pressedElevation = 12.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .testTag("end_call_and_block_button")
                .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
        )
    }
}
}

@Composable
fun SecurityProofSummaryCard(
    totalProtectedMinutes: Int,
    blockedFraudAttempts: Int,
    blockedCallers: List<BlockedCallerEntity>,
    onUnblockCaller: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("security_proof_summary_card")
            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Platform Security Proof",
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "SECURITY VALUE PROOF",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = CyberTextPrimary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "Tangible defense metrics backed by local Room database",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberTextMuted,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonEmerald.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "ROOM PERSISTED",
                        color = NeonEmerald,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = CyberBorder)

            // Two Big Metric Proof Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Metric 1: Total Protected Minutes
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberBgSecondary),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
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
                            Text(
                                text = "PROTECTED TIME",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyberTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        val hours = totalProtectedMinutes / 60
                        val mins = totalProtectedMinutes % 60
                        val displayStr = if (hours > 0) "${hours}h ${mins}m" else "${mins} mins"

                        Text(
                            text = displayStr,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = ElectricCyan,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                        )

                        Text(
                            text = "Total Protected Minutes across analyzed voice streams",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CyberTextMuted,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        )
                    }
                }

                // Metric 2: Blocked Fraud Attempts
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberBgSecondary),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, AlertCrimson.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
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
                            Text(
                                text = "BLOCKED FRAUD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyberTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = AlertCrimson,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = "$blockedFraudAttempts THREATS",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = AlertCrimson,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                        )

                        Text(
                            text = "Blocked Fraud Attempts & deepfake impersonations",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CyberTextMuted,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        )
                    }
                }
            }

            // Room Database Local Blocklist Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ROOM DATABASE LOCAL BLOCKLIST",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberTextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                    )
                    Text(
                        text = "${blockedCallers.size} Blocked",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (blockedCallers.isNotEmpty()) AlertCrimson else CyberTextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                if (blockedCallers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurface.copy(alpha = 0.5f))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No callers blocked yet. Tap \"End Call & Block\" during any call to immediately terminate and store the caller in Room DB.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CyberTextMuted,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        blockedCallers.take(4).forEach { blocked ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = blocked.callerName,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = CyberTextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = "${blocked.phoneNumber} • ${blocked.threatCategory}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = CyberTextSecondary,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onUnblockCaller(blocked.phoneNumber) },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text(
                                            text = "Unblock",
                                            fontSize = 11.sp,
                                            color = ElectricCyan
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

@Composable
private fun CallStatusHeaderCard(
    isCallActive: Boolean,
    isShieldActive: Boolean,
    isAudioCapturing: Boolean,
    callerName: String,
    callerNumber: String,
    durationSeconds: Int,
    riskScore: Int,
    onSimulateThreat: () -> Unit,
    onSimulateSafe: () -> Unit,
    onDisconnect: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (riskScore >= 70) AlertCrimson.copy(alpha = 0.6f) else ElectricCyan.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCallActive) {
                                    if (riskScore >= 70) AlertCrimson.copy(alpha = 0.2f) else NeonEmerald.copy(alpha = 0.2f)
                                } else CyberSurface
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCallActive) Icons.Default.Phone else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isCallActive) {
                                if (riskScore >= 70) AlertCrimson else NeonEmerald
                            } else ElectricCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = callerName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CyberTextPrimary
                            )
                        )
                        Text(
                            text = callerNumber,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CyberTextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }

                // Call Duration & Pulse Badge
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isCallActive) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AlertCrimson.copy(alpha = pulseAlpha))
                            )
                        }
                        val mins = durationSeconds / 60
                        val secs = durationSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", mins, secs),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = if (isCallActive) NeonEmerald else CyberTextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                    Text(
                        text = if (isCallActive) "LIVE PCM 16kHz" else "IDLE / READY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isCallActive) ElectricCyan else CyberTextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            HorizontalDivider(color = CyberBorder)

            // Status details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = if (isShieldActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isShieldActive) NeonEmerald else NeonAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isShieldActive) "Call Shield: Active" else "Call Shield: Disabled",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isShieldActive) NeonEmerald else NeonAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Text(
                    text = if (isAudioCapturing) "AudioRecord Stream: Connected" else "AudioRecord: Standby",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isAudioCapturing) ElectricCyan else CyberTextMuted,
                        fontSize = 10.sp
                    )
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isCallActive) {
                    Button(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_disconnect_button")
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DISCONNECT CALL IMMEDIATELY", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onSimulateThreat,
                        colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson.copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_threat_call_button")
                    ) {
                        Text("🚨 Simulate Threat Call", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onSimulateSafe,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald.copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_safe_call_button")
                    ) {
                        Text("✅ Simulate Safe Call", color = Color.Black, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreatLevelIndicatorsCard(
    riskScore: Int,
    liveCallState: com.example.ui.LiveCallState,
    isCallActive: Boolean,
    onFireDeepfakeBanner: () -> Unit,
    onFireFraudBanner: () -> Unit
) {
    val isCritical = riskScore >= 70 || liveCallState.audioAnomalyFlag?.contains("CRITICAL", ignoreCase = true) == true
    val isSuspicious = (riskScore in 35..69) || liveCallState.audioAnomalyFlag != null ||
        liveCallState.phaseInconsistencyScore > 0.4f || liveCallState.prosodyAnomalyScore > 0.4f
    val isThreatDetected = isCritical || isSuspicious

    val animatedRiskScore by animateIntAsState(
        targetValue = riskScore,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "animated_risk_score"
    )

    val meterColor by animateColorAsState(
        targetValue = when {
            isCritical -> AlertCrimson
            isSuspicious -> NeonAmber
            else -> NeonEmerald
        },
        animationSpec = tween(300),
        label = "meter_color"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "threat_visualizer")
    val dialPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCritical) 1.26f else if (isSuspicious) 1.15f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isCritical) 450 else if (isSuspicious) 800 else 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dial_pulse"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isCritical) 450 else if (isSuspicious) 800 else 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )
    val strobeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isCritical) 350 else 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "strobe_alpha"
    )
    val scanBeamX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_beam_x"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberBgSecondary),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                if (isThreatDetected) meterColor.copy(alpha = strobeAlpha) else meterColor.copy(alpha = 0.35f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Threat Level Title & Score Dial with Sonar Radar Halo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(if (isThreatDetected) dialPulseScale else 1f)
                                .clip(CircleShape)
                                .background(meterColor)
                        )
                        Text(
                            text = "REAL-TIME THREAT LEVEL",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = CyberTextPrimary,
                                letterSpacing = 0.8.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = when {
                            isCritical -> "CRITICAL: Synthetic Vocoder / Deepfake Detected"
                            isSuspicious -> "SUSPICIOUS: Acoustic Jitter & Coercive Script"
                            else -> "AUTHENTIC: Natural Human Vocal Tract"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = meterColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Dial Badge with Pulsing Sonar Halo
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(76.dp)
                ) {
                    // Expanding outer sonar wave
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .scale(dialPulseScale)
                            .clip(CircleShape)
                            .border(1.5.dp, meterColor.copy(alpha = haloAlpha), CircleShape)
                    )

                    // Secondary sonar ripple on threat
                    if (isThreatDetected) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .scale(dialPulseScale * 0.92f)
                                .clip(CircleShape)
                                .border(1.dp, meterColor.copy(alpha = haloAlpha * 0.7f), CircleShape)
                        )
                    }

                    // Central score dial badge
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(meterColor.copy(alpha = 0.16f))
                            .border(2.dp, meterColor.copy(alpha = strobeAlpha), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$animatedRiskScore%",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = meterColor,
                                    fontSize = 15.sp
                                )
                            )
                            Text(
                                text = "RISK",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 7.5.sp,
                                    color = meterColor,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            // 2. Immediate Threat Alert Banner (Slides down when suspicious pattern is identified)
            AnimatedVisibility(
                visible = isThreatDetected,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCritical) AlertCrimson.copy(alpha = 0.14f) else NeonAmber.copy(alpha = 0.14f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isCritical) AlertCrimson.copy(alpha = strobeAlpha) else NeonAmber.copy(alpha = strobeAlpha),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .scale(dialPulseScale)
                                .clip(CircleShape)
                                .background(meterColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCritical) Icons.Default.Warning else Icons.Default.Shield,
                                contentDescription = "Threat Alert",
                                tint = meterColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isCritical) "🚨 SYNTHETIC VOICE CLONE PATTERN IDENTIFIED" else "⚠️ SUSPICIOUS AUDIO PATTERN IDENTIFIED",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = meterColor,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.4.sp
                                )
                            )
                            Text(
                                text = liveCallState.audioAnomalyFlag
                                    ?: if (isCritical) "Neural vocoder phase cancellation detected in caller speech buffer."
                                    else "Acoustic prosody anomaly and coercive script detected.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = CyberTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Action: Do not disclose OTP, passwords, or banking credentials.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = meterColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }

            // 3. High-Precision Audio Stream Waveform Canvas with Spectral Anomaly Highlighting & Laser Scan
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    val wavePoints = liveCallState.waveformPoints.ifEmpty {
                        List(28) { 0.2f }
                    }
                    val gridBorderColor = CyberBorder

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    ) {
                        val count = wavePoints.size
                        val spacing = size.width / (count + 1)
                        val centerY = size.height / 2f

                        // Draw subtle background cyber grid lines
                        val gridAlpha = 0.15f
                        drawLine(
                            color = gridBorderColor.copy(alpha = gridAlpha),
                            start = Offset(0f, size.height * 0.25f),
                            end = Offset(size.width, size.height * 0.25f),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = gridBorderColor.copy(alpha = gridAlpha),
                            start = Offset(0f, centerY),
                            end = Offset(size.width, centerY),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = gridBorderColor.copy(alpha = gridAlpha),
                            start = Offset(0f, size.height * 0.75f),
                            end = Offset(size.width, size.height * 0.75f),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Draw audio spectrum frequency bars
                        for (i in 0 until count) {
                            val x = spacing * (i + 1)
                            val amp = (wavePoints[i] * (size.height / 2f)).coerceAtLeast(4f)
                            val isAnomalousBin = isThreatDetected && (i % 3 == 0 || wavePoints[i] > 0.55f)

                            val barColor = if (isAnomalousBin) {
                                AlertCrimson.copy(alpha = 0.95f)
                            } else {
                                meterColor.copy(alpha = 0.8f)
                            }

                            // Frequency bar
                            drawLine(
                                color = barColor,
                                start = Offset(x, centerY - amp),
                                end = Offset(x, centerY + amp),
                                strokeWidth = 3.dp.toPx()
                            )

                            // Anomaly spike cap pins when threat is identified
                            if (isAnomalousBin) {
                                drawCircle(
                                    color = AlertCrimson,
                                    radius = 2.5.dp.toPx(),
                                    center = Offset(x, centerY - amp)
                                )
                                drawCircle(
                                    color = AlertCrimson,
                                    radius = 2.5.dp.toPx(),
                                    center = Offset(x, centerY + amp)
                                )
                            }
                        }

                        // Real-time AI laser scan beam sweep
                        val scanX = scanBeamX * size.width
                        drawLine(
                            color = meterColor.copy(alpha = 0.7f),
                            start = Offset(scanX, 0f),
                            end = Offset(scanX, size.height),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Live Waveform Telemetry Status Bar
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
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(meterColor)
                            )
                            Text(
                                text = "16kHz SPECTRAL STREAM",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyberTextMuted,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Text(
                            text = if (isThreatDetected) "⚠️ ANOMALOUS SPECTRUM SPIKE DETECTED" else "STABLE ACOUSTIC CONTINUITY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isThreatDetected) meterColor else CyberTextMuted,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // 4. Detailed Forensic Telemetry Indicators with Live Gauge Fill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ForensicMetricPill(
                    title = "Phase Inconsistency",
                    value = String.format("%.0f%%", liveCallState.phaseInconsistencyScore * 100),
                    percentFill = liveCallState.phaseInconsistencyScore.coerceIn(0f, 1f),
                    isAnomalous = liveCallState.phaseInconsistencyScore > 0.4f,
                    modifier = Modifier.weight(1f)
                )
                ForensicMetricPill(
                    title = "Prosody Drift",
                    value = String.format("%.0f%%", liveCallState.prosodyAnomalyScore * 100),
                    percentFill = liveCallState.prosodyAnomalyScore.coerceIn(0f, 1f),
                    isAnomalous = liveCallState.prosodyAnomalyScore > 0.4f,
                    modifier = Modifier.weight(1f)
                )
                ForensicMetricPill(
                    title = "Vocoder Signature",
                    value = if (riskScore >= 50) "HiFi-GAN" else "Natural",
                    percentFill = if (riskScore >= 50) 0.88f else 0.12f,
                    isAnomalous = riskScore >= 50,
                    modifier = Modifier.weight(1f)
                )
            }

            // 5. Heads-Up Alert Banner Dispatchers for testing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onFireDeepfakeBanner,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("trigger_deepfake_banner_button")
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = AlertCrimson, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Deepfake Banner", color = AlertCrimson, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onFireFraudBanner,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("trigger_fraud_banner_button")
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fraud Banner", color = NeonAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ForensicMetricPill(
    title: String,
    value: String,
    percentFill: Float = 0f,
    isAnomalous: Boolean,
    modifier: Modifier = Modifier
) {
    val pillColor = if (isAnomalous) AlertCrimson else NeonEmerald

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        modifier = modifier.border(
            1.dp,
            if (isAnomalous) AlertCrimson.copy(alpha = 0.55f) else CyberBorder,
            RoundedCornerShape(8.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isAnomalous) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(AlertCrimson)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(color = CyberTextMuted, fontSize = 8.5.sp),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = pillColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Mini live gauge indicator bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(CyberBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percentFill)
                        .height(2.dp)
                        .background(pillColor)
                )
            }
        }
    }
}

@Composable
private fun TranscriptLineCard(transcript: TranscriptLine) {
    val isCaller = transcript.speaker == "CALLER"
    val isAI = transcript.speaker == "VOICEGUARD_AI"
    val isCritical = transcript.riskLevel == "CRITICAL"
    val isSuspicious = transcript.riskLevel == "SUSPICIOUS"

    val containerColor = when {
        isAI -> if (isCritical) AlertCrimson.copy(alpha = 0.15f) else ElectricCyan.copy(alpha = 0.1f)
        isCritical -> AlertCrimson.copy(alpha = 0.12f)
        isSuspicious -> NeonAmber.copy(alpha = 0.12f)
        else -> CyberCardBg
    }

    val borderColor = when {
        isCritical -> AlertCrimson.copy(alpha = 0.5f)
        isSuspicious -> NeonAmber.copy(alpha = 0.4f)
        isAI -> ElectricCyan.copy(alpha = 0.4f)
        else -> CyberBorder
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    isAI -> ElectricCyan.copy(alpha = 0.2f)
                                    isCaller -> AlertCrimson.copy(alpha = 0.2f)
                                    else -> NeonEmerald.copy(alpha = 0.2f)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = transcript.speaker,
                            color = when {
                                isAI -> ElectricCyan
                                isCaller -> AlertCrimson
                                else -> NeonEmerald
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp
                        )
                    }

                    if (isCritical) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AlertCrimson)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("FRAUD FLAGGED", color = Color.White, fontWeight = FontWeight.Black, fontSize = 8.5.sp)
                        }
                    }
                }

                val mins = transcript.timestampSeconds / 60
                val secs = transcript.timestampSeconds % 60
                Text(
                    text = String.format("+%02d:%02d", mins, secs),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyberTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            Text(
                text = transcript.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = CyberTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            )

            if (transcript.flaggedKeywords.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Trigger Words:", style = MaterialTheme.typography.labelSmall.copy(color = CyberTextMuted, fontSize = 9.sp))
                    transcript.flaggedKeywords.forEach { word ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AlertCrimson.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = word,
                                color = AlertCrimson,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
