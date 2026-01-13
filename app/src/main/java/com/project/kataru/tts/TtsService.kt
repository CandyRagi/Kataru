package com.project.kataru.tts

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/*
 * TtsService - Text-to-Speech service using Sherpa-ONNX with VITS-VCTK model.
 * Uses official pre-packaged model with 109 speakers (male and female).
 * Streams audio directly to file to avoid memory issues.
 */

class TtsService(private val context: Context) {

    private var tts: OfflineTts? = null
    private var isInitialized = false
    
    companion object {
        private const val TAG = "TtsService"
        private const val MODEL_DIR = "vits-vctk"
        const val SAMPLE_RATE = 22050
        const val NUM_SPEAKERS = 109 // VCTK has 109 speakers
    }

    fun isReady(): Boolean = isInitialized

    /**
     * Initialize the TTS engine with VITS-VCTK model
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        synchronized(this@TtsService) {
            if (isInitialized) return@synchronized true

            try {
                val modelDir = File(context.filesDir, MODEL_DIR)
                
                // Check if all model files exist
                val modelFile = File(modelDir, "vits-vctk.onnx")
                val tokensFile = File(modelDir, "tokens.txt")
                val lexiconFile = File(modelDir, "lexicon.txt")
                
                if (!modelFile.exists() || !tokensFile.exists() || !lexiconFile.exists()) {
                    Log.e(TAG, "TTS model files not found. Please download the model first.")
                    return@synchronized false
                }

                val vitsModelConfig = OfflineTtsVitsModelConfig(
                    model = modelFile.absolutePath,
                    tokens = tokensFile.absolutePath,
                    lexicon = lexiconFile.absolutePath,
                    noiseScale = 0.667f,
                    noiseScaleW = 0.8f,
                    lengthScale = 1.0f
                )

                val modelConfig = OfflineTtsModelConfig(
                    vits = vitsModelConfig,
                    numThreads = 2,
                    debug = false,
                    provider = "cpu"
                )

                val config = OfflineTtsConfig(
                    model = modelConfig,
                    maxNumSentences = 1
                )

                // Initialize with null AssetManager for filesystem models
                tts = OfflineTts(assetManager = null, config = config)
                
                isInitialized = true
                Log.i(TAG, "Sherpa-ONNX TTS initialized with VCTK model (${NUM_SPEAKERS} speakers)")
                true
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to initialize TTS: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Generate audio and write directly to a WAV file
     * @param speakerId Speaker ID from 0-108 (different male/female voices)
     */
    suspend fun generateAudioToFile(
        text: String,
        outputFile: File,
        speakerId: Int = 0,
        speed: Float = 1.0f,
        onProgress: ((processed: Int, total: Int) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isInitialized || tts == null) {
            Log.e(TAG, "TTS not initialized")
            return@withContext false
        }

        try {
            val tempDir = File(context.cacheDir, "tts-chunks")
            tempDir.mkdirs()
            
            val cleanedText = cleanTextForTts(text)
            val chunks = splitIntoChunks(cleanedText)
            val chunkFiles = mutableListOf<File>()
            var processedChars = 0
            
            // Generate audio for each chunk
            for ((index, chunk) in chunks.withIndex()) {
                val tempFile = File(tempDir, "chunk_$index.wav")
                
                val success = synchronized(this@TtsService) {
                    if (!isInitialized || tts == null) {
                        return@synchronized false
                    }
                    
                    try {
                        val audio = tts!!.generate(
                            text = chunk,
                            sid = speakerId.coerceIn(0, NUM_SPEAKERS - 1),
                            speed = speed
                        )
                        
                        // Write samples to temp WAV file
                        writeWavFile(audio.samples, audio.sampleRate, tempFile)
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Generation failed for chunk $index: ${e.message}")
                        false
                    }
                }
                
                if (success && tempFile.exists() && tempFile.length() > 44) {
                    chunkFiles.add(tempFile)
                }
                
                processedChars += chunk.length
                onProgress?.invoke(processedChars, cleanedText.length)
            }
            
            if (chunkFiles.isEmpty()) {
                Log.e(TAG, "No audio chunks generated")
                return@withContext false
            }
            
            // Concatenate all chunks
            concatenateWavFiles(chunkFiles, outputFile)
            
            // Cleanup
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
    
    private fun splitIntoChunks(text: String, maxSize: Int = 500): List<String> {
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
    
    private fun writeWavFile(samples: FloatArray, sampleRate: Int, file: File) {
        val numChannels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = numChannels * bitsPerSample / 8
        val dataSize = samples.size * 2
        
        FileOutputStream(file).use { fos ->
            // WAV header
            fos.write("RIFF".toByteArray())
            fos.write(intToBytes(36 + dataSize))
            fos.write("WAVE".toByteArray())
            fos.write("fmt ".toByteArray())
            fos.write(intToBytes(16))
            fos.write(shortToBytes(1))
            fos.write(shortToBytes(numChannels))
            fos.write(intToBytes(sampleRate))
            fos.write(intToBytes(byteRate))
            fos.write(shortToBytes(blockAlign))
            fos.write(shortToBytes(bitsPerSample))
            fos.write("data".toByteArray())
            fos.write(intToBytes(dataSize))
            
            // Audio data
            for (sample in samples) {
                val intSample = (sample * 32767).toInt().coerceIn(-32768, 32767)
                fos.write(intSample and 0xFF)
                fos.write((intSample shr 8) and 0xFF)
            }
        }
    }
    
    private fun concatenateWavFiles(inputFiles: List<File>, outputFile: File) {
        var totalDataSize = 0
        var sampleRate = 22050
        var bitsPerSample = 16
        var numChannels = 1
        
        // Calculate total size and read header info
        for (file in inputFiles) {
            java.io.RandomAccessFile(file, "r").use { raf ->
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
        
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = numChannels * bitsPerSample / 8
        
        FileOutputStream(outputFile).use { fos ->
            // WAV header
            fos.write("RIFF".toByteArray())
            fos.write(intToBytes(36 + totalDataSize))
            fos.write("WAVE".toByteArray())
            fos.write("fmt ".toByteArray())
            fos.write(intToBytes(16))
            fos.write(shortToBytes(1))
            fos.write(shortToBytes(numChannels))
            fos.write(intToBytes(sampleRate))
            fos.write(intToBytes(byteRate))
            fos.write(shortToBytes(blockAlign))
            fos.write(shortToBytes(bitsPerSample))
            fos.write("data".toByteArray())
            fos.write(intToBytes(totalDataSize))
            
            // Append audio data
            val buffer = ByteArray(8192)
            for (file in inputFiles) {
                FileInputStream(file).use { fis ->
                    fis.skip(44)
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

    fun getSampleRate(): Int = tts?.sampleRate() ?: SAMPLE_RATE

    fun release() {
        synchronized(this) {
            try {
                tts?.release()
                tts = null
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing TTS: ${e.message}")
            }
            isInitialized = false
        }
    }

    fun isModelDownloaded(): Boolean {
        val modelDir = File(context.filesDir, MODEL_DIR)
        return modelDir.exists() && 
               File(modelDir, "vits-vctk.onnx").exists() &&
               File(modelDir, "tokens.txt").exists() &&
               File(modelDir, "lexicon.txt").exists()
    }

    fun getModelDir(): File = File(context.filesDir, MODEL_DIR)
}
