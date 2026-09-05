package com.example.engine

/**
 * Authoritative Call State Machine for VoiceGuard.
 * Strictly controls lifecycle transitions:
 * IDLE -> RINGING -> ANSWERED -> ANALYZING -> SAFE / SUSPICIOUS / HIGH_RISK -> ENDED
 *
 * CRITICAL RULE:
 * While RINGING, audio capture, Aurigin detection, STT, and threat popups are STRICTLY FORBIDDEN.
 * Live audio streaming begins ONLY after the call is ANSWERED.
 */
enum class CallState {
    IDLE,
    RINGING,
    ANSWERED,
    ANALYZING,
    SAFE,
    SUSPICIOUS,
    HIGH_RISK,
    ENDED
}
