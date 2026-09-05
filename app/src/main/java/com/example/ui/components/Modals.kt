package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.IncidentEntity
import com.example.data.model.AnalyzedCallEntity
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.WarningAmber

@Composable
fun ActiveChallengeDialog(
    callerName: String,
    riskScore: Int,
    status: String,
    onVerifySuccess: () -> Unit,
    onVerifyFail: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, AlertCrimson, RoundedCornerShape(16.dp))
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(AlertCrimson.copy(alpha = 0.2f))
                        .border(1.dp, AlertCrimson, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = AlertCrimson,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "ZERO-TRUST CALLER CHALLENGE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                )

                Text(
                    text = "Caller identity uncertain (Risk Score $riskScore%). The AI Voice & Continuous Speaker baseline detected synthetic anomalies.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = CyberTextSecondary,
                        fontSize = 13.sp
                    )
                )

                // Challenge Information box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurface)
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "TARGET CALLER: $callerName",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "METHOD: Out-of-Band Biometric / Challenge-Response Token Verification",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = CyberTextPrimary,
                            fontSize = 12.sp
                        )
                    )
                }

                if (status == "VERIFIED") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonEmerald.copy(alpha = 0.2f))
                            .padding(10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = NeonEmerald)
                        Text(
                            text = "Identity Verified! Call Trust Restored.",
                            color = NeonEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                } else if (status == "FAILED") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AlertCrimson.copy(alpha = 0.2f))
                            .padding(10.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = AlertCrimson)
                        Text(
                            text = "Verification Failed! Threat Blocked.",
                            color = AlertCrimson,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onVerifyFail,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("challenge_reject_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertCrimson),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(AlertCrimson))
                    ) {
                        Text("Reject & Block", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onVerifySuccess,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("challenge_verify_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color(0xFF05101E))
                    ) {
                        Text("Approve Auth", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun IncidentDetailDialog(
    incident: IncidentEntity,
    onResolve: (IncidentEntity) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "INCIDENT FORENSIC RECORD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ElectricCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = incident.callerNumber,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (incident.riskScore >= 80) AlertCrimson.copy(alpha = 0.2f)
                                else WarningAmber.copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${incident.riskScore}% RISK",
                            color = if (incident.riskScore >= 80) AlertCrimson else WarningAmber,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }

                // Attack Story Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "🚨 ATTACK STORY GENERATOR",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = WarningAmber,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = incident.attackStory,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = CyberTextPrimary,
                            fontSize = 13.sp
                        )
                    )
                }

                // Forensic Authenticity Matrix
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🧬 ACOUSTIC AUTHENTICITY PROFILE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    ForensicMetricRow("Spectral Anomaly", incident.spectralAnomaly)
                    ForensicMetricRow("Phase Consistency", incident.phaseConsistency)
                    ForensicMetricRow("Prosody Naturalness", incident.prosodyNaturalness)
                    ForensicMetricRow("AI Voice Probability", "${(incident.aiProbability * 100).toInt()}%")
                    ForensicMetricRow("Speaker Baseline Match", "${(incident.speakerConfidence * 100).toInt()}%")
                }

                // Cryptographic Proof Hash
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF090E1A))
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "CRYPTOGRAPHIC EVIDENCE SHA-256",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberTextMuted,
                            fontSize = 9.sp
                        )
                    )
                    Text(
                        text = incident.evidenceHash,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = NeonEmerald,
                            fontSize = 10.sp
                        )
                    )
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onDelete(incident.id) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertCrimson)
                    ) {
                        Text("Delete", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { onResolve(incident) },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color(0xFF05101E))
                    ) {
                        Text(if (incident.isResolved) "Resolved ✓" else "Mark Resolved", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ForensicMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(color = CyberTextSecondary, fontSize = 12.sp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = when (value) {
                    "HIGH", "CRITICAL", "FRAUD_CRITICAL" -> AlertCrimson
                    "LOW", "SAFE", "AUTHENTIC", "VERIFIED_SAFE" -> NeonEmerald
                    "MEDIUM", "SUSPICIOUS", "MODERATE" -> WarningAmber
                    else -> ElectricCyan
                }
            )
        )
    }
}

@Composable
fun AnalyzedCallDetailDialog(
    call: AnalyzedCallEntity,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isCritical = call.securityRiskLevel == "CRITICAL" || call.riskScore >= 75
    val isSafe = call.securityRiskLevel == "VERIFIED_SAFE" || call.securityRiskLevel == "LOW"

    val headerColor = when {
        isSafe -> NeonEmerald
        isCritical -> AlertCrimson
        else -> WarningAmber
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI ANALYZED CALL REPORT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ElectricCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = call.callerLabel,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = call.phoneNumber,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CyberTextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(headerColor.copy(alpha = 0.2f))
                            .border(1.dp, headerColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${call.securityScore}/100",
                                color = headerColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "SECURITY",
                                color = headerColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        }
                    }
                }

                // AI Security Risk Verdict Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(headerColor.copy(alpha = 0.12f))
                        .border(1.dp, headerColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "RISK LEVEL: ${call.securityRiskLevel}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = headerColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Threat: ${call.threatType}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CyberTextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Text(
                        text = call.status,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = headerColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                // Multi-Model Forensics Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🤖 AI MODEL BREAKDOWN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    ForensicMetricRow("Engine Pipeline", call.aiModelNames)
                    ForensicMetricRow("TFLite AI Voice Prob", "${(call.tfliteAiProbability * 100).toInt()}%")
                    ForensicMetricRow("Spectral Anomaly", call.tfliteSpectralAnomaly)
                    ForensicMetricRow("Vocoder Artifacts", call.tfliteVocoderSignature)
                    ForensicMetricRow("Gemini Intent Category", call.geminiIntentCategory)
                    ForensicMetricRow("Gemini Fraud Risk", "${call.geminiFraudRiskScore}%")
                    ForensicMetricRow("Gemini Verdict", call.geminiSecurityVerdict)
                }

                // Transcript Snippet
                if (call.transcriptSnippet.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurface)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🎙️ ANALYZED AUDIO TRANSCRIPT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = WarningAmber,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "\"${call.transcriptSnippet}\"",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CyberTextPrimary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // AI Forensic Rationale
                if (call.aiVerdictSummary.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurface)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🛡️ AI VERDICT SUMMARY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ElectricCyan,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = call.aiVerdictSummary,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CyberTextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // Cryptographic Proof Hash
                if (call.evidenceHash.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF090E1A))
                            .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "CRYPTOGRAPHIC EVIDENCE SHA-256",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyberTextMuted,
                                fontSize = 9.sp
                            )
                        )
                        Text(
                            text = call.evidenceHash,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = NeonEmerald,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onDelete(call.id) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertCrimson)
                    ) {
                        Text("Delete Log", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF05101E))
                    ) {
                        Text("Close Report", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
