package com.example.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RtfHandler: Motor de exportación e importación para documentos Rich Text Format (.rtf).
 * 
 * Genera archivos RTF estándar compatibles con WordPad, Microsoft Word, TextEdit de Apple,
 * LibreOffice y visores ofimáticos móviles, e interpreta archivos .rtf para importarlos
 * con formato enriquecido a la hoja de papel de DocuSheet.
 */
object RtfHandler {

    /**
     * Exporta el contenido del documento a un archivo .rtf universal con fuentes,
     * jerarquía tipográfica, colores, tablas y saltos de página.
     */
    fun exportToRtf(
        context: Context,
        title: String,
        content: String
    ): Uri? {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanTitle = sanitizeFileName(title)
            val outputFile = File(exportDir, "$cleanTitle.rtf")

            val rtfText = buildRtfDocument(title, content)
            outputFile.writeText(rtfText, Charsets.US_ASCII)

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
     * Importa un archivo .rtf extrayendo el contenido textual, jerarquías, negritas,
     * cursivas y saltos de página hacia la sintaxis de DocuSheet.
     */
    fun importFromRtf(inputStream: InputStream): Pair<String, String> {
        val rawRtf = inputStream.bufferedReader(Charsets.ISO_8859_1).readText()
        val parsedContent = parseRtfContent(rawRtf)

        // Deducir título a partir del contenido o primera línea
        val lines = parsedContent.lines()
        var detectedTitle = ""
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isNotBlank()) {
                detectedTitle = if (trimmed.startsWith("# ")) {
                    trimmed.removePrefix("# ").trim()
                } else if (trimmed.length <= 60 && !trimmed.startsWith("|") && !trimmed.startsWith("•")) {
                    trimmed
                } else {
                    "Documento RTF Importado"
                }
                break
            }
        }

        if (detectedTitle.isBlank()) {
            detectedTitle = "Documento RTF Importado"
        }

        return Pair(detectedTitle, parsedContent)
    }

    // ==========================================
    // Generación de Documento RTF
    // ==========================================

    private fun buildRtfDocument(title: String, content: String): String {
        val sb = StringBuilder()
        
        // Cabecera RTF estándar
        sb.append("{\\rtf1\\ansi\\ansicpg1252\\deff0\\nouicompat\\deflang1034\n")
        // Tabla de fuentes
        sb.append("{\\fonttbl{\\f0\\fnil\\fcharset0 Calibri;}{\\f1\\froman\\fcharset0 Times New Roman;}{\\f2\\fmodern\\fcharset0 Courier New;}}\n")
        // Tabla de colores
        sb.append("{\\colortbl ;\\red15\\green23\\blue42;\\red30\\green58\\blue138;\\red71\\green85\\blue105;\\red5\\green150\\blue105;\\red185\\green28\\blue28;}\n")
        sb.append("\\viewkind4\\uc1\n")
        sb.append("\\pard\\sa200\\sl276\\slmult1\\f0\\fs24\\lang1034\n")

        // Título del documento centrado y grande
        if (title.isNotBlank()) {
            sb.append("\\pard\\qc\\sa240{\\b\\fs40\\cf1 ").append(escapeRtf(title)).append("}\\par\n")
            sb.append("\\pard\\sa180\\sl276\\slmult1\n")
        }

        val lines = content.lines()
        var inTable = false
        val currentTableRows = mutableListOf<List<String>>()

        fun flushTable() {
            if (currentTableRows.isNotEmpty()) {
                sb.append(generateRtfTable(currentTableRows))
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
                sb.append("\\page\\par\n")
                continue
            }

            // Encabezados
            if (trimmed.startsWith("# ")) {
                val text = trimmed.removePrefix("# ").trim()
                sb.append("\\pard\\sb240\\sa120{\\b\\fs36\\cf1 ").append(escapeRtf(text)).append("}\\par\n")
                sb.append("\\pard\\sa180\\sl276\\slmult1\n")
            } else if (trimmed.startsWith("## ")) {
                val text = trimmed.removePrefix("## ").trim()
                sb.append("\\pard\\sb180\\sa100{\\b\\fs30\\cf2 ").append(escapeRtf(text)).append("}\\par\n")
                sb.append("\\pard\\sa180\\sl276\\slmult1\n")
            } else if (trimmed.startsWith("### ")) {
                val text = trimmed.removePrefix("### ").trim()
                sb.append("\\pard\\sb120\\sa80{\\b\\fs26\\cf3 ").append(escapeRtf(text)).append("}\\par\n")
                sb.append("\\pard\\sa180\\sl276\\slmult1\n")
            } else if (trimmed.startsWith("> ")) {
                // Cita editorial con sangría izquierda y cursiva
                val text = trimmed.removePrefix("> ").trim()
                sb.append("\\pard\\li720\\sa160\\cf3{\\i ").append(escapeRtf(text)).append("}\\par\n")
                sb.append("\\pard\\sa180\\sl276\\slmult1\n")
            } else if (trimmed.startsWith("• ") || trimmed.startsWith("- ")) {
                // Lista con viñeta
                val text = trimmed.removePrefix("• ").removePrefix("- ").trim()
                sb.append("\\pard\\li360\\sa100{\\bullet\\tab ").append(formatRtfInline(text)).append("}\\par\n")
                sb.append("\\pard\\sa180\\sl276\\slmult1\n")
            } else if (trimmed.matches(Regex("^\\d+\\.\\s.*"))) {
                // Lista numerada
                val match = Regex("^(\\d+\\.)\\s*(.*)").find(trimmed)
                val number = match?.groupValues?.get(1) ?: "1."
                val text = match?.groupValues?.get(2) ?: ""
                sb.append("\\pard\\li360\\sa100{\\b ").append(escapeRtf(number)).append("\\tab }").append(formatRtfInline(text)).append("\\par\n")
                sb.append("\\pard\\sa180\\sl276\\slmult1\n")
            } else if (trimmed.startsWith("[ ] ") || trimmed.startsWith("[x] ") || trimmed.startsWith("[X] ")) {
                // Casilla de tarea
                val isChecked = trimmed.startsWith("[x] ") || trimmed.startsWith("[X] ")
                val text = trimmed.substring(4).trim()
                val icon = if (isChecked) "[X] " else "[  ] "
                sb.append("\\pard\\li360\\sa100{\\b\\cf4 ").append(escapeRtf(icon)).append("}")
                if (isChecked) sb.append("{\\strike ")
                sb.append(formatRtfInline(text))
                if (isChecked) sb.append("}")
                sb.append("\\par\n")
                sb.append("\\pard\\sa180\\sl276\\slmult1\n")
            } else if (trimmed.startsWith("[shape:") && trimmed.contains("[/shape]")) {
                val innerText = if (trimmed.contains("]")) trimmed.substringAfter("]").substringBefore("[/shape]") else ""
                sb.append("\\pard\\qc\\sa160{\\b\\cf2 [FIGURA: ").append(escapeRtf(innerText)).append("]}\\par\n")
                sb.append("\\pard\\sa180\\sl276\\slmult1\n")
            } else if (trimmed.startsWith("[nodes:") && trimmed.contains("[/nodes]")) {
                val nodesContent = trimmed.substringAfter("]").substringBefore("[/nodes]").trim()
                sb.append("\\pard\\qc\\sa160{\\b\\cf4 [NODOS: ").append(escapeRtf(nodesContent)).append("]}\\par\n")
                sb.append("\\pard\\sa180\\sl276\\slmult1\n")
            } else if (trimmed.isEmpty()) {
                sb.append("\\pard\\sa120\\par\n")
            } else {
                // Párrafo de texto normal
                var cleanLine = trimmed
                var alignDirective = "\\qj" // Justificado
                if (cleanLine.startsWith("[align:left]")) {
                    alignDirective = "\\ql"
                    cleanLine = cleanLine.removePrefix("[align:left]")
                } else if (cleanLine.startsWith("[align:center]")) {
                    alignDirective = "\\qc"
                    cleanLine = cleanLine.removePrefix("[align:center]")
                } else if (cleanLine.startsWith("[align:right]")) {
                    alignDirective = "\\qr"
                    cleanLine = cleanLine.removePrefix("[align:right]")
                } else if (cleanLine.startsWith("[align:justify]")) {
                    alignDirective = "\\qj"
                    cleanLine = cleanLine.removePrefix("[align:justify]")
                }

                sb.append("\\pard").append(alignDirective).append("\\sa180\\sl276\\slmult1 ")
                sb.append(formatRtfInline(cleanLine))
                sb.append("\\par\n")
            }
        }

        if (inTable) {
            flushTable()
        }

        sb.append("}\n")
        return sb.toString()
    }

    private fun generateRtfTable(rows: List<List<String>>): String {
        if (rows.isEmpty()) return ""
        val sb = StringBuilder()
        val numCols = rows.maxOfOrNull { it.size } ?: 1
        val cellWidth = 8500 / numCols.coerceAtLeast(1)

        for ((rowIndex, row) in rows.withIndex()) {
            val isHeader = rowIndex == 0
            sb.append("\\trowd\\trgaph108\\trleft-108")
            for (colIndex in 0 until numCols) {
                val rightEdge = (colIndex + 1) * cellWidth
                sb.append("\\clbrdrt\\brdrs\\brdrw10\\clbrdrl\\brdrs\\brdrw10\\clbrdrb\\brdrs\\brdrw10\\clbrdrr\\brdrs\\brdrw10")
                if (isHeader) {
                    sb.append("\\clcbpat2") // Fondo resaltado
                }
                sb.append("\\cellx").append(rightEdge)
            }
            sb.append("\\pard\\intbl\\sa60\\sl240 ")
            for (colIndex in 0 until numCols) {
                val cellText = row.getOrNull(colIndex) ?: ""
                if (isHeader) {
                    sb.append("{\\b ").append(escapeRtf(cellText)).append("}\\cell ")
                } else {
                    sb.append(formatRtfInline(cellText)).append("\\cell ")
                }
            }
            sb.append("\\row\n")
        }
        sb.append("\\pard\\sa180\\sl276\\slmult1\n")
        return sb.toString()
    }

    private fun formatRtfInline(text: String): String {
        var clean = text
        // Limpiar etiquetas de directivas complejas
        clean = clean
            .replace(Regex("\\[color:#[0-9A-Fa-f]{6}\\]"), "")
            .replace("[/color]", "")
            .replace(Regex("\\[3d:[^]]*\\]"), "")
            .replace("[/3d]", "")
            .replace(Regex("\\[weight:[^]]*\\]"), "")
            .replace("[/weight]", "")
            .replace(Regex("\\[font:[^]]*\\]"), "")

        val hasBold = clean.contains("**")
        val hasItalic = clean.contains("*")
        val hasUnderline = clean.contains("<u>") || clean.contains("__")
        val hasStrike = clean.contains("~~")

        clean = clean
            .replace("**", "")
            .replace("<u>", "").replace("</u>", "")
            .replace("~~", "")
            .replace("`", "")

        val sb = StringBuilder()
        if (hasBold) sb.append("{\\b ")
        if (hasItalic) sb.append("{\\i ")
        if (hasUnderline) sb.append("{\\ul ")
        if (hasStrike) sb.append("{\\strike ")

        sb.append(escapeRtf(clean))

        if (hasStrike) sb.append("}")
        if (hasUnderline) sb.append("}")
        if (hasItalic) sb.append("}")
        if (hasBold) sb.append("}")

        return sb.toString()
    }

    private fun escapeRtf(text: String): String {
        val sb = StringBuilder()
        for (char in text) {
            when (char) {
                '\\' -> sb.append("\\\\")
                '{' -> sb.append("\\{")
                '}' -> sb.append("\\}")
                '\n' -> sb.append("\\par ")
                else -> {
                    val code = char.code
                    if (code in 32..126) {
                        sb.append(char)
                    } else if (code > 126) {
                        // Secuencia unicode en RTF: \uN?
                        sb.append("\\u").append(code).append("?")
                    } else {
                        // Caracteres especiales
                        sb.append(char)
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun sanitizeFileName(name: String): String {
        val sanitized = name.trim().replace(Regex("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ_\\-\\s]"), "")
        return if (sanitized.isBlank()) "Documento_DocuSheet" else sanitized.replace("\\s+".toRegex(), "_")
    }

    // ==========================================
    // Parser para Importación de RTF
    // ==========================================

    private fun parseRtfContent(rtf: String): String {
        val out = StringBuilder()
        val len = rtf.length
        var i = 0
        var groupDepth = 0
        var skipGroupDepth = -1
        var isBold = false
        var isItalic = false
        var isUnderline = false
        var inTable = false
        var tableCells = mutableListOf<String>()
        val currentCellText = StringBuilder()

        while (i < len) {
            val c = rtf[i]
            if (c == '{') {
                groupDepth++
                i++
                continue
            }
            if (c == '}') {
                if (groupDepth == skipGroupDepth) {
                    skipGroupDepth = -1
                }
                groupDepth--
                i++
                continue
            }

            if (skipGroupDepth != -1 && groupDepth >= skipGroupDepth) {
                // Ignorando grupo especial (fuentes, colores, estilos)
                i++
                continue
            }

            if (c == '\\') {
                i++
                if (i >= len) break
                val next = rtf[i]
                if (next == '\\' || next == '{' || next == '}') {
                    out.append(next)
                    i++
                    continue
                }
                if (next == '\'') {
                    // Carácter hexadecimal \'hh
                    i++
                    if (i + 1 < len) {
                        val hex = rtf.substring(i, i + 2)
                        val code = hex.toIntOrNull(16)
                        if (code != null) {
                            out.append(code.toChar())
                        }
                        i += 2
                    }
                    continue
                }

                // Leer comando de control
                val cmdStart = i
                while (i < len && rtf[i].isLetter()) {
                    i++
                }
                val cmd = rtf.substring(cmdStart, i)
                
                // Leer parámetro numérico opcional (ej: \fs24, \b0)
                val paramStart = i
                if (i < len && (rtf[i] == '-' || rtf[i].isDigit())) {
                    if (rtf[i] == '-') i++
                    while (i < len && rtf[i].isDigit()) {
                        i++
                    }
                }
                val param = rtf.substring(paramStart, i).toIntOrNull()

                // Si hay un espacio delimitador después de la orden, se consume
                if (i < len && rtf[i] == ' ') {
                    i++
                }

                when (cmd) {
                    "fonttbl", "colortbl", "stylesheet", "info", "header", "footer" -> {
                        skipGroupDepth = groupDepth
                    }
                    "b" -> isBold = (param == null || param != 0)
                    "i" -> isItalic = (param == null || param != 0)
                    "ul" -> isUnderline = true
                    "ulnone" -> isUnderline = false
                    "par", "line" -> {
                        out.append("\n\n")
                    }
                    "page" -> {
                        out.append("\n[--- Salto de Página ---]\n\n")
                    }
                    "bullet" -> {
                        out.append("• ")
                    }
                    "cell" -> {
                        tableCells.add(currentCellText.toString().trim())
                        currentCellText.clear()
                    }
                    "row" -> {
                        if (tableCells.isNotEmpty()) {
                            out.append("| ").append(tableCells.joinToString(" | ")).append(" |\n")
                            tableCells.clear()
                        }
                    }
                    "u" -> {
                        // Carácter unicode \uN
                        if (param != null) {
                            out.append(param.toChar())
                            // RTF consume el siguiente carácter alternativo si existe
                            if (i < len && rtf[i] == '?') {
                                i++
                            }
                        }
                    }
                }
                continue
            }

            if (c == '\r' || c == '\n') {
                i++
                continue
            }

            out.append(c)
            i++
        }

        return out.toString().trim()
    }
}
