package com.example.engine

/**
 * Conversation / Scam Analysis Engine.
 * Evaluates semantic content and live transcripts to detect multi-factor social engineering tactics:
 * - Coercive Urgency & Pressure
 * - OTP & Credential Harvesting
 * - Authority & Legal Impersonation (Police, CBI, Customs, Court)
 * - Financial Extortion & Unverified Transfers
 *
 * Distinct from acoustic deepfake detection. Speech-to-text answers "What was said?",
 * while Aurigin answers "Does the voice appear synthetic?".
 */
class ConversationScamAnalyzer {

    data class AnalysisResult(
        val conversationRiskScore: Int,
        val detectedTactics: List<String>,
        val flaggedKeywords: List<String>,
        val confidence: Float,
        val explanation: String
    )

    private val urgencyPatterns = listOf(
        "immediately", "right now", "within 10 minutes", "urgent", "before your account is blocked",
        "do not hang up", "stay on line", "police arriving", "arrest warrant"
    )

    private val financialPatterns = listOf(
        "otp", "one time password", "pin", "cvv", "card number", "bank transfer",
        "security deposit", "wire transfer", "send money", "verify account", "upi id"
    )

    private val authorityPatterns = listOf(
        "cbi", "police officer", "customs department", "income tax", "rbi",
        "narcotics bureau", "supreme court", "digital arrest", "cyber crime branch",
        "fraud department", "trai", "sim deactivation"
    )

    private val extortionPatterns = listOf(
        "illegal package", "passport seized", "money laundering", "fir registered",
        "case filed", "legal penalty", "seized contraband", "confidential investigation"
    )

    /**
     * Evaluates the aggregated transcript text against threat patterns.
     */
    fun analyzeTranscript(fullText: String): AnalysisResult {
        if (fullText.isBlank() || fullText.length < 5) {
            return AnalysisResult(
                conversationRiskScore = 0,
                detectedTactics = emptyList(),
                flaggedKeywords = emptyList(),
                confidence = 0f,
                explanation = "Insufficient conversation data for intent profiling."
            )
        }

        val lower = fullText.lowercase()
        val detectedTactics = mutableListOf<String>()
        val flaggedKeywords = mutableListOf<String>()
        var score = 0

        // 1. Urgency evaluation
        val matchedUrgency = urgencyPatterns.filter { lower.contains(it) }
        if (matchedUrgency.isNotEmpty()) {
            detectedTactics.add("Artificial Urgency & Coercive Pressure")
            flaggedKeywords.addAll(matchedUrgency)
            score += 25
        }

        // 2. Financial / OTP demand
        val matchedFinancial = financialPatterns.filter { lower.contains(it) }
        if (matchedFinancial.isNotEmpty()) {
            detectedTactics.add("Credential / OTP Harvesting Attempt")
            flaggedKeywords.addAll(matchedFinancial)
            score += 35
        }

        // 3. Authority Impersonation
        val matchedAuthority = authorityPatterns.filter { lower.contains(it) }
        if (matchedAuthority.isNotEmpty()) {
            detectedTactics.add("Authority / Law Enforcement Impersonation")
            flaggedKeywords.addAll(matchedAuthority)
            score += 30
        }

        // 4. Extortion & Threats
        val matchedExtortion = extortionPatterns.filter { lower.contains(it) }
        if (matchedExtortion.isNotEmpty()) {
            detectedTactics.add("Extortion / Legal Penalty Intimidation")
            flaggedKeywords.addAll(matchedExtortion)
            score += 30
        }

        val finalScore = score.coerceIn(0, 100)
        val confidence = when {
            finalScore >= 70 -> 0.92f
            finalScore >= 40 -> 0.75f
            finalScore > 0 -> 0.50f
            else -> 0f
        }

        val explanation = when {
            finalScore >= 70 -> "High probability of coordinated social engineering scam detected across multiple behavioral indicators."
            finalScore >= 40 -> "Suspicious conversational patterns detected (financial/urgency keywords identified)."
            else -> "No coercive or fraudulent intent indicators detected in conversation."
        }

        return AnalysisResult(
            conversationRiskScore = finalScore,
            detectedTactics = detectedTactics.distinct(),
            flaggedKeywords = flaggedKeywords.distinct(),
            confidence = confidence,
            explanation = explanation
        )
    }
}
