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
import java.io.FileInputStream
import java.util.Locale
import kotlin.coroutines.resume

/*
 * TtsService - Wrapper for Android's native TextToSpeech engine.
 * Uses synthesizeToFile to generate audio chunks without OOM issues.
 */

class TtsService(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    
    companion object {
        private const val TAG = "TtsService"
    }

    fun isReady(): Boolean = isInitialized

    suspend fun initialize(): Boolean = suspendCancellableCoroutine { continuation ->
        if (isInitialized) {
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported")
                    continuation.resume(false)
                } else {
                    // Try to find a high quality voice
                    val bestVoice = tts?.voices?.firstOrNull { 
                        it.locale == Locale.US && 
                        it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS) &&
                        !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
                    } ?: tts?.defaultVoice

                    if (bestVoice != null) {
                        tts?.voice = bestVoice
                        Log.i(TAG, "Selected voice: ${bestVoice.name} (Network: ${bestVoice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS)})")
                    }

                    // Set default parameters for more realistic sound
                    tts?.setPitch(1.0f)
                    tts?.setSpeechRate(0.9f) // Slightly slower is often more natural

                    isInitialized = true
                    Log.i(TAG, "Native TTS initialized successfully")
                    continuation.resume(true)
                }
            } else {
                Log.e(TAG, "Initialization failed")
                continuation.resume(false)
            }
        }
    }

    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch)
    }

    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate)
    }

    suspend fun generateAudioToFile(
        text: String,
        outputFile: File,
        onProgress: ((processed: Int, total: Int) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isInitialized || tts == null) {
            Log.e(TAG, "TTS not initialized")
            return@withContext false
        }

        val tempDir = File(context.cacheDir, "tts-chunks")
        tempDir.mkdirs()
        
        val cleanedText = cleanTextForTts(text)
        // Native TTS handles larger chunks better, but keeping it reasonable
        val chunks = splitIntoChunks(cleanedText, maxSize = 3000)
        val chunkFiles = mutableListOf<File>()
        var processedChars = 0
        
        try {
            for ((index, chunk) in chunks.withIndex()) {
                val tempFile = File(tempDir, "chunk_$index.wav")
                val utteranceId = "chunk_$index"
                
                val success = suspendCancellableCoroutine<Boolean> { continuation ->
                    val listener = object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}

                        override fun onDone(utteranceId: String?) {
                            continuation.resume(true)
                        }

                        override fun onError(utteranceId: String?) {
                            Log.e(TAG, "Error generating chunk $index")
                            continuation.resume(false)
                        }
                    }
                    
                    tts?.setOnUtteranceProgressListener(listener)
                    
                    val params = Bundle()
                    params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                    
                    val result = tts?.synthesizeToFile(chunk, params, tempFile, utteranceId)
                    
                    if (result != TextToSpeech.SUCCESS) {
                        Log.e(TAG, "synthesizeToFile failed for chunk $index")
                        continuation.resume(false)
                    }
                }
                
                if (success && tempFile.exists()) {
                    chunkFiles.add(tempFile)
                } else {
                    Log.e(TAG, "Failed to generate chunk $index")
                    return@withContext false
                }
                
                processedChars += chunk.length
                onProgress?.invoke(processedChars, cleanedText.length)
            }
            
            if (chunkFiles.isEmpty()) {
                return@withContext false
            }
            
            // Concatenate all chunks
            concatenateWavFiles(chunkFiles, outputFile)
            
            // Cleanup
            chunkFiles.forEach { it.delete() }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    private fun cleanTextForTts(text: String): String {
        return text
            .replace(Regex("[\\x00-\\x1F\\x7F]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    
    private fun splitIntoChunks(text: String, maxSize: Int = 3000): List<String> {
        val chunks = mutableListOf<String>()
        var remaining = text
        
        while (remaining.isNotEmpty()) {
            if (remaining.length <= maxSize) {
                chunks.add(remaining)
                break
            }
            
            var breakPoint = remaining.lastIndexOf(". ", maxSize)
            if (breakPoint < maxSize / 2) {
                breakPoint = remaining.lastIndexOf(", ", maxSize)
            }
            if (breakPoint < maxSize / 2) {
                breakPoint = remaining.lastIndexOf(" ", maxSize)
            }
            if (breakPoint < maxSize / 2) {
                breakPoint = maxSize
            }
            
            chunks.add(remaining.substring(0, breakPoint + 1))
            remaining = remaining.substring(breakPoint + 1)
        }
        
        return chunks
    }
    
    private fun concatenateWavFiles(inputFiles: List<File>, outputFile: File) {
        // Simple concatenation for WAV files generated by Android TTS
        // Note: Android TTS usually outputs PCM 16bit mono/stereo depending on voice
        // We assume consistent format for all chunks from same session
        
        if (inputFiles.isEmpty()) return
        
        val firstFile = inputFiles.first()
        var totalDataSize = 0
        
        // Calculate total size
        inputFiles.forEach { file ->
            totalDataSize += (file.length() - 44).toInt()
        }
        
        // Read header from first file
        val header = ByteArray(44)
        FileInputStream(firstFile).use { it.read(header) }
        
        // Update size in header
        val totalSize = totalDataSize + 36
        header[4] = (totalSize and 0xff).toByte()
        header[5] = ((totalSize shr 8) and 0xff).toByte()
        header[6] = ((totalSize shr 16) and 0xff).toByte()
        header[7] = ((totalSize shr 24) and 0xff).toByte()
        
        header[40] = (totalDataSize and 0xff).toByte()
        header[41] = ((totalDataSize shr 8) and 0xff).toByte()
        header[42] = ((totalDataSize shr 16) and 0xff).toByte()
        header[43] = ((totalDataSize shr 24) and 0xff).toByte()
        
        java.io.FileOutputStream(outputFile).use { fos ->
            fos.write(header)
            
            val buffer = ByteArray(8192)
            inputFiles.forEach { file ->
                FileInputStream(file).use { fis ->
                    fis.skip(44) // Skip header
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        fos.write(buffer, 0, bytesRead)
                    }
                }
            }
        }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
