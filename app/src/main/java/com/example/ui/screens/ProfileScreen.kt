package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.AppLanguage
import com.example.ui.VoiceGuardViewModel
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
import com.example.ui.components.SunMoonThemeToggleBtn
import com.example.ui.util.AppStrings

@Composable
fun ProfileScreen(
    viewModel: VoiceGuardViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val authUserState by viewModel.authUserState.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()

    var isRegisterMode by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }

    // Multi-permission array for single-button grant
    val requiredPermissions = remember {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.SEND_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            list.add(Manifest.permission.READ_PHONE_NUMBERS)
        }
        list.toTypedArray()
    }

    val hasAllPermissions = requiredPermissions.all { perm ->
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val grantedCount = results.values.count { it }
        val total = results.size
        if (grantedCount == total) {
            Toast.makeText(context, if (appLanguage == AppLanguage.HINDI) "सभी अनुमतियां स्वीकृत!" else "All permissions granted successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Granted $grantedCount/$total permissions", Toast.LENGTH_SHORT).show()
        }
        viewModel.refreshContactsAndCallLogs()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CyberBg
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar with Close Action
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(NeonEmerald, ElectricCyan))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = AppStrings.profileTitle(appLanguage),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CyberTextPrimary,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = if (authUserState.isAuthenticated) AppStrings.authenticatedStatus(appLanguage) else AppStrings.loginRegisterTitle(appLanguage),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (authUserState.isAuthenticated) NeonEmerald else ElectricCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sun / Moon Symbol 1-Click Toggle (No text information)
                        SunMoonThemeToggleBtn(
                            isDarkMode = isDarkMode,
                            onClick = { viewModel.toggleDarkMode() }
                        )

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyberSurface)
                                .border(1.dp, CyberBorder, CircleShape)
                                .testTag("close_profile_screen_btn")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = CyberTextPrimary)
                        }
                    }
                }
            }

            // APP TEXT LANGUAGE SELECTION
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeonEmerald.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = NeonEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = AppStrings.languageSectionTitle(appLanguage),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = NeonEmerald,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // English Option Button
                            Button(
                                onClick = { viewModel.setAppLanguage(AppLanguage.ENGLISH) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("select_lang_english_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (appLanguage == AppLanguage.ENGLISH) NeonEmerald else CyberSurface,
                                    contentColor = if (appLanguage == AppLanguage.ENGLISH) Color.White else CyberTextSecondary
                                )
                            ) {
                                Text(
                                    text = "🇬🇧 English",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            // Hindi Option Button
                            Button(
                                onClick = { viewModel.setAppLanguage(AppLanguage.HINDI) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("select_lang_hindi_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (appLanguage == AppLanguage.HINDI) NeonEmerald else CyberSurface,
                                    contentColor = if (appLanguage == AppLanguage.HINDI) Color.White else CyberTextSecondary
                                )
                            ) {
                                Text(
                                    text = "🇮🇳 हिंदी (Hindi)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // FEATURE 3: SINGLE BUTTON TO GIVE ALL PERMISSIONS (NO PERMISSION LIST)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (hasAllPermissions) NeonEmerald.copy(alpha = 0.5f) else AlertCrimson.copy(alpha = 0.5f),
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (hasAllPermissions) Icons.Default.CheckCircle else Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (hasAllPermissions) NeonEmerald else AlertCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = AppStrings.permissionsSectionTitle(appLanguage),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (hasAllPermissions) NeonEmerald else AlertCrimson,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }

                        Text(
                            text = if (hasAllPermissions) AppStrings.allPermissionsGranted(appLanguage) else AppStrings.permissionsSubtitle(appLanguage),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (hasAllPermissions) NeonEmerald else CyberTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (hasAllPermissions) FontWeight.Bold else FontWeight.Normal
                            )
                        )

                        // Single Unified 1-Click Permission Button
                        Button(
                            onClick = { permissionLauncher.launch(requiredPermissions) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasAllPermissions) NeonEmerald else ElectricCyan,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("grant_all_permissions_single_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (hasAllPermissions) Icons.Default.CheckCircle else Icons.Default.Security,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (hasAllPermissions) {
                                        if (appLanguage == AppLanguage.HINDI) "सभी अनुमतियां सक्रिय हैं" else "ALL PERMISSIONS GRANTED (PROTECTED)"
                                    } else {
                                        AppStrings.grantAllPermissionsBtn(appLanguage)
                                    },
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }
                }
            }

            if (authUserState.isAuthenticated) {
                // User is Authenticated: Profile Dashboard Details
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeonEmerald.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(NeonEmerald.copy(alpha = 0.2f))
                                    .border(2.dp, NeonEmerald, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = NeonEmerald,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Text(
                                text = authUserState.displayName.ifBlank { "VoiceGuard Protected User" },
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CyberTextPrimary
                                )
                            )

                            Text(
                                text = authUserState.email,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = CyberTextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(NeonEmerald.copy(alpha = 0.15f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = if (appLanguage == AppLanguage.HINDI) "क्लाउड सुरक्षा सक्रिय" else "Cloud Attestation Active",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = NeonEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            Text(
                                text = "UID: ${authUserState.uid.take(16)}...",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyberTextMuted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp
                                )
                            )

                            OutlinedButton(
                                onClick = {
                                    viewModel.logout()
                                    Toast.makeText(context, if (appLanguage == AppLanguage.HINDI) "वॉक्सन से लॉग आउट किया गया" else "Logged out of Voxen", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_sign_out_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertCrimson),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AlertCrimson.copy(alpha = 0.5f))
                            ) {
                                Text(AppStrings.logOutBtn(appLanguage), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // User is Not Authenticated: Sign In / Registration Form
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberCardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = if (isRegisterMode) {
                                    if (appLanguage == AppLanguage.HINDI) "नया सुरक्षा प्रोफ़ाइल बनाएं" else "CREATE NEW GUARD PROFILE"
                                } else {
                                    if (appLanguage == AppLanguage.HINDI) "वॉक्सन में साइन इन करें" else "SIGN IN TO VOICEGUARD"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = ElectricCyan,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )

                            Text(
                                text = if (isRegisterMode) {
                                    if (appLanguage == AppLanguage.HINDI) "निरंतर एआई खतरे की निगरानी और आपातकालीन अलर्ट अनलॉक करने के लिए खाता पंजीकृत करें।"
                                    else "Register your account to unlock continuous AI threat monitoring, automated cloud attestation, and emergency SMS alerts."
                                } else {
                                    if (appLanguage == AppLanguage.HINDI) "अपनी सुरक्षा सेटिंग्स और कॉल फोरेंसिक इतिहास को सिंक करने के लिए साइन इन करें।"
                                    else "Sign in to synchronize your security rules, call forensic recordings, and incident history."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(color = CyberTextMuted, fontSize = 12.sp)
                            )

                            if (authUserState.errorMessage != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AlertCrimson.copy(alpha = 0.15f))
                                        .border(1.dp, AlertCrimson.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                    ) {
                                    Text(
                                        text = authUserState.errorMessage ?: "Authentication error",
                                        style = MaterialTheme.typography.bodySmall.copy(color = AlertCrimson, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            if (isRegisterMode) {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text(if (appLanguage == AppLanguage.HINDI) "नाम" else "Display Name / Owner Name", color = CyberTextSecondary) },
                                    placeholder = { Text("Piyush Goyal", color = CyberTextMuted) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ElectricCyan) },
                                    modifier = Modifier.fillMaxWidth().testTag("auth_name_input"),
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
                            }

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it },
                                label = { Text(if (appLanguage == AppLanguage.HINDI) "ईमेल पता" else "Email Address", color = CyberTextSecondary) },
                                placeholder = { Text("user@voiceguard.security", color = CyberTextMuted) },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ElectricCyan) },
                                modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
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

                            OutlinedTextField(
                                value = passInput,
                                onValueChange = { passInput = it },
                                label = { Text(if (appLanguage == AppLanguage.HINDI) "पासवर्ड" else "Password", color = CyberTextSecondary) },
                                placeholder = { Text("Minimum 6 characters", color = CyberTextMuted) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ElectricCyan) },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
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

                            if (authUserState.isLoading) {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = ElectricCyan, modifier = Modifier.size(32.dp))
                                }
                            } else {
                                // Action Button
                                Button(
                                    onClick = {
                                        if (isRegisterMode) {
                                            viewModel.signUpWithEmail(
                                                email = emailInput,
                                                pass = passInput,
                                                displayName = nameInput,
                                                onSuccess = {
                                                    Toast.makeText(context, "Registration Successful!", Toast.LENGTH_SHORT).show()
                                                    permissionLauncher.launch(requiredPermissions)
                                                },
                                                onFailure = { err ->
                                                    Toast.makeText(context, "Registration Failed: $err", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        } else {
                                            viewModel.signInWithEmail(
                                                email = emailInput,
                                                pass = passInput,
                                                onSuccess = {
                                                    Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                                                    permissionLauncher.launch(requiredPermissions)
                                                },
                                                onFailure = { err ->
                                                    Toast.makeText(context, "Sign In Failed: $err", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("auth_submit_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRegisterMode) NeonEmerald else ElectricCyan,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(
                                        text = if (isRegisterMode) AppStrings.createAccountBtn(appLanguage) else AppStrings.signInBtn(appLanguage),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                // Google Sign-In Option
                                OutlinedButton(
                                    onClick = {
                                        viewModel.signInWithGoogle(
                                            onSuccess = {
                                                Toast.makeText(context, "Google Sign-In Successful!", Toast.LENGTH_SHORT).show()
                                                permissionLauncher.launch(requiredPermissions)
                                            },
                                            onFailure = { err ->
                                                Toast.makeText(context, "Google Auth: $err", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("auth_google_btn"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberTextPrimary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                                        Text(if (appLanguage == AppLanguage.HINDI) "गूगल से साइन इन करें" else "Continue with Google Sign-In", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            // Toggle Mode
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isRegisterMode) {
                                        if (appLanguage == AppLanguage.HINDI) "पहले से खाता है? " else "Already have an account? "
                                    } else {
                                        if (appLanguage == AppLanguage.HINDI) "खाता नहीं है? " else "Don't have an account? "
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(color = CyberTextMuted)
                                )
                                Text(
                                    text = if (isRegisterMode) {
                                        if (appLanguage == AppLanguage.HINDI) "साइन इन करें" else "Sign In"
                                    } else {
                                        if (appLanguage == AppLanguage.HINDI) "प्रोफ़ाइल बनाएं" else "Create Profile"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = ElectricCyan,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier
                                        .clickable { isRegisterMode = !isRegisterMode }
                                        .testTag("toggle_auth_mode_btn")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
