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
     * Exporta el documento como HTML editorial estructurado (.html).
     * 
     * Genera un documento HTML5 autónomo y responsivo con hoja de estilos CSS moderna,
     * respetando la tipografía elegida, justificado de texto editorial, tablas estilizadas,
     * citas elegantes, encabezados jerárquicos y simulación visual de hoja de papel física.
     */
    fun exportToHtml(
        context: Context,
        title: String,
        content: String,
        fontStyle: String = "SERIF"
    ): Uri? {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanTitle = sanitizeFileName(title)
            val outputFile = File(exportDir, "$cleanTitle.html")

            val fontFamilyCss = when (fontStyle) {
                "SANS_SERIF" -> "'Inter', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif"
                "MONOSPACE" -> "'JetBrains Mono', 'Fira Code', 'Courier New', Courier, monospace"
                "CURSIVE" -> "'Caveat', 'Dancing Script', 'Brush Script MT', cursive"
                else -> "'Merriweather', 'Georgia', 'Cambria', serif"
            }

            val dateStr = SimpleDateFormat("dd 'de' MMMM 'de' yyyy, HH:mm", Locale.getDefault()).format(Date())
            val displayTitle = if (title.isBlank()) "Documento DocuSheet" else title

            val htmlBody = convertContentToHtmlBody(content)

            val fullHtml = """
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${escapeHtml(displayTitle)}</title>
    <style>
        :root {
            --bg-canvas: #f1f5f9;
            --paper-bg: #ffffff;
            --text-main: #1e293b;
            --text-muted: #64748b;
            --border-color: #e2e8f0;
            --accent-color: #2563eb;
            --paper-shadow: 0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px -2px rgba(0,0,0,0.1), 0 0 0 1px rgba(0,0,0,0.05);
        }
        @media (prefers-color-scheme: dark) {
            :root {
                --bg-canvas: #0f172a;
                --paper-bg: #1e293b;
                --text-main: #f8fafc;
                --text-muted: #94a3b8;
                --border-color: #334155;
                --accent-color: #38bdf8;
                --paper-shadow: 0 4px 6px -1px rgba(0,0,0,0.3);
            }
        }
        body {
            margin: 0;
            padding: 24px 16px;
            background-color: var(--bg-canvas);
            color: var(--text-main);
            font-family: $fontFamilyCss;
            line-height: 1.7;
            font-size: 16px;
            -webkit-font-smoothing: antialiased;
        }
        .page-container {
            max-width: 800px;
            margin: 0 auto;
            background: var(--paper-bg);
            padding: 48px 40px;
            border-radius: 8px;
            box-shadow: var(--paper-shadow);
            border: 1px solid var(--border-color);
            position: relative;
        }
        .doc-header {
            border-bottom: 2px solid var(--border-color);
            padding-bottom: 20px;
            margin-bottom: 32px;
        }
        .doc-title {
            font-size: 28px;
            font-weight: 800;
            margin: 0 0 8px 0;
            color: var(--text-main);
            letter-spacing: -0.02em;
        }
        .doc-meta {
            font-size: 13px;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 0.05em;
            display: flex;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 8px;
        }
        .doc-body {
            word-wrap: break-word;
        }
        h1, h2, h3 {
            color: var(--text-main);
            font-weight: 700;
            margin-top: 1.5em;
            margin-bottom: 0.5em;
            line-height: 1.3;
        }
        h1 { font-size: 24px; border-bottom: 1px solid var(--border-color); padding-bottom: 6px; }
        h2 { font-size: 20px; }
        h3 { font-size: 17px; }
        p {
            margin: 0 0 1.2em 0;
            text-align: justify;
            text-justify: inter-word;
        }
        blockquote {
            margin: 1.5em 0;
            padding: 12px 20px;
            border-left: 4px solid var(--accent-color);
            background: rgba(37, 99, 235, 0.05);
            font-style: italic;
            border-radius: 0 6px 6px 0;
        }
        hr {
            border: 0;
            height: 1px;
            background: var(--border-color);
            margin: 2em 0;
        }
        .page-break {
            margin: 3em 0;
            border-top: 2px dashed var(--border-color);
            text-align: center;
            position: relative;
        }
        .page-break::after {
            content: "— Salto de Página —";
            position: absolute;
            top: -10px;
            left: 50%;
            transform: translateX(-50%);
            background: var(--paper-bg);
            padding: 0 12px;
            font-size: 11px;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 0.1em;
        }
        ul, ol {
            padding-left: 24px;
            margin-bottom: 1.2em;
        }
        li {
            margin-bottom: 0.4em;
        }
        .doc-footer {
            margin-top: 48px;
            padding-top: 20px;
            border-top: 1px solid var(--border-color);
            display: flex;
            justify-content: space-between;
            font-size: 12px;
            color: var(--text-muted);
        }
        @media print {
            body {
                background: none;
                padding: 0;
            }
            .page-container {
                box-shadow: none;
                border: none;
                padding: 0;
                max-width: 100%;
            }
            .page-break {
                page-break-after: always;
                border: none;
            }
            .page-break::after {
                display: none;
            }
        }
    </style>
</head>
<body>
    <article class="page-container">
        <header class="doc-header">
            <h1 class="doc-title">${escapeHtml(displayTitle)}</h1>
            <div class="doc-meta">
                <span>DocuSheet Editorial</span>
                <time datetime="${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}">$dateStr</time>
            </div>
        </header>
        <section class="doc-body">
            $htmlBody
        </section>
        <footer class="doc-footer">
            <span>DocuSheet Móvil</span>
            <span>Documento Formal</span>
        </footer>
    </article>
</body>
</html>
            """.trimIndent()

            outputFile.writeText(fullHtml, Charsets.UTF_8)

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
     * Exporta el documento como Documento de Texto Plano (.txt).
     * Universal, ultra-ligero y compatible con cualquier dispositivo o sistema operativo.
     */
    fun exportToPlainText(
        context: Context,
        title: String,
        content: String
    ): Uri? {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanTitle = sanitizeFileName(title)
            val outputFile = File(exportDir, "$cleanTitle.txt")

            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            val separatorLine = "=".repeat(60)
            val plainText = buildString {
                append(title.ifBlank { "DOCUMENTO SIN TÍTULO" }.uppercase())
                append("\n")
                append(separatorLine)
                append("\nExportado desde DocuSheet | Fecha: $dateStr\n")
                append(separatorLine)
                append("\n\n")
                append(content)
                append("\n\n")
                append(separatorLine)
                append("\nFin del documento - DocuSheet\n")
            }

            outputFile.writeText(plainText, Charsets.UTF_8)

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

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun convertContentToHtmlBody(rawContent: String): String {
        val lines = rawContent.lines()
        val sb = StringBuilder()
        var inList = false
        var listType = "" // "ul" or "ol"

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed == "[--- Salto de Página ---]" || trimmed == "---") {
                if (inList) {
                    sb.append("</$listType>\n")
                    inList = false
                }
                sb.append("<div class=\"page-break\"></div>\n")
                continue
            }

            if (trimmed.startsWith("# ")) {
                if (inList) { sb.append("</$listType>\n"); inList = false }
                sb.append("<h1>").append(escapeHtml(trimmed.removePrefix("# "))).append("</h1>\n")
            } else if (trimmed.startsWith("## ")) {
                if (inList) { sb.append("</$listType>\n"); inList = false }
                sb.append("<h2>").append(escapeHtml(trimmed.removePrefix("## "))).append("</h2>\n")
            } else if (trimmed.startsWith("### ")) {
                if (inList) { sb.append("</$listType>\n"); inList = false }
                sb.append("<h3>").append(escapeHtml(trimmed.removePrefix("### "))).append("</h3>\n")
            } else if (trimmed.startsWith("> ")) {
                if (inList) { sb.append("</$listType>\n"); inList = false }
                sb.append("<blockquote>").append(escapeHtml(trimmed.removePrefix("> "))).append("</blockquote>\n")
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("• ")) {
                if (!inList || listType != "ul") {
                    if (inList) sb.append("</$listType>\n")
                    sb.append("<ul>\n")
                    inList = true
                    listType = "ul"
                }
                val itemContent = trimmed.removePrefix("- ").removePrefix("• ")
                sb.append("  <li>").append(escapeHtml(itemContent)).append("</li>\n")
            } else if (trimmed.matches(Regex("^\\d+\\.\\s.*"))) {
                if (!inList || listType != "ol") {
                    if (inList) sb.append("</$listType>\n")
                    sb.append("<ol>\n")
                    inList = true
                    listType = "ol"
                }
                val itemContent = trimmed.replaceFirst(Regex("^\\d+\\.\\s*"), "")
                sb.append("  <li>").append(escapeHtml(itemContent)).append("</li>\n")
            } else if (trimmed.isEmpty()) {
                if (inList) {
                    sb.append("</$listType>\n")
                    inList = false
                }
                // Línea vacía: espacio entre párrafos
            } else {
                if (inList) {
                    sb.append("</$listType>\n")
                    inList = false
                }
                sb.append("<p>").append(escapeHtml(trimmed)).append("</p>\n")
            }
        }
        if (inList) {
            sb.append("</$listType>\n")
        }
        return sb.toString()
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
