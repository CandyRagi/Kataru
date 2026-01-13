package com.project.kataru.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/*
 * TtsService - Text-to-Speech service using Android's built-in TTS engine.
 * Simpler and more reliable than external libraries. Uses device's installed
 * TTS engine (Google TTS, Samsung TTS, etc.)
 */

class TtsService(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    
    companion object {
        private const val TAG = "TtsService"
        const val SAMPLE_RATE = 22050
    }

    /**
     * Check if TTS is ready
     */
    fun isReady(): Boolean = isInitialized

    /**
     * Initialize the TTS engine
     * @return true if initialization successful
     */
    suspend fun initialize(): Boolean = suspendCancellableCoroutine { continuation ->
        if (isInitialized && tts != null) {
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                isInitialized = result != TextToSpeech.LANG_MISSING_DATA && 
                               result != TextToSpeech.LANG_NOT_SUPPORTED
                
                // Set speech rate for better audiobook quality
                tts?.setSpeechRate(0.9f)
                tts?.setPitch(1.0f)
                
                Log.i(TAG, "TTS initialized: $isInitialized, language result: $result")
                continuation.resume(isInitialized)
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
                continuation.resume(false)
            }
        }
    }

    /**
     * Synthesize text to a WAV file
     * @param text Text to convert to speech
     * @param outputFile Output file for the audio
     * @param onProgress Callback with progress (0-100)
     * @return true if synthesis was successful
     */
    suspend fun synthesizeToFile(
        text: String,
        outputFile: File,
        onProgress: ((progress: Int) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isInitialized || tts == null) {
            Log.e(TAG, "TTS not initialized")
            return@withContext false
        }

        // Clean text for TTS (remove special characters that might cause issues)
        val cleanedText = cleanTextForTts(text)
        if (cleanedText.isBlank()) {
            Log.e(TAG, "No text to synthesize after cleaning")
            return@withContext false
        }

        // Make sure parent directory exists
        outputFile.parentFile?.mkdirs()
        
        // Delete existing file if it exists
        if (outputFile.exists()) {
            outputFile.delete()
        }

        Log.d(TAG, "Synthesizing ${cleanedText.length} chars to ${outputFile.absolutePath}")

        suspendCancellableCoroutine { continuation ->
            val utteranceId = "tts_${System.currentTimeMillis()}"
            val hasResumed = AtomicBoolean(false)
            
            val progressListener = object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "TTS started: $utteranceId")
                    onProgress?.invoke(10)
                }

                override fun onDone(utteranceId: String?) {
                    Log.i(TAG, "TTS synthesis complete: $utteranceId")
                    onProgress?.invoke(100)
                    if (hasResumed.compareAndSet(false, true)) {
                        continuation.resume(true)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "TTS error (deprecated) for: $utteranceId")
                    if (hasResumed.compareAndSet(false, true)) {
                        continuation.resume(false)
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.e(TAG, "TTS error: $errorCode for: $utteranceId")
                    if (hasResumed.compareAndSet(false, true)) {
                        continuation.resume(false)
                    }
                }
            }
            
            tts?.setOnUtteranceProgressListener(progressListener)
            
            // Prepare parameters
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            
            // Limit text length - Android TTS has a limit of ~4000 characters per utterance
            val textToSynthesize = if (cleanedText.length > 4000) {
                Log.w(TAG, "Text too long (${cleanedText.length}), truncating to 4000 chars")
                cleanedText.take(4000)
            } else {
                cleanedText
            }
            
            onProgress?.invoke(5)
            
            val result = tts?.synthesizeToFile(textToSynthesize, params, outputFile, utteranceId)
            Log.d(TAG, "synthesizeToFile called, result: $result")
            
            if (result != TextToSpeech.SUCCESS) {
                Log.e(TAG, "synthesizeToFile failed with result: $result")
                if (hasResumed.compareAndSet(false, true)) {
                    continuation.resume(false)
                }
            }
        }
    }

    /**
     * Clean text for TTS - remove problematic characters
     */
    private fun cleanTextForTts(text: String): String {
        return text
            .replace(Regex("[\\x00-\\x1F]"), " ") // Remove control characters
            .replace(Regex("\\s+"), " ") // Collapse multiple spaces
            .replace("\"", "") // Remove quotes that might cause issues
            .replace("\n", ". ") // Replace newlines with periods for natural pauses
            .replace("\r", " ")
            .replace("\t", " ")
            .trim()
    }

    /**
     * Get available TTS engines on the device
     */
    fun getAvailableEngines(): List<TextToSpeech.EngineInfo> {
        return tts?.engines ?: emptyList()
    }

    /**
     * Set speech rate (0.5 = half speed, 2.0 = double speed)
     */
    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }

    /**
     * Set pitch (0.5 = lower pitch, 2.0 = higher pitch)
     */
    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
    }

    /**
     * Release TTS resources
     */
    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing TTS: ${e.message}")
        }
        tts = null
        isInitialized = false
    }

    /**
     * TTS is always "downloaded" since it uses device's built-in engine
     */
    fun isModelDownloaded(): Boolean = true

    /**
     * Get sample rate (not directly available from Android TTS, using default)
     */
    fun getSampleRate(): Int = SAMPLE_RATE
}
