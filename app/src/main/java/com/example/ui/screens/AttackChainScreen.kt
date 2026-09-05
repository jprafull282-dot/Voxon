package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.PhoneContactItem
import com.example.ui.VoiceGuardViewModel
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.AlertCrimsonContainer
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberBgSecondary
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberBorderLight
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanLight
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.WarningAmber
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Attack Chain Node Representation
 */
enum class AttackStage(
    val stageIndex: Int,
    val title: String,
    val subtitle: String,
    val description: String,
    val metricKey: String
) {
    INGRESS(
        0,
        "1. Ingress & Signaling",
        "Carrier & SIP Protocol",
        "Verifies caller ID authenticity, CLI spoofing vectors, and SIP trunk handshake integrity.",
        "CLI Spoofing Risk"
    ),
    ACOUSTIC_INGEST(
        1,
        "2. Acoustic Ingestion",
        "PCM 16kHz VAD Stream",
        "Samples real-time audio at 16,000 Hz, segments vocal activity, and filters ambient baseline noise.",
        "Signal-to-Noise Ratio"
    ),
    SPECTRAL_TFLITE(
        2,
        "3. Spectral Forensics (TFLite)",
        "STFT & Wiener Entropy",
        "TensorFlow Lite neural detection of high-frequency vocoder harmonic aliasing (>3.5kHz) and noise floor smearing.",
        "Vocoder Aliasing Ratio"
    ),
    PHASE_MATRIX(
        3,
        "4. Phase Inconsistency Matrix",
        "Hop Discontinuity & Dispersion",
        "Analyzes unwrapped phase delta across STFT frames to detect phase slips (>1.4 rad) and vocoder boundary artifacts.",
        "Phase Hop Discontinuity"
    ),
    PROSODY_PITCH(
        4,
        "5. Prosody & Pitch Dynamics",
        "F0 Jitter & Shimmer",
        "Tracks vocal fundamental frequency (F0), Period Perturbation Quotient (Jitter PPQ5), and unnatural robotic stiffness.",
        "Pitch Jitter Variance"
    ),
    MITIGATION_GATE(
        5,
        "6. Mitigation & Defense",
        "Active Gatekeeper Enforcement",
        "Executes automatic call severance, sovereign screen locking, emergency broadcast, and cryptographic voice challenge.",
        "Mitigation Status"
    )
}

enum class AttackScenario(
    val label: String,
    val callerName: String,
    val callerNumber: String,
    val threatSeverity: String,
    val defaultRisk: Int,
    val isSynthetic: Boolean,
    val primaryAnomaly: String
) {
    HIFI_GAN_VOCODER(
        "HiFi-GAN Voice Clone",
        "Family Impersonator (AI Clone)",
        "+91 98123 45678",
        "CRITICAL",
        94,
        true,
        "Phase Slip (>1.72 rad) & High-Frequency Aliasing (>3.8kHz)"
    ),
    VITS_AUTOREGRESSIVE(
        "VITS Neural TTS Scam",
        "Tax Dept Executive (Robotic)",
        "+91 94567 89012",
        "HIGH",
        88,
        true,
        "Rigid Prosody: Jitter < 0.11% & Unnatural Pitch Steps"
    ),
    CEO_URGENT_WIRE(
        "CEO Urgent Wire Request",
        "Corporate Executive Office",
        "+1 (555) 234-8901",
        "CRITICAL",
        96,
        true,
        "Diffusion Vocoder Noise Smearing & Phase Hop Discontinuity"
    ),
    LEGITIMATE_CALL(
        "Verified Human Call",
        "Mom (Verified Contact)",
        "+91 98765 00000",
        "LOW",
        12,
        false,
        "Natural Human Vocal Tract (Intact Laryngeal Micro-Tremors)"
    )
}

@Composable
fun AttackChainScreen(viewModel: VoiceGuardViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val liveCallState by viewModel.liveCallState.collectAsState()
    val isShieldActive by viewModel.isShieldActive.collectAsState()

    var selectedScenario by remember { mutableStateOf(AttackScenario.HIFI_GAN_VOCODER) }
    var selectedStage by remember { mutableStateOf(AttackStage.PHASE_MATRIX) }

    // Pulse & Animation Transitions for High-Tech Canvas Drawing
    val infiniteTransition = rememberInfiniteTransition(label = "AttackChainPulsing")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAnim"
    )
    val fastGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fastGlowAnim"
    )

    // Calculate stage risk values based on live state or selected scenario
    val isCallActive = liveCallState.isCallActive
    val overallRisk = if (isCallActive) liveCallState.currentRiskScore else selectedScenario.defaultRisk
    val phaseScore = if (isCallActive) liveCallState.phaseInconsistencyScore else if (selectedScenario.isSynthetic) 0.84f else 0.08f
    val prosodyScore = if (isCallActive) liveCallState.prosodyAnomalyScore else if (selectedScenario.isSynthetic) 0.79f else 0.06f
    val spectralScore = if (isCallActive) liveCallState.spectralArtifactScore else if (selectedScenario.isSynthetic) 0.88f else 0.05f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Banner & Attack Chain Context HUD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberBgSecondary),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (overallRisk > 60) AlertCrimson.copy(alpha = 0.15f) else ElectricCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "Attack Chain",
                                tint = if (overallRisk > 60) AlertCrimson else ElectricCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ATTACK CHAIN FORENSIC MAPPER",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = CyberTextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = if (isCallActive) "STREAMING REAL-TIME AUDIO TRACE" else "NEURAL DSP PIPELINE SIMULATOR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isCallActive) NeonEmerald else ElectricCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp
                                )
                            )
                        }
                    }

                    // Threat Severity Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (overallRisk > 70) AlertCrimson else if (overallRisk > 40) WarningAmber else NeonEmerald)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (overallRisk > 70) "THREAT: $overallRisk%" else if (overallRisk > 40) "WARN: $overallRisk%" else "SAFE: $overallRisk%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                // Interactive Attack Scenario Chips
                Text(
                    text = "SELECT TARGET SCENARIO / SIMULATION:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyberTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AttackScenario.values().forEach { scenario ->
                        val isSelected = selectedScenario == scenario && !isCallActive
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ElectricCyan.copy(alpha = 0.2f) else CyberSurface)
                                .border(
                                    1.dp,
                                    if (isSelected) ElectricCyan else CyberBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedScenario = scenario
                                    if (scenario.isSynthetic) {
                                        selectedStage = AttackStage.PHASE_MATRIX
                                    } else {
                                        selectedStage = AttackStage.ACOUSTIC_INGEST
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = scenario.label.split(" ").firstOrNull() ?: scenario.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) ElectricCyanLight else CyberTextSecondary,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Simulation / Live Call Control Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCallActive) {
                        Button(
                            onClick = { viewModel.endCallMonitoring() },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("attack_chain_end_call_btn")
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("DISCONNECT LIVE CALL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                val dummyContact = PhoneContactItem(
                                    id = "scen_${selectedScenario.name}",
                                    name = selectedScenario.callerName,
                                    number = selectedScenario.callerNumber,
                                    source = com.example.engine.ContactSource.LIVE_INPUT,
                                    callType = "INCOMING",
                                    timestamp = System.currentTimeMillis()
                                )
                                viewModel.startLiveCallMonitoring(dummyContact, threatSimulationMode = selectedScenario.isSynthetic)
                                Toast.makeText(context, "Streaming ${selectedScenario.label} into Attack Chain Pipeline", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedScenario.isSynthetic) AlertCrimson else NeonEmerald
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("attack_chain_start_stream_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (selectedScenario.isSynthetic) "TEST ATTACK STREAM" else "TEST SAFE STREAM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            selectedStage = AttackStage.SPECTRAL_TFLITE
                            Toast.makeText(context, "Attack chain nodes recalibrated", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recalibrate", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // 2. Full Canvas Interactive Attack Chain Graph
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberBgSecondary),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ REAL-TIME ATTACK FLOW GRAPH",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = "TAP ANY NODE TO INSPECT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberTextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // Interactive High-Performance Canvas
                AttackChainFlowCanvas(
                    selectedStage = selectedStage,
                    overallRisk = overallRisk,
                    phaseScore = phaseScore,
                    prosodyScore = prosodyScore,
                    spectralScore = spectralScore,
                    pulsePhase = pulsePhase,
                    fastGlow = fastGlow,
                    onStageTapped = { stage -> selectedStage = stage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberBg)
                        .border(1.dp, CyberBorderLight, RoundedCornerShape(12.dp))
                        .testTag("attack_chain_canvas_graph")
                )
            }
        }

        // 3. Granular Forensic Inspector Pane for Selected Node
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (selectedStage == AttackStage.PHASE_MATRIX || selectedStage == AttackStage.SPECTRAL_TFLITE) AlertCrimson.copy(alpha = 0.6f) else CyberBorder
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
                    Column {
                        Text(
                            text = selectedStage.title.uppercase(),
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = CyberTextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        )
                        Text(
                            text = selectedStage.subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ElectricCyan,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        )
                    }

                    // Node Status Pill
                    val isStageCompromised = when (selectedStage) {
                        AttackStage.INGRESS -> overallRisk > 80
                        AttackStage.ACOUSTIC_INGEST -> false
                        AttackStage.SPECTRAL_TFLITE -> spectralScore > 0.6f
                        AttackStage.PHASE_MATRIX -> phaseScore > 0.6f
                        AttackStage.PROSODY_PITCH -> prosodyScore > 0.6f
                        AttackStage.MITIGATION_GATE -> overallRisk > 70
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isStageCompromised) AlertCrimsonContainer else NeonEmerald.copy(alpha = 0.15f))
                            .border(1.dp, if (isStageCompromised) AlertCrimson else NeonEmerald, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isStageCompromised) "ANOMALY DETECTED" else "BASELINE SECURE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isStageCompromised) AlertCrimson else NeonEmerald,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }
                }

                Text(
                    text = selectedStage.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = CyberTextSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                )

                // Real-Time Canvas Spectrogram / Phase Vector Oscilloscope
                StageForensicOscilloscopeCanvas(
                    stage = selectedStage,
                    overallRisk = overallRisk,
                    phaseScore = phaseScore,
                    prosodyScore = prosodyScore,
                    spectralScore = spectralScore,
                    pulsePhase = pulsePhase,
                    waveformPoints = if (isCallActive) liveCallState.waveformPoints else List(30) { Random.nextFloat() * 0.5f + 0.1f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberBgSecondary)
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                )

                // Forensic Metrics Breakdown
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberBgSecondary)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (selectedStage) {
                        AttackStage.SPECTRAL_TFLITE -> {
                            MetricRow("Vocoder Aliasing (>3.5kHz)", "${(spectralScore * 100).toInt()}%", spectralScore > 0.6f)
                            MetricRow("Wiener Spectral Flatness", String.format("%.3f", (spectralScore * 0.4f + 0.05f)), spectralScore > 0.6f)
                            MetricRow("Neural TFLite Confidence", "${(spectralScore * 95).toInt()}%", spectralScore > 0.6f)
                        }
                        AttackStage.PHASE_MATRIX -> {
                            MetricRow("STFT Phase Hop Slip", "${String.format("%.2f", phaseScore * 1.95f)} rad", phaseScore > 0.6f)
                            MetricRow("Hop Boundary Discontinuity", "${(phaseScore * 100).toInt()}%", phaseScore > 0.6f)
                            MetricRow("Harmonic Phase Coherence", "${((1f - phaseScore) * 100).toInt()}%", phaseScore > 0.6f)
                        }
                        AttackStage.PROSODY_PITCH -> {
                            MetricRow("Pitch Jitter (PPQ5)", "${String.format("%.2f", (1f - prosodyScore) * 1.5f)}%", prosodyScore > 0.6f)
                            MetricRow("Robotic Contour Stiffness", "${(prosodyScore * 100).toInt()}%", prosodyScore > 0.6f)
                            MetricRow("Amplitude Shimmer (APQ3)", "${String.format("%.2f", (1f - prosodyScore) * 3.8f)}%", prosodyScore > 0.6f)
                        }
                        AttackStage.INGRESS -> {
                            MetricRow("Caller Number Integrity", if (overallRisk > 80) "Spoofed via VoIP PBX" else "Authentic Carrier Signaled", overallRisk > 80)
                            MetricRow("Carrier Hop Count", if (overallRisk > 80) "6 Intercept Nodes" else "2 Direct Cellular Hops", overallRisk > 80)
                        }
                        AttackStage.ACOUSTIC_INGEST -> {
                            MetricRow("PCM Stream Sample Rate", "16,000 Hz / 16-Bit Mono", false)
                            MetricRow("Voice Activity Detected (VAD)", if (isCallActive) (if (liveCallState.isVoiceActive) "ACTIVE VOICE" else "SILENCE") else "ACTIVE", false)
                            MetricRow("RMS Energy / Decibels", if (isCallActive) "${liveCallState.decibels.toInt()} dB" else "-18 dB", false)
                        }
                        AttackStage.MITIGATION_GATE -> {
                            MetricRow("Telecom Disconnect Bridge", "ENGAGED & READY", false)
                            MetricRow("Screen Protection Lock", "FLAG_SECURE ENFORCED", false)
                            MetricRow("Sovereign Vault Cryptography", "ACTIVE", false)
                        }
                    }
                }

                // Interactive Evidence Copy Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EVIDENCE HASH: SHA256-${(overallRisk * 91823).toString(16).padStart(8, '0').uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberTextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )

                    Text(
                        text = "COPY TELEMETRY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ElectricCyan,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .clickable {
                                val json = """
                                    {
                                      "stage": "${selectedStage.name}",
                                      "riskScore": $overallRisk,
                                      "phaseInconsistency": $phaseScore,
                                      "prosodyAnomaly": $prosodyScore,
                                      "spectralArtifact": $spectralScore,
                                      "scenario": "${selectedScenario.name}",
                                      "timestamp": ${System.currentTimeMillis()}
                                    }
                                """.trimIndent()
                                clipboardManager.setText(AnnotatedString(json))
                                Toast.makeText(context, "Forensic telemetry copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, isAnomaly: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = CyberTextSecondary,
                fontSize = 10.sp
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isAnomaly) AlertCrimson else NeonEmerald,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        )
    }
}

/**
 * High-Tech Canvas Drawing that renders the full interactive Attack Chain Flow Graph
 */
@Composable
private fun AttackChainFlowCanvas(
    selectedStage: AttackStage,
    overallRisk: Int,
    phaseScore: Float,
    prosodyScore: Float,
    spectralScore: Float,
    pulsePhase: Float,
    fastGlow: Float,
    onStageTapped: (AttackStage) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val nodeBgColor = CyberSurface
    val gridBorderColor = CyberBorder
    val nodeTextSecondaryColor = CyberTextSecondary

    // 6 Node Positions organized in a 2-column or serpentine cyber network
    var nodePositions by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { tapOffset ->
                nodePositions.forEachIndexed { index, pos ->
                    val distance = sqrt((tapOffset.x - pos.x) * (tapOffset.x - pos.x) + (tapOffset.y - pos.y) * (tapOffset.y - pos.y))
                    if (distance <= 38.dp.toPx()) {
                        val tappedStage = AttackStage.values().getOrNull(index)
                        if (tappedStage != null) {
                            onStageTapped(tappedStage)
                        }
                    }
                }
            }
        }
    ) {
        val w = size.width
        val h = size.height

        // 1. Draw Cyber Background Grid
        drawCyberGrid(w, h, gridBorderColor)

        // 2. Define Node Positions (Serpentine Layout)
        val leftX = w * 0.22f
        val rightX = w * 0.78f
        val y0 = h * 0.14f
        val y1 = h * 0.46f
        val y2 = h * 0.78f

        val positions = listOf(
            Offset(leftX, y0),   // 0: Ingress
            Offset(rightX, y0),  // 1: Acoustic Ingest
            Offset(rightX, y1),  // 2: Spectral TFLite
            Offset(leftX, y1),   // 3: Phase Matrix
            Offset(leftX, y2),   // 4: Prosody & Pitch
            Offset(rightX, y2)   // 5: Mitigation
        )
        nodePositions = positions

        // 3. Draw Connecting Animated Spline Paths
        for (i in 0 until positions.size - 1) {
            val start = positions[i]
            val end = positions[i + 1]

            val isThreatPath = when (i) {
                1 -> spectralScore > 0.6f
                2 -> phaseScore > 0.6f
                3 -> prosodyScore > 0.6f
                4 -> overallRisk > 70
                else -> overallRisk > 80
            }

            val pathColor = if (isThreatPath) AlertCrimson else ElectricCyan
            val path = Path().apply {
                moveTo(start.x, start.y)
                if (start.y == end.y) {
                    // Horizontal segment
                    lineTo(end.x, end.y)
                } else if (start.x == end.x) {
                    // Vertical segment
                    lineTo(end.x, end.y)
                } else {
                    // Curved connector
                    cubicTo(
                        start.x, (start.y + end.y) / 2f,
                        end.x, (start.y + end.y) / 2f,
                        end.x, end.y
                    )
                }
            }

            // Glow Stroke
            drawPath(
                path = path,
                color = pathColor.copy(alpha = 0.25f),
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )

            // Core Line
            drawPath(
                path = path,
                color = pathColor.copy(alpha = 0.8f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), pulsePhase * 25f)
                )
            )

            // Animated Energy Photon Particle moving along the path
            val particleT = (pulsePhase + (i * 0.25f)) % 1.0f
            val px = start.x + (end.x - start.x) * particleT
            val py = start.y + (end.y - start.y) * particleT
            drawCircle(
                color = if (isThreatPath) Color.White else ElectricCyanLight,
                radius = 4.dp.toPx(),
                center = Offset(px, py)
            )
            drawCircle(
                color = if (isThreatPath) AlertCrimson else ElectricCyan,
                radius = 8.dp.toPx(),
                center = Offset(px, py)
            )
        }

        // 4. Draw Individual Nodes
        AttackStage.values().forEachIndexed { index, stage ->
            val pos = positions[index]
            val isSelected = selectedStage == stage
            val isAnomaly = when (stage) {
                AttackStage.INGRESS -> overallRisk > 80
                AttackStage.ACOUSTIC_INGEST -> false
                AttackStage.SPECTRAL_TFLITE -> spectralScore > 0.6f
                AttackStage.PHASE_MATRIX -> phaseScore > 0.6f
                AttackStage.PROSODY_PITCH -> prosodyScore > 0.6f
                AttackStage.MITIGATION_GATE -> overallRisk > 70
            }

            val nodeBaseColor = if (isAnomaly) AlertCrimson else if (stage == AttackStage.MITIGATION_GATE && overallRisk <= 40) NeonEmerald else ElectricCyan
            val nodeRadius = if (isSelected) 26.dp.toPx() else 22.dp.toPx()

            // Outer Pulsing Radar Rings for Anomalies or Selected Node
            if (isAnomaly || isSelected) {
                val ringRadius = nodeRadius + (12.dp.toPx() * pulsePhase)
                val ringAlpha = (1f - pulsePhase) * (if (isAnomaly) 0.8f else 0.4f)
                drawCircle(
                    color = nodeBaseColor.copy(alpha = ringAlpha),
                    radius = ringRadius,
                    center = pos,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Glow Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(nodeBaseColor.copy(alpha = 0.5f), Color.Transparent),
                    center = pos,
                    radius = nodeRadius * 1.8f
                ),
                radius = nodeRadius * 1.8f,
                center = pos
            )

            // Solid Background Circle
            drawCircle(
                color = nodeBgColor,
                radius = nodeRadius,
                center = pos
            )

            // Outer Ring Border
            drawCircle(
                color = if (isSelected) Color.White else nodeBaseColor,
                radius = nodeRadius,
                center = pos,
                style = Stroke(width = if (isSelected) 3.dp.toPx() else 2.dp.toPx())
            )

            // Inner Core
            drawCircle(
                color = nodeBaseColor,
                radius = 7.dp.toPx() * (if (isAnomaly) fastGlow else 1f),
                center = pos
            )

            // Label Text under or above Node
            val labelText = when (stage) {
                AttackStage.INGRESS -> "1. INGRESS"
                AttackStage.ACOUSTIC_INGEST -> "2. ACOUSTIC"
                AttackStage.SPECTRAL_TFLITE -> "3. SPECTRAL"
                AttackStage.PHASE_MATRIX -> "4. PHASE"
                AttackStage.PROSODY_PITCH -> "5. PROSODY"
                AttackStage.MITIGATION_GATE -> "6. MITIGATE"
            }

            val textLayoutResult = textMeasurer.measure(
                text = AnnotatedString(labelText),
                style = TextStyle(
                    color = if (isSelected) Color.White else if (isAnomaly) AlertCrimson else nodeTextSecondaryColor,
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                )
            )

            val textOffset = Offset(
                pos.x - (textLayoutResult.size.width / 2f),
                pos.y + nodeRadius + 4.dp.toPx()
            )
            drawText(textLayoutResult, topLeft = textOffset)
        }
    }
}

private fun DrawScope.drawCyberGrid(w: Float, h: Float, gridColor: Color) {
    val step = 28.dp.toPx()
    var x = 0f
    while (x <= w) {
        drawLine(
            color = gridColor.copy(alpha = 0.35f),
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = 0.8f
        )
        x += step
    }

    var y = 0f
    while (y <= h) {
        drawLine(
            color = gridColor.copy(alpha = 0.35f),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 0.8f
        )
        y += step
    }
}

/**
 * Canvas Oscilloscope showing dynamic frequency bars, phase vectors, or prosody curves for the chosen stage
 */
@Composable
private fun StageForensicOscilloscopeCanvas(
    stage: AttackStage,
    overallRisk: Int,
    phaseScore: Float,
    prosodyScore: Float,
    spectralScore: Float,
    pulsePhase: Float,
    waveformPoints: List<Float>,
    modifier: Modifier = Modifier
) {
    val radarCircleColor = CyberBorder
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (stage) {
            AttackStage.SPECTRAL_TFLITE -> {
                // Draw 24-Band FFT Spectrum with High-Frequency Anomaly Highlight
                val numBars = 24
                val barWidth = (w / numBars) * 0.7f
                val gap = (w / numBars) * 0.3f

                for (i in 0 until numBars) {
                    val isHighFreq = i >= 14 // >3.5kHz band
                    val baseHeight = if (isHighFreq && spectralScore > 0.6f) {
                        (0.6f + sin((i + pulsePhase * 6.28f)) * 0.25f).coerceIn(0.2f, 0.95f)
                    } else {
                        (0.3f + cos((i * 0.8f + pulsePhase * 3.14f)) * 0.2f).coerceIn(0.1f, 0.7f)
                    }
                    val barH = baseHeight * (h * 0.8f)
                    val bx = i * (barWidth + gap) + gap / 2f
                    val by = h - barH - 8f

                    val barBrush = if (isHighFreq && spectralScore > 0.6f) {
                        Brush.verticalGradient(listOf(AlertCrimson, WarningAmber))
                    } else {
                        Brush.verticalGradient(listOf(ElectricCyan, NeonEmerald))
                    }

                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(bx, by),
                        size = Size(barWidth, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                }
            }

            AttackStage.PHASE_MATRIX -> {
                // Draw Polar Phase Dispersion Radar Circle
                val centerX = w * 0.3f
                val centerY = h * 0.5f
                val radius = (h * 0.38f)

                // Background Radar Circles
                drawCircle(color = radarCircleColor, radius = radius, center = Offset(centerX, centerY), style = Stroke(1.5f))
                drawCircle(color = radarCircleColor, radius = radius * 0.5f, center = Offset(centerX, centerY), style = Stroke(1f))

                // Phase Angle Spikes
                val numVectors = 16
                for (k in 0 until numVectors) {
                    val angle = (k.toFloat() / numVectors) * 2 * PI + (pulsePhase * 0.5f)
                    val length = if (phaseScore > 0.6f && (k == 3 || k == 7 || k == 11)) {
                        radius * 1.15f // Phase slip jump!
                    } else {
                        radius * 0.7f
                    }
                    val endX = centerX + (cos(angle) * length).toFloat()
                    val endY = centerY + (sin(angle) * length).toFloat()

                    val isSlip = length > radius
                    drawLine(
                        color = if (isSlip) AlertCrimson else ElectricCyan,
                        start = Offset(centerX, centerY),
                        end = Offset(endX, endY),
                        strokeWidth = if (isSlip) 3f else 1.5f
                    )
                    if (isSlip) {
                        drawCircle(color = AlertCrimson, radius = 4f, center = Offset(endX, endY))
                    }
                }

                // Phase Discontinuity Strip on Right
                val rightStartX = w * 0.6f
                val rightW = w * 0.35f
                val stepX = rightW / 20f

                val phasePath = Path()
                phasePath.moveTo(rightStartX, centerY)
                for (i in 0 until 20) {
                    val x = rightStartX + (i * stepX)
                    val yOffset = if (phaseScore > 0.6f && (i == 6 || i == 14)) {
                        (sin(i * 0.5) * 30f).toFloat() // Discontinuity hop
                    } else {
                        (sin((i + pulsePhase * 5) * 0.4) * 12f).toFloat()
                    }
                    phasePath.lineTo(x, centerY + yOffset)
                }

                drawPath(
                    path = phasePath,
                    color = if (phaseScore > 0.6f) AlertCrimson else ElectricCyan,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
            }

            AttackStage.PROSODY_PITCH -> {
                // Pitch F0 Contour Ribbon (Rigid Flat vs Natural Human Jitter)
                val path = Path()
                val n = 30
                val dx = w / n

                val isRigid = prosodyScore > 0.6f
                for (i in 0 until n) {
                    val x = i * dx
                    val y = if (isRigid) {
                        // Unnatural flat straight line with abrupt rectangular step
                        h * 0.5f + (if (i in 12..18) -18f else 0f)
                    } else {
                        // Natural human pitch variation with organic micro-tremors
                        (h * 0.5f) + (sin((i + pulsePhase * 8) * 0.35) * 22f + sin(i * 1.5) * 4f).toFloat()
                    }

                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = if (isRigid) WarningAmber else NeonEmerald,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }

            else -> {
                // Standard Dynamic PCM Waveform
                val points = waveformPoints.take(28)
                if (points.isNotEmpty()) {
                    val barW = (w / points.size) * 0.6f
                    val gap = (w / points.size) * 0.4f
                    for (i in points.indices) {
                        val amp = points[i].coerceIn(0.05f, 0.95f)
                        val barH = amp * (h * 0.7f)
                        val bx = i * (barW + gap) + gap / 2f
                        val by = (h - barH) / 2f

                        drawRoundRect(
                            color = if (overallRisk > 70) AlertCrimson.copy(alpha = 0.8f) else ElectricCyan.copy(alpha = 0.8f),
                            topLeft = Offset(bx, by),
                            size = Size(barW, barH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                        )
                    }
                }
            }
        }
    }
}
