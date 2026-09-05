package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class CyberPalette(
    val bg: Color,
    val bgSecondary: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val cardBg: Color,
    val border: Color,
    val borderLight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val isDark: Boolean
)

val LightCyberPalette = CyberPalette(
    bg = Color(0xFFFFFFFF),
    bgSecondary = Color(0xFFF8FAFC),
    surface = Color(0xFFF1F5F9),
    surfaceElevated = Color(0xFFFFFFFF),
    cardBg = Color(0xFFFFFFFF),
    border = Color(0xFFE2E8F0),
    borderLight = Color(0xFFCBD5E1),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF64748B),
    isDark = false
)

val DarkCyberPalette = CyberPalette(
    bg = Color(0xFF0B0F17),
    bgSecondary = Color(0xFF111827),
    surface = Color(0xFF1E293B),
    surfaceElevated = Color(0xFF1F2937),
    cardBg = Color(0xFF111827),
    border = Color(0xFF334155),
    borderLight = Color(0xFF475569),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    isDark = true
)

val LocalCyberPalette = staticCompositionLocalOf { LightCyberPalette }

object CyberTheme {
    val colors: CyberPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalCyberPalette.current
}

// Dynamic Composable Color Getters for Day/Night Theme
val CyberBg: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCyberPalette.current.bg

val CyberBgSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCyberPalette.current.bgSecondary

val CyberSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCyberPalette.current.surface

val CyberSurfaceElevated: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCyberPalette.current.surfaceElevated

val CyberBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCyberPalette.current.border

val CyberBorderLight: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCyberPalette.current.borderLight

val CyberCardBg: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCyberPalette.current.cardBg

val CyberTextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCyberPalette.current.textPrimary

val CyberTextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCyberPalette.current.textSecondary

val CyberTextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCyberPalette.current.textMuted

// Threat Levels & Exact Visual Indicator Colors
val EmeraldGreen = Color(0xFF10B981)   // 0% – 9% SAFE / LOW RISK
val AmberWarning = Color(0xFFF59E0B)   // 10% – 29% SUSPICIOUS / ELEVATED
val VividOrange = Color(0xFFF97316)    // 30% – 59% HIGH RISK / DEEPFAKE WARNING
val CrimsonAlert = Color(0xFFEF4444)   // 60% – 100% CRITICAL THREAT / EMERGENCY

// Semantic Alert Aliases aligned with Threat Ranges
val AlertCrimson = CrimsonAlert
val AlertRed = CrimsonAlert
val AlertCrimsonContainer = Color(0xFFFEE2E2)

// Blue Accents & Modern Telemetry
val ElectricCyan = Color(0xFF2563EB)
val ElectricCyanDark = Color(0xFF1D4ED8)
val ElectricCyanLight = Color(0xFF3B82F6)

// Green Safe Status & Verification (Emerald Green #10B981)
val NeonEmerald = EmeraldGreen
val NeonEmeraldDark = Color(0xFF059669)
val NeonEmeraldGlow = Color(0xFF34D399)

// Yellow Warnings & Suspicious Markers (Amber Warning #F59E0B)
val WarningAmber = AmberWarning
val NeonAmber = AmberWarning
val WarningAmberContainer = Color(0xFFFEF3C7)

val CyberCardBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalCyberPalette.current.border

val CyberNeonGreen = EmeraldGreen
val CyberNeonRed = CrimsonAlert





