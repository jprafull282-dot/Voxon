package com.example.data.model

/**
 * Enumerates real, implemented security features accessible from the Security Dashboard.
 * Clicking each feature opens a dedicated in-depth technical analysis view.
 */
enum class SecurityFeatureType(
    val title: String,
    val subtitle: String,
    val badge: String
) {
    VOICE_AUTHENTICITY(
        title = "Voice Authenticity & Anti-Spoofing",
        subtitle = "Real-time streaming deepfake detection powered by Aurigin.ai via secure backend proxy",
        badge = "ACTIVE"
    ),
    CONVERSATION_SCAM(
        title = "Conversation Intent Analysis",
        subtitle = "Semantic threat intelligence, financial demand detection, and digital arrest profiling",
        badge = "ACTIVE"
    ),
    ROLLING_RISK_ENGINE(
        title = "Multi-Signal Risk Aggregator",
        subtitle = "Temporal consecutive observation window preventing false alarms from single anomalies",
        badge = "ACTIVE"
    ),
    VOICE_ACTIVITY_DETECTOR(
        title = "Audio Preprocessing & VAD",
        subtitle = "16kHz PCM normalization and voice activity gating to conserve bandwidth and filter noise",
        badge = "ACTIVE"
    ),
    SCREEN_CAPTURE_GUARD(
        title = "Mobile Screen & Memory Guard",
        subtitle = "Hardware-enforced FLAG_SECURE preventing screenshot extortion and remote share snooping",
        badge = "ACTIVE"
    ),
    ZERO_STORAGE_VAULT(
        title = "Zero-Audio Forensic Vault",
        subtitle = "Privacy-first metadata persistence with SHA-256 evidence verification and zero raw audio",
        badge = "ACTIVE"
    )
}
