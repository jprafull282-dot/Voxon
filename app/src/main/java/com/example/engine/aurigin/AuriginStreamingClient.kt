package com.example.engine.aurigin

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AuriginDetectionResult(
    val isSynthetic: Boolean,
    val confidence: Float,
    val riskScore: Int,
    val verdict: String,
    val characteristics: List<String>,
    val latencyMs: Int,
    val isTechnicalError: Boolean = false,
    val errorMessage: String? = null
)

/**
 * AuriginStreamingClient
 *
 * Implements real-time streaming anti-spoofing and deepfake detection through
 * the VoiceGuard backend proxy.
 * Architecture: LIVE CALL -> Audio Record (VAD) -> WebSocket -> VoiceGuard Backend -> Aurigin API -> Risk Engine.
 */
class AuriginStreamingClient(
    private val serverBaseUrl: String,
    private val scope: CoroutineScope,
    private val onResult: (AuriginDetectionResult) -> Unit
) {
    companion object {
        private const val TAG = "AuriginStreamClient"
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    @Volatile
    private var isConnected = false

    fun startSession(sessionId: String) {
        val wsUrl = convertToWsUrl(serverBaseUrl) + "/ws/aurigin-stream"
        Log.d(TAG, "Connecting to Aurigin live stream: $wsUrl for session $sessionId")

        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                Log.d(TAG, "Aurigin WebSocket stream connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch(Dispatchers.Default) {
                    try {
                        val json = JSONObject(text)
                        val type = json.optString("type")
                        if (type == "AURIGIN_DETECTION_RESULT") {
                            val resObj = json.optJSONObject("result") ?: return@launch
                            val isTechErr = resObj.optBoolean("isTechnicalError", false)
                            if (isTechErr) {
                                val msg = resObj.optString("message", "Service temporarily unavailable")
                                Log.w(TAG, "Aurigin technical status: $msg")
                                onResult(
                                    AuriginDetectionResult(
                                        isSynthetic = false,
                                        confidence = 0f,
                                        riskScore = 0,
                                        verdict = "UNAVAILABLE",
                                        characteristics = emptyList(),
                                        latencyMs = resObj.optInt("latencyMs", 0),
                                        isTechnicalError = true,
                                        errorMessage = msg
                                    )
                                )
                            } else {
                                val isSynth = resObj.optBoolean("isSynthetic", false)
                                val conf = resObj.optDouble("confidence", 0.0).toFloat()
                                val risk = resObj.optInt("riskScore", 0)
                                val verdict = resObj.optString("verdict", "real")
                                val latency = resObj.optInt("latencyMs", 0)
                                val charsArray = resObj.optJSONArray("characteristics")
                                val charsList = mutableListOf<String>()
                                if (charsArray != null) {
                                    for (i in 0 until charsArray.length()) {
                                        charsList.add(charsArray.getString(i))
                                    }
                                }

                                onResult(
                                    AuriginDetectionResult(
                                        isSynthetic = isSynth,
                                        confidence = conf,
                                        riskScore = risk,
                                        verdict = verdict,
                                        characteristics = charsList,
                                        latencyMs = latency,
                                        isTechnicalError = false
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing Aurigin detection message", e)
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.w(TAG, "Aurigin WebSocket stream disconnected/unavailable: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                Log.d(TAG, "Aurigin WebSocket stream closed: $code / $reason")
            }
        })
    }

    fun sendAudioChunk(pcmChunk: ByteArray) {
        if (!isConnected || webSocket == null) return
        try {
            // Send as base64 JSON payload to ensure cross-platform compatibility
            val b64 = Base64.encodeToString(pcmChunk, Base64.NO_WRAP)
            val payload = JSONObject().apply {
                put("type", "AUDIO_CHUNK")
                put("audioBase64", b64)
                put("sampleRate", 16000)
            }
            webSocket?.send(payload.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send audio chunk to Aurigin WebSocket", e)
        }
    }

    fun close() {
        try {
            webSocket?.close(1000, "Call Ended")
            webSocket = null
            isConnected = false
        } catch (e: Exception) {
            Log.e(TAG, "Error closing Aurigin stream", e)
        }
    }

    private fun convertToWsUrl(url: String): String {
        return when {
            url.startsWith("https://") -> "wss://" + url.removePrefix("https://")
            url.startsWith("http://") -> "ws://" + url.removePrefix("http://")
            else -> "ws://$url"
        }
    }
}
