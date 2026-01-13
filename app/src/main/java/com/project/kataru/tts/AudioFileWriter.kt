package com.project.kataru.tts

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.File

/*
 * AudioFileWriter - Writes audio samples to WAV files in the source folder.
 * Handles WAV header generation and sample conversion.
 * Supports writing to SAF (Storage Access Framework) document URIs.
 */

class AudioFileWriter(private val context: Context) {

    companion object {
        private const val BITS_PER_SAMPLE = 16
        private const val CHANNELS = 1 // Mono
    }

    /**
     * Write audio samples to a WAV file in the source folder
     * @param samples Audio samples as FloatArray
     * @param sampleRate Sample rate (e.g., 22050)
     * @param fileName Name for the output file (without extension)
     * @param sourceFolderUri URI of the source folder
     * @return Uri of the created file, or null on error
     */
    suspend fun writeWavFile(
        samples: FloatArray,
        sampleRate: Int,
        fileName: String,
        sourceFolderUri: Uri
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val sourceFolder = DocumentFile.fromTreeUri(context, sourceFolderUri)
                ?: return@withContext null

            if (!sourceFolder.canWrite()) {
                return@withContext null
            }

            // Create the output file
            val wavFileName = "${sanitizeFileName(fileName)}.wav"
            val newFile = sourceFolder.createFile("audio/wav", wavFileName)
                ?: return@withContext null

            // Convert float samples to 16-bit PCM
            val pcmData = floatToPcm16(samples)
            
            // Write WAV file
            context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                DataOutputStream(outputStream).use { dos ->
                    writeWavHeader(dos, pcmData.size, sampleRate)
                    dos.write(pcmData)
                }
            }

            newFile.uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Write WAV header
     */
    private fun writeWavHeader(dos: DataOutputStream, dataSize: Int, sampleRate: Int) {
        val byteRate = sampleRate * CHANNELS * BITS_PER_SAMPLE / 8
        val blockAlign = CHANNELS * BITS_PER_SAMPLE / 8

        // RIFF header
        dos.writeBytes("RIFF")
        dos.writeInt(Integer.reverseBytes(36 + dataSize))
        dos.writeBytes("WAVE")

        // fmt chunk
        dos.writeBytes("fmt ")
        dos.writeInt(Integer.reverseBytes(16)) // Subchunk1Size
        dos.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // AudioFormat (PCM)
        dos.writeShort(java.lang.Short.reverseBytes(CHANNELS.toShort()).toInt())
        dos.writeInt(Integer.reverseBytes(sampleRate))
        dos.writeInt(Integer.reverseBytes(byteRate))
        dos.writeShort(java.lang.Short.reverseBytes(blockAlign.toShort()).toInt())
        dos.writeShort(java.lang.Short.reverseBytes(BITS_PER_SAMPLE.toShort()).toInt())

        // data chunk
        dos.writeBytes("data")
        dos.writeInt(Integer.reverseBytes(dataSize))
    }

    /**
     * Convert float samples (-1.0 to 1.0) to 16-bit PCM bytes
     */
    private fun floatToPcm16(samples: FloatArray): ByteArray {
        val bytes = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            // Clamp and convert to 16-bit
            val sample = (samples[i].coerceIn(-1f, 1f) * 32767).toInt()
            bytes[i * 2] = (sample and 0xFF).toByte()
            bytes[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    /**
     * Sanitize filename for filesystem
     */
    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(100) // Limit length
    }
}
