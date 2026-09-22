package com.example.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * DocxHandler: Motor de importación y exportación para documentos Microsoft Word (.docx).
 * 
 * Implementa la especificación Office Open XML (ECMA-376) empaquetando y descomprimiendo
 * estructuras ZIP en memoria sin librerías pesadas externas, garantizando máximo rendimiento
 * y bajo consumo de memoria en dispositivos móviles.
 */
object DocxHandler {

    /**
     * Exporta el contenido del documento a un archivo .docx completo y compatible con Microsoft Word,
     * Google Docs, LibreOffice Writer y procesadores móviles.
     */
    fun exportToDocx(
        context: Context,
        title: String,
        content: String
    ): Uri? {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanTitle = sanitizeFileName(title)
            val outputFile = File(exportDir, "$cleanTitle.docx")

            ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
                // 1. [Content_Types].xml
                zipOut.putNextEntry(ZipEntry("[Content_Types].xml"))
                zipOut.write(getContentTypesXml().toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()

                // 2. _rels/.rels
                zipOut.putNextEntry(ZipEntry("_rels/.rels"))
                zipOut.write(getRootRelsXml().toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()

                // 3. word/_rels/document.xml.rels
                zipOut.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
                zipOut.write(getDocumentRelsXml().toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()

                // 4. word/styles.xml
                zipOut.putNextEntry(ZipEntry("word/styles.xml"))
                zipOut.write(getStylesXml().toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()

                // 5. word/document.xml (Cuerpo del documento traducido)
                zipOut.putNextEntry(ZipEntry("word/document.xml"))
                val docXml = buildDocumentXml(title, content)
                zipOut.write(docXml.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()
            }

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
     * Importa un archivo .docx extrayendo el texto estructurado, títulos, párrafos y tablas
     * desde el paquete OpenXML interno.
     */
    fun importFromDocx(inputStream: InputStream): Pair<String, String> {
        val extractedText = StringBuilder()
        var detectedTitle = ""

        try {
            ZipInputStream(inputStream).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        // Leemos todo el XML de document.xml
                        val xmlContent = zipIn.bufferedReader(Charsets.UTF_8).readText()
                        parseDocumentXml(xmlContent, extractedText)
                        break
                    }
                    entry = zipIn.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val fullText = extractedText.toString().trim()
        // Intentar deducir título de la primera línea si comienza como título
        val lines = fullText.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isNotBlank()) {
                detectedTitle = if (trimmed.startsWith("# ")) {
                    trimmed.removePrefix("# ").trim()
                } else if (trimmed.length <= 60 && !trimmed.startsWith("|") && !trimmed.startsWith("•")) {
                    trimmed
                } else {
                    "Documento Word Importado"
                }
                break
            }
        }

        if (detectedTitle.isBlank()) {
            detectedTitle = "Documento Word Importado"
        }

        return Pair(detectedTitle, fullText)
    }

    // ==========================================
    // Generación de XML para DOCX
    // ==========================================

    private fun getContentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>""".trimIndent()

    private fun getRootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".trimIndent()

    private fun getDocumentRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""".trimIndent()

    private fun getStylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
    <w:name w:val="Normal"/>
    <w:rPr>
      <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/>
      <w:sz w:val="24"/>
      <w:color w:val="1E293B"/>
    </w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading1">
    <w:name w:val="heading 1"/>
    <w:basedOn w:val="Normal"/>
    <w:pPr>
      <w:spacing w:before="240" w:after="120"/>
    </w:pPr>
    <w:rPr>
      <w:b/>
      <w:sz w:val="40"/>
      <w:color w:val="0F172A"/>
    </w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading2">
    <w:name w:val="heading 2"/>
    <w:basedOn w:val="Normal"/>
    <w:pPr>
      <w:spacing w:before="180" w:after="80"/>
    </w:pPr>
    <w:rPr>
      <w:b/>
      <w:sz w:val="32"/>
      <w:color w:val="1E3A8A"/>
    </w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading3">
    <w:name w:val="heading 3"/>
    <w:basedOn w:val="Normal"/>
    <w:pPr>
      <w:spacing w:before="120" w:after="60"/>
    </w:pPr>
    <w:rPr>
      <w:b/>
      <w:sz w:val="26"/>
      <w:color w:val="334155"/>
    </w:rPr>
  </w:style>
</w:styles>""".trimIndent()

    private fun buildDocumentXml(title: String, content: String): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
""")

        // Título del documento como encabezado principal si existe
        if (title.isNotBlank()) {
            sb.append("    <w:p><w:pPr><w:pStyle w:val=\"Heading1\"/><w:jc w:val=\"center\"/></w:pPr>")
            sb.append("<w:r><w:rPr><w:b/><w:sz w:val=\"48\"/></w:rPr><w:t>${escapeXml(title)}</w:t></w:r></w:p>\n")
        }

        val lines = content.lines()
        var inTable = false
        val currentTableRows = mutableListOf<List<String>>()

        fun flushTable() {
            if (currentTableRows.isNotEmpty()) {
                sb.append(generateDocxTable(currentTableRows))
                currentTableRows.clear()
            }
            inTable = false
        }

        for (line in lines) {
            val trimmed = line.trim()

            // Detección de tablas Markdown
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                if (!inTable) {
                    inTable = true
                    currentTableRows.clear()
                }
                // Si es la línea de separación (|---|---|), la ignoramos para las celdas
                val isSeparator = trimmed.split("|")
                    .filter { it.isNotBlank() }
                    .all { it.trim().matches(Regex("^:?-+:?$")) }
                if (!isSeparator) {
                    val cells = trimmed.split("|")
                        .drop(1)
                        .dropLast(1)
                        .map { it.trim() }
                    currentTableRows.add(cells)
                }
                continue
            } else if (inTable) {
                flushTable()
            }

            // Saltos de página
            if (trimmed == "[--- Salto de Página ---]" || trimmed == "---") {
                sb.append("    <w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>\n")
                continue
            }

            // Encabezados
            if (trimmed.startsWith("# ")) {
                val text = trimmed.removePrefix("# ").trim()
                sb.append("    <w:p><w:pPr><w:pStyle w:val=\"Heading1\"/></w:pPr>")
                sb.append(parseFormattedRuns(text))
                sb.append("</w:p>\n")
            } else if (trimmed.startsWith("## ")) {
                val text = trimmed.removePrefix("## ").trim()
                sb.append("    <w:p><w:pPr><w:pStyle w:val=\"Heading2\"/></w:pPr>")
                sb.append(parseFormattedRuns(text))
                sb.append("</w:p>\n")
            } else if (trimmed.startsWith("### ")) {
                val text = trimmed.removePrefix("### ").trim()
                sb.append("    <w:p><w:pPr><w:pStyle w:val=\"Heading3\"/></w:pPr>")
                sb.append(parseFormattedRuns(text))
                sb.append("</w:p>\n")
            } else if (trimmed.startsWith("> ")) {
                // Cita editorial
                val text = trimmed.removePrefix("> ").trim()
                sb.append("    <w:p><w:pPr><w:ind w:left=\"720\"/><w:pBdr><w:left w:val=\"single\" w:sz=\"24\" w:space=\"8\" w:color=\"64748B\"/></w:pBdr></w:pPr>")
                sb.append(parseFormattedRuns(text, defaultItalic = true, defaultColor = "475569"))
                sb.append("</w:p>\n")
            } else if (trimmed.startsWith("• ") || trimmed.startsWith("- ")) {
                // Viñeta
                val text = trimmed.removePrefix("• ").removePrefix("- ").trim()
                sb.append("    <w:p><w:pPr><w:ind w:left=\"360\"/></w:pPr>")
                sb.append("<w:r><w:t xml:space=\"preserve\">•  </w:t></w:r>")
                sb.append(parseFormattedRuns(text))
                sb.append("</w:p>\n")
            } else if (trimmed.matches(Regex("^\\d+\\.\\s.*"))) {
                // Lista numerada
                val match = Regex("^(\\d+\\.)\\s*(.*)").find(trimmed)
                val number = match?.groupValues?.get(1) ?: "1."
                val text = match?.groupValues?.get(2) ?: ""
                sb.append("    <w:p><w:pPr><w:ind w:left=\"360\"/></w:pPr>")
                sb.append("<w:r><w:b/><w:t xml:space=\"preserve\">$number  </w:t></w:r>")
                sb.append(parseFormattedRuns(text))
                sb.append("</w:p>\n")
            } else if (trimmed.startsWith("[ ] ") || trimmed.startsWith("[x] ") || trimmed.startsWith("[X] ")) {
                // Casilla de tarea
                val isChecked = trimmed.startsWith("[x] ") || trimmed.startsWith("[X] ")
                val text = trimmed.substring(4).trim()
                val icon = if (isChecked) "☑ " else "☐ "
                sb.append("    <w:p><w:pPr><w:ind w:left=\"360\"/></w:pPr>")
                sb.append("<w:r><w:rPr><w:color w:val=\"${if (isChecked) "059669" else "64748B"}\"/></w:rPr><w:t xml:space=\"preserve\">$icon </w:t></w:r>")
                sb.append(parseFormattedRuns(text, defaultStrikethrough = isChecked))
                sb.append("</w:p>\n")
            } else if (trimmed.startsWith("[shape:") && trimmed.contains("[/shape]")) {
                // Figura geométrica convertida a bloque destacado
                val shapeContent = trimmed.substringAfter("[/shape]").trim()
                val shapeInfo = trimmed.substringBefore("[/shape]").removePrefix("[shape:")
                val innerText = if (trimmed.contains("]")) trimmed.substringAfter("]").substringBefore("[/shape]") else ""
                sb.append("    <w:p><w:pPr><w:jc w:val=\"center\"/><w:pBdr><w:top w:val=\"single\" w:sz=\"12\" w:color=\"3B82F6\"/><w:left w:val=\"single\" w:sz=\"12\" w:color=\"3B82F6\"/><w:bottom w:val=\"single\" w:sz=\"12\" w:color=\"3B82F6\"/><w:right w:val=\"single\" w:sz=\"12\" w:color=\"3B82F6\"/></w:pBdr><w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"F1F5F9\"/></w:pPr>")
                sb.append("<w:r><w:rPr><w:b/><w:color w:val=\"1E40AF\"/></w:rPr><w:t xml:space=\"preserve\">[FIGURA: $innerText] </w:t></w:r>")
                sb.append("</w:p>\n")
            } else if (trimmed.startsWith("[nodes:") && trimmed.contains("[/nodes]")) {
                // Diagrama de nodos
                val nodesContent = trimmed.substringAfter("]").substringBefore("[/nodes]").trim()
                sb.append("    <w:p><w:pPr><w:jc w:val=\"center\"/><w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"EEF2F6\"/></w:pPr>")
                sb.append("<w:r><w:rPr><w:b/><w:color w:val=\"0F766E\"/></w:rPr><w:t xml:space=\"preserve\">[DIAGRAMA DE NODOS: $nodesContent] </w:t></w:r>")
                sb.append("</w:p>\n")
            } else if (trimmed.isEmpty()) {
                // Párrafo vacío
                sb.append("    <w:p/>\n")
            } else {
                // Párrafo de texto normal con soporte de alineación
                var align = "both" // Justificado por defecto en DocuSheet
                var cleanLine = trimmed
                if (cleanLine.startsWith("[align:left]")) {
                    align = "left"
                    cleanLine = cleanLine.removePrefix("[align:left]")
                } else if (cleanLine.startsWith("[align:center]")) {
                    align = "center"
                    cleanLine = cleanLine.removePrefix("[align:center]")
                } else if (cleanLine.startsWith("[align:right]")) {
                    align = "right"
                    cleanLine = cleanLine.removePrefix("[align:right]")
                } else if (cleanLine.startsWith("[align:justify]")) {
                    align = "both"
                    cleanLine = cleanLine.removePrefix("[align:justify]")
                }

                sb.append("    <w:p><w:pPr><w:jc w:val=\"$align\"/></w:pPr>")
                sb.append(parseFormattedRuns(cleanLine))
                sb.append("</w:p>\n")
            }
        }

        if (inTable) {
            flushTable()
        }

        sb.append("""  </w:body>
</w:document>""")
        return sb.toString()
    }

    private fun generateDocxTable(rows: List<List<String>>): String {
        if (rows.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append("    <w:tbl>\n")
        sb.append("""      <w:tblPr>
        <w:tblW w:w="0" w:type="auto"/>
        <w:tblBorders>
          <w:top w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
          <w:left w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
          <w:bottom w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
          <w:right w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
          <w:insideH w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>
          <w:insideV w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>
        </w:tblBorders>
      </w:tblPr>
""")

        for ((rowIndex, row) in rows.withIndex()) {
            val isHeader = rowIndex == 0
            sb.append("      <w:tr>\n")
            if (isHeader) {
                sb.append("        <w:trPr><w:tblHeader/></w:trPr>\n")
            }
            for (cell in row) {
                sb.append("        <w:tc>\n")
                sb.append("          <w:tcPr>")
                if (isHeader) {
                    sb.append("<w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"F1F5F9\"/>")
                }
                sb.append("</w:tcPr>\n")
                sb.append("          <w:p>")
                if (isHeader) {
                    sb.append("<w:pPr><w:jc w:val=\"center\"/></w:pPr>")
                    sb.append(parseFormattedRuns(cell, defaultBold = true, defaultColor = "0F172A"))
                } else {
                    sb.append(parseFormattedRuns(cell))
                }
                sb.append("</w:p>\n")
                sb.append("        </w:tc>\n")
            }
            sb.append("      </w:tr>\n")
        }
        sb.append("    </w:tbl>\n")
        return sb.toString()
    }

    /**
     * Parsea etiquetas Markdown y directivas de DocuSheet en fragmentos <w:r> de Word.
     */
    private fun parseFormattedRuns(
        text: String,
        defaultBold: Boolean = false,
        defaultItalic: Boolean = false,
        defaultUnderline: Boolean = false,
        defaultStrikethrough: Boolean = false,
        defaultColor: String? = null
    ): String {
        val runs = StringBuilder()

        // Verificamos si contiene fragmentos 3D para segmentarlos con relieve y sombra nativa de Word
        val threeDRegex = Regex("""\[3d(?::([^,\]]+))?(?:,([^\]]+))?\](.*?)\[/3d\]""", RegexOption.IGNORE_CASE)
        if (threeDRegex.containsMatchIn(text)) {
            var lastIdx = 0
            for (m in threeDRegex.findAll(text)) {
                if (m.range.first > lastIdx) {
                    val pre = text.substring(lastIdx, m.range.first)
                    runs.append(parseFormattedRuns(pre, defaultBold, defaultItalic, defaultUnderline, defaultStrikethrough, defaultColor))
                }
                val frontParam = m.groupValues[2].trim().ifBlank { m.groupValues[1].trim() }.removePrefix("#")
                val frontVal = if (frontParam.matches(Regex("[0-9A-Fa-f]{6}"))) frontParam else "EA580C"
                val innerText = m.groupValues[3]
                runs.append("<w:r><w:rPr><w:b/><w:shadow/><w:color w:val=\"$frontVal\"/></w:rPr><w:t xml:space=\"preserve\">${escapeXml(innerText)}</w:t></w:r>")
                lastIdx = m.range.last + 1
            }
            if (lastIdx < text.length) {
                val post = text.substring(lastIdx)
                runs.append(parseFormattedRuns(post, defaultBold, defaultItalic, defaultUnderline, defaultStrikethrough, defaultColor))
            }
            return runs.toString()
        }

        var clean = text
        val hasBold = defaultBold || clean.contains("**")
        val hasItalic = defaultItalic || clean.contains("*")
        val hasUnderline = defaultUnderline || clean.contains("<u>") || clean.contains("__")
        val hasStrike = defaultStrikethrough || clean.contains("~~")

        // Extracción de color personalizado si existe [color:#HEX]
        var textColor = defaultColor
        val colorMatch = Regex("\\[color:(#[0-9A-Fa-f]{6})\\]").find(clean)
        if (colorMatch != null) {
            textColor = colorMatch.groupValues[1].removePrefix("#")
            clean = clean.replace(Regex("\\[color:#[0-9A-Fa-f]{6}\\]"), "").replace("[/color]", "")
        }

        // Limpiar directivas restantes de weight, font, etc.
        clean = clean
            .replace(Regex("\\[weight:[^]]*\\]"), "")
            .replace("[/weight]", "")
            .replace(Regex("\\[font:[^]]*\\]"), "")
            .replace("**", "")
            .replace("<u>", "").replace("</u>", "")
            .replace("~~", "")
            .replace("`", "")

        runs.append("<w:r><w:rPr>")
        if (hasBold) runs.append("<w:b/>")
        if (hasItalic) runs.append("<w:i/>")
        if (hasUnderline) runs.append("<w:u w:val=\"single\"/>")
        if (hasStrike) runs.append("<w:strike/>")
        if (textColor != null) runs.append("<w:color w:val=\"$textColor\"/>")
        runs.append("</w:rPr><w:t xml:space=\"preserve\">${escapeXml(clean)}</w:t></w:r>")

        return runs.toString()
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun sanitizeFileName(name: String): String {
        val sanitized = name.trim().replace(Regex("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ_\\-\\s]"), "")
        return if (sanitized.isBlank()) "Documento_DocuSheet" else sanitized.replace("\\s+".toRegex(), "_")
    }

    // ==========================================
    // Parser para Importación desde document.xml
    // ==========================================

    private fun parseDocumentXml(xmlContent: String, out: StringBuilder) {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xmlContent))

        var eventType = parser.eventType
        var inParagraph = false
        var inTable = false
        var inTableRow = false
        var inTableCell = false
        var isBold = false
        var isItalic = false
        var isUnderline = false
        var isHeading1 = false
        var isHeading2 = false
        var isHeading3 = false
        var isPageBreak = false
        var paragraphText = StringBuilder()
        var tableRowCells = mutableListOf<String>()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name ?: ""
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (name) {
                        "tbl" -> {
                            inTable = true
                        }
                        "tr" -> {
                            inTableRow = true
                            tableRowCells.clear()
                        }
                        "tc" -> {
                            inTableCell = true
                            paragraphText.clear()
                        }
                        "p" -> {
                            inParagraph = true
                            if (!inTableCell) {
                                paragraphText.clear()
                            }
                            isHeading1 = false
                            isHeading2 = false
                            isHeading3 = false
                            isPageBreak = false
                        }
                        "pStyle" -> {
                            val styleVal = parser.getAttributeValue(null, "val") ?: ""
                            if (styleVal.equals("Heading1", ignoreCase = true) || styleVal.equals("heading 1", ignoreCase = true)) {
                                isHeading1 = true
                            } else if (styleVal.equals("Heading2", ignoreCase = true) || styleVal.equals("heading 2", ignoreCase = true)) {
                                isHeading2 = true
                            } else if (styleVal.equals("Heading3", ignoreCase = true) || styleVal.equals("heading 3", ignoreCase = true)) {
                                isHeading3 = true
                            }
                        }
                        "b" -> isBold = true
                        "i" -> isItalic = true
                        "u" -> isUnderline = true
                        "br" -> {
                            val type = parser.getAttributeValue(null, "type")
                            if (type == "page") {
                                isPageBreak = true
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inParagraph) {
                        val text = parser.text ?: ""
                        if (text.isNotEmpty()) {
                            val formatted = when {
                                isBold && isItalic -> "***$text***"
                                isBold -> "**$text**"
                                isItalic -> "*$text*"
                                isUnderline -> "<u>$text</u>"
                                else -> text
                            }
                            paragraphText.append(formatted)
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (name) {
                        "b" -> isBold = false
                        "i" -> isItalic = false
                        "u" -> isUnderline = false
                        "p" -> {
                            inParagraph = false
                            if (isPageBreak) {
                                out.append("\n[--- Salto de Página ---]\n\n")
                            }
                            if (!inTableCell) {
                                val line = paragraphText.toString().trim()
                                if (line.isNotEmpty()) {
                                    val prefix = when {
                                        isHeading1 -> "# "
                                        isHeading2 -> "## "
                                        isHeading3 -> "### "
                                        else -> ""
                                    }
                                    out.append(prefix).append(line).append("\n\n")
                                }
                            }
                        }
                        "tc" -> {
                            inTableCell = false
                            tableRowCells.add(paragraphText.toString().trim())
                        }
                        "tr" -> {
                            inTableRow = false
                            if (tableRowCells.isNotEmpty()) {
                                out.append("| ").append(tableRowCells.joinToString(" | ")).append(" |\n")
                            }
                        }
                        "tbl" -> {
                            inTable = false
                            out.append("\n")
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    }
}
