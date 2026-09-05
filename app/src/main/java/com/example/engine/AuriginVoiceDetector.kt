package com.example.engine

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Implementation of VoiceAuthenticityDetector that streams live audio to the
 * secure VoiceGuard backend proxy, which forwards requests to the official Aurigin.ai API.
 *
 * CRITICAL SECURITY PRINCIPLE:
 * The Aurigin API key is NEVER stored or handled on the Android client.
 * All credentials reside strictly on the backend server.
 */
class AuriginVoiceDetector(
    private val backendBaseUrl: String = DEFAULT_BACKEND_URL
) : VoiceAuthenticityDetector {

    companion object {
        private const val TAG = "AuriginVoiceDetector"
        // 10.0.2.2 is Android emulator host loopback; on real device, point to backend server
        const val DEFAULT_BACKEND_URL = "http://10.0.2.2:8000"
        const val DEFAULT_WS_URL = "ws://10.0.2.2:8000/ws/aurigin-stream"
    }

    override val detectorName: String = "Aurigin.ai (Secure Backend Proxy)"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _detectionResults = MutableSharedFlow<DetectionResult>(extraBufferCapacity = 64)
    override val detectionResults: SharedFlow<DetectionResult> = _detectionResults.asSharedFlow()

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var isConnectedState = false

    private var activeSessionId: String = ""

    // Buffer audio chunks locally if websocket is reconnecting or streaming
    private val pcmBuffer = ByteArrayOutputStream()
    private val bufferLock = Any()

    override suspend fun connect(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        disconnect()
        activeSessionId = sessionId
        synchronized(bufferLock) {
            pcmBuffer.reset()
        }

        val wsUrl = backendBaseUrl.replace("http://", "ws://").replace("https://", "wss://") + "/ws/aurigin-stream"
        Log.i(TAG, "Connecting to secure Aurigin backend proxy: $wsUrl for session $sessionId")

        return@withContext try {
            val request = Request.Builder().url(wsUrl).build()
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    isConnectedState = true
                    Log.i(TAG, "Connected to Aurigin backend proxy stream")
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    handleServerMessage(text)
                }

                override fun onMessage(ws: WebSocket, bytes: ByteString) {
                    handleServerBinary(bytes)
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    isConnectedState = false
                    Log.w(TAG, "WebSocket failure: ${t.message}. Emitting technical notice.")
                    scope.launch {
                        _detectionResults.emit(
                            DetectionResult(
                                isSynthetic = false,
                                confidence = 0f,
                                riskScore = 0,
                                verdict = "INCONCLUSIVE",
                                isTechnicalError = true,
                                errorMessage = "Aurigin backend proxy connection error: ${t.message}"
                            )
                        )
                    }
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    isConnectedState = false
                    Log.i(TAG, "Aurigin WebSocket closed: $code $reason")
                }
            })
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to backend proxy: ${e.message}")
            isConnectedState = false
            _detectionResults.emit(
                DetectionResult(
                    isSynthetic = false,
                    confidence = 0f,
                    riskScore = 0,
                    verdict = "INCONCLUSIVE",
                    isTechnicalError = true,
                    errorMessage = "Aurigin proxy unavailable: ${e.message}"
                )
            )
            false
        }
    }

    override suspend fun sendAudioChunk(pcmChunk: ByteArray): Boolean = withContext(Dispatchers.IO) {
        if (pcmChunk.isEmpty()) return@withContext false

        val ws = webSocket
        if (ws != null && isConnectedState) {
            try {
                // Send raw PCM binary frame directly through WebSocket
                ws.send(pcmChunk.toByteString())
                return@withContext true
            } catch (e: Exception) {
                Log.w(TAG, "Error sending chunk over WS: ${e.message}. Buffering.")
            }
        }

        // If WebSocket is not yet open or active, buffer the audio and attempt REST proxy if needed
        synchronized(bufferLock) {
            pcmBuffer.write(pcmChunk)
            // If we have >= 2 seconds of 16kHz audio (64,000 bytes) and WebSocket is inactive, use REST proxy
            if (pcmBuffer.size() >= 32000 && !isConnectedState) {
                val bufferedData = pcmBuffer.toByteArray()
                pcmBuffer.reset()
                scope.launch {
                    fallbackRestPredict(bufferedData)
                }
            }
        }
        return@withContext false
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        isConnectedState = false
        try {
            webSocket?.close(1000, "Call finished")
        } catch (e: Exception) {
            Log.w(TAG, "Error closing WebSocket: ${e.message}")
        }
        webSocket = null
        synchronized(bufferLock) {
            pcmBuffer.reset()
        }
    }

    override fun isConnected(): Boolean = isConnectedState

    private fun handleServerMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type")
            if (type == "AURIGIN_DETECTION_RESULT") {
                val resObj = json.optJSONObject("result") ?: return
                val isTechnicalError = resObj.optBoolean("isTechnicalError", false)
                val status = resObj.optString("status", "SUCCESS")

                if (isTechnicalError || status == "KEY_REQUIRED" || status == "SERVICE_UNAVAILABLE") {
                    val msg = resObj.optString("message", "Service unavailable")
                    scope.launch {
                        _detectionResults.emit(
                            DetectionResult(
                                isSynthetic = false,
                                confidence = 0f,
                                riskScore = 0,
                                verdict = "INCONCLUSIVE",
                                isTechnicalError = true,
                                errorMessage = msg
                            )
                        )
                    }
                    return
                }

                val verdict = resObj.optString("verdict", "real").uppercase()
                val isSynthetic = resObj.optBoolean("isSynthetic", false)
                val confidence = resObj.optDouble("confidence", 0.0).toFloat()
                val riskScore = resObj.optInt("riskScore", 0)
                val latencyMs = resObj.optLong("latencyMs", 0L)

                val charsList = mutableListOf<String>()
                val charsArr = resObj.optJSONArray("characteristics")
                if (charsArr != null) {
                    for (i in 0 until charsArr.length()) {
                        charsList.add(charsArr.optString(i))
                    }
                }

                val result = DetectionResult(
                    isSynthetic = isSynthetic,
                    confidence = confidence,
                    riskScore = riskScore,
                    verdict = verdict,
                    latencyMs = latencyMs,
                    characteristics = charsList,
                    isTechnicalError = false
                )
                scope.launch {
                    _detectionResults.emit(result)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing Aurigin proxy message: ${e.message}")
        }
    }

    private fun handleServerBinary(bytes: ByteString) {
        // Optional binary telemetry protocol handling
    }

    private suspend fun fallbackRestPredict(pcmBytes: ByteArray) = withContext(Dispatchers.IO) {
        try {
            val base64Data = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
            val requestBody = JSONObject().apply {
                put("sessionId", activeSessionId)
                put("audioBase64", base64Data)
                put("sampleRate", 16000)
            }

            val mediaType = "application/json".toMediaTypeOrNull()
            val body = okhttp3.RequestBody.create(
                mediaType,
                requestBody.toString()
            )
            val request = Request.Builder()
                .url("$backendBaseUrl/api/v1/aurigin/predict")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val respStr = response.body?.string()
            if (response.isSuccessful && respStr != null) {
                val json = JSONObject(respStr)
                handleServerMessage(json.toString())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallback REST predict failed: ${e.message}")
        }
    }
}
