package com.example.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Result of real-time multi-language call analysis.
 */
data class RealtimeCallThreatEvaluation(
    val riskScore: Int,
    val threatLevel: ThreatLevel,
    val detectedLanguage: String,
    val matchedScamKeywords: List<String>,
    val urgencyScore: Int,
    val acousticThreatScore: Int,
    val intentThreatScore: Int,
    val isDeepfakeAcoustics: Boolean,
    val summary: String,
    val recommendedAction: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Real-time Multilingual Call Security Analyzer.
 * Continuously evaluates live calls across multiple languages (English, Hindi, Hinglish, Spanish,
 * French, German, Mandarin, Arabic, Russian, Japanese, Indian Regional languages)
 * with extreme sensitivity to:
 * - Neural vocoder synthetic audio artifacts (Phase hops, Jitter < 0.08% or > 0.28%, High-freq smearing)
 * - Multilingual scam keywords & coercive pressure tactics
 * - Coercive urgency, digital arrest scripts, OTP demands, and virtual kidnapping claims
 */
class RealtimeMultilingualCallAnalyzer(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "MultilingualCallAnalyzer"

        // Multilingual Keyword Lexicon by Threat Category & Language
        private val MULTILINGUAL_LEXICON = mapOf(
            // English
            "en" to listOf(
                "digital arrest" to 95, "arrest warrant" to 92, "cbi officer" to 94, "customs parcel" to 88,
                "narcotics drugs" to 90, "send money immediately" to 85, "share your otp" to 96, "one time password" to 92,
                "wire transfer" to 75, "urgent payment" to 72, "tax fraud" to 80, "bank account blocked" to 86,
                "do not hang up" to 78, "virtual kidnapping" to 98, "bail money" to 90, "gift card payment" to 85,
                "verify identity now" to 68, "remote access" to 82, "law enforcement raid" to 90
            ),
            // Hindi (हिंदी)
            "hi" to listOf(
                "डिजिटल अरेस्ट" to 98, "क्राइम ब्रांच" to 92, "सीबीआई" to 95, "पुलिस रेड" to 90,
                "पार्सल में ड्रग्स" to 96, "नशीले पदार्थ" to 92, "बिजली बिल कटना" to 88, "खाता ब्लॉक" to 86,
                "ओटीपी बताओ" to 96, "पिन नंबर" to 90, "बच्चा अरेस्ट" to 98, "एक्सीडेंट हो गया" to 92,
                "जेल भेज देंगे" to 94, "कस्टम अधिकारी" to 90, "तुरंत पैसे भेजो" to 88, "यूपीआई पर पैसे" to 85,
                "फोन मत काटना" to 82, "कोर्ट का नोटिस" to 86, "आधार कार्ड लॉक" to 80
            ),
            // Hinglish (Code-switched)
            "hinglish" to listOf(
                "digital arrest" to 98, "bijli bill" to 88, "disconnect ho jayega" to 85, "police thana" to 92,
                "fir register" to 90, "account freeze" to 88, "otp share karo" to 96, "upi transfer karo" to 85,
                "parcel confiscated" to 92, "customs raid" to 90, "cyber crime officer" to 92,
                "phone disconnect mat karna" to 82, "immediate verification" to 75, "court summons" to 86
            ),
            // Spanish (Español)
            "es" to listOf(
                "secuestro virtual" to 98, "tenemos a tu hijo" to 98, "dinero urgente" to 85, "no cuelgues el teléfono" to 88,
                "policía aduana" to 90, "transferencia inmediata" to 84, "cuenta bancaria bloqueada" to 86,
                "código de verificación" to 94, "fianza urgente" to 90, "cartel" to 92, "abogado de oficio" to 75
            ),
            // French (Français)
            "fr" to listOf(
                "blocage compte bancaire" to 88, "mandat d'arrêt" to 95, "colis suspect douane" to 90,
                "virement urgent" to 86, "faux conseiller" to 88, "police nationale" to 90,
                "gendarmerie" to 90, "amende immédiate" to 82, "code de sécurité" to 92
            ),
            // German (Deutsch)
            "de" to listOf(
                "polizei haftbefehl" to 95, "zoll paket drogen" to 92, "bankkonto gesperrt" to 88,
                "enkeltrick" to 96, "schockanruf" to 96, "sofortüberweisung" to 85,
                "bundeskriminalamt" to 94, "staatsanwaltschaft" to 90, "kaution zahlen" to 92
            ),
            // Mandarin Chinese (中文)
            "zh" to listOf(
                "公检法" to 96, "涉嫌洗钱" to 96, "逮捕令" to 96, "海关违禁包裹" to 92,
                "银行卡冻结" to 90, "安全账户" to 94, "立即转账" to 88, "不要挂断电话" to 85,
                "引渡回国" to 95, "验证码" to 92
            ),
            // Arabic (العربية)
            "ar" to listOf(
                "تجميد الحساب" to 90, "الشرطة والمحكمة" to 94, "طرد جمركي مشبوه" to 90,
                "تحويل فوري" to 86, "رمز التحقق" to 94, "ابتزاز" to 92, "إيقاف الخدمات" to 88
            ),
            // Russian (Русский)
            "ru" to listOf(
                "следственный комитет" to 95, "блокировка счёта" to 90, "безопасный счёт" to 95,
                "уголовное дело" to 92, "фсб" to 95, "залог" to 90, "дистанционный арест" to 96
            ),
            // Japanese (日本語)
            "ja" to listOf(
                "オレオレ詐欺" to 98, "警察逮捕状" to 96, "銀行口座凍結" to 90,
                "示談金至急振込" to 94, "荷物税関" to 90, "暗証番号" to 95
            ),
            // Indian Regional (Tamil, Telugu, Bengali, Marathi)
            "regional_in" to listOf(
                "வங்கி கணக்கு" to 88, "காவல் துறை" to 92, "பணம் அனுப்பு" to 86,
                "ఖాతా బ్లాక్" to 88, "పోలీసు అరెస్ట్" to 92, "డబ్బులు పంపండి" to 86,
                "অ্যাকাউন্ট ব্লক" to 88, "পুলিশ গ্রেফতার" to 92, "টাকা পাঠান" to 86,
                "बँक खाते ब्लॉक" to 88, "पोलीस अटक" to 92, "पैसे पाठवा" to 86
            )
        )
    }

    private val _evaluationState = MutableStateFlow(
        RealtimeCallThreatEvaluation(
            riskScore = 4,
            threatLevel = ThreatLevel.SAFE,
            detectedLanguage = "Multilingual Scanner Active",
            matchedScamKeywords = emptyList(),
            urgencyScore = 0,
            acousticThreatScore = 3,
            intentThreatScore = 0,
            isDeepfakeAcoustics = false,
            summary = "Call Secure / No Synthetic Voice Detected",
            recommendedAction = "Normal call. Status shows \"Call Secure / No Synthetic Voice Detected\"."
        )
    )
    val evaluationState: StateFlow<RealtimeCallThreatEvaluation> = _evaluationState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val transcriptBuffer = StringBuilder()

    /**
     * Analyzes an ongoing audio frame with real-time acoustic DSP and multilingual keywords.
     * High sensitivity calibration:
     * - 0% – 9%: SAFE / LOW RISK
     * - 10% – 29%: SUSPICIOUS / ELEVATED
     * - 30% – 59%: HIGH RISK / DEEPFAKE WARNING
     * - 60% – 100%: CRITICAL THREAT / EMERGENCY
     */
    fun analyzeFrame(
        anomalyScore: Float,
        anomalyFlag: String?,
        pitchJitter: Float,
        vocoderRatio: Float,
        spectralFlatness: Float,
        isVoiceActive: Boolean,
        callerNumber: String = "",
        callerName: String = "",
        isContactWhitelisted: Boolean = false
    ): RealtimeCallThreatEvaluation {
        // 1. Acoustic Deepfake Calculation
        val vocoderRisk = (vocoderRatio * 100).toInt()
        val jitterAbnormal = pitchJitter < 0.008f || pitchJitter > 0.035f // Rigid monotone or artificial jitter
        val anomalyPercent = (anomalyScore * 100).toInt()

        var acousticScore = 4 // Clean human baseline in 0-9% range

        if (anomalyPercent >= 60 || vocoderRatio >= 0.28f) {
            acousticScore = max(acousticScore, min(98, (anomalyPercent * 1.05f).toInt()))
        } else if (anomalyPercent >= 28 || vocoderRatio >= 0.16f || jitterAbnormal) {
            acousticScore = max(acousticScore, min(58, max(32, (anomalyPercent * 1.15f).toInt())))
        } else if (anomalyPercent >= 10 || vocoderRatio >= 0.08f) {
            acousticScore = max(acousticScore, min(28, max(12, anomalyPercent)))
        } else {
            // Safe range: 0 - 9
            acousticScore = if (isContactWhitelisted) 3 else min(8, max(2, anomalyPercent))
        }

        // 2. Multilingual Keyword & Intent Scan from live buffer
        val currentText = transcriptBuffer.toString().lowercase(Locale.ROOT)
        var highestKeywordThreat = 0
        val matchedKeywords = mutableListOf<String>()
        var detectedLang = "English / Universal"

        for ((lang, keywords) in MULTILINGUAL_LEXICON) {
            for ((keyword, threatWeight) in keywords) {
                if (currentText.contains(keyword.lowercase(Locale.ROOT))) {
                    matchedKeywords.add(keyword)
                    if (threatWeight > highestKeywordThreat) {
                        highestKeywordThreat = threatWeight
                        detectedLang = when (lang) {
                            "hi" -> "Hindi (हिंदी)"
                            "hinglish" -> "Hinglish (Hindi-English)"
                            "es" -> "Spanish (Español)"
                            "fr" -> "French (Français)"
                            "de" -> "German (Deutsch)"
                            "zh" -> "Mandarin Chinese (中文)"
                            "ar" -> "Arabic (العربية)"
                            "ru" -> "Russian (Русский)"
                            "ja" -> "Japanese (日本語)"
                            "regional_in" -> "Indian Regional Language"
                            else -> "English (Global)"
                        }
                    }
                }
            }
        }

        // 3. Sensitive Unified Score Aggregation
        val intentScore = if (highestKeywordThreat > 0) highestKeywordThreat else 0
        val isDeepfake = acousticScore >= 30 || vocoderRatio >= 0.18f

        val finalScore = when {
            // Confirmed deepfake vocoder OR severe scam keyword -> Critical (60-100%)
            acousticScore >= 60 || intentScore >= 80 -> {
                max(62, max(acousticScore, intentScore)).coerceIn(60, 100)
            }
            // Moderate vocoder anomaly OR scam pressure keyword -> High Risk (30-59%)
            acousticScore >= 30 || intentScore >= 45 -> {
                max(32, max(acousticScore, intentScore)).coerceIn(30, 59)
            }
            // Minor jitter, unusual caller, unverified audio -> Suspicious (10-29%)
            acousticScore >= 10 || intentScore >= 15 || (!isContactWhitelisted && anomalyPercent >= 8) -> {
                max(12, max(acousticScore, intentScore)).coerceIn(10, 29)
            }
            // Clean natural human voice verified -> Safe (0-9%)
            else -> {
                min(8, acousticScore).coerceIn(0, 9)
            }
        }

        val level = ThreatLevel.fromScore(finalScore)

        val eval = RealtimeCallThreatEvaluation(
            riskScore = finalScore,
            threatLevel = level,
            detectedLanguage = detectedLang,
            matchedScamKeywords = matchedKeywords.distinct(),
            urgencyScore = if (finalScore >= 60) 90 else if (finalScore >= 30) 55 else 10,
            acousticThreatScore = acousticScore,
            intentThreatScore = intentScore,
            isDeepfakeAcoustics = isDeepfake,
            summary = when (level) {
                ThreatLevel.SAFE -> "Call Secure / No Synthetic Voice Detected"
                ThreatLevel.SUSPICIOUS -> "Live caution banner. Warns of unusual vocal jitter, pitch anomalies, or unverified caller patterns."
                ThreatLevel.HIGH_RISK -> "Persistent heads-up warning notification + audible warning pulse. Flags potential AI voice clone or high-pressure scam."
                ThreatLevel.CRITICAL -> "Urgent full-screen/overlay alert popup, continuous vibration alarm, and an instant \"Disconnect Call\" action button."
            },
            recommendedAction = level.systemActionDescription
        )

        _evaluationState.value = eval
        return eval
    }

    /**
     * Starts continuous real-time multilingual speech recognition if supported by Android subsystem.
     */
    fun startRealtimeTranscription() {
        if (isListening) return
        scope.launch(Dispatchers.Main) {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {}
                            override fun onBeginningOfSpeech() {}
                            override fun onRmsChanged(rmsdB: Float) {}
                            override fun onBufferReceived(buffer: ByteArray?) {}
                            override fun onEndOfSpeech() {}
                            override fun onError(error: Int) {
                                Log.w(TAG, "SpeechRecognizer warning: $error")
                            }

                            override fun onResults(results: Bundle?) {
                                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                matches?.firstOrNull()?.let { text ->
                                    appendLiveTranscript(text)
                                }
                            }

                            override fun onPartialResults(partialResults: Bundle?) {
                                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                matches?.firstOrNull()?.let { text ->
                                    appendLiveTranscript(text)
                                }
                            }

                            override fun onEvent(eventType: Int, params: Bundle?) {}
                        })
                    }

                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    }
                    speechRecognizer?.startListening(intent)
                    isListening = true
                    Log.i(TAG, "SpeechRecognizer started for real-time multilingual call analysis.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "SpeechRecognizer init failed (falling back to acoustic DSP): ${e.message}")
            }
        }
    }

    fun appendLiveTranscript(text: String) {
        if (text.isNotBlank()) {
            transcriptBuffer.append(" ").append(text)
            if (transcriptBuffer.length > 2000) {
                transcriptBuffer.delete(0, 1000)
            }
        }
    }

    fun stopRealtimeTranscription() {
        isListening = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        transcriptBuffer.clear()
    }
}
