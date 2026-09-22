package com.example.ui.delegates

/**
 * Calculador de estadísticas y métricas cuantitativas del documento.
 * 
 * Provee cómputos puros para:
 * - Recuento exacto de palabras y caracteres
 * - Estimación de páginas editoriales según densidad de texto
 * - Conteo de párrafos y oraciones
 * - Tiempo estimado de lectura silenciosa (200 ppm) y oratoria (130 ppm)
 */
object DocumentStatsCalculator {

    /**
     * Calcula estadísticas de palabras del texto omitiendo espacios vacíos.
     */
    fun getWordCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    }

    /**
     * Calcula estadísticas de caracteres totales del texto.
     */
    fun getCharCount(text: String): Int {
        return text.length
    }

    /**
     * Calcula la cantidad de páginas estimadas basadas en la partición geométrica de letras y bloques de hoja.
     */
    fun getEstimatedPages(text: String, charsPerPage: Int = 1900): Int {
        if (text.isBlank()) return 1
        return com.example.ui.components.paper.PaperSheetPaginator.partitionIntoPages(text, charsPerPage).size
    }

    /**
     * Calcula la cantidad de párrafos del documento.
     */
    fun getParagraphCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.split("\n\n", "\n").count { it.trim().isNotEmpty() }
    }

    /**
     * Calcula la cantidad de oraciones aproximadas basadas en delimitadores de puntuación.
     */
    fun getSentenceCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.split(Regex("[.!?]+\\s*")).count { it.trim().isNotEmpty() }
    }

    /**
     * Calcula el tiempo estimado de lectura en minutos (a 200 palabras por minuto).
     */
    fun getReadingTimeMinutes(text: String): Int {
        val words = getWordCount(text)
        return maxOf(1, (words + 199) / 200)
    }

    /**
     * Calcula el tiempo estimado de discurso oral en minutos (a 130 palabras por minuto).
     */
    fun getSpeakingTimeMinutes(text: String): Int {
        val words = getWordCount(text)
        return maxOf(1, (words + 129) / 130)
    }
}
