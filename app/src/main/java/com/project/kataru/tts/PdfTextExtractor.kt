package com.project.kataru.tts

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * PdfTextExtractor - Extracts text content from PDF files for TTS conversion.
 * Uses PdfBox-Android to parse PDFs and extract text page by page.
 * Provides progress callbacks for UI updates during extraction.
 */

class PdfTextExtractor(private val context: Context) {

    init {
        // Initialize PDFBox resources (required for Android)
        PDFBoxResourceLoader.init(context)
    }

    /**
     * Extract all text from a PDF file.
     * @param pdfUri URI of the PDF file
     * @param onProgress Callback with (currentPage, totalPages)
     * @return Extracted text content
     */
    suspend fun extractText(
        pdfUri: Uri,
        onProgress: ((currentPage: Int, totalPages: Int) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(pdfUri)
            ?: throw IllegalArgumentException("Cannot open PDF file")

        inputStream.use { stream ->
            val document = PDDocument.load(stream)
            document.use { doc ->
                val totalPages = doc.numberOfPages
                val textBuilder = StringBuilder()
                val stripper = PDFTextStripper()

                for (pageNum in 1..totalPages) {
                    stripper.startPage = pageNum
                    stripper.endPage = pageNum
                    
                    val pageText = stripper.getText(doc)
                    textBuilder.append(pageText)
                    textBuilder.append("\n\n") // Page separator
                    
                    onProgress?.invoke(pageNum, totalPages)
                }

                textBuilder.toString().trim()
            }
        }
    }

    /**
     * Get the filename from a PDF URI
     */
    fun getFileName(pdfUri: Uri): String {
        var fileName = "converted_pdf"
        val cursor = context.contentResolver.query(pdfUri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    val name = it.getString(nameIndex)
                    // Remove .pdf extension
                    fileName = name.removeSuffix(".pdf").removeSuffix(".PDF")
                }
            }
        }
        return fileName
    }
}
