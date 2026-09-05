package com.example.engine

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
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
import com.example.ui.theme.VoiceGuardTheme

class CallThreatPopupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen & turn screen on for immediate emergency HUD visibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val callerName = intent.getStringExtra("EXTRA_CALLER_NAME") ?: "Incoming Call"
        val callerNumber = intent.getStringExtra("EXTRA_CALLER_NUMBER") ?: "+91 000-000-0000"
        val riskScore = intent.getIntExtra("EXTRA_RISK_SCORE", 96)
        val threatType = intent.getStringExtra("EXTRA_THREAT_TYPE") ?: "AI Voice Deepfake"
        val explanation = intent.getStringExtra("EXTRA_EXPLANATION") ?: "Synthetic acoustic artifacts detected in real-time."
        val incidentId = intent.getStringExtra("EXTRA_INCIDENT_ID") ?: ""

        setContent {
            VoiceGuardTheme {
                CallThreatPopupContent(
                    callerName = callerName,
                    callerNumber = callerNumber,
                    riskScore = riskScore,
                    threatType = threatType,
                    explanation = explanation,
                    onHangUp = {
                        val disconnectIntent = Intent(this, ThreatNotificationActionReceiver::class.java).apply {
                            action = ThreatNotificationActionReceiver.ACTION_DISCONNECT_CALL
                            putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NAME, callerName)
                            putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NUMBER, callerNumber)
                            putExtra(ThreatNotificationActionReceiver.EXTRA_INCIDENT_ID, incidentId)
                            putExtra(ThreatNotificationActionReceiver.EXTRA_RISK_SCORE, riskScore)
                        }
                        sendBroadcast(disconnectIntent)
                        finish()
                    },
                    onMarkSafe = {
                        val markSafeIntent = Intent(this, ThreatNotificationActionReceiver::class.java).apply {
                            action = ThreatNotificationActionReceiver.ACTION_MARK_SAFE
                            putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NAME, callerName)
                            putExtra(ThreatNotificationActionReceiver.EXTRA_CALLER_NUMBER, callerNumber)
                            putExtra(ThreatNotificationActionReceiver.EXTRA_INCIDENT_ID, incidentId)
                        }
                        sendBroadcast(markSafeIntent)
                        finish()
                    },
                    onOpenApp = {
                        val appIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("EXTRA_NAVIGATE_TO", "INCIDENT_DETAIL")
                            putExtra("EXTRA_INCIDENT_ID", incidentId)
                        }
                        startActivity(appIntent)
                        finish()
                    },
                    onDismiss = {
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun CallThreatPopupContent(
    callerName: String,
    callerNumber: String,
    riskScore: Int,
    threatType: String,
    explanation: String,
    onHangUp: () -> Unit,
    onMarkSafe: () -> Unit,
    onOpenApp: () -> Unit,
    onDismiss: () -> Unit
) {
    val threatTier = ThreatLevel.fromScore(riskScore)
    val threatColor = threatTier.colorCompose

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CyberBgSecondary),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, threatColor, RoundedCornerShape(22.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Flashing Danger Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(threatColor.copy(alpha = 0.2f))
                        .border(2.dp, threatColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = threatColor,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🚨 ${threatTier.title}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = threatColor,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = threatTier.systemActionDescription,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = CyberTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }

                // Caller & Threat Info Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurface)
                        .border(1.dp, threatColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = callerName,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = CyberTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = callerNumber,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = CyberTextMuted,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(threatColor)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$riskScore% RISK",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Threat: $threatType",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NeonAmber,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CyberTextSecondary,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }

                // Safety Rule Warning
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(threatColor.copy(alpha = 0.12f))
                        .border(1.dp, threatColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = threatColor, modifier = Modifier.size(18.dp))
                        Text(
                            text = "DO NOT share banking passwords, OTPs, or transfer money. High deepfake scam risk.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = threatColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onHangUp,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = threatColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("popup_hangup_btn")
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("DISCONNECT & TERMINATE CALL", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onMarkSafe,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("popup_mark_safe_btn")
                        ) {
                            Text("Mark Safe", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = onOpenApp,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("popup_view_forensics_btn")
                        ) {
                            Text("View Forensics", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("popup_dismiss_btn")
                        ) {
                            Text("Dismiss", color = CyberTextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
