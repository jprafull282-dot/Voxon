package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
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
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.ElectricCyan

/**
 * Dedicated In-Depth Feature Analysis Screen.
 * Opened when clicking any feature card from the Security Dashboard.
 * Displays real runtime telemetry, detector architecture, verified scores,
 * detected indicators, and actionable mitigation guidance.
 */
@Composable
fun FeatureAnalysisScreen(
    featureType: SecurityFeatureType,
    viewModel: VoiceGuardViewModel,
    onBack: () -> Unit
) {
    val liveCallState by viewModel.liveCallState.collectAsState()
    val detectorResult by viewModel.liveAnalysisDetectorResult.collectAsState()
    val evaluation by viewModel.liveAnalysisEvaluation.collectAsState()
    val scamResult by viewModel.liveAnalysisScamResult.collectAsState()
    val isShieldActive by viewModel.isShieldActive.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBg)
    ) {
        // Top Navigation Header
        Surface(
            color = CyberBgSecondary,
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("feature_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate Back",
                        tint = ElectricCyan
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = featureType.title,
                        color = CyberTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Forensic Telemetry & Runtime Verification",
                        color = CyberTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            // Feature Hero Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                    border = BorderStroke(1.dp, CyberCardBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(ElectricCyan.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                val icon = when (featureType) {
                                    SecurityFeatureType.VOICE_AUTHENTICITY -> Icons.Default.RecordVoiceOver
                                    SecurityFeatureType.CONVERSATION_SCAM -> Icons.Default.Psychology
                                    SecurityFeatureType.ROLLING_RISK_ENGINE -> Icons.Default.Speed
                                    SecurityFeatureType.VOICE_ACTIVITY_DETECTOR -> Icons.Default.GraphicEq
                                    SecurityFeatureType.SCREEN_CAPTURE_GUARD -> Icons.Default.Shield
                                    SecurityFeatureType.ZERO_STORAGE_VAULT -> Icons.Default.Security
                                }
                                Icon(icon, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(24.dp))
                            }

                            Surface(
                                color = if (isShieldActive) CyberNeonGreen.copy(alpha = 0.15f) else CyberNeonRed.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, if (isShieldActive) CyberNeonGreen else CyberNeonRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isShieldActive) "STATUS: ACTIVE" else "STATUS: PAUSED",
                                    color = if (isShieldActive) CyberNeonGreen else CyberNeonRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = featureType.subtitle,
                            color = CyberTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Real Runtime Metrics & System Architecture
            item {
                Text(
                    text = "ENGINE ARCHITECTURE & METRICS",
                    color = ElectricCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberBgSecondary),
                    border = BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (featureType) {
                            SecurityFeatureType.VOICE_AUTHENTICITY -> {
                                MetricRow(label = "Detector Engine", value = "Aurigin.ai Voice Deepfake API")
                                MetricRow(label = "Network Channel", value = "WSS Secure Backend Proxy (/ws/aurigin-stream)")
                                MetricRow(label = "Client Key Exposure", value = "ZERO (Keys isolated on server)")
                                MetricRow(label = "Analysis Sampling", value = "16,000 Hz, 16-Bit Mono PCM")
                                MetricRow(
                                    label = "Latest Verdict",
                                    value = detectorResult?.verdict ?: "MONITORING / INCONCLUSIVE",
                                    valueColor = if (detectorResult?.isSynthetic == true) CyberNeonRed else CyberNeonGreen
                                )
                                MetricRow(
                                    label = "AI Confidence Score",
                                    value = detectorResult?.let { "${(it.confidence * 100).toInt()}%" } ?: "Awaiting Speech"
                                )
                                MetricRow(
                                    label = "Streaming Latency",
                                    value = detectorResult?.let { "${it.latencyMs} ms" } ?: "< 50 ms target"
                                )
                            }
                            SecurityFeatureType.CONVERSATION_SCAM -> {
                                MetricRow(label = "Intent Engine", value = "Multilingual Semantic Threat Classifier")
                                MetricRow(label = "Analyzed Vectors", value = "Urgency, OTP Demand, Police/CBI Impersonation")
                                MetricRow(
                                    label = "Current Risk Score",
                                    value = scamResult?.let { "${it.conversationRiskScore}/100" } ?: "0/100 (Safe)",
                                    valueColor = if ((scamResult?.conversationRiskScore ?: 0) > 50) CyberNeonRed else CyberNeonGreen
                                )
                                MetricRow(
                                    label = "Flagged Keywords",
                                    value = scamResult?.flaggedKeywords?.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "None"
                                )
                            }
                            SecurityFeatureType.ROLLING_RISK_ENGINE -> {
                                MetricRow(label = "Temporal Smoothing", value = "5-Frame Consecutive Observation Window")
                                MetricRow(label = "Hysteresis Dampening", value = "Requires 3 confirmed anomaly frames")
                                MetricRow(
                                    label = "Aggregated Threat Level",
                                    value = evaluation?.riskLevel?.name ?: "MONITORING",
                                    valueColor = if (evaluation?.riskLevel == com.example.engine.RollingRiskEngine.RiskLevel.CRITICAL) CyberNeonRed else ElectricCyan
                                )
                                MetricRow(
                                    label = "False Positive Defense",
                                    value = "Enforced: Single anomalies never trigger alarms"
                                )
                            }
                            SecurityFeatureType.VOICE_ACTIVITY_DETECTOR -> {
                                MetricRow(label = "VAD Technique", value = "Energy RMS + Zero-Crossing Flux")
                                MetricRow(label = "Target Format", value = "Standard 16 kHz 16-Bit PCM Mono")
                                MetricRow(label = "Bandwidth Optimization", value = "Silence frames dropped before streaming")
                                MetricRow(label = "Hangover Smoothing", value = "3-Frame inter-syllable protection")
                            }
                            SecurityFeatureType.SCREEN_CAPTURE_GUARD -> {
                                MetricRow(label = "Android Security Flag", value = "WindowManager.LayoutParams.FLAG_SECURE")
                                MetricRow(label = "Protection Vector", value = "Screenshots, Screen Recordings, Recents Preview")
                                MetricRow(label = "State", value = "HARDWARE ENFORCED", valueColor = CyberNeonGreen)
                            }
                            SecurityFeatureType.ZERO_STORAGE_VAULT -> {
                                MetricRow(label = "Storage Policy", value = "Zero Raw Audio Persisted (Privacy-First)")
                                MetricRow(label = "Telemetry Stored", value = "Structured SecurityReportEntity Only")
                                MetricRow(label = "Evidence Verification", value = "Cryptographic SHA-256 Hash Digest")
                                MetricRow(label = "Database Engine", value = "Room 2.6 with destructive migration safety")
                            }
                        }
                    }
                }
            }

            // Evidence & Recommendations
            item {
                Text(
                    text = "FORENSIC ASSESSMENT & MITIGATION",
                    color = ElectricCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberBgSecondary),
                    border = BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Technical Findings:",
                            color = CyberTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = evaluation?.summary
                                ?: "The audio pipeline operates with continuous rolling telemetry. In the absence of confirmed synthetic vocoder signatures, calls maintain normal security standing.",
                            color = CyberTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Security Recommendation:",
                            color = CyberTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = evaluation?.recommendedAction
                                ?: "Maintain standard verification protocols. Never disclose banking passwords, UPI PINs, or one-time verification codes.",
                            color = ElectricCyan,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    valueColor: Color = CyberTextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = CyberTextMuted,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
