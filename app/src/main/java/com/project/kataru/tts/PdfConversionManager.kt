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
import java.io.FileInputStream

/*
 * PdfConversionManager - Orchestrates the entire PDF to audio conversion process.
 * Handles PDF extraction, TTS conversion, and audio file writing.
 * Uses streaming approach to handle large documents without memory issues.
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
        object CheckingModel : ConversionState()
        data class DownloadingModel(val progress: Int) : ConversionState()
        data class ExtractingText(val currentPage: Int, val totalPages: Int) : ConversionState()
        data class GeneratingAudio(val progress: Int) : ConversionState()
        object WritingFile : ConversionState()
        data class Success(val fileName: String) : ConversionState()
        data class Error(val message: String) : ConversionState()
    }

    suspend fun convertPdfToAudio(
        pdfUri: Uri,
        sourceFolderUri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            _conversionState.value = ConversionState.CheckingModel

            // Initialize TTS
            if (!ttsService.initialize()) {
                _conversionState.value = ConversionState.Error("Failed to initialize TTS engine")
                return@withContext false
            }

            // Extract text from PDF
            val fileName = try {
                pdfExtractor.getFileName(pdfUri).removeSuffix(".pdf") + ".wav"
            } catch (e: Exception) {
                "audiobook.wav"
            }

            val text = try {
                pdfExtractor.extractText(pdfUri) { current, total ->
                    _conversionState.value = ConversionState.ExtractingText(current, total)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to extract text: ${e.message}")
                _conversionState.value = ConversionState.Error("Failed to read PDF: ${e.message}")
                return@withContext false
            }

            if (text.isBlank()) {
                _conversionState.value = ConversionState.Error("PDF contains no readable text")
                return@withContext false
            }

            Log.d(TAG, "Extracted ${text.length} characters from PDF")

            // Generate audio directly to a temp file
            val tempFile = File(context.cacheDir, "temp_audio_$${System.currentTimeMillis()}.wav")
            
            val success = ttsService.generateAudioToFile(
                text = text,
                outputFile = tempFile
            ) { processed, total ->
                val progress = (processed * 100) / total
                _conversionState.value = ConversionState.GeneratingAudio(progress)
            }

            if (!success || !tempFile.exists()) {
                _conversionState.value = ConversionState.Error("Failed to generate audio")
                return@withContext false
            }

            Log.d(TAG, "Generated audio file: ${tempFile.length()} bytes")

            // Copy to source folder
            _conversionState.value = ConversionState.WritingFile
            val outputUri = copyToSourceFolder(tempFile, sourceFolderUri, fileName)
            
            // Cleanup temp file
            tempFile.delete()

            if (outputUri != null) {
                _conversionState.value = ConversionState.Success(fileName)
                Log.i(TAG, "Successfully created audio file: $fileName")
                true
            } else {
                _conversionState.value = ConversionState.Error("Failed to save audio file")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Conversion failed: ${e.message}")
            e.printStackTrace()
            _conversionState.value = ConversionState.Error("Conversion failed: ${e.message}")
            false
        }
    }
    
    private fun copyToSourceFolder(sourceFile: File, sourceFolderUri: Uri, fileName: String): Uri? {
        return try {
            val sourceFolder = DocumentFile.fromTreeUri(context, sourceFolderUri)
                ?: return null
            
            // Create new file in source folder
            val newFile = sourceFolder.createFile("audio/wav", fileName)
                ?: return null
            
            // Copy data
            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                FileInputStream(sourceFile).use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
            
            newFile.uri
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy to source folder: ${e.message}")
            null
        }
    }

    fun reset() {
        _conversionState.value = ConversionState.Idle
    }

    fun release() {
        ttsService.release()
        reset()
    }
}
