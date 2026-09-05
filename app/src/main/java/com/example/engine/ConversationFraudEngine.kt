package com.example.engine

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Two-Layer Conversation Fraud Engine.
 *
 * Answers strictly:
 * "Does this conversation show signs of fraud, impersonation, social engineering, or financial manipulation?"
 * It must NOT answer whether the voice is AI-generated (that is Aurigin's responsibility).
 *
 * LAYER A: Deterministic Security Signal Extraction
 * Detects objective high-risk indicators: OTP requests, UPI/financial transfer demands,
 * credential harvesting, remote-access requests, coercion/threats, urgency.
 *
 * LAYER B: Contextual LLM Analysis (Gemini API with fallback)
 * Evaluates contextual deception, subtle manipulation, and semantic context.
 * Requires strict JSON schema:
 * {
 *   "scamScore": Float,
 *   "confidence": Float,
 *   "category": String,
 *   "evidence": List<String>,
 *   "recommendedAction": String
 * }
 */
class ConversationFraudEngine(
    private val context: Context
) {

    companion object {
        private const val TAG = "ConversationFraudEngine"
        private const val GEMINI_MODEL = "gemini-2.5-flash"
        private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    data class SecuritySignal(
        val category: String,
        val description: String,
        val matchedTokens: List<String>,
        val riskWeight: Float
    )

    // --- Deterministic Layer A Pattern Dictionaries ---
    private val otpKeywords = listOf(
        "otp", "one time password", "verification code", "6-digit code", "pin", "cvv", "security code"
    )

    private val financialKeywords = listOf(
        "upi", "wire transfer", "bank transfer", "send money", "verify account", "account number",
        "transfer funds", "security deposit", "payment link", "qr code", "bizum"
    )

    private val credentialKeywords = listOf(
        "password", "login credentials", "netbanking password", "debit card", "credit card", "mother's maiden name"
    )

    private val remoteAccessKeywords = listOf(
        "anydesk", "teamviewer", "quicksupport", "rustdesk", "screen share", "install app", "download apk",
        "remote support", "grant permission", "accessibility service"
    )

    private val urgencyKeywords = listOf(
        "immediately", "right now", "within 10 minutes", "urgent", "before midnight", "do not hang up",
        "stay on the line", "time is running out", "final notice"
    )

    private val authorityKeywords = listOf(
        "cbi", "police", "customs department", "income tax", "rbi", "narcotics", "supreme court",
        "cyber crime branch", "trai", "telecom authority", "interpol", "federal agent"
    )

    private val coercionKeywords = listOf(
        "digital arrest", "arrest warrant", "fir filed", "sim disconnection", "illegal parcel",
        "drugs seized", "money laundering", "passport seized", "penal action", "jail"
    )

    private val secrecyKeywords = listOf(
        "do not tell anyone", "confidential investigation", "keep this secret", "do not tell your family",
        "leave the room", "do not consult anyone"
    )

    /**
     * Executes two-layer analysis on the transcribed speech and returns a standardized EngineResult.
     */
    suspend fun analyzeConversation(
        transcript: String,
        analyzedDurationSeconds: Float = 0f
    ): EngineResult = withContext(Dispatchers.IO) {
        val trimmed = transcript.trim()
        val wordCount = if (trimmed.isBlank()) 0 else trimmed.split("\\s+".toRegex()).size

        if (wordCount < 4) {
            return@withContext EngineResult.insufficientData(
                engine = "CONVERSATION",
                message = "Insufficient conversation transcript to profile linguistic intent."
            )
        }

        // LAYER A: Deterministic Security Signals
        val layerASignals = extractLayerASignals(trimmed)
        val layerARiskScore = calculateLayerARisk(layerASignals)

        // LAYER B: Contextual Analysis (via Gemini if API key configured, otherwise enhanced heuristic)
        val (layerBScore, layerBConfidence, layerBCategory, layerBEvidence) = executeLayerB(trimmed)

        // Combine Layer A & Layer B without letting LLM directly dictate final score
        val combinedScore = if (layerASignals.isNotEmpty()) {
            // When hard deterministic triggers exist (OTP, wire, arrest), Layer A carries heavy weight
            (layerARiskScore * 0.60f + layerBScore * 0.40f).coerceIn(0f, 1f)
        } else {
            (layerBScore * 0.85f).coerceIn(0f, 1f)
        }

        val confidence = if (layerASignals.isNotEmpty()) {
            max(0.85f, layerBConfidence)
        } else {
            layerBConfidence
        }

        val allEvidence = mutableListOf<String>()
        layerASignals.forEach { allEvidence.add("${it.category}: ${it.description} (${it.matchedTokens.joinToString()})") }
        layerBEvidence.forEach { if (!allEvidence.contains(it)) allEvidence.add(it) }

        val evidenceQuality = EvidenceQualityCalculator.computeConversationEvidenceQuality(
            transcriptWordCount = wordCount,
            detectedTacticsCount = layerASignals.size,
            llmConfidence = confidence
        )

        val verdict = when {
            combinedScore >= 0.70f -> EngineVerdict.HIGH
            combinedScore >= 0.30f -> EngineVerdict.ELEVATED
            combinedScore > 0.05f -> EngineVerdict.LOW
            else -> EngineVerdict.LOW
        }

        return@withContext EngineResult(
            engine = "CONVERSATION",
            score = combinedScore,
            confidence = confidence,
            verdict = verdict,
            evidenceQuality = evidenceQuality,
            evidence = allEvidence,
            analyzedDuration = analyzedDurationSeconds,
            status = EngineStatus.AVAILABLE
        )
    }

    private fun extractLayerASignals(text: String): List<SecuritySignal> {
        val lower = text.lowercase()
        val signals = mutableListOf<SecuritySignal>()

        val otpMatches = otpKeywords.filter { lower.contains(it) }
        if (otpMatches.isNotEmpty()) {
            signals.add(SecuritySignal("OTP / CODE HARVESTING", "Direct request for OTP or verification code", otpMatches, 0.45f))
        }

        val finMatches = financialKeywords.filter { lower.contains(it) }
        if (finMatches.isNotEmpty()) {
            signals.add(SecuritySignal("FINANCIAL TRANSFER DEMAND", "Request for UPI, wire transfer, or fund redirection", finMatches, 0.35f))
        }

        val credMatches = credentialKeywords.filter { lower.contains(it) }
        if (credMatches.isNotEmpty()) {
            signals.add(SecuritySignal("CREDENTIAL HARVESTING", "Solicitation of banking credentials or passwords", credMatches, 0.40f))
        }

        val remoteMatches = remoteAccessKeywords.filter { lower.contains(it) }
        if (remoteMatches.isNotEmpty()) {
            signals.add(SecuritySignal("REMOTE ACCESS ATTEMPT", "Instructing user to install remote support or screen sharing tool", remoteMatches, 0.45f))
        }

        val urgencyMatches = urgencyKeywords.filter { lower.contains(it) }
        if (urgencyMatches.isNotEmpty()) {
            signals.add(SecuritySignal("COERCIVE URGENCY", "Artificial deadline or high-pressure timeline", urgencyMatches, 0.25f))
        }

        val authMatches = authorityKeywords.filter { lower.contains(it) }
        if (authMatches.isNotEmpty()) {
            signals.add(SecuritySignal("AUTHORITY IMPERSONATION", "Claiming representation of law enforcement, bank, or government", authMatches, 0.35f))
        }

        val coercionMatches = coercionKeywords.filter { lower.contains(it) }
        if (coercionMatches.isNotEmpty()) {
            signals.add(SecuritySignal("EXTORTION / LEGAL THREAT", "Threat of digital arrest, SIM disconnection, or legal prosecution", coercionMatches, 0.40f))
        }

        val secrecyMatches = secrecyKeywords.filter { lower.contains(it) }
        if (secrecyMatches.isNotEmpty()) {
            signals.add(SecuritySignal("ISOLATION / SECRECY", "Demanding caller not consult relatives or advisors", secrecyMatches, 0.30f))
        }

        return signals
    }

    private fun calculateLayerARisk(signals: List<SecuritySignal>): Float {
        if (signals.isEmpty()) return 0f
        var sum = 0f
        signals.forEach { sum += it.riskWeight }
        return sum.coerceIn(0f, 1f)
    }

    private suspend fun executeLayerB(transcript: String): LayerBOutput {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey.isEmpty() || apiKey.equals("MY_GEMINI_API_KEY", ignoreCase = true)) {
            return fallbackLayerB(transcript)
        }

        return try {
            val systemInstruction = """
                You are a real-time Fraud Intent Classifier for VoiceGuard.
                Evaluate the transcript solely for social engineering, financial fraud, impersonation, or coercion.
                DO NOT guess or evaluate voice synthesis.
                You MUST return ONLY valid JSON conforming to:
                {
                  "scamScore": 0.0 to 1.0,
                  "confidence": 0.0 to 1.0,
                  "category": "NONE" | "DIGITAL_ARREST" | "OTP_THEFT" | "BANKING_IMPERSONATION" | "VIRTUAL_KIDNAPPING" | "OTHER_FRAUD",
                  "evidence": ["short string 1", "short string 2"],
                  "recommendedAction": "short instruction"
                }
            """.trimIndent()

            val prompt = "$systemInstruction\n\nTRANSCRIPT:\n\"$transcript\""

            val jsonBody = JSONObject().apply {
                val contentsArr = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArr = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", partsArr)
                    }
                    put(contentObj)
                }
                put("contents", contentsArr)
                put("generationConfig", JSONObject().apply {
                    put("response_mime_type", "application/json")
                    put("temperature", 0.1)
                })
            }

            val request = Request.Builder()
                .url("$GEMINI_BASE_URL?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            if (response.isSuccessful && respBody.isNotBlank()) {
                parseGeminiJson(respBody)
            } else {
                fallbackLayerB(transcript)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini Layer B evaluation error: ${e.message}. Using deterministic fallback.")
            fallbackLayerB(transcript)
        }
    }

    private fun parseGeminiJson(rawResponse: String): LayerBOutput {
        try {
            val root = JSONObject(rawResponse)
            val candidates = root.optJSONArray("candidates") ?: return fallbackLayerB("")
            if (candidates.length() == 0) return fallbackLayerB("")

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content") ?: return fallbackLayerB("")
            val parts = content.optJSONArray("parts") ?: return fallbackLayerB("")
            if (parts.length() == 0) return fallbackLayerB("")

            val text = parts.getJSONObject(0).optString("text", "")
            val parsedJson = JSONObject(text.trim())

            val score = parsedJson.optDouble("scamScore", 0.0).toFloat().coerceIn(0f, 1f)
            val confidence = parsedJson.optDouble("confidence", 0.7).toFloat().coerceIn(0f, 1f)
            val category = parsedJson.optString("category", "NONE")

            val evidenceList = mutableListOf<String>()
            val evidenceArr = parsedJson.optJSONArray("evidence")
            if (evidenceArr != null) {
                for (i in 0 until evidenceArr.length()) {
                    evidenceList.add(evidenceArr.optString(i))
                }
            }

            return LayerBOutput(
                scamScore = score,
                confidence = confidence,
                category = category,
                evidence = evidenceList
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Gemini JSON output: ${e.message}")
            return fallbackLayerB("")
        }
    }

    private fun fallbackLayerB(transcript: String): LayerBOutput {
        val lower = transcript.lowercase()
        val evidence = mutableListOf<String>()
        var score = 0f

        if (lower.contains("digital arrest") || lower.contains("fir registered")) {
            score += 0.85f
            evidence.add("Digital Arrest or coercive criminal accusation phrasing")
        }
        if (lower.contains("otp") || lower.contains("verification code")) {
            score += 0.75f
            evidence.add("Security verification code request detected")
        }
        if (lower.contains("transfer") || lower.contains("upi") || lower.contains("send money")) {
            score += 0.60f
            evidence.add("Direct financial transfer instructions")
        }

        return LayerBOutput(
            scamScore = score.coerceIn(0f, 1f),
            confidence = if (score > 0f) 0.80f else 0.50f,
            category = if (score >= 0.70f) "SUSPECTED_FRAUD" else "NONE",
            evidence = evidence
        )
    }

    private data class LayerBOutput(
        val scamScore: Float,
        val confidence: Float,
        val category: String,
        val evidence: List<String>
    )
}
