package com.example.ui.components

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
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CyberBg
import com.example.ui.theme.CyberBgSecondary
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.WarningAmber
import com.example.engine.ThreatLevel

/**
 * Persistent / Floating In-App Overlay UI Component that clearly indicates
 * when a live phone call is being actively scanned for synthetic deepfake audio patterns.
 */
@Composable
fun LiveCallScanningOverlayBar(
    isScanningActive: Boolean,
    callerName: String,
    callerNumber: String,
    riskScore: Int,
    durationSeconds: Int,
    threatSummary: String,
    onSimulateAttack: () -> Unit,
    onMarkSafe: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenDashboard: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isScanningActive,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        var isExpanded by remember { mutableStateOf(false) }

        val infiniteTransition = rememberInfiniteTransition(label = "scan_pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
        val waveBar1 by infiniteTransition.animateFloat(
            initialValue = 0.2f, targetValue = 0.95f,
            animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "wb1"
        )
        val waveBar2 by infiniteTransition.animateFloat(
            initialValue = 0.6f, targetValue = 0.3f,
            animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse), label = "wb2"
        )
        val waveBar3 by infiniteTransition.animateFloat(
            initialValue = 0.1f, targetValue = 0.85f,
            animationSpec = infiniteRepeatable(tween(350), RepeatMode.Reverse), label = "wb3"
        )
        val waveBar4 by infiniteTransition.animateFloat(
            initialValue = 0.9f, targetValue = 0.4f,
            animationSpec = infiniteRepeatable(tween(480), RepeatMode.Reverse), label = "wb4"
        )

        val threatTier = ThreatLevel.fromScore(riskScore)
        val accentColor = threatTier.colorCompose
        val statusBadgeColor = accentColor

        val formattedDuration = remember(durationSeconds) {
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            String.format("%02d:%02d", mins, secs)
        }

        val statusHeaderText = "🚨 ${threatTier.title}"
        val badgeLabel = "${threatTier.shortLabel} $riskScore%"

        Surface(
            color = CyberBgSecondary.copy(alpha = 0.96f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .border(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = pulseAlpha),
                            accentColor,
                            accentColor.copy(alpha = pulseAlpha)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .clickable { isExpanded = !isExpanded }
                .testTag("live_call_scanning_overlay_bar")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Row with Pulsing Radar and Live Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pulsing status LED
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(statusBadgeColor.copy(alpha = pulseAlpha))
                        )

                        Text(
                            text = statusHeaderText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = accentColor,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        )

                        // Mini Audio Equalizer Animation
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.height(14.dp)
                        ) {
                            listOf(waveBar1, waveBar2, waveBar3, waveBar4).forEach { factor ->
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height((14 * factor).dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(accentColor)
                                )
                            }
                        }
                    }

                    // Risk Badge & Call Timer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formattedDuration,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = CyberTextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Surface(
                            color = statusBadgeColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, statusBadgeColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = badgeLabel,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = statusBadgeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                // Caller Information & Real-time Analysis summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (callerName.isNotBlank()) "$callerName ($callerNumber)" else callerNumber,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = CyberTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = threatSummary.ifEmpty {
                                when {
                                    riskScore >= 60 -> "Urgent alert: Synthetic voice clone or emergency scam detected. Disconnect immediately."
                                    riskScore >= 30 -> "Warning: Potential AI voice clone or high-pressure scam flagged."
                                    riskScore >= 10 -> "Caution: Unusual vocal jitter, pitch anomalies, or unverified caller patterns."
                                    else -> "Call Secure / No Synthetic Voice Detected"
                                }
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CyberTextMuted,
                                fontSize = 11.5.sp
                            ),
                            maxLines = if (isExpanded) 4 else 1
                        )
                    }

                    Text(
                        text = if (isExpanded) "▲ LESS" else "▼ MORE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ElectricCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Expanded Diagnostic Cards & Quick Actions
                if (isExpanded) {
                    Surface(
                        color = CyberBg.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "AI Detection Engine:",
                                    style = MaterialTheme.typography.labelSmall.copy(color = CyberTextMuted)
                                )
                                Text(
                                    text = "TFLite Vocoder + Gemini 3.5 Flash",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = ElectricCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Harmonic Phase Status:",
                                    style = MaterialTheme.typography.labelSmall.copy(color = CyberTextMuted)
                                )
                                Text(
                                    text = if (riskScore >= 60) "DISCONTINUOUS (SYNTHETIC)" else if (riskScore >= 30) "ATYPICAL (POTENTIAL CLONE)" else "COHERENT (NATURAL)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = statusBadgeColor,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSimulateAttack,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("simulate_attack_btn"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WarningAmber.copy(alpha = 0.2f),
                                contentColor = WarningAmber
                            )
                        ) {
                            Text("Simulate Threat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onMarkSafe,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("mark_safe_overlay_btn"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonEmerald.copy(alpha = 0.2f),
                                contentColor = NeonEmerald
                            )
                        ) {
                            Text("Mark Safe", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onDisconnect,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("disconnect_call_overlay_btn"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AlertCrimson,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Disconnect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                    }
                }
            }
        }
    }
}
