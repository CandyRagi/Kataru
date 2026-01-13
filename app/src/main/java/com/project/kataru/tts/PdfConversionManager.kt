package com.project.kataru.tts

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/*
 * PdfConversionManager - Orchestrates the entire PDF to audio conversion process.
 * Downloads official Sherpa-ONNX VITS-VCTK model (109 speakers) on first use.
 * Uses streaming approach to handle large documents without memory issues.
 */

class PdfConversionManager(private val context: Context) {

    private val pdfExtractor = PdfTextExtractor(context)
    private val ttsService = TtsService(context)

    private val _conversionState = MutableStateFlow<ConversionState>(ConversionState.Idle)
    val conversionState: StateFlow<ConversionState> = _conversionState

    companion object {
        private const val TAG = "PdfConversionManager"
        
        // Official Sherpa-ONNX VCTK model (109 speakers, male and female voices)
        private const val MODEL_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-vctk.tar.bz2"
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
        sourceFolderUri: Uri,
        speakerId: Int = 0  // 0-108, different voices (male/female)
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            _conversionState.value = ConversionState.CheckingModel

            // Step 1: Download model if needed
            if (!ttsService.isModelDownloaded()) {
                if (!downloadModel()) {
                    _conversionState.value = ConversionState.Error("Failed to download TTS model")
                    return@withContext false
                }
            }

            // Step 2: Initialize TTS
            if (!ttsService.initialize()) {
                _conversionState.value = ConversionState.Error("Failed to initialize TTS engine")
                return@withContext false
            }

            // Step 3: Extract text from PDF
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

            // Step 4: Generate audio
            val tempFile = File(context.cacheDir, "temp_audio_${System.currentTimeMillis()}.wav")
            
            val success = ttsService.generateAudioToFile(
                text = text,
                outputFile = tempFile,
                speakerId = speakerId
            ) { processed, total ->
                val progress = (processed * 100) / total
                _conversionState.value = ConversionState.GeneratingAudio(progress)
            }

            if (!success || !tempFile.exists()) {
                _conversionState.value = ConversionState.Error("Failed to generate audio")
                return@withContext false
            }

            Log.d(TAG, "Generated audio file: ${tempFile.length()} bytes")

            // Step 5: Copy to source folder
            _conversionState.value = ConversionState.WritingFile
            val outputUri = copyToSourceFolder(tempFile, sourceFolderUri, fileName)
            
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
    
    private suspend fun downloadModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelDir = ttsService.getModelDir()
            modelDir.mkdirs()

            // Download tar.bz2 file
            _conversionState.value = ConversionState.DownloadingModel(0)
            val tarBz2File = File(context.cacheDir, "vits-vctk.tar.bz2")
            
            downloadFile(MODEL_URL, tarBz2File) { progress ->
                _conversionState.value = ConversionState.DownloadingModel((progress * 0.8).toInt())
            }

            // Extract tar.bz2
            _conversionState.value = ConversionState.DownloadingModel(85)
            extractTarBz2(tarBz2File, context.filesDir)
            
            // Cleanup
            tarBz2File.delete()

            _conversionState.value = ConversionState.DownloadingModel(100)
            Log.i(TAG, "Model downloaded and extracted successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download model: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    private fun extractTarBz2(tarBz2File: File, destDir: File) {
        Log.d(TAG, "Extracting ${tarBz2File.name} to ${destDir.absolutePath}")
        
        FileInputStream(tarBz2File).use { fis ->
            BufferedInputStream(fis).use { bis ->
                BZip2CompressorInputStream(bis).use { bzis ->
                    TarArchiveInputStream(bzis).use { tais ->
                        var entry = tais.nextTarEntry
                        while (entry != null) {
                            val outputFile = File(destDir, entry.name)
                            
                            if (entry.isDirectory) {
                                outputFile.mkdirs()
                            } else {
                                outputFile.parentFile?.mkdirs()
                                FileOutputStream(outputFile).use { fos ->
                                    val buffer = ByteArray(8192)
                                    var len: Int
                                    while (tais.read(buffer).also { len = it } != -1) {
                                        fos.write(buffer, 0, len)
                                    }
                                }
                            }
                            
                            entry = tais.nextTarEntry
                        }
                    }
                }
            }
        }
        
        Log.d(TAG, "Extraction complete")
    }
    
    private fun downloadFile(urlString: String, outputFile: File, onProgress: (Int) -> Unit) {
        Log.d(TAG, "Downloading $urlString")
        var url = URL(urlString)
        var connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 30000
        connection.readTimeout = 120000
        connection.instanceFollowRedirects = true

        // Handle redirects
        var redirectCount = 0
        while (redirectCount < 5) {
            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || 
                status == HttpURLConnection.HTTP_MOVED_PERM || 
                status == HttpURLConnection.HTTP_SEE_OTHER) {
                
                val newUrl = connection.getHeaderField("Location")
                Log.d(TAG, "Redirecting to $newUrl")
                url = URL(newUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 120000
                redirectCount++
            } else {
                break
            }
        }

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("Server returned HTTP ${connection.responseCode}")
        }

        connection.inputStream.use { input ->
            FileOutputStream(outputFile).use { output ->
                val contentLength = connection.contentLength
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (contentLength > 0) {
                        val progress = ((totalBytesRead * 100) / contentLength).toInt()
                        onProgress(progress.coerceIn(0, 100))
                    }
                }
            }
        }
        Log.d(TAG, "Download complete: ${outputFile.length()} bytes")
    }
    
    private fun copyToSourceFolder(sourceFile: File, sourceFolderUri: Uri, fileName: String): Uri? {
        return try {
            val sourceFolder = DocumentFile.fromTreeUri(context, sourceFolderUri)
                ?: return null
            
            val newFile = sourceFolder.createFile("audio/wav", fileName)
                ?: return null
            
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
