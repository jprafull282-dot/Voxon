package com.example.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.data.model.CampaignEntity
import com.example.data.model.IncidentEntity
import com.example.data.model.PolicyEntity
import com.example.data.model.CallRecordingEntity
import com.example.data.model.CallMetadataEntity
import com.example.data.model.AnalyzedCallEntity
import com.example.data.model.CallLogEntity
import com.example.data.model.BlockedCallerEntity
import com.example.data.model.SecurityReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyzedCallDao {
    @Query("SELECT * FROM analyzed_calls ORDER BY timestamp DESC")
    fun getAllAnalyzedCalls(): Flow<List<AnalyzedCallEntity>>

    @Query("SELECT * FROM analyzed_calls ORDER BY timestamp DESC")
    suspend fun getAllAnalyzedCallsList(): List<AnalyzedCallEntity>

    @Query("SELECT * FROM analyzed_calls WHERE securityRiskLevel = :riskLevel ORDER BY timestamp DESC")
    fun getAnalyzedCallsByRisk(riskLevel: String): Flow<List<AnalyzedCallEntity>>

    @Query("SELECT * FROM analyzed_calls WHERE phoneNumber LIKE '%' || :query || '%' OR callerLabel LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchAnalyzedCalls(query: String): Flow<List<AnalyzedCallEntity>>

    @Query("SELECT * FROM analyzed_calls WHERE id = :id")
    suspend fun getAnalyzedCallById(id: String): AnalyzedCallEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalyzedCall(call: AnalyzedCallEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(calls: List<AnalyzedCallEntity>)

    @Update
    suspend fun updateAnalyzedCall(call: AnalyzedCallEntity)

    @Query("DELETE FROM analyzed_calls WHERE id = :id")
    suspend fun deleteAnalyzedCallById(id: String)

    @Query("DELETE FROM analyzed_calls")
    suspend fun clearAll()
}

@Dao
interface CallMetadataDao {
    @Query("SELECT * FROM call_metadata ORDER BY timestamp DESC")
    fun getAllCallMetadata(): Flow<List<CallMetadataEntity>>

    @Query("SELECT * FROM call_metadata ORDER BY timestamp DESC")
    suspend fun getAllCallMetadataList(): List<CallMetadataEntity>

    @Query("SELECT * FROM call_metadata WHERE callId = :callId")
    suspend fun getMetadataById(callId: String): CallMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: CallMetadataEntity)

    @Update
    suspend fun updateMetadata(metadata: CallMetadataEntity)

    @Query("DELETE FROM call_metadata WHERE callId = :callId")
    suspend fun deleteMetadataById(callId: String)

    @Query("DELETE FROM call_metadata")
    suspend fun clearAll()
}

@Dao
interface CallRecordingDao {
    @Query("SELECT * FROM call_recordings ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<CallRecordingEntity>>

    @Query("SELECT * FROM call_recordings ORDER BY timestamp DESC")
    suspend fun getAllRecordingsList(): List<CallRecordingEntity>

    @Query("SELECT * FROM call_recordings WHERE isDeepfake = 1 ORDER BY timestamp DESC")
    fun getDeepfakeRecordings(): Flow<List<CallRecordingEntity>>

    @Query("SELECT * FROM call_recordings WHERE id = :id")
    suspend fun getRecordingById(id: String): CallRecordingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: CallRecordingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recordings: List<CallRecordingEntity>)

    @Query("DELETE FROM call_recordings WHERE id = :id")
    suspend fun deleteRecording(id: String)

    @Query("DELETE FROM call_recordings")
    suspend fun clearAll()
}

@Dao
interface IncidentDao {
    @Query("SELECT * FROM incidents ORDER BY timestamp DESC")
    fun getAllIncidents(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE severity = :severity ORDER BY timestamp DESC")
    fun getIncidentsBySeverity(severity: String): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE id = :id")
    suspend fun getIncidentById(id: String): IncidentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(incidents: List<IncidentEntity>)

    @Update
    suspend fun updateIncident(incident: IncidentEntity)

    @Query("DELETE FROM incidents WHERE id = :id")
    suspend fun deleteIncidentById(id: String)

    @Query("DELETE FROM incidents")
    suspend fun clearAll()
}

@Dao
interface PolicyDao {
    @Query("SELECT * FROM policies")
    fun getAllPolicies(): Flow<List<PolicyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPolicy(policy: PolicyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(policies: List<PolicyEntity>)

    @Update
    suspend fun updatePolicy(policy: PolicyEntity)
}

@Dao
interface CampaignDao {
    @Query("SELECT * FROM campaigns")
    fun getAllCampaigns(): Flow<List<CampaignEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(campaigns: List<CampaignEntity>)
}

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogs(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    suspend fun getAllCallLogsList(): List<CallLogEntity>

    @Query("SELECT * FROM call_logs WHERE threatLevel = :threatLevel ORDER BY timestamp DESC")
    fun getCallLogsByThreatLevel(threatLevel: String): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE callerIdentifier LIKE '%' || :query || '%' OR callerName LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchCallLogs(query: String): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE id = :id")
    suspend fun getCallLogById(id: String): CallLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(callLogs: List<CallLogEntity>)

    @Update
    suspend fun updateCallLog(callLog: CallLogEntity)

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun deleteCallLogById(id: String)

    @Query("DELETE FROM call_logs")
    suspend fun clearAllCallLogs()
}

@Dao
interface BlockedCallerDao {
    @Query("SELECT * FROM blocked_callers ORDER BY blockedAt DESC")
    fun getAllBlockedCallers(): Flow<List<BlockedCallerEntity>>

    @Query("SELECT * FROM blocked_callers ORDER BY blockedAt DESC")
    suspend fun getAllBlockedCallersList(): List<BlockedCallerEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_callers WHERE phoneNumber = :phoneNumber)")
    suspend fun isBlocked(phoneNumber: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedCaller(caller: BlockedCallerEntity)

    @Query("DELETE FROM blocked_callers WHERE phoneNumber = :phoneNumber")
    suspend fun deleteBlockedCaller(phoneNumber: String)

    @Query("SELECT COUNT(*) FROM blocked_callers")
    fun getBlockedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM blocked_callers")
    suspend fun getBlockedCountSync(): Int

    @Query("DELETE FROM blocked_callers")
    suspend fun clearAll()
}

@Dao
interface SecurityReportDao {
    @Query("SELECT * FROM security_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<SecurityReportEntity>>

    @Query("SELECT * FROM security_reports ORDER BY timestamp DESC")
    suspend fun getAllReportsList(): List<SecurityReportEntity>

    @Query("SELECT * FROM security_reports WHERE id = :id")
    suspend fun getReportById(id: String): SecurityReportEntity?

    @Query("SELECT * FROM security_reports WHERE overallRiskScore >= :minRisk ORDER BY timestamp DESC")
    fun getReportsByMinRisk(minRisk: Int): Flow<List<SecurityReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: SecurityReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reports: List<SecurityReportEntity>)

    @Query("DELETE FROM security_reports WHERE id = :id")
    suspend fun deleteReportById(id: String)

    @Query("DELETE FROM security_reports")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM security_reports")
    fun getReportCount(): Flow<Int>
}

@Database(
    entities = [
        IncidentEntity::class,
        PolicyEntity::class,
        CampaignEntity::class,
        CallRecordingEntity::class,
        CallMetadataEntity::class,
        AnalyzedCallEntity::class,
        CallLogEntity::class,
        BlockedCallerEntity::class,
        SecurityReportEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun incidentDao(): IncidentDao
    abstract fun policyDao(): PolicyDao
    abstract fun campaignDao(): CampaignDao
    abstract fun callRecordingDao(): CallRecordingDao
    abstract fun callMetadataDao(): CallMetadataDao
    abstract fun analyzedCallDao(): AnalyzedCallDao
    abstract fun callLogDao(): CallLogDao
    abstract fun blockedCallerDao(): BlockedCallerDao
    abstract fun securityReportDao(): SecurityReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "voxen_security.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
