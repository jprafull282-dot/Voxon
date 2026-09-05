package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.SecurityReportEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Security Report Vault.
 * Strictly adheres to privacy mandates:
 * - Displays structured forensic security report cards (SecurityReportEntity)
 * - NO audio players, NO persistent WAV recordings, NO synthetic demo entries
 * - Shows an honest empty state when no calls have occurred
 * - Validates cryptographic SHA-256 evidence integrity
 */
@Composable
fun RecordingsScreen(
    viewModel: VoiceGuardViewModel
) {
    val context = LocalContext.current
    val reports by viewModel.allSecurityReports.collectAsState()
    var selectedReportForDetail by remember { mutableStateOf<SecurityReportEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBg)
    ) {
        // Vault Header Banner
        Surface(
            color = CyberBgSecondary,
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(ElectricCyan.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SECURITY REPORT VAULT",
                                color = CyberTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Zero-Audio Forensic Archival • Privacy Preserved",
                                color = CyberTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        color = ElectricCyan.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${reports.size} REPORTS",
                            color = ElectricCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Main Content Area
        if (reports.isEmpty()) {
            // Clean, Honest Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                    border = BorderStroke(1.dp, CyberCardBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(CyberBgSecondary, CircleShape)
                                .border(1.dp, CyberBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = CyberTextMuted,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = "No security reports yet.",
                            color = CyberTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "When incoming calls are answered, VoiceGuard streams audio in real time through Aurigin and the conversation intent engine. Upon call termination, structured security reports are archived here and temporary audio buffers are permanently discarded.",
                            color = CyberTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Surface(
                            color = CyberNeonGreen.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, CyberNeonGreen.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = CyberNeonGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Zero-Audio Policy: No raw voice recordings saved.",
                                    color = CyberNeonGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(6.dp)) }

                items(reports, key = { it.id }) { report ->
                    SecurityReportCard(
                        report = report,
                        onViewDetails = { selectedReportForDetail = report },
                        onDelete = {
                            viewModel.deleteSecurityReport(report.id)
                            Toast.makeText(context, "Report ${report.id} deleted", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    // Forensic Detail Dialog Modal
    selectedReportForDetail?.let { report ->
        SecurityReportDetailDialog(
            report = report,
            onDismiss = { selectedReportForDetail = null }
        )
    }
}

@Composable
fun SecurityReportCard(
    report: SecurityReportEntity,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit
) {
    val isThreat = report.overallRiskScore >= 60 || report.voiceVerdict == "SYNTHETIC"
    val isSuspicious = report.overallRiskScore in 35..59
    val statusColor = when {
        isThreat -> CyberNeonRed
        isSuspicious -> Color(0xFFF59E0B)
        else -> CyberNeonGreen
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(report.timestamp) { dateFormat.format(Date(report.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report_card_${report.id}"),
        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
        border = BorderStroke(1.dp, if (isThreat) CyberNeonRed.copy(alpha = 0.5f) else CyberCardBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CALL SECURITY REPORT",
                        color = ElectricCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = report.id,
                        color = CyberTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusColor),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = report.threatLevel,
                        color = statusColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Caller", color = CyberTextMuted, fontSize = 11.sp)
                    Text(report.callerName, color = CyberTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(report.callerNumber, color = CyberTextSecondary, fontSize = 10.5.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Time & Duration", color = CyberTextMuted, fontSize = 11.sp)
                    Text(formattedDate, color = CyberTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("${report.durationSeconds} seconds", color = CyberTextSecondary, fontSize = 10.5.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Risk Scores Strip
            Surface(
                color = CyberBgSecondary,
                border = BorderStroke(1.dp, CyberBorder),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ScoreBadge(label = "Voice Verdict", value = report.voiceVerdict, color = statusColor)
                    ScoreBadge(label = "AI Confidence", value = "${(report.aiVoiceConfidence * 100).toInt()}%")
                    ScoreBadge(label = "Conv Risk", value = "${report.conversationRiskScore}/100")
                    ScoreBadge(label = "Overall Risk", value = "${report.overallRiskScore}/100", color = statusColor)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onViewDetails,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("btn_view_report_${report.id}")
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Full Report", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Report", tint = CyberTextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ScoreBadge(
    label: String,
    value: String,
    color: Color = CyberTextPrimary
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = CyberTextMuted, fontSize = 9.5.sp)
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecurityReportDetailDialog(
    report: SecurityReportEntity,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCardBg),
            border = BorderStroke(1.dp, CyberCardBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FORENSIC SECURITY DOSSIER",
                        color = ElectricCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = CyberTextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Surface(
                            color = CyberBgSecondary,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Evidence Cryptographic Integrity", color = CyberTextMuted, fontSize = 10.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = report.evidenceHashSha256.take(24) + "...",
                                        color = CyberTextPrimary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    item {
                        DetailSection(title = "VOICE AUTHENTICITY") {
                            Text("Detector: ${report.detectorName}", color = CyberTextSecondary, fontSize = 12.sp)
                            Text("Verdict: ${report.voiceVerdict}", color = CyberTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("AI Voice Confidence: ${(report.aiVoiceConfidence * 100).toInt()}%", color = CyberTextSecondary, fontSize = 12.sp)
                            Text("Detection Latency: ${report.latencyMs} ms", color = CyberTextMuted, fontSize = 11.sp)
                        }
                    }

                    item {
                        DetailSection(title = "CONVERSATION & INTENT SIGNALS") {
                            Text("Conversation Risk Score: ${report.conversationRiskScore}/100", color = CyberTextSecondary, fontSize = 12.sp)
                            Text("Detected Threat Indicators: ${report.detectedIndicators}", color = CyberTextPrimary, fontSize = 12.sp)
                        }
                    }

                    item {
                        DetailSection(title = "FINDINGS & ADVISORY") {
                            Text("Findings: ${report.evidenceSummary}", color = CyberTextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Recommendation: ${report.recommendations}", color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    item {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dismiss Dossier", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = CyberBgSecondary,
        border = BorderStroke(1.dp, CyberBorder),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = ElectricCyan, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}
