package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.engine.gemini.GeminiFraudIntentResult
import com.example.ui.theme.AlertCrimson
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

/**
 * GeminiFraudAnalysisCard
 *
 * Dedicated Material 3 UI component presenting real-time Gemini AI intent
 * scanning, behavioral fraud profiling, and dynamic Security Score evaluation.
 */
@Composable
fun GeminiFraudAnalysisCard(
    result: GeminiFraudIntentResult?,
    isAnalyzing: Boolean,
    transcriptSnippet: String,
    onAnalyzeRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gemini_glow")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gemini_pulse"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (result != null && result.securityScore < 50) AlertCrimson.copy(alpha = 0.7f)
                else if (isAnalyzing) ElectricCyan.copy(alpha = pulseGlow)
                else CyberBorder,
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Gemini AI Intelligence Banner
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
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        ElectricCyan.copy(alpha = 0.25f),
                                        Color(0xFF8A2BE2).copy(alpha = 0.35f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gemini AI",
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "GEMINI FRAUD INTENT ANALYZER",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = CyberTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = if (result?.isGeminiLiveApi == true) "gemini-3.5-flash • Intent Engine" else "Gemini Threat Engine • Active",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ElectricCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                // Security Score Pill
                if (result != null) {
                    val score = result.securityScore
                    val isSafe = score >= 70
                    val isSuspicious = score in 40..69
                    val scoreColor = if (isSafe) NeonEmerald else if (isSuspicious) WarningAmber else AlertCrimson

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(scoreColor.copy(alpha = 0.15f))
                            .border(1.dp, scoreColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "SECURITY SCORE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = scoreColor,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            )
                            Text(
                                text = "$score/100",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = scoreColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }
                }
            }

            // Audio Transcript Snippet Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberBgSecondary)
                    .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRANSCRIBED CALL SNIPPET",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = "16kHz PCM Stream",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ElectricCyan,
                            fontSize = 9.sp
                        )
                    )
                }

                Text(
                    text = "\"$transcriptSnippet\"",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                )
            }

            // Real-Time Analysis Output Block
            if (result != null) {
                val isHighFraud = result.fraudRiskScore >= 65
                val accentColor = if (isHighFraud) AlertCrimson else if (result.fraudRiskScore >= 35) WarningAmber else NeonEmerald

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurface)
                        .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Top Bar: Detected Language + Threat Category + Fraud Risk Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isHighFraud) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = result.intentCategory.replace("_", " "),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Detected Language Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ElectricCyan.copy(alpha = 0.15f))
                                    .border(1.dp, ElectricCyan.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = result.detectedLanguage.take(16),
                                    color = ElectricCyan,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Fraud Risk Score Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(accentColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "RISK: ${result.fraudRiskScore}%",
                                    color = accentColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    // Vernacular Threat Badge (if present)
                    if (result.vernacularThreatTag.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isHighFraud) AlertCrimson.copy(alpha = 0.12f) else WarningAmber.copy(alpha = 0.12f))
                                .border(1.dp, if (isHighFraud) AlertCrimson.copy(alpha = 0.4f) else WarningAmber.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = result.vernacularThreatTag,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isHighFraud) AlertCrimson else WarningAmber,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Intent Summary
                    Text(
                        text = result.summary,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = CyberTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    )

                    // Plain-Language Clarity Breakdown (Easy for any user to understand)
                    if (result.clarityExplanation.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberBgSecondary)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "WHY THIS IS DANGEROUS (स्पष्टीकरण):",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = WarningAmber,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            )
                            Text(
                                text = result.clarityExplanation,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp
                                )
                            )
                        }
                    }

                    // Detected Tactics Tags
                    if (result.detectedTactics.isNotEmpty() && result.detectedTactics.firstOrNull() != "None") {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "COERCION TACTICS IDENTIFIED:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyberTextMuted,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                result.detectedTactics.take(2).forEach { tactic ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(AlertCrimson.copy(alpha = 0.15f))
                                            .border(1.dp, AlertCrimson.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = tactic,
                                            color = AlertCrimson,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Tactical Safety Instruction & Vernacular Advice
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isHighFraud) AlertCrimson.copy(alpha = 0.15f) else ElectricCyan.copy(alpha = 0.1f))
                            .border(1.dp, if (isHighFraud) AlertCrimson.copy(alpha = 0.4f) else ElectricCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (isHighFraud) AlertCrimson else ElectricCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = result.recommendedAction,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isHighFraud) AlertCrimson else CyberTextPrimary,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        if (result.vernacularAdvice.isNotEmpty() && result.vernacularAdvice != "No action required.") {
                            Text(
                                text = "🛡️ सुरक्षा सलाह: ${result.vernacularAdvice}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isHighFraud) AlertCrimson.copy(alpha = 0.9f) else ElectricCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }

            // Action Button: Trigger Gemini Scan
            Button(
                onClick = onAnalyzeRequested,
                enabled = !isAnalyzing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gemini_analyze_snippet_btn")
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Gemini AI Analyzing Intent...", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (result == null) "Analyze Call Snippet with Gemini AI" else "Re-Evaluate with Gemini AI",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
