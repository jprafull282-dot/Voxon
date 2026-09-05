package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertCrimson
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

import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import com.example.ui.AppLanguage
import com.example.ui.util.AppStrings

@Composable
fun SunMoonThemeToggleBtn(
    isDarkMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                if (isDarkMode) Color(0xFF1E293B) else Color(0xFFFEF3C7)
            )
            .border(
                1.dp,
                if (isDarkMode) Color(0xFFF59E0B).copy(alpha = 0.6f) else Color(0xFFD97706).copy(alpha = 0.5f),
                CircleShape
            )
            .testTag("sun_half_moon_toggle_btn")
    ) {
        // Sun symbol when in Dark Mode (click to switch to Day), Half Moon symbol when in Light Mode (click to switch to Night)
        if (isDarkMode) {
            Icon(
                imageVector = Icons.Default.LightMode, // ☀️ Sun
                contentDescription = "Switch to Day Mode (Sun)",
                tint = Color(0xFFFBBF24), // Vibrant Sunny Gold
                modifier = Modifier.size(19.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.DarkMode, // 🌓 Half Moon
                contentDescription = "Switch to Night Mode (Moon)",
                tint = Color(0xFF4338CA), // Deep Moonlight Indigo
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
fun TopCyberHeader(
    isShieldActive: Boolean,
    onRefreshClick: () -> Unit
) {
    Surface(
        color = CyberBgSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Side: App Logo & Brand
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(listOf(NeonEmerald, ElectricCyan))
                        )
                        .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Voxen Logo",
                        tint = Color(0xFF0B0D11),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "VOXEN",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = CyberTextPrimary,
                                letterSpacing = 0.8.sp,
                                fontSize = 16.sp
                            )
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeonEmerald.copy(alpha = 0.15f))
                                .border(1.dp, NeonEmerald.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "SOC 24/7",
                                color = NeonEmerald,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Zero-Trust AI Voice Defense",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberTextSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Right Side: Shield Live Status + Refresh
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Live Status Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isShieldActive) NeonEmerald.copy(alpha = 0.12f)
                            else AlertCrimson.copy(alpha = 0.12f)
                        )
                        .border(
                            1.dp,
                            if (isShieldActive) NeonEmerald.copy(alpha = 0.35f)
                            else AlertCrimson.copy(alpha = 0.35f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isShieldActive) NeonEmerald else AlertCrimson)
                        )
                        Text(
                            text = if (isShieldActive) "ACTIVE" else "MUTED",
                            color = if (isShieldActive) NeonEmerald else AlertCrimson,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Refresh Button
                IconButton(
                    onClick = onRefreshClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyberSurface)
                        .border(1.dp, CyberBorder, CircleShape)
                        .testTag("top_bar_refresh_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Data",
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PulsingDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    Box(
        modifier = Modifier
            .size((7 * scale).dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun RiskGauge(
    score: Int,
    modifier: Modifier = Modifier,
    sizeDp: Int = 110
) {
    val scoreColor = ThreatLevel.getColor(score)

    val animatedSweep = (score / 100f) * 240f

    Box(
        modifier = modifier.size(sizeDp.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp.dp)) {
            val strokeWidth = 10.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Background Arc (240 deg sweep)
            drawArc(
                color = Color(0xFF1E2638),
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Foreground Score Arc
            drawArc(
                color = scoreColor,
                startAngle = 150f,
                sweepAngle = animatedSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$score%",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = scoreColor,
                    fontSize = 22.sp
                )
            )
            Text(
                text = when {
                    score >= 60 -> "CRITICAL"
                    score >= 30 -> "HIGH RISK"
                    score >= 10 -> "SUSPICIOUS"
                    else -> "SAFE"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    color = scoreColor,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
fun AudioWaveformVisualizer(
    waveforms: List<Float>,
    isDeepfake: Boolean = false,
    modifier: Modifier = Modifier
) {
    val barColor = if (isDeepfake) AlertCrimson else ElectricCyan
    val displayList = if (waveforms.isEmpty()) {
        listOf(0.2f, 0.4f, 0.7f, 0.9f, 0.6f, 0.3f, 0.8f, 0.5f, 0.2f, 0.6f, 0.8f, 0.4f)
    } else {
        waveforms
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(CyberSurface)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        displayList.take(24).forEach { amp ->
            val h = (amp * 28.dp.value).coerceIn(4f, 28f)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun ForensicMetricRow(
    label: String,
    score: Float,
    isThreat: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = CyberTextSecondary, fontSize = 11.5.sp)
        )
        Text(
            text = "${(score * 100).toInt()}% Anomaly",
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isThreat) AlertCrimson else NeonEmerald,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        )
    }
}

@Composable
fun ForensicMetricRow(
    label: String,
    valueText: String,
    isThreat: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = CyberTextSecondary, fontSize = 11.5.sp)
        )
        Text(
            text = valueText,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isThreat) AlertCrimson else ElectricCyan,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        )
    }
}

@Composable
fun ActiveChallengeDialog(
    challengeType: String,
    challengeStatus: String,
    onDismiss: () -> Unit,
    onVerifyVoice: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CyberBgSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, ElectricCyan, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(36.dp))
                Text(
                    text = challengeType,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = CyberTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Requesting cryptographic passphrase challenge to verify speaker authenticity and defeat vocoder synthesis.",
                    style = MaterialTheme.typography.bodySmall.copy(color = CyberTextMuted, fontSize = 11.sp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Button(
                    onClick = onVerifyVoice,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Verify Challenge Response", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
