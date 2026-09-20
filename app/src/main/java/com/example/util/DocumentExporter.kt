package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DocumentExporter: Motor de exportación digital para DocuSheet.
 * 
 * Permite convertir cualquier documento redactado en la aplicación a:
 * 1. Documento PDF digital (.pdf): Renderizado en alta resolución con formato A4,
 *    márgenes proporcionales, encabezados, numeración de páginas y saltos automáticos.
 * 2. Archivo Markdown (.md): Formato universal ligero para respaldo y edición multiplataforma.
 * 
 * Utiliza FileProvider seguro para permitir la apertura directa o guardado mediante
 * cualquier aplicación instalada en el teléfono (Google Drive, WhatsApp, Adobe Reader, etc.).
 */
object DocumentExporter {

    // Dimensiones estándar A4 a 72 DPI (puntos de impresión)
    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842

    private const val MARGIN_HORIZONTAL = 48
    private const val MARGIN_TOP = 56
    private const val MARGIN_BOTTOM = 56
    private const val CONTENT_WIDTH = A4_WIDTH - (MARGIN_HORIZONTAL * 2)
    private const val USABLE_PAGE_HEIGHT = A4_HEIGHT - MARGIN_TOP - MARGIN_BOTTOM

    /**
     * Exporta el documento como archivo PDF digital multipágina.
     */
    fun exportToPdf(
        context: Context,
        title: String,
        content: String,
        fontStyle: String
    ): Uri? {
        val pdfDocument = PdfDocument()

        try {
            val baseTypeface = when (fontStyle) {
                "SANS_SERIF" -> Typeface.SANS_SERIF
                "MONOSPACE" -> Typeface.MONOSPACE
                "CURSIVE" -> Typeface.create("casual", Typeface.NORMAL)
                else -> Typeface.SERIF
            }

            // Pincel para texto del cuerpo
            val bodyPaint = TextPaint().apply {
                isAntiAlias = true
                textSize = 12f
                color = Color.rgb(30, 41, 59)
                typeface = baseTypeface
            }

            // Pincel para encabezados
            val headerPaint = Paint().apply {
                isAntiAlias = true
                textSize = 9f
                color = Color.rgb(100, 116, 139)
                typeface = baseTypeface
            }

            // Pincel para líneas decorativas
            val linePaint = Paint().apply {
                isAntiAlias = true
                strokeWidth = 0.8f
                color = Color.rgb(226, 232, 240)
            }

            // Procesar el contenido en bloques por saltos de página explícitos o automáticos
            val explicitPages = content.split("\n[--- Salto de Página ---]\n", "\n---\n")
            var pageNumber = 1

            for (pageText in explicitPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                // Dibujar encabezado de página
                val displayTitle = if (title.isBlank()) "DOCUMENTO DOCUSHEET" else title.uppercase()
                canvas.drawText(displayTitle, MARGIN_HORIZONTAL.toFloat(), 38f, headerPaint)
                canvas.drawLine(
                    MARGIN_HORIZONTAL.toFloat(),
                    44f,
                    (A4_WIDTH - MARGIN_HORIZONTAL).toFloat(),
                    44f,
                    linePaint
                )

                // Dibujar pie de página
                val footerText = "— Página $pageNumber —"
                val footerWidth = headerPaint.measureText(footerText)
                canvas.drawText(
                    footerText,
                    (A4_WIDTH - footerWidth) / 2f,
                    (A4_HEIGHT - 30).toFloat(),
                    headerPaint
                )

                canvas.drawText(
                    "DocuSheet",
                    (A4_WIDTH - MARGIN_HORIZONTAL - headerPaint.measureText("DocuSheet")).toFloat(),
                    (A4_HEIGHT - 30).toFloat(),
                    headerPaint
                )

                // Renderizar el contenido con StaticLayout para soporte de multilínea e interlineado
                val staticLayout = StaticLayout.Builder
                    .obtain(pageText, 0, pageText.length, bodyPaint, CONTENT_WIDTH)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(4f, 1.2f)
                    .setIncludePad(true)
                    .build()

                canvas.save()
                canvas.translate(MARGIN_HORIZONTAL.toFloat(), MARGIN_TOP.toFloat())
                staticLayout.draw(canvas)
                canvas.restore()

                pdfDocument.finishPage(page)
                pageNumber++
            }

            // Guardar en el directorio seguro de caché para exportaciones
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanTitle = sanitizeFileName(title)
            val outputFile = File(exportDir, "$cleanTitle.pdf")

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Exporta el documento como archivo Markdown (.md).
     */
    fun exportToMarkdown(
        context: Context,
        title: String,
        content: String
    ): Uri? {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanTitle = sanitizeFileName(title)
            val outputFile = File(exportDir, "$cleanTitle.md")

            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val markdownContent = buildString {
                append("# ").append(title.ifBlank { "Documento sin título" }).append("\n\n")
                append("> *Exportado desde DocuSheet el $dateStr*\n\n")
                append("---\n\n")
                append(content)
                append("\n")
            }

            outputFile.writeText(markdownContent, Charsets.UTF_8)

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Lanza el menú del sistema Android para compartir o guardar el archivo exportado.
     */
    fun shareExportedFile(
        context: Context,
        uri: Uri,
        mimeType: String,
        title: String
    ) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Exportar $title")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun sanitizeFileName(name: String): String {
        val sanitized = name.trim().replace(Regex("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ_\\-\\s]"), "")
        return if (sanitized.isBlank()) "Documento_DocuSheet" else sanitized.replace("\\s+".toRegex(), "_")
    }
}
