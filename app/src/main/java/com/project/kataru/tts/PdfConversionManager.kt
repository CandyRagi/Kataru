package com.project.kataru.tts

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

/*
 * PdfConversionManager - Orchestrates the entire PDF to audio conversion process.
 * Handles PDF extraction, TTS conversion, and audio file writing.
 * Provides state updates for UI progress display.
 */

class PdfConversionManager(private val context: Context) {

    private val pdfExtractor = PdfTextExtractor(context)
    private val ttsService = TtsService(context)

    private val _conversionState = MutableStateFlow<ConversionState>(ConversionState.Idle)
    val conversionState: StateFlow<ConversionState> = _conversionState

    companion object {
        private const val TAG = "PdfConversionManager"
    }

    sealed class ConversionState {
        object Idle : ConversionState()
        object Initializing : ConversionState()
        data class ExtractingText(val currentPage: Int, val totalPages: Int) : ConversionState()
        data class GeneratingAudio(val progress: Int) : ConversionState()
        object WritingFile : ConversionState()
        data class Success(val fileName: String) : ConversionState()
        data class Error(val message: String) : ConversionState()
    }

    /**
     * Convert a PDF file to audio and save to source folder
     */
    suspend fun convertPdfToAudio(
        pdfUri: Uri,
        sourceFolderUri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            _conversionState.value = ConversionState.Initializing

            // Step 1: Initialize TTS
            if (!ttsService.initialize()) {
                _conversionState.value = ConversionState.Error("Failed to initialize TTS engine. Please check your device's TTS settings.")
                return@withContext false
            }

            // Step 2: Extract text from PDF
            val fileName = pdfExtractor.getFileName(pdfUri)
            val text = pdfExtractor.extractText(pdfUri) { current, total ->
                _conversionState.value = ConversionState.ExtractingText(current, total)
            }

            if (text.isBlank()) {
                _conversionState.value = ConversionState.Error("PDF contains no readable text")
                return@withContext false
            }

            Log.d(TAG, "Extracted ${text.length} characters from PDF")

            // Step 3: Generate audio using Android TTS
            _conversionState.value = ConversionState.GeneratingAudio(0)
            
            // Create temp file for TTS output
            val tempFile = File(context.cacheDir, "tts_temp_${System.currentTimeMillis()}.wav")
            
            val success = ttsService.synthesizeToFile(
                text = text,
                outputFile = tempFile
            ) { progress ->
                _conversionState.value = ConversionState.GeneratingAudio(progress)
            }

            if (!success || !tempFile.exists()) {
                _conversionState.value = ConversionState.Error("Failed to generate audio")
                tempFile.delete()
                return@withContext false
            }

            Log.d(TAG, "Generated audio file: ${tempFile.length()} bytes")

            // Step 4: Copy to source folder
            _conversionState.value = ConversionState.WritingFile
            
            val sourceFolder = DocumentFile.fromTreeUri(context, sourceFolderUri)
            if (sourceFolder == null || !sourceFolder.canWrite()) {
                _conversionState.value = ConversionState.Error("Cannot write to source folder")
                tempFile.delete()
                return@withContext false
            }

            val sanitizedName = sanitizeFileName(fileName)
            val newFile = sourceFolder.createFile("audio/wav", "$sanitizedName.wav")
            
            if (newFile != null) {
                context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                    tempFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                tempFile.delete()
                
                _conversionState.value = ConversionState.Success(fileName)
                Log.i(TAG, "Successfully created audio file: $fileName")
                true
            } else {
                _conversionState.value = ConversionState.Error("Failed to create output file")
                tempFile.delete()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Conversion failed: ${e.message}")
            e.printStackTrace()
            _conversionState.value = ConversionState.Error("Conversion failed: ${e.message}")
            false
        } finally {
            ttsService.release()
        }
    }

    /**
     * Sanitize filename for filesystem
     */
    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._\\-\\s]"), "_")
            .take(100)
    }

    /**
     * Reset conversion state to idle
     */
    fun reset() {
        _conversionState.value = ConversionState.Idle
    }

    /**
     * Release resources
     */
    fun release() {
        ttsService.release()
        reset()
    }
}
