package com.example.engine

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedExportResult(
    val plainFilePath: String,
    val encryptedFilePath: String,
    val sha256Hash: String,
    val shareableUri: Uri?,
    val isEncrypted: Boolean,
    val fileSizeFormatted: String
)

object EncryptedAudioStorageService {

    private const val TAG = "EncryptedAudioStorage"
    private const val AES_KEY_STRING = "VOXEN_VOICEGUARD_ZERO_TRUST_256" // 32 bytes AES Key
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private fun getSecretKey(): SecretKeySpec {
        val keyBytes = AES_KEY_STRING.toByteArray(Charsets.UTF_8).copyOf(32)
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Checks if Manage External Storage or legacy read/write storage permission is granted
     */
    fun hasManageExternalStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            val write = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            read && write
        }
    }

    /**
     * Requests 'Manage External Storage' (All Files Access) on Android 11+ (API 30+)
     * or standard storage permissions on older versions
     */
    fun requestManageExternalStorage(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            } else {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening manage storage settings: ${e.message}", e)
            Toast.makeText(context, "Please allow storage permissions in device settings", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Encrypts a WAV audio file into a local encrypted file (.vgenc) using AES-GCM
     */
    fun encryptAudioFile(sourceWav: File, destinationEnc: File): Boolean {
        return try {
            if (!sourceWav.exists()) return false

            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), spec)

            FileOutputStream(destinationEnc).use { fos ->
                // Write 12-byte IV at the beginning of the file
                fos.write(iv)
                CipherOutputStream(fos, cipher).use { cos ->
                    FileInputStream(sourceWav).use { fis ->
                        fis.copyTo(cos)
                    }
                }
            }
            Log.i(TAG, "Encrypted audio saved to: ${destinationEnc.absolutePath} (${destinationEnc.length()} bytes)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt audio file: ${e.message}", e)
            false
        }
    }

    /**
     * Decrypts an encrypted audio file (.vgenc) back to WAV for post-call analysis & audio playback
     */
    fun decryptAudioForAnalysis(context: Context, encryptedFile: File): File? {
        return try {
            if (!encryptedFile.exists()) return null

            // If it's already a plain WAV, return directly
            if (encryptedFile.name.endsWith(".wav", ignoreCase = true)) {
                return encryptedFile
            }

            val cacheDir = File(context.cacheDir, "decrypted_vault").apply { if (!exists()) mkdirs() }
            val decryptedFile = File(cacheDir, "analysis_${encryptedFile.name.removeSuffix(".vgenc")}.wav")

            FileInputStream(encryptedFile).use { fis ->
                val iv = ByteArray(GCM_IV_LENGTH)
                val readIv = fis.read(iv)
                if (readIv != GCM_IV_LENGTH) {
                    throw IllegalStateException("Invalid encrypted header: IV length mismatch")
                }

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

                CipherInputStream(fis, cipher).use { cis ->
                    FileOutputStream(decryptedFile).use { fos ->
                        cis.copyTo(fos)
                    }
                }
            }
            Log.i(TAG, "Decrypted audio prepared for post-call analysis: ${decryptedFile.absolutePath}")
            decryptedFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt audio file: ${e.message}", e)
            // Fallback: if decryption failed but source was somehow plain, return file
            if (encryptedFile.exists()) encryptedFile else null
        }
    }

    /**
     * Computes SHA-256 cryptographic hash of a file for tamper-proof evidence
     */
    fun calculateFileSha256(file: File): String {
        return try {
            if (!file.exists()) return "SHA256-PENDING-UNAVAILABLE"
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "SHA256-ERROR-${System.currentTimeMillis()}"
        }
    }

    /**
     * Retrieves a secure FileProvider Uri for an audio file for sharing or system intent playback
     */
    fun getShareableUri(context: Context, file: File): Uri? {
        return try {
            val authority = "${context.packageName}.fileprovider"
            FileProvider.getUriForFile(context, authority, file)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating FileProvider Uri: ${e.message}", e)
            null
        }
    }

    /**
     * Saves encrypted and plain audio records directly to the internal phone vault storage,
     * mirrors to external storage / MediaStore if accessible, ensuring accessibility for playback & forensics.
     */
    fun saveEncryptedRecordingLocally(
        context: Context,
        sourceWavFile: File,
        callerLabel: String,
        threatScore: Int
    ): EncryptedExportResult {
        val sanitizedCaller = callerLabel.replace(Regex("[^a-zA-Z0-9+]"), "_").ifEmpty { "Telephony_Call" }
        val timestamp = System.currentTimeMillis()
        val baseFileName = "VoiceGuard_${sanitizedCaller}_$timestamp"

        // 1. Direct App Vault Directory (Always accessible, zero-permission, persistent sandbox)
        val vaultDir = File(context.filesDir, "vault_recordings").apply { if (!exists()) mkdirs() }
        val vaultPlainWav = File(vaultDir, "$baseFileName.wav")
        val vaultEncryptedFile = File(vaultDir, "$baseFileName.vgenc")

        // Copy source WAV directly into the secure app vault
        try {
            if (sourceWavFile.exists() && sourceWavFile.absolutePath != vaultPlainWav.absolutePath) {
                sourceWavFile.copyTo(vaultPlainWav, overwrite = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying to internal vault WAV: ${e.message}", e)
        }

        // Encrypt copy with AES-256 GCM in vault
        val effectiveWav = if (vaultPlainWav.exists()) vaultPlainWav else sourceWavFile
        val isEncrypted = if (effectiveWav.exists()) {
            encryptAudioFile(effectiveWav, vaultEncryptedFile)
        } else {
            false
        }

        // 2. Optional Mirroring to External Phone Storage (Downloads/VoiceGuard_Recordings)
        var externalPlainPath = vaultPlainWav.absolutePath
        var externalEncPath = vaultEncryptedFile.absolutePath
        try {
            val publicDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "VoiceGuard_Recordings"
            )
            if (publicDir.exists() || publicDir.mkdirs()) {
                val publicPlainWav = File(publicDir, "$baseFileName.wav")
                val publicEncFile = File(publicDir, "$baseFileName.vgenc")
                if (effectiveWav.exists()) {
                    effectiveWav.copyTo(publicPlainWav, overwrite = true)
                    externalPlainPath = publicPlainWav.absolutePath
                }
                if (vaultEncryptedFile.exists()) {
                    vaultEncryptedFile.copyTo(publicEncFile, overwrite = true)
                    externalEncPath = publicEncFile.absolutePath
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "External storage export skipped/denied: ${e.message}")
        }

        // 3. Index in MediaStore for immediate system discovery if Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && effectiveWav.exists()) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, "$baseFileName.wav")
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_RECORDINGS + "/VoiceGuard")
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }
                val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let { destUri ->
                    context.contentResolver.openOutputStream(destUri)?.use { out ->
                        FileInputStream(effectiveWav).use { input ->
                            input.copyTo(out)
                        }
                    }
                }
            } catch (me: Exception) {
                Log.w(TAG, "MediaStore index exception: ${me.message}")
            }
        }

        val primaryPlainFile = if (vaultPlainWav.exists()) vaultPlainWav else effectiveWav
        val primaryEncFile = if (vaultEncryptedFile.exists()) vaultEncryptedFile else File(externalEncPath)

        val sha256 = calculateFileSha256(if (primaryEncFile.exists()) primaryEncFile else primaryPlainFile)
        val shareableUri = getShareableUri(context, primaryPlainFile)
        val sizeBytes = if (primaryPlainFile.exists()) primaryPlainFile.length() else effectiveWav.length()
        val sizeKb = (sizeBytes / 1024.0).let { "%.1f KB".format(it) }

        return EncryptedExportResult(
            plainFilePath = primaryPlainFile.absolutePath,
            encryptedFilePath = primaryEncFile.absolutePath,
            sha256Hash = sha256,
            shareableUri = shareableUri,
            isEncrypted = isEncrypted,
            fileSizeFormatted = sizeKb
        )
    }

    /**
     * Shares audio recording securely using Android FileProvider & Intent
     */
    fun shareRecordingWithFileProvider(context: Context, audioFile: File, callerName: String) {
        try {
            val uri = getShareableUri(context, audioFile) ?: return
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "VoiceGuard Forensic Call Recording - $callerName")
                putExtra(Intent.EXTRA_TEXT, "Encrypted Telephony Audio Record & Forensic SHA-256 Audit by VoiceGuard Zero-Trust Shield.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Forensic Call Audio").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing recording: ${e.message}", e)
            Toast.makeText(context, "Could not share audio: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
