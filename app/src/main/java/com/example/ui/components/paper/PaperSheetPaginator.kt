package com.example.ui.components.paper

/**
 * Representa una porción u hoja física dentro del flujo global del documento en Cascada.
 */
data class PageSlice(
    val index: Int,
    val text: String,
    val startOffset: Int,
    val endOffset: Int
)

/**
 * Motor de paginación física y desborde continuo de hojas de papel para DocuSheet.
 * 
 * Basado en la capacidad geométrica real de la hoja física (A4 estándar de 1.900 letras/caracteres
 * equivalentes), computando el impacto espacial de tablas, figuras geométricas, diagramas
 * de nodos y renglones físicos para garantizar paridad 1:1 con la exportación a PDF.
 */
object PaperSheetPaginator {

    /**
     * Capacidad estándar por defecto de letras/caracteres por hoja física A4.
     */
    const val DEFAULT_CHARS_PER_PAGE = 1900

    /**
     * Ancho promedio estimado de una línea de texto en caracteres en formato A4 con márgenes estándar.
     */
    private const val CHARS_PER_LINE = 70

    /**
     * Cuenta las palabras de un fragmento de texto de forma precisa.
     */
    fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    }

    /**
     * Cuenta las letras o caracteres de un fragmento de texto.
     */
    fun countChars(text: String): Int {
        return text.length
    }

    /**
     * Calcula la carga equivalente en letras/caracteres de un bloque de contenido,
     * ponderando la altura física que ocupan tablas, figuras, títulos y saltos de línea.
     */
    fun calculateEffectiveCharLoad(block: String): Int {
        val trimmed = block.trim()
        if (trimmed.isEmpty()) return 40 // Espacio de párrafo vacío

        // Tablas Markdown: cada fila equivale aproximadamente a 2 líneas de texto (~140 letras de espacio)
        if (trimmed.startsWith("|") && trimmed.contains("\n|")) {
            val rowCount = trimmed.lines().count { it.trim().startsWith("|") && !it.contains("---") }
            return (rowCount * 140).coerceAtLeast(trimmed.length)
        }

        // Figuras geométricas [shape:...]: ocupan aproximadamente 6 líneas de texto (~400 letras de espacio)
        if (trimmed.startsWith("[shape:") && trimmed.contains("[/shape]")) {
            return 400.coerceAtLeast(trimmed.length)
        }

        // Diagramas de nodos [nodes:...]: ocupan aproximadamente 8 líneas de texto (~500 letras de espacio)
        if (trimmed.startsWith("[nodes:") && trimmed.contains("[/nodes]")) {
            return 500.coerceAtLeast(trimmed.length)
        }

        // Títulos editoriales H1, H2, H3
        if (trimmed.startsWith("# ")) {
            return (trimmed.length + 140) // H1 grande con espaciado
        } else if (trimmed.startsWith("## ")) {
            return (trimmed.length + 100) // H2
        } else if (trimmed.startsWith("### ")) {
            return (trimmed.length + 70) // H3
        }

        // Para texto normal, calcular cuántas líneas visuales ocupa como mínimo
        val lines = block.lines()
        var totalEquivalent = 0
        for (line in lines) {
            val len = line.length
            // Cada salto de línea forzado ocupa al menos una línea completa (CHARS_PER_LINE)
            val lineEquivalent = if (len == 0) CHARS_PER_LINE / 2 else maxOf(len, CHARS_PER_LINE)
            totalEquivalent += lineEquivalent
        }

        return maxOf(totalEquivalent, block.length)
    }

    /**
     * Divide el texto en hojas lógicas para la vista en cascada respetando el límite dinámico
     * de letras/caracteres por hoja. Cuando el volumen de caracteres o elementos visuales
     * sobrepasa la capacidad de la hoja, desborda naturalmente a la siguiente hoja.
     */
    fun partitionIntoPages(content: String, charsPerPageLimit: Int = DEFAULT_CHARS_PER_PAGE): List<String> {
        if (content.isBlank()) return listOf("")
        val explicitPages = content.split(Regex("\\n\\[--- Salto de Página ---\\]\\n|\\n---\\n"))
        val result = mutableListOf<String>()
        val safeLimit = charsPerPageLimit.coerceAtLeast(300)

        for (part in explicitPages) {
            val totalEffectiveChars = calculateEffectiveCharLoad(part)
            if (totalEffectiveChars <= safeLimit) {
                result.add(part)
            } else {
                // Dividir inteligentemente por párrafos o bloques respetando el límite de letras/espacio de la hoja física
                val paragraphs = part.split("\n\n")
                var currentBuffer = StringBuilder()
                var currentLoad = 0

                for (p in paragraphs) {
                    val pLoad = calculateEffectiveCharLoad(p)
                    if (currentLoad + pLoad > safeLimit && currentBuffer.isNotEmpty()) {
                        result.add(currentBuffer.toString().trimEnd())
                        currentBuffer = StringBuilder()
                        currentLoad = 0
                    }

                    if (pLoad > safeLimit) {
                        // Si un único bloque o párrafo supera la capacidad de una hoja completa, dividirlo por líneas
                        val lines = p.split("\n")
                        for (line in lines) {
                            val lineLoad = maxOf(line.length, CHARS_PER_LINE)
                            if (currentLoad + lineLoad > safeLimit && currentBuffer.isNotEmpty()) {
                                result.add(currentBuffer.toString().trimEnd())
                                currentBuffer = StringBuilder()
                                currentLoad = 0
                            }
                            if (currentBuffer.isNotEmpty()) currentBuffer.append("\n")
                            currentBuffer.append(line)
                            currentLoad += lineLoad
                        }
                    } else {
                        if (currentBuffer.isNotEmpty()) currentBuffer.append("\n\n")
                        currentBuffer.append(p)
                        currentLoad += pLoad
                    }
                }
                if (currentBuffer.isNotEmpty()) {
                    result.add(currentBuffer.toString())
                }
            }
        }
        return if (result.isEmpty()) listOf("") else result
    }

    /**
     * Calcula los límites exactos de caracteres de cada hoja dentro del documento global,
     * asegurando la sincronización atómica entre las hojas físicas y el TextFieldValue de PC.
     */
    fun calculatePageSlices(content: String, charsPerPageLimit: Int = DEFAULT_CHARS_PER_PAGE): List<PageSlice> {
        if (content.isEmpty()) return listOf(PageSlice(0, "", 0, 0))
        val rawPages = partitionIntoPages(content, charsPerPageLimit)
        val slices = mutableListOf<PageSlice>()
        var searchStart = 0
        for ((index, pText) in rawPages.withIndex()) {
            val foundStart = if (pText.isNotEmpty()) {
                val idx = content.indexOf(pText, startIndex = searchStart)
                if (idx >= 0) idx else searchStart
            } else {
                searchStart
            }
            val foundEnd = (foundStart + pText.length).coerceAtMost(content.length)
            slices.add(PageSlice(index, pText, foundStart, foundEnd))
            searchStart = foundEnd
        }
        return if (slices.isEmpty()) listOf(PageSlice(0, "", 0, 0)) else slices
    }

    /**
     * Vuelve a unir las páginas en un único flujo de texto con saltos de página.
     */
    fun joinPages(pages: List<String>): String {
        return pages.joinToString(separator = "\n[--- Salto de Página ---]\n")
    }
}
