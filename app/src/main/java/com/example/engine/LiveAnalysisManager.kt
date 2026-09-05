package com.example.engine

import android.content.Context
import android.util.Log
import com.example.data.model.SecurityReportEntity
import com.example.data.repository.VoiceGuardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

/**
 * Single Authoritative Call & Live Analysis Manager.
 *
 * Orchestrates the full real-time forensic pipeline:
 *
 * LIVE CALL AUDIO
 *      ↓
 * AUDIO QUALITY CHECK (AudioQualityChecker)
 *      ↓
 * VOICE ACTIVITY DETECTION (VoiceActivityDetector)
 *      ↓
 * REAL-TIME AUDIO WINDOWS
 *      ↓                              ↓
 * ENGINE 1: AURIGIN           ENGINE 2: CONVERSATION FRAUD ENGINE
 * (Voice Authenticity Only)    (Two-Layer Social Engineering Intent)
 *      ↓                              ↓
 *      └──────────────┬───────────────┘
 *                     ↓
 *           EVIDENCE QUALITY EVALUATION
 *                     ↓
 *           TEMPORAL RISK AGGREGATOR
 *                     ↓
 *             RISK FUSION ENGINE
 *                     ↓
 *           FINAL RISK ASSESSMENT (3 Independent Scores & Verdict)
 *                     ↓
 *         SECURITY REPORT VAULT PERSISTENCE
 */
class LiveAnalysisManager(
    private val context: Context,
    private val repository: VoiceGuardRepository,
    private val voiceDetector: VoiceAuthenticityDetector = AuriginVoiceDetector(),
    private val conversationEngine: ConversationFraudEngine = ConversationFraudEngine(context)
) {
    companion object {
        private const val TAG = "LiveAnalysisManager"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var detectorJob: Job? = null

    // Authoritative audio source manager (Option A: VoIP, Option B: Telephony backend, Local Mic)
    val audioSourceManager = CallAudioSourceManager(context) { pcmChunk ->
        handleAudioChunk(pcmChunk)
    }

    // Authoritative single fusion engine
    private val riskFusionEngine = RiskFusionEngine()

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _finalRiskAssessment = MutableStateFlow<FinalRiskAssessment?>(null)
    val finalRiskAssessment: StateFlow<FinalRiskAssessment?> = _finalRiskAssessment.asStateFlow()

    private val _latestVoiceEngineResult = MutableStateFlow<EngineResult?>(null)
    val latestVoiceEngineResult: StateFlow<EngineResult?> = _latestVoiceEngineResult.asStateFlow()

    private val _latestConvEngineResult = MutableStateFlow<EngineResult?>(null)
    val latestConvEngineResult: StateFlow<EngineResult?> = _latestConvEngineResult.asStateFlow()

    private val _isSpeechDetected = MutableStateFlow(false)
    val isSpeechDetected: StateFlow<Boolean> = _isSpeechDetected.asStateFlow()

    private val _activeSessionId = MutableStateFlow("")
    val activeSessionId: StateFlow<String> = _activeSessionId.asStateFlow()

    private val _callerName = MutableStateFlow("Unknown Caller")
    val callerName: StateFlow<String> = _callerName.asStateFlow()

    private val _callerNumber = MutableStateFlow("+91 98765 43210")
    val callerNumber: StateFlow<String> = _callerNumber.asStateFlow()

    private var sessionStartTimeMs = 0L
    private val aggregatedTranscript = StringBuilder()
    private var analyzedWindowsCount = 0

    init {
        // Observe Engine 1 (Aurigin) streaming results
        detectorJob = voiceDetector.detectionResults.onEach { detectionResult ->
            handleVoiceDetectionResult(detectionResult)
        }.launchIn(scope)
    }

    /**
     * Call received / ringing state.
     * MANDATORY LIFECYCLE RULE: While RINGING, audio capture and analysis are strictly disabled.
     */
    fun onCallRinging(callerName: String, callerNumber: String) {
        _callState.value = CallState.RINGING
        _callerName.value = callerName
        _callerNumber.value = callerNumber
        Log.i(TAG, "Call RINGING from $callerNumber. Audio pipeline inactive.")
    }

    /**
     * User answers the call. Live audio streaming and analysis pipeline initializes.
     */
    fun onCallAnswered(callerName: String = _callerName.value, callerNumber: String = _callerNumber.value) {
        if (_callState.value == CallState.ANALYZING) return

        _callState.value = CallState.ANSWERED
        _callerName.value = callerName
        _callerNumber.value = callerNumber
        sessionStartTimeMs = System.currentTimeMillis()
        val sessionId = "sess_${System.currentTimeMillis()}"
        _activeSessionId.value = sessionId

        aggregatedTranscript.clear()
        analyzedWindowsCount = 0
        riskFusionEngine.reset()
        audioSourceManager.resetTelemetry()

        Log.i(TAG, "Call ANSWERED. Starting real-time analysis pipeline for session $sessionId")

        _callState.value = CallState.ANALYZING
        scope.launch {
            // Connect Engine 1 (Aurigin) through secure backend proxy
            voiceDetector.connect(sessionId)

            // Start hardware audio capture (Option: local mic uplink)
            audioSourceManager.startHardwareCapture()
        }
    }

    /**
     * Ingests remote PCM audio from an accessible controlled media source
     * (Option A: VoIP/WebRTC, Option B: Telephony Backend Media Stream, or Ground-Truth test feed).
     */
    fun ingestRemoteAudio(pcmBytes: ByteArray, source: AudioSourceType) {
        if (_callState.value != CallState.ANALYZING) return
        audioSourceManager.ingestRemoteAudioChunk(pcmBytes, source)
    }

    /**
     * Ingests live streaming transcript from STT for Engine 2 (Conversation Fraud Engine).
     */
    fun onTranscriptReceived(text: String) {
        if (_callState.value != CallState.ANALYZING || text.isBlank()) return

        aggregatedTranscript.append(" ").append(text)
        scope.launch {
            val speechDuration = audioSourceManager.telemetry.value.voiceActiveDurationSeconds
            val convResult = conversationEngine.analyzeConversation(
                transcript = aggregatedTranscript.toString(),
                analyzedDurationSeconds = speechDuration
            )
            _latestConvEngineResult.value = convResult
            triggerFusionEvaluation()
        }
    }

    private fun handleAudioChunk(pcmChunk: ByteArray) {
        if (_callState.value != CallState.ANALYZING || pcmChunk.isEmpty()) return

        analyzedWindowsCount++
        _isSpeechDetected.value = true

        // Forward audio window to Engine 1 (Aurigin) via secure backend proxy
        scope.launch {
            voiceDetector.sendAudioChunk(pcmChunk)
        }
    }

    private fun handleVoiceDetectionResult(detectionResult: DetectionResult) {
        val telemetry = audioSourceManager.telemetry.value
        val speechDuration = telemetry.voiceActiveDurationSeconds
        val qualityScore = telemetry.qualityScore

        val engineResult = if (detectionResult.isTechnicalError) {
            EngineResult.error("AURIGIN", detectionResult.errorMessage ?: "Technical connection error")
        } else {
            val verdict = when {
                detectionResult.isSynthetic && detectionResult.confidence >= 0.70f -> EngineVerdict.HIGH
                detectionResult.isSynthetic -> EngineVerdict.ELEVATED
                detectionResult.confidence >= 0.70f -> EngineVerdict.LOW
                else -> EngineVerdict.UNKNOWN
            }

            val evidenceQuality = EvidenceQualityCalculator.computeVoiceEvidenceQuality(
                speechDurationSec = speechDuration,
                audioQualityScore = qualityScore,
                detectorConfidence = detectionResult.confidence,
                consistentWindowCount = analyzedWindowsCount
            )

            val score = if (detectionResult.isSynthetic) {
                detectionResult.confidence
            } else {
                (1.0f - detectionResult.confidence).coerceAtLeast(0.02f)
            }

            EngineResult(
                engine = "AURIGIN",
                score = score,
                confidence = detectionResult.confidence,
                verdict = verdict,
                evidenceQuality = evidenceQuality,
                evidence = detectionResult.characteristics,
                analyzedDuration = speechDuration,
                timestamp = detectionResult.timestamp,
                status = EngineStatus.AVAILABLE
            )
        }

        _latestVoiceEngineResult.value = engineResult
        triggerFusionEvaluation()
    }

    private fun triggerFusionEvaluation() {
        val voiceRes = _latestVoiceEngineResult.value
        val convRes = _latestConvEngineResult.value
        val isSpeech = audioSourceManager.telemetry.value.voiceActiveDurationSeconds > 0.5f

        val assessment = riskFusionEngine.evaluate(
            voiceResult = voiceRes,
            conversationResult = convRes,
            replayResult = null,
            isSpeechActive = isSpeech
        )

        _finalRiskAssessment.value = assessment

        // Synchronize UI CallState with final verdict
        if (_callState.value == CallState.ANALYZING) {
            when (assessment.finalVerdict) {
                FinalRiskVerdict.CRITICAL, FinalRiskVerdict.HIGH_RISK -> _callState.value = CallState.HIGH_RISK
                FinalRiskVerdict.SUSPICIOUS -> _callState.value = CallState.SUSPICIOUS
                FinalRiskVerdict.SAFE -> _callState.value = CallState.SAFE
                FinalRiskVerdict.INCONCLUSIVE -> { /* keep analyzing */ }
            }
        }
    }

    /**
     * Terminates call, releases capture and network resources,
     * persists forensic SecurityReportEntity to Room, and discards raw audio.
     */
    fun onCallEnded(): SecurityReportEntity? {
        Log.i(TAG, "Call ENDED. Stopping pipelines and generating forensic report.")
        _callState.value = CallState.ENDED

        audioSourceManager.stopAllCapture()
        scope.launch {
            voiceDetector.disconnect()
        }

        val durationSec = if (sessionStartTimeMs > 0) {
            ((System.currentTimeMillis() - sessionStartTimeMs) / 1000).toInt().coerceAtLeast(1)
        } else 0

        val assessment = _finalRiskAssessment.value
        val voiceRes = _latestVoiceEngineResult.value

        val overallRisk = assessment?.overallThreatScore ?: 0
        val aiConfidence = assessment?.aiVoiceScore ?: 0f
        val convRisk = ((assessment?.conversationFraudScore ?: 0f) * 100).toInt()

        val voiceVerdict = when {
            voiceRes == null || voiceRes.status != EngineStatus.AVAILABLE -> "INCONCLUSIVE"
            voiceRes.score >= 0.70f -> "SYNTHETIC"
            voiceRes.score < 0.30f -> "SAFE"
            else -> "SUSPICIOUS"
        }

        val threatLevelStr = assessment?.finalVerdict?.name ?: "SAFE"
        val detectedIndicators = assessment?.detectedTactics ?: emptyList()
        val indicatorsString = if (detectedIndicators.isEmpty()) "Clean Audio Stream" else detectedIndicators.joinToString(", ")
        val evidenceSummary = assessment?.explanation ?: "Call completed with natural voice dynamics."
        val recommendations = assessment?.recommendedAction ?: "No security action required."

        val currentTs = System.currentTimeMillis()
        val rawHashData = "${_activeSessionId.value}:${_callerNumber.value}:$overallRisk:$aiConfidence:$currentTs"
        val sha256 = hashSha256(rawHashData)

        val report = SecurityReportEntity(
            id = "REP-${UUID.randomUUID().toString().take(8).uppercase()}",
            callSessionId = _activeSessionId.value.ifEmpty { "sess_$currentTs" },
            callerName = _callerName.value,
            callerNumber = _callerNumber.value,
            timestamp = currentTs,
            durationSeconds = durationSec,
            voiceVerdict = voiceVerdict,
            aiVoiceConfidence = aiConfidence,
            conversationRiskScore = convRisk,
            overallRiskScore = overallRisk,
            threatLevel = threatLevelStr,
            detectedIndicators = indicatorsString,
            evidenceSummary = evidenceSummary,
            recommendations = recommendations,
            detectorName = "Aurigin.ai & Two-Layer Fraud Engine",
            analysisStatus = if (assessment?.finalVerdict == FinalRiskVerdict.INCONCLUSIVE) "INCONCLUSIVE" else "COMPLETED",
            latencyMs = 45L,
            evidenceHashSha256 = sha256
        )

        scope.launch {
            repository.saveSecurityReport(report)
        }

        return report
    }

    private fun hashSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
