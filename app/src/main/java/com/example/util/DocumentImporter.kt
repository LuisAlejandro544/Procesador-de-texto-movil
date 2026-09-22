package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Resultado de la importación estructurada de un documento externo.
 */
data class ImportedDocument(
    val title: String,
    val content: String,
    val formatName: String,
    val wordCount: Int,
    val charCount: Int
)

/**
 * DocumentImporter: Coordinador de importación universal para DocuSheet.
 * 
 * Permite leer e interpretar documentos en formatos:
 * - Microsoft Word (.docx)
 * - Rich Text Format (.rtf)
 * - LaTeX Académico (.tex)
 * - Markdown (.md)
 * - Texto Plano (.txt)
 * 
 * Ejecuta todas las operaciones de descompresión y parseo de forma asíncrona en Dispatchers.IO.
 */
object DocumentImporter {

    /**
     * Importa un documento desde un URI de Android, detecta su formato, extrae
     * el título y reconstruye el contenido enriquecido para la hoja física.
     */
    suspend fun importFromUri(context: Context, uri: Uri): ImportedDocument = withContext(Dispatchers.IO) {
        val fileName = getFileName(context, uri)
        val extension = fileName.substringAfterLast('.', "").lowercase()

        val contentResolver = context.contentResolver

        var detectedTitle = fileName.substringBeforeLast('.')
        var extractedContent = ""
        var formatLabel = "Texto"

        contentResolver.openInputStream(uri)?.use { inputStream ->
            when {
                extension == "docx" -> {
                    formatLabel = "Microsoft Word (.docx)"
                    val result = DocxHandler.importFromDocx(inputStream)
                    if (result.first.isNotBlank() && result.first != "Documento Word Importado") {
                        detectedTitle = result.first
                    }
                    extractedContent = result.second
                }
                extension == "rtf" -> {
                    formatLabel = "Texto Enriquecido (.rtf)"
                    val result = RtfHandler.importFromRtf(inputStream)
                    if (result.first.isNotBlank() && result.first != "Documento RTF Importado") {
                        detectedTitle = result.first
                    }
                    extractedContent = result.second
                }
                extension == "tex" -> {
                    formatLabel = "LaTeX Académico (.tex)"
                    val result = LatexHandler.importFromLatex(inputStream)
                    if (result.first.isNotBlank() && result.first != "Documento LaTeX Importado") {
                        detectedTitle = result.first
                    }
                    extractedContent = result.second
                }
                extension == "md" || extension == "markdown" -> {
                    formatLabel = "Markdown (.md)"
                    extractedContent = inputStream.bufferedReader(Charsets.UTF_8).readText()
                    // Si la primera línea es un encabezado #, usarla como título
                    val firstLine = extractedContent.lines().firstOrNull { it.trim().isNotBlank() }?.trim() ?: ""
                    if (firstLine.startsWith("# ")) {
                        detectedTitle = firstLine.removePrefix("# ").trim()
                    }
                }
                else -> {
                    // Intento de detección automática por cabecera
                    val buffered = inputStream.buffered()
                    buffered.mark(1024)
                    val headerBytes = ByteArray(512)
                    val read = buffered.read(headerBytes)
                    buffered.reset()

                    val headerStr = if (read > 0) String(headerBytes, 0, read, Charsets.ISO_8859_1) else ""

                    if (headerStr.startsWith("PK")) {
                        formatLabel = "Microsoft Word (.docx)"
                        val result = DocxHandler.importFromDocx(buffered)
                        if (result.first.isNotBlank() && result.first != "Documento Word Importado") {
                            detectedTitle = result.first
                        }
                        extractedContent = result.second
                    } else if (headerStr.startsWith("{\\rtf")) {
                        formatLabel = "Texto Enriquecido (.rtf)"
                        val result = RtfHandler.importFromRtf(buffered)
                        if (result.first.isNotBlank() && result.first != "Documento RTF Importado") {
                            detectedTitle = result.first
                        }
                        extractedContent = result.second
                    } else if (headerStr.contains("\\documentclass") || headerStr.contains("\\begin{document}")) {
                        formatLabel = "LaTeX Académico (.tex)"
                        val result = LatexHandler.importFromLatex(buffered)
                        if (result.first.isNotBlank() && result.first != "Documento LaTeX Importado") {
                            detectedTitle = result.first
                        }
                        extractedContent = result.second
                    } else {
                        formatLabel = "Texto Plano (.txt)"
                        extractedContent = buffered.bufferedReader(Charsets.UTF_8).readText()
                    }
                }
            }
        } ?: throw IllegalArgumentException("No se pudo abrir el archivo seleccionado")

        val words = if (extractedContent.isBlank()) 0 else extractedContent.trim().split(Regex("\\s+")).size
        val chars = extractedContent.length

        ImportedDocument(
            title = detectedTitle.ifBlank { "Documento Importado" },
            content = extractedContent,
            formatName = formatLabel,
            wordCount = words,
            charCount = chars
        )
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "documento"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex) ?: name
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return name
    }
}
