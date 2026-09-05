package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.data.model.IncidentEntity
import com.example.ui.VoiceGuardViewModel
import com.example.ui.components.IncidentDetailDialog
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IncidentsScreen(viewModel: VoiceGuardViewModel) {
    val incidents by viewModel.incidents.collectAsState()
    val selectedIncident by viewModel.selectedIncidentForDetail.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedSeverityFilter by remember { mutableStateOf("ALL") }

    if (selectedIncident != null) {
        IncidentDetailDialog(
            incident = selectedIncident!!,
            onResolve = { viewModel.resolveIncident(it) },
            onDelete = { viewModel.deleteIncident(it) },
            onDismiss = { viewModel.selectIncident(null) }
        )
    }

    val filteredIncidents = incidents.filter { inc ->
        val matchesQuery = inc.callerNumber.contains(searchQuery, ignoreCase = true) ||
                inc.callerLabel.contains(searchQuery, ignoreCase = true) ||
                inc.threatType.contains(searchQuery, ignoreCase = true)
        val matchesSeverity = when (selectedSeverityFilter) {
            "ALL" -> true
            "RESOLVED" -> inc.isResolved
            else -> inc.severity.equals(selectedSeverityFilter, ignoreCase = true) && !inc.isResolved
        }
        matchesQuery && matchesSeverity
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header & Clear Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "INCIDENT SOC LOGS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )
                Text(
                    text = "${incidents.size} Recorded Forensic Events",
                    style = MaterialTheme.typography.labelSmall.copy(color = CyberTextSecondary)
                )
            }

            IconButton(
                onClick = { viewModel.clearAllIncidents() },
                modifier = Modifier.testTag("clear_incidents_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ClearAll,
                    contentDescription = "Clear All",
                    tint = CyberTextMuted
                )
            }
        }

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search caller, threat, or category...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElectricCyan) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("incident_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CyberSurface,
                unfocusedContainerColor = CyberSurface,
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = CyberBorder,
                focusedTextColor = CyberTextPrimary,
                unfocusedTextColor = CyberTextPrimary
            ),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "CRITICAL", "SUSPICIOUS", "RESOLVED").forEach { filter ->
                val isSelected = selectedSeverityFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedSeverityFilter = filter },
                    label = { Text(filter, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                        selectedLabelColor = ElectricCyan,
                        containerColor = CyberSurface,
                        labelColor = CyberTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) ElectricCyan else CyberBorder
                    )
                )
            }
        }

        if (filteredIncidents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = NeonEmerald,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No Incidents Matching Filter",
                        style = MaterialTheme.typography.titleSmall.copy(color = CyberTextSecondary)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredIncidents, key = { it.id }) { incident ->
                    IncidentCard(
                        incident = incident,
                        onClick = { viewModel.selectIncident(incident) }
                    )
                }
            }
        }
    }
}

@Composable
fun IncidentCard(
    incident: IncidentEntity,
    onClick: () -> Unit
) {
    val isCritical = incident.riskScore >= 80
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val dateStr = timeFormat.format(Date(incident.timestamp))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (incident.isResolved) NeonEmerald.copy(alpha = 0.4f)
                else if (isCritical) AlertCrimson.copy(alpha = 0.6f)
                else WarningAmber.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .testTag("incident_card_${incident.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (incident.isResolved) NeonEmerald.copy(alpha = 0.2f)
                                else if (isCritical) AlertCrimson.copy(alpha = 0.2f)
                                else WarningAmber.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (incident.isResolved) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (incident.isResolved) NeonEmerald else if (isCritical) AlertCrimson else WarningAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = incident.callerNumber,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        )
                        Text(
                            text = incident.callerLabel,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = CyberTextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${incident.riskScore}%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (incident.isResolved) NeonEmerald else if (isCritical) AlertCrimson else WarningAmber
                        )
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall.copy(color = CyberTextMuted, fontSize = 9.sp)
                    )
                }
            }

            Text(
                text = incident.threatType,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                )
            )

            Text(
                text = incident.attackStory,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = CyberTextPrimary,
                    fontSize = 12.sp
                ),
                maxLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SHA-256: ${incident.evidenceHash.take(12)}...",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = CyberTextMuted,
                        fontSize = 9.sp
                    )
                )

                Text(
                    text = "Tap to Inspect Forensics →",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ElectricCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}
