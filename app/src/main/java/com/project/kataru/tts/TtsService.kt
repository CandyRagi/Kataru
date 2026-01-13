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
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/*
 * TtsService - Text-to-Speech service using Android's built-in TTS.
 * Streams audio directly to file to avoid memory issues with large documents.
 * Uses synthesizeToFile for each chunk, then concatenates them.
 */

class TtsService(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    
    companion object {
        private const val TAG = "TtsService"
        private const val OUTPUT_DIR = "tts-output"
        const val SAMPLE_RATE = 22050
        private const val MAX_CHUNK_SIZE = 3500
    }

    fun isReady(): Boolean = isInitialized

    suspend fun initialize(): Boolean = withContext(Dispatchers.Main) {
        if (isInitialized) return@withContext true
        
        suspendCancellableCoroutine { continuation ->
            val resumed = AtomicBoolean(false)
            
            tts = TextToSpeech(context) { status ->
                if (resumed.compareAndSet(false, true)) {
                    if (status == TextToSpeech.SUCCESS) {
                        val result = tts?.setLanguage(Locale.US)
                        isInitialized = result != TextToSpeech.LANG_MISSING_DATA && 
                                        result != TextToSpeech.LANG_NOT_SUPPORTED
                        
                        if (isInitialized) {
                            Log.i(TAG, "Android TTS initialized successfully")
                        } else {
                            Log.e(TAG, "Language not supported: $result")
                        }
                        continuation.resume(isInitialized)
                    } else {
                        Log.e(TAG, "TTS initialization failed: $status")
                        continuation.resume(false)
                    }
                }
            }
            
            continuation.invokeOnCancellation {
                if (resumed.compareAndSet(false, true)) {
                    tts?.shutdown()
                    tts = null
                }
            }
        }
    }

    /**
     * Generate audio and write directly to a WAV file
     * Returns the path to the generated WAV file, or null on error
     */
    suspend fun generateAudioToFile(
        text: String,
        outputFile: File,
        onProgress: ((processed: Int, total: Int) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isInitialized || tts == null) {
            Log.e(TAG, "TTS not initialized")
            return@withContext false
        }

        try {
            val tempDir = File(context.cacheDir, OUTPUT_DIR)
            tempDir.mkdirs()
            
            val cleanedText = cleanTextForTts(text)
            val chunks = splitIntoChunks(cleanedText)
            val chunkFiles = mutableListOf<File>()
            var processedChars = 0
            
            // Generate audio for each chunk
            for ((index, chunk) in chunks.withIndex()) {
                val tempFile = File(tempDir, "chunk_$index.wav")
                
                val success = synthesizeChunk(chunk, tempFile)
                
                if (success && tempFile.exists() && tempFile.length() > 44) {
                    chunkFiles.add(tempFile)
                } else {
                    Log.w(TAG, "Failed to synthesize chunk $index")
                }
                
                processedChars += chunk.length
                onProgress?.invoke(processedChars, cleanedText.length)
            }
            
            if (chunkFiles.isEmpty()) {
                Log.e(TAG, "No audio chunks generated")
                return@withContext false
            }
            
            // Concatenate all chunk files into the output file
            concatenateWavFiles(chunkFiles, outputFile)
            
            // Cleanup temp files
            chunkFiles.forEach { it.delete() }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate audio: ${e.message}")
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
    
    private fun splitIntoChunks(text: String): List<String> {
        val chunks = mutableListOf<String>()
        var remaining = text
        
        while (remaining.isNotEmpty()) {
            if (remaining.length <= MAX_CHUNK_SIZE) {
                chunks.add(remaining)
                break
            }
            
            var breakPoint = remaining.lastIndexOf(". ", MAX_CHUNK_SIZE)
            if (breakPoint < MAX_CHUNK_SIZE / 2) {
                breakPoint = remaining.lastIndexOf(", ", MAX_CHUNK_SIZE)
            }
            if (breakPoint < MAX_CHUNK_SIZE / 2) {
                breakPoint = remaining.lastIndexOf(" ", MAX_CHUNK_SIZE)
            }
            if (breakPoint < MAX_CHUNK_SIZE / 2) {
                breakPoint = MAX_CHUNK_SIZE
            }
            
            chunks.add(remaining.substring(0, breakPoint + 1))
            remaining = remaining.substring(breakPoint + 1)
        }
        
        return chunks
    }
    
    private suspend fun synthesizeChunk(text: String, outputFile: File): Boolean = 
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val resumed = AtomicBoolean(false)
                val utteranceId = "chunk_${System.currentTimeMillis()}"
                
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    
                    override fun onDone(id: String?) {
                        if (id == utteranceId && resumed.compareAndSet(false, true)) {
                            continuation.resume(true)
                        }
                    }
                    
                    override fun onError(id: String?) {
                        if (id == utteranceId && resumed.compareAndSet(false, true)) {
                            Log.e(TAG, "TTS error for chunk")
                            continuation.resume(false)
                        }
                    }
                    
                    @Deprecated("Deprecated in API")
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        if (utteranceId == utteranceId && resumed.compareAndSet(false, true)) {
                            Log.e(TAG, "TTS error code: $errorCode")
                            continuation.resume(false)
                        }
                    }
                })
                
                val params = Bundle()
                val result = tts?.synthesizeToFile(text, params, outputFile, utteranceId)
                
                if (result != TextToSpeech.SUCCESS) {
                    if (resumed.compareAndSet(false, true)) {
                        Log.e(TAG, "synthesizeToFile failed: $result")
                        continuation.resume(false)
                    }
                }
                
                continuation.invokeOnCancellation {
                    if (resumed.compareAndSet(false, true)) {
                        tts?.stop()
                    }
                }
            }
        }
    
    /**
     * Concatenate multiple WAV files into one output file
     * Streams data directly to avoid memory issues
     */
    private fun concatenateWavFiles(inputFiles: List<File>, outputFile: File) {
        // Calculate total data size
        var totalDataSize = 0
        var sampleRate = 22050
        var bitsPerSample = 16
        var numChannels = 1
        
        for (file in inputFiles) {
            RandomAccessFile(file, "r").use { raf ->
                // Read WAV header info from first file
                if (file == inputFiles.first()) {
                    raf.seek(22)
                    numChannels = (raf.readByte().toInt() and 0xFF) or ((raf.readByte().toInt() and 0xFF) shl 8)
                    sampleRate = (raf.readByte().toInt() and 0xFF) or 
                                 ((raf.readByte().toInt() and 0xFF) shl 8) or
                                 ((raf.readByte().toInt() and 0xFF) shl 16) or
                                 ((raf.readByte().toInt() and 0xFF) shl 24)
                    raf.seek(34)
                    bitsPerSample = (raf.readByte().toInt() and 0xFF) or ((raf.readByte().toInt() and 0xFF) shl 8)
                }
                totalDataSize += (file.length() - 44).toInt()
            }
        }
        
        // Write output file with proper header
        FileOutputStream(outputFile).use { fos ->
            // WAV header
            val byteRate = sampleRate * numChannels * bitsPerSample / 8
            val blockAlign = numChannels * bitsPerSample / 8
            
            fos.write("RIFF".toByteArray())
            fos.write(intToBytes(36 + totalDataSize))
            fos.write("WAVE".toByteArray())
            fos.write("fmt ".toByteArray())
            fos.write(intToBytes(16)) // Subchunk1Size
            fos.write(shortToBytes(1)) // AudioFormat (PCM)
            fos.write(shortToBytes(numChannels))
            fos.write(intToBytes(sampleRate))
            fos.write(intToBytes(byteRate))
            fos.write(shortToBytes(blockAlign))
            fos.write(shortToBytes(bitsPerSample))
            fos.write("data".toByteArray())
            fos.write(intToBytes(totalDataSize))
            
            // Append audio data from each file
            val buffer = ByteArray(8192)
            for (file in inputFiles) {
                FileInputStream(file).use { fis ->
                    fis.skip(44) // Skip WAV header
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        fos.write(buffer, 0, bytesRead)
                    }
                }
            }
        }
    }
    
    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }
    
    private fun shortToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte()
        )
    }

    fun getSampleRate(): Int = SAMPLE_RATE

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    fun isModelDownloaded(): Boolean = true

    fun getModelDir(): File = File(context.filesDir, OUTPUT_DIR)
}
