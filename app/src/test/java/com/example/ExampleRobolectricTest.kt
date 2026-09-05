package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.SecurityReportEntity
import com.example.engine.AuriginVoiceDetector
import com.example.engine.ConversationScamAnalyzer
import com.example.engine.RollingRiskEngine
import com.example.engine.VoiceActivityDetector
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("VoiceGuard", appName)
  }

  @Test
  fun `voice activity detector distinguishes speech from silence`() {
    val vad = VoiceActivityDetector(energyThreshold = 0.02f)

    // Test with silence frame (all zeros)
    val silenceBuffer = ShortArray(512) { 0 }
    val silenceRes = vad.processFrame(silenceBuffer, 512)
    assertFalse("Silence buffer should not trigger active speech", silenceRes.isSpeech)

    // Test with simulated active speech frame (loud sine wave)
    val speechBuffer = ShortArray(512) { (sin(it * 0.2) * 16000).toInt().toShort() }
    val speechRes = vad.processFrame(speechBuffer, 512)
    assertTrue("High energy buffer should detect speech", speechRes.isSpeech)
    assertTrue("Decibels should be above -30 dB for loud speech", speechRes.decibels > -30f)
  }

  @Test
  fun `conversation scam analyzer detects financial urgency and digital arrest keywords`() {
    val analyzer = ConversationScamAnalyzer()

    val normalTranscript = "Hello, I am calling to confirm our meeting tomorrow morning."
    val safeRes = analyzer.analyzeTranscript(normalTranscript)
    assertEquals(0, safeRes.conversationRiskScore)
    assertTrue(safeRes.detectedScamPhrases.isEmpty())

    val scamTranscript = "Sir, this is the police regarding a digital arrest warrant. Share your bank OTP immediately."
    val threatRes = analyzer.analyzeTranscript(scamTranscript)
    assertTrue("Scam score should be high for digital arrest & OTP demand", threatRes.conversationRiskScore >= 70)
    assertTrue("Should flag OTP keyword", threatRes.flaggedKeywords.contains("otp"))
    assertNotNull(threatRes.recommendedAction)
  }

  @Test
  fun `rolling risk engine suppresses single weak anomaly and triggers on sustained threat`() {
    val engine = RollingRiskEngine(windowSize = 5, alertThreshold = 60)

    val weakAcoustic = AuriginVoiceDetector.AcousticDetails(0.02f, 0.03f, 0.04f, 0.95f)
    val weakAnomalyResult = AuriginVoiceDetector.AuriginDetectionResult(
      verdict = "SUSPICIOUS",
      aiVoiceProbability = 0.40f,
      confidence = 0.70f,
      riskScore = 40,
      latencyMs = 25,
      detectedIndicators = listOf("Minor high-frequency variance"),
      acousticDetails = weakAcoustic
    )
    val emptyScam = ConversationScamAnalyzer.AnalysisResult(0, emptyList(), emptyList(), "Normal", null)

    // 1st frame with weak anomaly: Should NOT trigger popup (Requirement 8)
    val eval1 = engine.evaluate(weakAnomalyResult, emptyScam)
    assertFalse("Single isolated weak anomaly should not trigger alert popup", eval1.shouldTriggerAlert)

    // High confidence synthetic deepfake frames
    val severeSynthetic = AuriginVoiceDetector.AuriginDetectionResult(
      verdict = "SYNTHETIC",
      aiVoiceProbability = 0.92f,
      confidence = 0.96f,
      riskScore = 92,
      latencyMs = 28,
      detectedIndicators = listOf("Neural vocoder phase discontinuity"),
      acousticDetails = AuriginVoiceDetector.AcousticDetails(0.08f, 0.90f, 0.95f, 0.10f)
    )

    val eval2 = engine.evaluate(severeSynthetic, emptyScam)
    assertTrue("High-confidence synthetic voice clone must trigger alert", eval2.shouldTriggerAlert)
    assertNotNull(eval2.alertTitle)
    assertNotNull(eval2.recommendedAction)
  }

  @Test
  fun `room database stores and retrieves structured security reports without audio recordings`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    val dao = db.securityReportDao()
    assertEquals(0, dao.getAllReportsList().size)

    val report = SecurityReportEntity(
      id = "VG-1042",
      timestamp = System.currentTimeMillis(),
      durationSeconds = 45,
      callerNumber = "+91 98765 43210",
      callerLabel = "Suspected Voice Clone",
      overallRiskScore = 91,
      overallRiskLevel = "HIGH_RISK",
      voiceAuthenticityResult = "Potentially Synthetic",
      aiVoiceProbability = 0.91f,
      threatProbability = 0.85f,
      auriginVerdict = "SYNTHETIC",
      auriginConfidence = 0.96f,
      threatClassification = "Deepfake Audio Impersonation",
      detectedEventsCount = 2,
      conversationRiskScore = 80,
      conversationRiskSummary = "Demand for OTP and biometric verification",
      detectedPatterns = "digital arrest, otp",
      securityRecommendations = "Do not share OTP, banking credentials, passwords, or financial information.",
      analyzedSegmentsCount = 24,
      speechDetectedPercent = 78,
      averageLatencyMs = 31,
      isSufficientData = true,
      evidenceHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    )

    dao.insertReport(report)

    val storedList = dao.getAllReportsList()
    assertEquals(1, storedList.size)
    val fetched = storedList.first()
    assertEquals("VG-1042", fetched.id)
    assertEquals(91, fetched.overallRiskScore)
    assertEquals("Potentially Synthetic", fetched.voiceAuthenticityResult)
    assertEquals("SYNTHETIC", fetched.auriginVerdict)
    assertTrue(fetched.evidenceHash.isNotEmpty())

    db.close()
  }
}
