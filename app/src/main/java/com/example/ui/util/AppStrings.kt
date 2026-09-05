package com.example.ui.util

import com.example.ui.AppLanguage

object AppStrings {

    fun appName(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "VOXEN"
        AppLanguage.HINDI -> "वॉक्सन (VOXEN)"
    }

    fun appTagline(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Zero-Trust AI Voice & Call Defense"
        AppLanguage.HINDI -> "शून्य-विश्वास एआई वॉयस और कॉल सुरक्षा"
    }

    // Navigation
    fun tabHome(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Home"
        AppLanguage.HINDI -> "होम"
    }

    fun tabCallDashboard(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Call SOC"
        AppLanguage.HINDI -> "कॉल डैशबोर्ड"
    }

    fun tabDialer(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Dialer"
        AppLanguage.HINDI -> "डायलर"
    }

    fun tabVault(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Vault"
        AppLanguage.HINDI -> "वॉल्ट"
    }

    fun tabSecurity(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Security"
        AppLanguage.HINDI -> "सुरक्षा"
    }

    fun tabProfile(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Profile"
        AppLanguage.HINDI -> "प्रोफ़ाइल"
    }

    // Profile & Appearance
    fun profileTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "USER IDENTITY & ACCESS"
        AppLanguage.HINDI -> "उपयोगकर्ता पहचान और सेटिंग्स"
    }

    fun authenticatedStatus(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "AUTHENTICATED PROFILE"
        AppLanguage.HINDI -> "सत्यापित खाता"
    }

    fun loginRegisterTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "LOGIN / REGISTRATION"
        AppLanguage.HINDI -> "लॉगिन / पंजीकरण"
    }

    fun themeSectionTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "APPEARANCE & THEME"
        AppLanguage.HINDI -> "थीम और दिखावट (Day/Night)"
    }

    fun darkModeTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Night Mode (Dark Theme)"
        AppLanguage.HINDI -> "नाइट मोड (डार्क थीम)"
    }

    fun lightModeTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Day Mode (Light Theme)"
        AppLanguage.HINDI -> "डे मोड (लाइट थीम)"
    }

    fun modeActive(lang: AppLanguage, isDark: Boolean): String = when (lang) {
        AppLanguage.ENGLISH -> if (isDark) "Night Mode Active 🌙" else "Day Mode Active ☀️"
        AppLanguage.HINDI -> if (isDark) "नाइट मोड सक्रिय 🌙" else "डे मोड सक्रिय ☀️"
    }

    fun switchThemePrompt(lang: AppLanguage, isDark: Boolean): String = when (lang) {
        AppLanguage.ENGLISH -> if (isDark) "Switch to Day Mode (Light)" else "Switch to Night Mode (Dark)"
        AppLanguage.HINDI -> if (isDark) "डे मोड (लाइट) में बदलें" else "नाइट मोड (डार्क) में बदलें"
    }

    // Sun and Half Moon Theme
    fun sunMoonToggleLabel(lang: AppLanguage, isDark: Boolean): String = when (lang) {
        AppLanguage.ENGLISH -> if (isDark) "☀️ Day Mode" else "🌓 Night Mode"
        AppLanguage.HINDI -> if (isDark) "☀️ डे मोड (सन)" else "🌓 नाइट मोड (हाफ मून)"
    }

    fun sunMoonToggleDesc(lang: AppLanguage, isDark: Boolean): String = when (lang) {
        AppLanguage.ENGLISH -> if (isDark) "Single-click to convert to Day Mode" else "Single-click to convert to Night Mode"
        AppLanguage.HINDI -> if (isDark) "1-क्लिक में डे मोड में बदलें" else "1-क्लिक में नाइट मोड में बदलें"
    }

    // Home Cockpit Strings
    fun homeHeroTitle(lang: AppLanguage, isShieldActive: Boolean): String = when (lang) {
        AppLanguage.ENGLISH -> if (isShieldActive) "VOXEN ZERO-TRUST DEFENSE: ACTIVE" else "VOXEN CALL DEFENSE: MUTED"
        AppLanguage.HINDI -> if (isShieldActive) "वॉक्सन ज़ीरो-ट्रस्ट सुरक्षा: सक्रिय" else "वॉक्सन सुरक्षा: म्यूट की गई"
    }

    fun homeHeroDesc(lang: AppLanguage, isShieldActive: Boolean): String = when (lang) {
        AppLanguage.ENGLISH -> if (isShieldActive) "On-Device Neural Vocoder Intercept • 16kHz DSP Active" else "Tap master shield to reactivate continuous AI voice security"
        AppLanguage.HINDI -> if (isShieldActive) "ऑन-डिवाइस न्यूरल वोकोडर इंटरसेप्ट • 16kHz DSP सक्रिय" else "निरंतर AI वॉयस सुरक्षा सक्रिय करने के लिए शील्ड पर टैप करें"
    }

    fun quickActionsTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "RAPID THREAT SIMULATIONS & AUDIT"
        AppLanguage.HINDI -> "त्वरित खतरा सिमुलेशन और ऑडिट"
    }

    fun systemHealthTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "SECURITY METRICS & VAULT STATUS"
        AppLanguage.HINDI -> "सुरक्षा मेट्रिक्स और वॉल्ट स्थिति"
    }

    fun recentInterceptsTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "RECENT CALL FORENSIC LOGS"
        AppLanguage.HINDI -> "हालिया कॉल फॉरेंसिक लॉग"
    }

    fun languageSectionTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "APP INTERFACE LANGUAGE"
        AppLanguage.HINDI -> "ऐप इंटरफ़ेस भाषा"
    }

    fun currentLanguageLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "English (Selected)"
        AppLanguage.HINDI -> "हिंदी (चयनित)"
    }

    fun switchLanguageBtn(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Switch to हिंदी (Hindi)"
        AppLanguage.HINDI -> "Switch to English"
    }

    fun permissionsSectionTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "SYSTEM SECURITY PERMISSIONS"
        AppLanguage.HINDI -> "सिस्टम सुरक्षा अनुमतियां"
    }

    fun permissionsSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "One-click access for real-time deepfake audio scan, call protection & emergency alert."
        AppLanguage.HINDI -> "रीयल-टाइम डीपफेक ऑडियो स्कैन, कॉल सुरक्षा और आपातकालीन अलर्ट के लिए एकल क्लिक अनुमति।"
    }

    fun grantAllPermissionsBtn(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "🛡️ GRANT ALL PERMISSIONS (1-CLICK)"
        AppLanguage.HINDI -> "🛡️ सभी अनुमतियां एक साथ प्रदान करें"
    }

    fun allPermissionsGranted(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "✅ All Core Permissions Active & Guarding"
        AppLanguage.HINDI -> "✅ सभी अनुमतियां सक्रिय और सुरक्षित हैं"
    }

    fun logOutBtn(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Log Out"
        AppLanguage.HINDI -> "लॉग आउट"
    }

    fun signInBtn(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Sign In"
        AppLanguage.HINDI -> "साइन इन"
    }

    fun createAccountBtn(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Create Account"
        AppLanguage.HINDI -> "नया खाता बनाएं"
    }
}
