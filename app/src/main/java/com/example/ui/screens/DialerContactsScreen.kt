package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.PhoneForwarded
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnalyzedCallEntity
import com.example.engine.ContactSource
import com.example.engine.PhoneContactItem
import com.example.ui.VoiceGuardViewModel
import com.example.ui.components.AnalyzedCallDetailDialog
import com.example.ui.theme.AlertCrimson
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
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DialContactSubTab(val title: String, val icon: ImageVector) {
    CONTACTS("Contacts", Icons.Default.Contacts),
    DIALPAD("Keypad", Icons.Default.Dialpad),
    ANALYZED("AI History", Icons.Default.Shield),
    RECENTS("Recents", Icons.Default.History)
}

@Composable
fun DialerContactsScreen(viewModel: VoiceGuardViewModel) {
    val context = LocalContext.current
    val contacts by viewModel.deviceContacts.collectAsState()
    val callLogs by viewModel.recentCallLogs.collectAsState()
    val analyzedCalls by viewModel.analyzedCallHistory.collectAsState()
    val selectedAnalyzedCall by viewModel.selectedAnalyzedCallForDetail.collectAsState()

    var activeSubTab by remember { mutableStateOf(DialContactSubTab.CONTACTS) }
    var searchQuery by remember { mutableStateOf("") }
    var dialedNumber by remember { mutableStateOf("") }
    var dialedName by remember { mutableStateOf("") }
    var selectedRiskFilter by remember { mutableStateOf<String?>(null) }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.number.replace("[\\s\\-\\(\\)]".toRegex(), "").contains(searchQuery.replace("[\\s\\-\\(\\)]".toRegex(), ""))
        }
    }

    val filteredCallLogs = remember(callLogs, searchQuery) {
        if (searchQuery.isBlank()) callLogs
        else callLogs.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.number.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBg)
    ) {
        TabRow(
            selectedTabIndex = activeSubTab.ordinal,
            containerColor = CyberBgSecondary,
            contentColor = ElectricCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubTab.ordinal]),
                    color = ElectricCyan,
                    height = 3.dp
                )
            },
            divider = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(CyberBorder)
                )
            }
        ) {
            DialContactSubTab.values().forEach { tab ->
                val isSelected = activeSubTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { activeSubTab = tab },
                    modifier = Modifier.testTag("dialer_subtab_${tab.name.lowercase()}"),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) ElectricCyan else CyberTextMuted,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = tab.title,
                                color = if (isSelected) ElectricCyan else CyberTextMuted,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                )
            }
        }

        when (activeSubTab) {
            DialContactSubTab.CONTACTS -> {
                ContactsListSection(
                    contacts = filteredContacts,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onStartLiveProtection = { contact, simulateThreat ->
                        viewModel.startLiveCallMonitoring(contact, threatSimulationMode = simulateThreat)
                        Toast.makeText(context, "Initiating shielded call for ${contact.name}", Toast.LENGTH_SHORT).show()
                    },
                    onNativePhoneDial = { number ->
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}"))
                        context.startActivity(intent)
                    },
                    onRefresh = { viewModel.refreshContactsAndCallLogs() }
                )
            }
            DialContactSubTab.DIALPAD -> {
                DialpadSection(
                    dialedNumber = dialedNumber,
                    dialedName = dialedName,
                    onNumberChange = { dialedNumber = it },
                    onNameChange = { dialedName = it },
                    onStartShieldedCall = { num, name, threat ->
                        val item = PhoneContactItem(
                            id = "dial_${System.currentTimeMillis()}",
                            name = name.ifBlank { "Dialed Call ($num)" },
                            number = num,
                            source = ContactSource.LIVE_INPUT,
                            callType = "Outgoing",
                            timestamp = System.currentTimeMillis()
                        )
                        viewModel.startLiveCallMonitoring(item, threatSimulationMode = threat)
                        Toast.makeText(context, "VoiceGuard Shield active for $num", Toast.LENGTH_SHORT).show()
                    },
                    onNativePhoneDial = { num ->
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(num)}"))
                        context.startActivity(intent)
                    }
                )
            }
            DialContactSubTab.ANALYZED -> {
                AnalyzedCallsHistorySection(
                    analyzedCalls = analyzedCalls,
                    searchQuery = searchQuery,
                    selectedRiskFilter = selectedRiskFilter,
                    onSearchQueryChange = { searchQuery = it },
                    onRiskFilterChange = { selectedRiskFilter = it },
                    onSelectCall = { viewModel.selectAnalyzedCall(it) },
                    onDeleteCall = { viewModel.deleteAnalyzedCall(it) },
                    onClearAll = { viewModel.clearAllAnalyzedCalls() },
                    onStartLiveProtection = { contact, simulateThreat ->
                        viewModel.startLiveCallMonitoring(contact, threatSimulationMode = simulateThreat)
                    },
                    onNativePhoneDial = { number ->
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}"))
                        context.startActivity(intent)
                    }
                )
            }
            DialContactSubTab.RECENTS -> {
                RecentsCallLogSection(
                    callLogs = filteredCallLogs,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onStartLiveProtection = { contact, simulateThreat ->
                        viewModel.startLiveCallMonitoring(contact, threatSimulationMode = simulateThreat)
                    },
                    onNativePhoneDial = { number ->
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}"))
                        context.startActivity(intent)
                    },
                    onRefresh = { viewModel.refreshContactsAndCallLogs() }
                )
            }
        }

        selectedAnalyzedCall?.let { call ->
            AnalyzedCallDetailDialog(
                call = call,
                onDelete = { id ->
                    viewModel.deleteAnalyzedCall(id)
                },
                onDismiss = { viewModel.selectAnalyzedCall(null) }
            )
        }
    }
}

@Composable
fun ContactsListSection(
    contacts: List<PhoneContactItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onStartLiveProtection: (PhoneContactItem, Boolean) -> Unit,
    onNativePhoneDial: (String) -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search by contact name or number...", color = CyberTextMuted, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElectricCyan) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = CyberTextMuted)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("contacts_search_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface,
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedTextColor = CyberTextPrimary,
                    unfocusedTextColor = CyberTextPrimary
                ),
                singleLine = true
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DEVICE ADDRESS BOOK (${contacts.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyberTextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )

                Text(
                    text = "Tap to Shield Call",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NeonEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        if (contacts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(CyberSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No Matching Contacts" else "No Contacts Loaded",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = CyberTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Check your spelling or search by phone number digits." else "Allow Contacts permission in Settings to list on-device contacts.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CyberTextMuted,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Sync Contacts", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            items(contacts, key = { it.id }) { contact ->
                ContactCardItem(
                    contact = contact,
                    onStartLiveProtection = onStartLiveProtection,
                    onNativePhoneDial = onNativePhoneDial
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ContactCardItem(
    contact: PhoneContactItem,
    onStartLiveProtection: (PhoneContactItem, Boolean) -> Unit,
    onNativePhoneDial: (String) -> Unit
) {
    var expandedActions by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            .clickable { expandedActions = !expandedActions }
            .testTag("contact_card_${contact.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        NeonEmerald.copy(alpha = 0.25f),
                                        ElectricCyan.copy(alpha = 0.25f)
                                    )
                                )
                            )
                            .border(1.dp, ElectricCyan.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contact.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = ElectricCyan,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }

                    Column {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = CyberTextPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = contact.number,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CyberTextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { onNativePhoneDial(contact.number) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberSurface)
                            .border(1.dp, CyberBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Phone Call",
                            tint = NeonEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { onStartLiveProtection(contact, false) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonEmerald.copy(alpha = 0.15f))
                            .border(1.dp, NeonEmerald.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Shield Call",
                            tint = NeonEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expandedActions) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                        .background(CyberSurface)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "VOICEGUARD AI SHIELD ACTIONS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onStartLiveProtection(contact, false) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Real Safe Monitor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onStartLiveProtection(contact, true) },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simulate Threat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialpadSection(
    dialedNumber: String,
    dialedName: String,
    onNumberChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onStartShieldedCall: (String, String, Boolean) -> Unit,
    onNativePhoneDial: (String) -> Unit
) {
    val keypadButtons = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        listOf("*" to "", "0" to "+", "#" to "")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = dialedName,
                onValueChange = onNameChange,
                placeholder = { Text("Caller label (e.g. Bank Support, Manager)", color = CyberTextMuted, fontSize = 12.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("dialer_name_input"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface,
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedTextColor = CyberTextPrimary,
                    unfocusedTextColor = CyberTextPrimary
                ),
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (dialedNumber.isEmpty()) "Enter Phone Number" else dialedNumber,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = if (dialedNumber.isEmpty()) CyberTextMuted else CyberTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = if (dialedNumber.length > 12) 22.sp else 28.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (dialedNumber.isNotEmpty()) {
                    IconButton(
                        onClick = { onNumberChange(dialedNumber.dropLast(1)) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Backspace",
                            tint = CyberTextSecondary
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            keypadButtons.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { (digit, letters) ->
                        KeypadButton(
                            digit = digit,
                            subtext = letters,
                            onClick = {
                                if (digit == "0") {
                                    onNumberChange(dialedNumber + "0")
                                } else {
                                    onNumberChange(dialedNumber + digit)
                                }
                            },
                            onLongClick = {
                                if (digit == "0") {
                                    onNumberChange(dialedNumber + "+")
                                }
                            }
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val num = if (dialedNumber.isNotBlank()) dialedNumber else "+91 98765 43210"
                        onStartShieldedCall(num, dialedName, false)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = Color.Black),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("dialer_shielded_call_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text("Shielded Call", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                IconButton(
                    onClick = {
                        val num = if (dialedNumber.isNotBlank()) dialedNumber else "+919876543210"
                        onNativePhoneDial(num)
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CyberSurfaceElevated)
                        .border(1.dp, ElectricCyan, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "System Call",
                        tint = ElectricCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Button(
                    onClick = {
                        val num = if (dialedNumber.isNotBlank()) dialedNumber else "+91 99999 00000"
                        onStartShieldedCall(num, dialedName.ifBlank { "Deepfake Threat Test" }, true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("dialer_threat_sim_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Test Spoof", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    digit: String,
    subtext: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(CyberSurface)
            .border(1.dp, CyberBorder, CircleShape)
            .clickable { onClick() }
            .testTag("keypad_btn_$digit"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = digit,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            )
            if (subtext.isNotEmpty()) {
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyberTextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
fun RecentsCallLogSection(
    callLogs: List<PhoneContactItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onStartLiveProtection: (PhoneContactItem, Boolean) -> Unit,
    onNativePhoneDial: (String) -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search call history logs...", color = CyberTextMuted, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElectricCyan) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recents_search_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface,
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedTextColor = CyberTextPrimary,
                    unfocusedTextColor = CyberTextPrimary
                ),
                singleLine = true
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT CALL LOGS (${callLogs.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyberTextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }

        if (callLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No Recent Call Logs",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = CyberTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Grant Call Log permission to see all dialed and received calls.",
                            style = MaterialTheme.typography.bodySmall.copy(color = CyberTextMuted),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(callLogs, key = { it.id }) { log ->
                val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
                val dateStr = remember(log.timestamp) { dateFormat.format(Date(log.timestamp)) }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val callIcon = when (log.callType) {
                                "Incoming" -> Icons.Default.PhoneCallback
                                "Outgoing" -> Icons.Default.PhoneForwarded
                                "Missed" -> Icons.Default.PhoneMissed
                                else -> Icons.Default.Phone
                            }
                            val iconTint = when (log.callType) {
                                "Incoming" -> NeonEmerald
                                "Outgoing" -> ElectricCyan
                                "Missed" -> AlertCrimson
                                else -> CyberTextMuted
                            }

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(iconTint.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = callIcon,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = log.name,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = CyberTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${log.number} • ${log.callType ?: "Call"} • $dateStr",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = CyberTextMuted,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { onNativePhoneDial(log.number) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(CyberSurface)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                            }

                            IconButton(
                                onClick = { onStartLiveProtection(log, false) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(NeonEmerald.copy(alpha = 0.15f))
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyzedCallsHistorySection(
    analyzedCalls: List<AnalyzedCallEntity>,
    searchQuery: String,
    selectedRiskFilter: String?,
    onSearchQueryChange: (String) -> Unit,
    onRiskFilterChange: (String?) -> Unit,
    onSelectCall: (AnalyzedCallEntity) -> Unit,
    onDeleteCall: (String) -> Unit,
    onClearAll: () -> Unit,
    onStartLiveProtection: (PhoneContactItem, Boolean) -> Unit,
    onNativePhoneDial: (String) -> Unit
) {
    val filteredList = remember(analyzedCalls, searchQuery, selectedRiskFilter) {
        analyzedCalls.filter { call ->
            val matchesQuery = searchQuery.isBlank() ||
                call.callerLabel.contains(searchQuery, ignoreCase = true) ||
                call.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                call.threatType.contains(searchQuery, ignoreCase = true) ||
                call.transcriptSnippet.contains(searchQuery, ignoreCase = true)

            val matchesFilter = selectedRiskFilter == null ||
                when (selectedRiskFilter) {
                    "CRITICAL" -> call.securityRiskLevel == "CRITICAL" || call.riskScore >= 75
                    "HIGH" -> call.securityRiskLevel == "HIGH" || (call.riskScore in 50..74)
                    "SAFE" -> call.securityRiskLevel == "VERIFIED_SAFE" || call.securityRiskLevel == "LOW" || call.riskScore < 30
                    else -> true
                }

            matchesQuery && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Header Info & Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ROOM DATABASE PERSISTENCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ElectricCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                    Text(
                        text = "Analyzed Calls History",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CyberTextPrimary
                        )
                    )
                }

                if (analyzedCalls.isNotEmpty()) {
                    Button(
                        onClick = onClearAll,
                        colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson.copy(alpha = 0.2f), contentColor = AlertCrimson),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search analyzed phone numbers, transcripts, threats...", color = CyberTextMuted, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null, tint = CyberTextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("analyzed_calls_search_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }

        item {
            // Risk Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    null to "All (${analyzedCalls.size})",
                    "CRITICAL" to "Critical",
                    "HIGH" to "High Risk",
                    "SAFE" to "Safe"
                )

                filters.forEach { (filterKey, label) ->
                    val isSelected = selectedRiskFilter == filterKey
                    val chipColor = when (filterKey) {
                        "CRITICAL" -> AlertCrimson
                        "HIGH" -> WarningAmber
                        "SAFE" -> NeonEmerald
                        else -> ElectricCyan
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) chipColor.copy(alpha = 0.2f) else CyberSurface)
                            .border(1.dp, if (isSelected) chipColor else CyberBorder, RoundedCornerShape(8.dp))
                            .clickable { onRiskFilterChange(if (isSelected && filterKey != null) null else filterKey) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) chipColor else CyberTextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No Analyzed Calls Found",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = CyberTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedRiskFilter != null)
                                "No analyzed calls match your current search or risk filter."
                            else
                                "Calls scanned by TensorFlow Lite and Gemini will be persistently archived here.",
                            style = MaterialTheme.typography.bodySmall.copy(color = CyberTextMuted),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredList, key = { it.id }) { call ->
                val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
                val dateStr = remember(call.timestamp) { dateFormat.format(Date(call.timestamp)) }

                val isCritical = call.securityRiskLevel == "CRITICAL" || call.riskScore >= 75
                val isSafe = call.securityRiskLevel == "VERIFIED_SAFE" || call.securityRiskLevel == "LOW"
                val riskColor = when {
                    isSafe -> NeonEmerald
                    isCritical -> AlertCrimson
                    else -> WarningAmber
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                        .clickable { onSelectCall(call) }
                        .testTag("analyzed_call_card_${call.id}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Top row: Contact info & Security Risk badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(riskColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isCritical) Icons.Default.Warning else Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = riskColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = call.callerLabel,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = CyberTextPrimary,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${call.phoneNumber} • ${call.durationSeconds}s • $dateStr",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = CyberTextMuted,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                }
                            }

                            // Security Score Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(riskColor.copy(alpha = 0.15f))
                                    .border(1.dp, riskColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${call.securityRiskLevel} (${call.securityScore}/100)",
                                    color = riskColor,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // AI Threat & Model Details
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberSurface)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Threat: ${call.threatType}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = CyberTextSecondary,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = call.aiModelNames.take(28),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ElectricCyan,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }

                        // Transcript Snippet Preview
                        if (call.transcriptSnippet.isNotBlank()) {
                            Text(
                                text = "\"${call.transcriptSnippet}\"",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = CyberTextMuted,
                                    fontSize = 11.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Bottom Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tap to view full forensic report →",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = ElectricCyan,
                                    fontSize = 11.sp
                                )
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { onNativePhoneDial(call.phoneNumber) },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(CyberSurface)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                                }

                                val contactItem = PhoneContactItem(
                                    id = call.id,
                                    name = call.callerLabel,
                                    number = call.phoneNumber,
                                    source = ContactSource.LIVE_INPUT,
                                    callType = "Incoming",
                                    timestamp = call.timestamp
                                )

                                IconButton(
                                    onClick = { onStartLiveProtection(contactItem, isCritical) },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(NeonEmerald.copy(alpha = 0.15f))
                                ) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
