package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.model.CampaignEntity
import com.example.data.model.IncidentEntity
import com.example.data.model.PolicyEntity
import com.example.data.model.CallRecordingEntity
import com.example.data.model.CallMetadataEntity
import com.example.data.model.AnalyzedCallEntity
import com.example.data.model.CallLogEntity
import com.example.data.model.BlockedCallerEntity
import com.example.data.model.SecurityReportEntity
import com.example.engine.CallAudioRecorder
import com.example.engine.RiskEngineResult
import com.example.engine.SecurityEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class VoiceGuardRepository(
    private val db: AppDatabase,
    private val context: Context? = null
) {

    val allIncidents: Flow<List<IncidentEntity>> = db.incidentDao().getAllIncidents()
    val allPolicies: Flow<List<PolicyEntity>> = db.policyDao().getAllPolicies()
    val allCampaigns: Flow<List<CampaignEntity>> = db.campaignDao().getAllCampaigns()
    val allRecordings: Flow<List<CallRecordingEntity>> = db.callRecordingDao().getAllRecordings()
    val allCallMetadata: Flow<List<CallMetadataEntity>> = db.callMetadataDao().getAllCallMetadata()
    val allAnalyzedCalls: Flow<List<AnalyzedCallEntity>> = db.analyzedCallDao().getAllAnalyzedCalls()
    val allCallLogs: Flow<List<CallLogEntity>> = db.callLogDao().getAllCallLogs()
    val allBlockedCallers: Flow<List<BlockedCallerEntity>> = db.blockedCallerDao().getAllBlockedCallers()
    val blockedCountFlow: Flow<Int> = db.blockedCallerDao().getBlockedCount()
    val allSecurityReports: Flow<List<SecurityReportEntity>> = db.securityReportDao().getAllReports()

    suspend fun initializeDefaultDataIfEmpty() = withContext(Dispatchers.IO) {
        val defaultPolicies = listOf(
            PolicyEntity(
                id = "pol_01",
                name = "Zero-Trust Financial Protection",
                condition = "IF AI_Voice > 80% AND Payment_Context == HIGH",
                action = "Immediate Screen Shield + Block Audio + Send SOC Alert",
                enabled = true,
                triggerCount = 48
            ),
            PolicyEntity(
                id = "pol_02",
                name = "Continuous Speaker Shift Challenge",
                condition = "IF Speaker_Match < 50% for > 15s in Active Call",
                action = "Prompt Secondary Biometric / Out-of-Band Auth",
                enabled = true,
                triggerCount = 19
            ),
            PolicyEntity(
                id = "pol_03",
                name = "Known Campaign Interception",
                condition = "IF Caller_Fingerprint in Threat_Intel_Graph",
                action = "Auto-Mute & Play Anti-Scam Advisory",
                enabled = true,
                triggerCount = 112
            ),
            PolicyEntity(
                id = "pol_04",
                name = "Indian Multilingual Urgency Shield",
                condition = "IF Language in [HI, MR, TE, TA, BN] AND Urgency_Score > 85",
                action = "Display Real-Time Native Language Threat Badge",
                enabled = true,
                triggerCount = 87
            )
        )
        db.policyDao().insertAll(defaultPolicies)

        val defaultCampaigns = listOf(
            CampaignEntity(
                id = "camp_101",
                name = "Fake Banking / KYC Deepfake Wave",
                affectedUsers = 1284,
                regions = "Delhi NCR, Mumbai, Bengaluru, Hyderabad",
                riskLevel = "CRITICAL",
                indicators = "SBI / HDFC voice clone | OTP theft | WhatsApp screen share",
                status = "ACTIVE_CONTAINMENT"
            ),
            CampaignEntity(
                id = "camp_102",
                name = "Virtual Kidnapping / Distress Clone",
                affectedUsers = 439,
                regions = "North & West Zones",
                riskLevel = "CRITICAL",
                indicators = "Family voice clone | High urgency crying | Immediate UPI demand",
                status = "MONITORING"
            ),
            CampaignEntity(
                id = "camp_103",
                name = "Telecom Digital Arrest Extortion",
                affectedUsers = 812,
                regions = "Nationwide (Metro Focus)",
                riskLevel = "HIGH",
                indicators = "TRAI / Police impersonation | Fake warrant PDF | Video interrogation",
                status = "ACTIVE_CONTAINMENT"
            )
        )
        db.campaignDao().insertAll(defaultCampaigns)
    }

    suspend fun saveCallLog(callLog: CallLogEntity) = withContext(Dispatchers.IO) {
        db.callLogDao().insertCallLog(callLog)
    }

    suspend fun getCallLogById(id: String): CallLogEntity? = withContext(Dispatchers.IO) {
        db.callLogDao().getCallLogById(id)
    }

    suspend fun deleteCallLog(id: String) = withContext(Dispatchers.IO) {
        db.callLogDao().deleteCallLogById(id)
    }

    suspend fun clearAllCallLogs() = withContext(Dispatchers.IO) {
        db.callLogDao().clearAllCallLogs()
    }

    suspend fun saveAnalyzedCall(call: AnalyzedCallEntity) = withContext(Dispatchers.IO) {
        db.analyzedCallDao().insertAnalyzedCall(call)
    }

    suspend fun getAnalyzedCallById(id: String): AnalyzedCallEntity? = withContext(Dispatchers.IO) {
        db.analyzedCallDao().getAnalyzedCallById(id)
    }

    suspend fun deleteAnalyzedCall(id: String) = withContext(Dispatchers.IO) {
        db.analyzedCallDao().deleteAnalyzedCallById(id)
    }

    suspend fun clearAllAnalyzedCalls() = withContext(Dispatchers.IO) {
        db.analyzedCallDao().clearAll()
    }

    suspend fun saveCallRecording(recording: CallRecordingEntity) = withContext(Dispatchers.IO) {
        db.callRecordingDao().insertRecording(recording)
    }

    suspend fun saveCallMetadata(metadata: CallMetadataEntity) = withContext(Dispatchers.IO) {
        db.callMetadataDao().insertMetadata(metadata)
    }

    suspend fun updateCallMetadata(metadata: CallMetadataEntity) = withContext(Dispatchers.IO) {
        db.callMetadataDao().updateMetadata(metadata)
    }

    suspend fun getCallMetadataById(callId: String): CallMetadataEntity? = withContext(Dispatchers.IO) {
        db.callMetadataDao().getMetadataById(callId)
    }

    suspend fun deleteCallMetadata(callId: String) = withContext(Dispatchers.IO) {
        db.callMetadataDao().deleteMetadataById(callId)
    }

    suspend fun clearAllCallMetadata() = withContext(Dispatchers.IO) {
        db.callMetadataDao().clearAll()
    }

    suspend fun deleteRecording(id: String) = withContext(Dispatchers.IO) {
        db.callRecordingDao().deleteRecording(id)
    }

    suspend fun clearAllRecordings() = withContext(Dispatchers.IO) {
        db.callRecordingDao().clearAll()
    }

    suspend fun recordCallEvaluation(
        callerNumber: String,
        callerLabel: String,
        language: String,
        threatType: String,
        result: RiskEngineResult
    ) = withContext(Dispatchers.IO) {
        val incident = IncidentEntity(
            id = "inc_${UUID.randomUUID().toString().take(8)}",
            timestamp = System.currentTimeMillis(),
            callerNumber = callerNumber,
            callerLabel = callerLabel,
            threatType = threatType,
            riskScore = result.finalRiskScore,
            severity = result.verdict.name,
            aiProbability = result.profile.aiVoiceProbability,
            spectralAnomaly = result.profile.spectralAnomaly,
            phaseConsistency = result.profile.phaseConsistency,
            prosodyNaturalness = result.profile.prosodyNaturalness,
            speakerConfidence = result.speakerTimeline.lastOrNull()?.confidence?.div(100f) ?: 0.5f,
            language = language,
            attackStory = result.attackStory,
            attackChain = result.attackChain.joinToString(" | "),
            evidenceHash = result.evidenceHash,
            status = if (result.finalRiskScore >= 80) "BLOCKED" else if (result.finalRiskScore >= 50) "FLAGGED" else "VERIFIED",
            isResolved = false
        )
        db.incidentDao().insertIncident(incident)
    }

    suspend fun togglePolicy(policy: PolicyEntity) = withContext(Dispatchers.IO) {
        db.policyDao().updatePolicy(policy.copy(enabled = !policy.enabled))
    }

    suspend fun resolveIncident(incident: IncidentEntity) = withContext(Dispatchers.IO) {
        db.incidentDao().updateIncident(incident.copy(isResolved = true, status = "VERIFIED_SAFE"))
    }

    suspend fun deleteIncident(id: String) = withContext(Dispatchers.IO) {
        db.incidentDao().deleteIncidentById(id)
    }

    suspend fun clearAllIncidents() = withContext(Dispatchers.IO) {
        db.incidentDao().clearAll()
    }

    suspend fun blockCaller(
        phoneNumber: String,
        callerName: String = "Unknown Caller",
        reason: String = "Blocked from Call Dashboard (Deepfake / Fraud Threat)",
        riskScore: Int = 85,
        threatCategory: String = "Fraud / Acoustic Impersonation"
    ) = withContext(Dispatchers.IO) {
        val entry = BlockedCallerEntity(
            phoneNumber = phoneNumber,
            callerName = callerName,
            blockedAt = System.currentTimeMillis(),
            reason = reason,
            riskScore = riskScore,
            threatCategory = threatCategory
        )
        db.blockedCallerDao().insertBlockedCaller(entry)
    }

    suspend fun unblockCaller(phoneNumber: String) = withContext(Dispatchers.IO) {
        db.blockedCallerDao().deleteBlockedCaller(phoneNumber)
    }

    suspend fun isCallerBlocked(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        db.blockedCallerDao().isBlocked(phoneNumber)
    }

    suspend fun clearAllBlockedCallers() = withContext(Dispatchers.IO) {
        db.blockedCallerDao().clearAll()
    }

    suspend fun saveSecurityReport(report: SecurityReportEntity) = withContext(Dispatchers.IO) {
        db.securityReportDao().insertReport(report)
    }

    suspend fun getAllSecurityReportsList(): List<SecurityReportEntity> = withContext(Dispatchers.IO) {
        db.securityReportDao().getAllReportsList()
    }

    suspend fun getSecurityReportById(id: String): SecurityReportEntity? = withContext(Dispatchers.IO) {
        db.securityReportDao().getReportById(id)
    }

    suspend fun deleteSecurityReport(id: String) = withContext(Dispatchers.IO) {
        db.securityReportDao().deleteReportById(id)
    }

    suspend fun clearAllSecurityReports() = withContext(Dispatchers.IO) {
        db.securityReportDao().clearAll()
    }
}
