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
 * LatexHandler: Motor de exportación e importación para documentos científicos y editoriales LaTeX (.tex).
 * 
 * Permite a estudiantes, investigadores, ingenieros y redactores técnicos exportar sus hojas
 * en código fuente LaTeX compilable con tipografía A4 de imprenta, e importar archivos .tex
 * estructurados hacia el lienzo interactivo de DocuSheet.
 */
object LatexHandler {

    /**
     * Exporta el documento como código fuente LaTeX (.tex) limpio y compilable con pdfLaTeX / XeLaTeX.
     */
    fun exportToLatex(
        context: Context,
        title: String,
        content: String
    ): Uri? {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanTitle = sanitizeFileName(title)
            val outputFile = File(exportDir, "$cleanTitle.tex")

            val latexDoc = buildLatexDocument(title, content)
            outputFile.writeText(latexDoc, Charsets.UTF_8)

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
     * Importa un archivo .tex extrayendo títulos (\section), párrafos, negritas,
     * cursivas, listas (\item) y tablas (\begin{tabular}) hacia la hoja de DocuSheet.
     */
    fun importFromLatex(inputStream: InputStream): Pair<String, String> {
        val rawTex = inputStream.bufferedReader(Charsets.UTF_8).readText()
        val (detectedTitle, parsedContent) = parseLatexDocument(rawTex)
        return Pair(detectedTitle, parsedContent)
    }

    // ==========================================
    // Generación de Documento LaTeX
    // ==========================================

    private fun buildLatexDocument(title: String, content: String): String {
        val sb = StringBuilder()
        val safeTitle = escapeLatex(title.ifBlank { "Documento DocuSheet" })
        val dateStr = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "ES")).format(Date())

        sb.append("""\documentclass[11pt,a4paper]{article}
\usepackage[utf8]{inputenc}
\usepackage[spanish,es-tabla]{babel}
\usepackage[margin=2.5cm]{geometry}
\usepackage{amsmath,amssymb}
\usepackage{booktabs}
\usepackage{xcolor}
\usepackage{tcolorbox}
\usepackage{enumitem}
\usepackage{hyperref}

\hypersetup{
    colorlinks=true,
    linkcolor=blue!70!black,
    urlcolor=blue!70!black
}

\title{\textbf{$safeTitle}}
\author{Redactado en DocuSheet Móvil}
\date{$dateStr}

\begin{document}

\maketitle

""")

        val lines = content.lines()
        var inList = false
        var listType = "" // "itemize" or "enumerate"
        var inTable = false
        val currentTableRows = mutableListOf<List<String>>()

        fun flushTable() {
            if (currentTableRows.isNotEmpty()) {
                sb.append(generateLatexTable(currentTableRows))
                currentTableRows.clear()
            }
            inTable = false
        }

        fun closeListIfNeeded() {
            if (inList) {
                sb.append("\\end{$listType}\n\n")
                inList = false
                listType = ""
            }
        }

        for (line in lines) {
            val trimmed = line.trim()

            // Detección de tablas Markdown
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                closeListIfNeeded()
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
                closeListIfNeeded()
                sb.append("\\newpage\n\n")
                continue
            }

            // Encabezados
            if (trimmed.startsWith("# ")) {
                closeListIfNeeded()
                val text = trimmed.removePrefix("# ").trim()
                sb.append("\\section{").append(formatLatexInline(text)).append("}\n\n")
            } else if (trimmed.startsWith("## ")) {
                closeListIfNeeded()
                val text = trimmed.removePrefix("## ").trim()
                sb.append("\\subsection{").append(formatLatexInline(text)).append("}\n\n")
            } else if (trimmed.startsWith("### ")) {
                closeListIfNeeded()
                val text = trimmed.removePrefix("### ").trim()
                sb.append("\\subsubsection{").append(formatLatexInline(text)).append("}\n\n")
            } else if (trimmed.startsWith("> ")) {
                closeListIfNeeded()
                val text = trimmed.removePrefix("> ").trim()
                sb.append("\\begin{quote}\n\\textit{").append(formatLatexInline(text)).append("}\n\\end{quote}\n\n")
            } else if (trimmed.startsWith("• ") || trimmed.startsWith("- ")) {
                val text = trimmed.removePrefix("• ").removePrefix("- ").trim()
                if (!inList || listType != "itemize") {
                    closeListIfNeeded()
                    sb.append("\\begin{itemize}\n")
                    inList = true
                    listType = "itemize"
                }
                sb.append("  \\item ").append(formatLatexInline(text)).append("\n")
            } else if (trimmed.matches(Regex("^\\d+\\.\\s.*"))) {
                val text = trimmed.replaceFirst(Regex("^\\d+\\.\\s*"), "")
                if (!inList || listType != "enumerate") {
                    closeListIfNeeded()
                    sb.append("\\begin{enumerate}\n")
                    inList = true
                    listType = "enumerate"
                }
                sb.append("  \\item ").append(formatLatexInline(text)).append("\n")
            } else if (trimmed.startsWith("[ ] ") || trimmed.startsWith("[x] ") || trimmed.startsWith("[X] ")) {
                val isChecked = trimmed.startsWith("[x] ") || trimmed.startsWith("[X] ")
                val text = trimmed.substring(4).trim()
                if (!inList || listType != "itemize") {
                    closeListIfNeeded()
                    sb.append("\\begin{itemize}\n")
                    inList = true
                    listType = "itemize"
                }
                val boxSymbol = if (isChecked) "$\\boxtimes$ " else "$\\square$ "
                sb.append("  \\item ").append(boxSymbol).append(formatLatexInline(text)).append("\n")
            } else if (trimmed.startsWith("[shape:") && trimmed.contains("[/shape]")) {
                closeListIfNeeded()
                val innerText = if (trimmed.contains("]")) trimmed.substringAfter("]").substringBefore("[/shape]") else ""
                sb.append("\\begin{tcolorbox}[colback=blue!5!white,colframe=blue!60!black,title={Figura}]\n")
                sb.append(formatLatexInline(innerText)).append("\n")
                sb.append("\\end{tcolorbox}\n\n")
            } else if (trimmed.startsWith("[nodes:") && trimmed.contains("[/nodes]")) {
                closeListIfNeeded()
                val nodesContent = trimmed.substringAfter("]").substringBefore("[/nodes]").trim()
                sb.append("\\begin{tcolorbox}[colback=teal!5!white,colframe=teal!60!black,title={Diagrama de Procesos}]\n")
                sb.append("\\texttt{").append(escapeLatex(nodesContent)).append("}\n")
                sb.append("\\end{tcolorbox}\n\n")
            } else if (trimmed.isEmpty()) {
                closeListIfNeeded()
                sb.append("\n")
            } else {
                closeListIfNeeded()
                // Párrafo con alineación opcional
                var cleanLine = trimmed
                var isCentered = false
                var isRight = false
                if (cleanLine.startsWith("[align:center]")) {
                    isCentered = true
                    cleanLine = cleanLine.removePrefix("[align:center]")
                } else if (cleanLine.startsWith("[align:right]")) {
                    isRight = true
                    cleanLine = cleanLine.removePrefix("[align:right]")
                } else if (cleanLine.startsWith("[align:left]")) {
                    cleanLine = cleanLine.removePrefix("[align:left]")
                } else if (cleanLine.startsWith("[align:justify]")) {
                    cleanLine = cleanLine.removePrefix("[align:justify]")
                }

                if (isCentered) {
                    sb.append("\\begin{center}\n").append(formatLatexInline(cleanLine)).append("\n\\end{center}\n\n")
                } else if (isRight) {
                    sb.append("\\begin{flushright}\n").append(formatLatexInline(cleanLine)).append("\n\\end{flushright}\n\n")
                } else {
                    sb.append(formatLatexInline(cleanLine)).append("\n\n")
                }
            }
        }

        closeListIfNeeded()
        if (inTable) {
            flushTable()
        }

        sb.append("\\end{document}\n")
        return sb.toString()
    }

    private fun generateLatexTable(rows: List<List<String>>): String {
        if (rows.isEmpty()) return ""
        val sb = StringBuilder()
        val numCols = rows.maxOfOrNull { it.size } ?: 1
        val colSpec = "l".repeat(numCols)

        sb.append("\\begin{center}\n")
        sb.append("\\begin{tabular}{").append(colSpec).append("}\n")
        sb.append("\\toprule\n")

        for ((rowIndex, row) in rows.withIndex()) {
            val formattedCells = (0 until numCols).map { colIndex ->
                val text = row.getOrNull(colIndex) ?: ""
                if (rowIndex == 0) {
                    "\\textbf{${formatLatexInline(text)}}"
                } else {
                    formatLatexInline(text)
                }
            }
            sb.append("  ").append(formattedCells.joinToString(" & ")).append(" \\\\\n")
            if (rowIndex == 0) {
                sb.append("\\midrule\n")
            }
        }

        sb.append("\\bottomrule\n")
        sb.append("\\end{tabular}\n")
        sb.append("\\end{center}\n\n")
        return sb.toString()
    }

    private fun formatLatexInline(text: String): String {
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

        var result = escapeLatex(clean)
        if (hasStrike) result = "\\sout{$result}"
        if (hasUnderline) result = "\\underline{$result}"
        if (hasItalic) result = "\\textit{$result}"
        if (hasBold) result = "\\textbf{$result}"

        return result
    }

    private fun escapeLatex(text: String): String {
        return text
            .replace("\\", "\\textbackslash{}")
            .replace("&", "\\&")
            .replace("%", "\\%")
            .replace("$", "\\$")
            .replace("#", "\\#")
            .replace("_", "\\_")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("~", "\\textasciitilde{}")
            .replace("^", "\\textasciicircum{}")
    }

    private fun sanitizeFileName(name: String): String {
        val sanitized = name.trim().replace(Regex("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ_\\-\\s]"), "")
        return if (sanitized.isBlank()) "Documento_DocuSheet" else sanitized.replace("\\s+".toRegex(), "_")
    }

    // ==========================================
    // Parser para Importación de LaTeX
    // ==========================================

    private fun parseLatexDocument(tex: String): Pair<String, String> {
        var detectedTitle = ""
        val titleMatch = Regex("\\\\title\\{(?:\\\\textbf\\{)?([^}]+)\\}?").find(tex)
        if (titleMatch != null) {
            detectedTitle = titleMatch.groupValues[1].replace("\\textbf{", "").replace("}", "").trim()
        }

        // Buscar inicio del cuerpo del documento
        var body = tex
        if (tex.contains("\\begin{document}")) {
            body = tex.substringAfter("\\begin{document}").substringBefore("\\end{document}")
        }

        // Quitar \maketitle
        body = body.replace("\\maketitle", "")

        val lines = body.lines()
        val out = StringBuilder()
        var inTabular = false
        val tabularRows = mutableListOf<List<String>>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("%")) continue // Comentarios LaTeX

            if (trimmed.contains("\\begin{tabular}")) {
                inTabular = true
                tabularRows.clear()
                continue
            }
            if (trimmed.contains("\\end{tabular}")) {
                inTabular = false
                if (tabularRows.isNotEmpty()) {
                    for ((idx, row) in tabularRows.withIndex()) {
                        out.append("| ").append(row.joinToString(" | ")).append(" |\n")
                        if (idx == 0) {
                            out.append("|").append(row.joinToString("|") { "---" }).append("|\n")
                        }
                    }
                    out.append("\n")
                }
                continue
            }

            if (inTabular) {
                if (trimmed.startsWith("\\toprule") || trimmed.startsWith("\\midrule") || trimmed.startsWith("\\bottomrule") || trimmed.startsWith("\\hline")) {
                    continue
                }
                val rowClean = trimmed.removeSuffix("\\\\").trim()
                val cells = rowClean.split("&").map { cleanLatexInline(it.trim()) }
                if (cells.isNotEmpty() && cells.any { it.isNotBlank() }) {
                    tabularRows.add(cells)
                }
                continue
            }

            if (trimmed == "\\newpage" || trimmed == "\\clearpage") {
                out.append("[--- Salto de Página ---]\n\n")
                continue
            }

            // Encabezados
            if (trimmed.startsWith("\\section{")) {
                val headerText = trimmed.substringAfter("\\section{").substringBefore("}")
                out.append("# ").append(cleanLatexInline(headerText)).append("\n\n")
                if (detectedTitle.isBlank()) detectedTitle = cleanLatexInline(headerText)
            } else if (trimmed.startsWith("\\subsection{")) {
                val headerText = trimmed.substringAfter("\\subsection{").substringBefore("}")
                out.append("## ").append(cleanLatexInline(headerText)).append("\n\n")
            } else if (trimmed.startsWith("\\subsubsection{")) {
                val headerText = trimmed.substringAfter("\\subsubsection{").substringBefore("}")
                out.append("### ").append(cleanLatexInline(headerText)).append("\n\n")
            } else if (trimmed.startsWith("\\item ")) {
                val itemText = trimmed.removePrefix("\\item ").trim()
                out.append("• ").append(cleanLatexInline(itemText)).append("\n")
            } else if (trimmed.startsWith("\\begin{quote}")) {
                // Inicio de cita
            } else if (trimmed.startsWith("\\end{quote}")) {
                out.append("\n")
            } else if (trimmed.startsWith("\\begin{") || trimmed.startsWith("\\end{")) {
                // Omitir comandos de entorno no mapeados
            } else if (trimmed.isEmpty()) {
                out.append("\n")
            } else {
                out.append(cleanLatexInline(trimmed)).append("\n\n")
            }
        }

        val resultText = out.toString().trim()
        if (detectedTitle.isBlank()) {
            detectedTitle = "Documento LaTeX Importado"
        }

        return Pair(detectedTitle, resultText)
    }

    private fun cleanLatexInline(text: String): String {
        return text
            .replace(Regex("\\\\textbf\\{([^}]+)\\}"), "**$1**")
            .replace(Regex("\\\\textit\\{([^}]+)\\}"), "*$1*")
            .replace(Regex("\\\\underline\\{([^}]+)\\}"), "<u>$1</u>")
            .replace(Regex("\\\\sout\\{([^}]+)\\}"), "~~$1~~")
            .replace(Regex("\\\\texttt\\{([^}]+)\\}"), "`$1`")
            .replace("\\&", "&")
            .replace("\\%", "%")
            .replace("\\$", "$")
            .replace("\\#", "#")
            .replace("\\_", "_")
            .replace("\\{", "{")
            .replace("\\}", "}")
            .replace("\\textbackslash{}", "\\")
            .replace("\\textasciitilde{}", "~")
            .replace("\\textasciicircum{}", "^")
    }
}
