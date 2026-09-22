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
 */
object PaperSheetPaginator {

    /**
     * Cuenta las palabras de un fragmento de texto de forma precisa.
     */
    fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    }

    /**
     * Divide el texto en hojas lógicas para la vista en cascada respetando el límite dinámico de palabras por hoja.
     * Cuando el número de palabras sobrepasa la capacidad configurada de la hoja, el texto sobrante
     * desborda naturalmente y se apila en una segunda, tercera o sucesivas hojas continuas.
     */
    fun partitionIntoPages(content: String, wordsPerPageLimit: Int = 350): List<String> {
        if (content.isBlank()) return listOf("")
        val explicitPages = content.split(Regex("\\n\\[--- Salto de Página ---\\]\\n|\\n---\\n"))
        val result = mutableListOf<String>()
        val safeLimit = wordsPerPageLimit.coerceAtLeast(50)

        for (part in explicitPages) {
            val totalWords = countWords(part)
            if (totalWords <= safeLimit) {
                result.add(part)
            } else {
                // Dividir inteligentemente por párrafos respetando el límite de palabras de la hoja física
                val paragraphs = part.split("\n\n")
                var currentBuffer = StringBuilder()
                var currentWordCount = 0

                for (p in paragraphs) {
                    val pWords = countWords(p)
                    if (currentWordCount + pWords > safeLimit && currentBuffer.isNotEmpty()) {
                        result.add(currentBuffer.toString().trimEnd())
                        currentBuffer = StringBuilder()
                        currentWordCount = 0
                    }

                    if (pWords > safeLimit) {
                        // Si un único párrafo extenso supera el límite de palabras de una sola hoja, dividirlo por líneas
                        val lines = p.split("\n")
                        for (line in lines) {
                            val lineWords = countWords(line)
                            if (currentWordCount + lineWords > safeLimit && currentBuffer.isNotEmpty()) {
                                result.add(currentBuffer.toString().trimEnd())
                                currentBuffer = StringBuilder()
                                currentWordCount = 0
                            }
                            if (currentBuffer.isNotEmpty()) currentBuffer.append("\n")
                            currentBuffer.append(line)
                            currentWordCount += lineWords
                        }
                    } else {
                        if (currentBuffer.isNotEmpty()) currentBuffer.append("\n\n")
                        currentBuffer.append(p)
                        currentWordCount += pWords
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
    fun calculatePageSlices(content: String, wordsPerPageLimit: Int = 350): List<PageSlice> {
        if (content.isEmpty()) return listOf(PageSlice(0, "", 0, 0))
        val rawPages = partitionIntoPages(content, wordsPerPageLimit)
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
