package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CampaignEntity
import com.example.data.model.PolicyEntity
import com.example.ui.VoiceGuardViewModel
import com.example.ui.components.ForensicMetricRow
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

@Composable
fun AdminSocScreen(viewModel: VoiceGuardViewModel) {
    val policies by viewModel.policies.collectAsState()
    val campaigns by viewModel.campaigns.collectAsState()
    val labState by viewModel.labState.collectAsState()
    val cloudSyncState by viewModel.cloudSyncState.collectAsState()

    var serverUrlInput by remember { mutableStateOf(cloudSyncState.serverUrl) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SOC Telemetry Header Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CyberBgSecondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
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
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = ElectricCyan)
                            Text(
                                text = "ENTERPRISE SOC TELEMETRY",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                        }
                        Text(
                            text = "LIVE CLOUD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NeonEmerald,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        )
                    }

                    // 4 Stat blocks in 2x2 grid
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SocStatBox(title = "Calls Analyzed", value = "14,280", sub = "Edge + Cloud Hybrid", modifier = Modifier.weight(1f), color = ElectricCyan)
                        SocStatBox(title = "Deepfakes Blocked", value = "429", sub = "96.4% Authenticity Acc", modifier = Modifier.weight(1f), color = AlertCrimson)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SocStatBox(title = "Detection Latency", value = "380 ms", sub = "0.4s Fast Edge Shield", modifier = Modifier.weight(1f), color = NeonEmerald)
                        SocStatBox(title = "Campaigns Active", value = "${campaigns.size}", sub = "1,284 Protected Users", modifier = Modifier.weight(1f), color = WarningAmber)
                    }
                }
            }
        }

        // Adversarial Testing Lab Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
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
                            Icon(Icons.Default.Science, contentDescription = null, tint = WarningAmber)
                            Text(
                                text = "🧪 ADVERSARIAL TESTING LAB",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            )
                        }

                        Button(
                            onClick = { viewModel.runAdversarialLabBenchmark() },
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber, contentColor = Color(0xFF05101E)),
                            modifier = Modifier.testTag("run_lab_benchmark_button")
                        ) {
                            Text(if (labState.isRunning) "Testing..." else "Run 50k Benchmark", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        text = "Validates the multi-agent detection models against 50,000 synthesized speech, vocoder replay, noisy audio, and Indian regional accent datasets.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = CyberTextSecondary, fontSize = 12.sp)
                    )

                    if (labState.isRunning) {
                        LinearProgressIndicator(
                            progress = { labState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = WarningAmber,
                            trackColor = CyberBg
                        )
                        Text(
                            text = "Processing sample ${labState.samplesTested} of 50,000...",
                            style = MaterialTheme.typography.labelSmall.copy(color = CyberTextMuted, fontSize = 10.sp)
                        )
                    }

                    ForensicMetricRow("Deepfake Detection Accuracy", "${labState.deepfakeAccuracy}%")
                    ForensicMetricRow("Voice Conversion Detection", "${labState.voiceConversionAccuracy}%")
                    ForensicMetricRow("Replay / Splice Detection", "${labState.replayAccuracy}%")
                    ForensicMetricRow("Indian Accent Dataset Accuracy", "${labState.indianAccentAccuracy}%")
                    ForensicMetricRow("Noisy Background Robustness", "${labState.noisyAudioAccuracy}%")
                }
            }
        }

        // Threat Campaigns Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberBgSecondary)
                    .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp),
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
                        Icon(Icons.Default.Public, contentDescription = null, tint = AlertCrimson)
                        Text(
                            text = "🌐 THREAT CAMPAIGN GRAPH",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        )
                    }
                    Text(
                        text = "National / Global",
                        style = MaterialTheme.typography.labelSmall.copy(color = CyberTextMuted)
                    )
                }

                campaigns.forEach { campaign ->
                    CampaignCard(campaign = campaign)
                }
            }
        }

        // Automated Zero-Trust Security Playbooks
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
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
                        Icon(Icons.Default.Security, contentDescription = null, tint = NeonEmerald)
                        Text(
                            text = "🤖 AUTOMATED SECURITY PLAYBOOKS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                policies.forEach { policy ->
                    PolicyRow(
                        policy = policy,
                        onToggle = { viewModel.togglePolicy(policy) }
                    )
                }
            }
        }

        // PC Backend Server Sync
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberBgSecondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                            Icon(Icons.Default.Computer, contentDescription = null, tint = ElectricCyan)
                            Text(
                                text = "💻 PC BACKEND & SOC WEB CLOUD",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (cloudSyncState.isConnected) NeonEmerald.copy(alpha = 0.2f) else CyberSurface)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (cloudSyncState.isConnected) "CONNECTED (${cloudSyncState.latencyMs}ms)" else "OFFLINE",
                                color = if (cloudSyncState.isConnected) NeonEmerald else CyberTextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = "Connect this mobile device to your PC running VoiceGuard X Cloud (`run_pc_backend.bat`):",
                        style = MaterialTheme.typography.bodyMedium.copy(color = CyberTextSecondary, fontSize = 12.sp)
                    )

                    OutlinedTextField(
                        value = serverUrlInput,
                        onValueChange = {
                            serverUrlInput = it
                            viewModel.updateServerUrl(it)
                        },
                        label = { Text("PC Server LAN URL", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CyberSurface,
                            unfocusedContainerColor = CyberSurface,
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = CyberBorder
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = { viewModel.syncWithPCCloud() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sync_pc_cloud_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF05101E))
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (cloudSyncState.isSyncing) "Syncing with PC..." else "Sync SOC Database with PC Cloud", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SocStatBox(
    title: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier,
    color: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CyberSurface)
            .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(color = CyberTextMuted, fontSize = 9.5.sp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = color,
                fontSize = 20.sp
            )
        )
        Text(
            text = sub,
            style = MaterialTheme.typography.labelSmall.copy(color = CyberTextSecondary, fontSize = 9.sp)
        )
    }
}

@Composable
fun CampaignCard(campaign: CampaignEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurface)
            .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = campaign.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(AlertCrimson.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = campaign.riskLevel,
                    color = AlertCrimson,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        Text(
            text = "Impact: ${campaign.affectedUsers} devices across ${campaign.regions}",
            style = MaterialTheme.typography.bodyMedium.copy(color = CyberTextSecondary, fontSize = 11.5.sp)
        )
        Text(
            text = "Indicators: ${campaign.indicators}",
            style = MaterialTheme.typography.labelSmall.copy(color = ElectricCyan, fontSize = 10.sp)
        )
    }
}

@Composable
fun PolicyRow(
    policy: PolicyEntity,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CyberBgSecondary)
            .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = policy.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp
                )
            )
            Text(
                text = policy.condition,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = ElectricCyan,
                    fontSize = 10.sp
                )
            )
            Text(
                text = "Action: ${policy.action} (Fired ${policy.triggerCount} times)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = CyberTextSecondary,
                    fontSize = 10.5.sp
                )
            )
        }

        Switch(
            checked = policy.enabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF05101E),
                checkedTrackColor = NeonEmerald,
                uncheckedThumbColor = CyberTextMuted,
                uncheckedTrackColor = CyberSurface
            ),
            modifier = Modifier.testTag("policy_switch_${policy.id}")
        )
    }
}
