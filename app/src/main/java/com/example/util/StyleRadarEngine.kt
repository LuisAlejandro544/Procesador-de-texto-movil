package com.example.util

import org.apache.lucene.analysis.es.SpanishAnalyzer
import org.apache.lucene.analysis.es.SpanishLightStemmer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute
import java.io.StringReader

/**
 * Nivel de severidad de proximidad para una repetición detectada en el texto.
 */
enum class ProximityLevel {
    NORMAL,     // Más de 120 palabras de distancia: distribución aceptable
    MODERATE,   // Entre 40 y 120 palabras: redundancia en el mismo párrafo
    CRITICAL    // Menos de 40 palabras: repetición inmediata en la misma frase o línea
}

/**
 * Representa una coincidencia individual localizada en el documento.
 *
 * @property index Índice secuencial de la coincidencia (base 0).
 * @property startOffset Posición de inicio en el texto completo.
 * @property endOffset Posición de fin en el texto completo.
 * @property pageIndex Índice de la hoja o página donde se encuentra.
 * @property matchedWord Palabra textual coincidente.
 * @property snippet Fragmento de contexto circundante.
 * @property wordIndex Posición ordinal de la palabra dentro del documento.
 * @property distanceToPrevious Distancia en palabras respecto a la coincidencia anterior.
 * @property proximityLevel Nivel de alerta de proximidad.
 */
data class SearchMatch(
    val index: Int,
    val startOffset: Int,
    val endOffset: Int,
    val pageIndex: Int,
    val matchedWord: String,
    val snippet: String,
    val wordIndex: Int,
    val distanceToPrevious: Int = -1,
    val proximityLevel: ProximityLevel = ProximityLevel.NORMAL
)

/**
 * Reporte integral generado por el Radar de Estilo y Detector de Redundancia.
 *
 * @property query Palabra buscada o analizada.
 * @property stem Raíz lematizada obtenida mediante Apache Lucene.
 * @property totalMatches Total de apariciones en el documento.
 * @property criticalCount Cantidad de repeticiones en proximidad crítica (< 40 palabras).
 * @property moderateCount Cantidad de repeticiones en proximidad moderada (40 - 120 palabras).
 * @property minWordDistance Distancia mínima encontrada entre dos apariciones.
 * @property diagnosticMessage Diagnóstico en lenguaje natural para el redactor.
 * @property suggestedAction Recomendación práctica de corrección de estilo.
 */
data class StyleRadarReport(
    val query: String,
    val stem: String,
    val totalMatches: Int,
    val criticalCount: Int,
    val moderateCount: Int,
    val minWordDistance: Int,
    val diagnosticMessage: String,
    val suggestedAction: String
)

/**
 * Token procesado por Apache Lucene durante la inspección del documento.
 */
data class LuceneToken(
    val term: String,
    val stem: String,
    val startOffset: Int,
    val endOffset: Int,
    val wordIndex: Int
)

/**
 * StyleRadarEngine: Motor de análisis lingüístico avanzado, proximidad y detección de redundancia.
 *
 * Utiliza Apache Lucene (SpanishAnalyzer y SpanishLightStemmer) para tokenización precisa,
 * descarte de palabras vacías (stopwords) en español y cálculo de distancias reales entre términos.
 */
class StyleRadarEngine {

    private val spanishAnalyzer = SpanishAnalyzer()
    private val spanishStemmer = SpanishLightStemmer()

    /**
     * Calcula la raíz lematizada (stem) de una palabra en español usando Lucene.
     */
    fun stemWord(word: String): String {
        val clean = word.trim().lowercase()
        if (clean.isEmpty()) return ""
        val chars = clean.toCharArray()
        val len = spanishStemmer.stem(chars, chars.size)
        return String(chars, 0, len)
    }

    /**
     * Tokeniza el texto completo utilizando Apache Lucene, conservando offsets de caracteres
     * y posiciones de palabras.
     */
    fun tokenizeText(text: String): List<LuceneToken> {
        val tokens = mutableListOf<LuceneToken>()
        if (text.isBlank()) return tokens

        try {
            spanishAnalyzer.tokenStream("content", StringReader(text)).use { tokenStream ->
                val termAttr = tokenStream.addAttribute(CharTermAttribute::class.java)
                val offsetAttr = tokenStream.addAttribute(OffsetAttribute::class.java)

                tokenStream.reset()
                var wordOrdinal = 0
                while (tokenStream.incrementToken()) {
                    val term = termAttr.toString()
                    val start = offsetAttr.startOffset()
                    val end = offsetAttr.endOffset()
                    val stem = stemWord(term)

                    tokens.add(
                        LuceneToken(
                            term = term,
                            stem = stem,
                            startOffset = start,
                            endOffset = end,
                            wordIndex = wordOrdinal
                        )
                    )
                    wordOrdinal++
                }
                tokenStream.end()
            }
        } catch (e: Exception) {
            // Fallback robusto en caso de cadenas irregulares
            return tokenizeFallback(text)
        }
        return tokens
    }

    /**
     * Tokenizador alternativo de respaldo si ocurre una anomalía de I/O en memoria.
     */
    private fun tokenizeFallback(text: String): List<LuceneToken> {
        val tokens = mutableListOf<LuceneToken>()
        val regex = Regex("\\b[\\p{L}\\p{Nd}_]+\\b")
        var ordinal = 0
        for (match in regex.findAll(text)) {
            val term = match.value.lowercase()
            tokens.add(
                LuceneToken(
                    term = term,
                    stem = stemWord(term),
                    startOffset = match.range.first,
                    endOffset = match.range.last + 1,
                    wordIndex = ordinal++
                )
            )
        }
        return tokens
    }

    /**
     * Localiza todas las coincidencias de una búsqueda (exacta o por raíz),
     * calcula distancias de proximidad y genera el reporte de radar de estilo.
     *
     * @param fullText Texto íntegro del documento.
     * @param query Término que el usuario está buscando.
     * @param wordsPerPage Promedio de palabras por hoja para calcular la página estimada.
     */
    fun analyzeSearch(
        fullText: String,
        query: String,
        wordsPerPage: Int = 350
    ): Pair<List<SearchMatch>, StyleRadarReport?> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty() || fullText.isBlank()) {
            return Pair(emptyList(), null)
        }

        val queryStem = stemWord(cleanQuery)
        val tokens = tokenizeText(fullText)

        // Encuentra coincidencias exactas o por lema
        val matchingTokens = tokens.filter { token ->
            token.term.equals(cleanQuery, ignoreCase = true) ||
                    (cleanQuery.length >= 4 && token.stem == queryStem)
        }

        if (matchingTokens.isEmpty()) {
            // Intentar búsqueda literal simple por subcadena si el tokenizador descartó la palabra (por ejemplo, stopwords)
            val literalMatches = findLiteralMatches(fullText, cleanQuery, wordsPerPage)
            return Pair(literalMatches, null)
        }

        val matches = mutableListOf<SearchMatch>()
        var criticalCount = 0
        var moderateCount = 0
        var minDistance = Int.MAX_VALUE

        for (i in matchingTokens.indices) {
            val current = matchingTokens[i]
            val distance = if (i > 0) {
                current.wordIndex - matchingTokens[i - 1].wordIndex
            } else {
                -1
            }

            val proximityLevel = when {
                distance in 0..39 -> {
                    criticalCount++
                    if (distance < minDistance) minDistance = distance
                    ProximityLevel.CRITICAL
                }
                distance in 40..120 -> {
                    moderateCount++
                    if (distance < minDistance) minDistance = distance
                    ProximityLevel.MODERATE
                }
                else -> ProximityLevel.NORMAL
            }

            // Fragmento de contexto (snippet)
            val snippetStart = (current.startOffset - 25).coerceAtLeast(0)
            val snippetEnd = (current.endOffset + 25).coerceAtMost(fullText.length)
            val rawSnippet = fullText.substring(snippetStart, snippetEnd).replace("\n", " ").trim()
            val snippet = "...$rawSnippet..."

            val pageIndex = (current.wordIndex / wordsPerPage.coerceAtLeast(100)).coerceAtLeast(0)

            matches.add(
                SearchMatch(
                    index = i,
                    startOffset = current.startOffset,
                    endOffset = current.endOffset,
                    pageIndex = pageIndex,
                    matchedWord = fullText.substring(current.startOffset, current.endOffset),
                    snippet = snippet,
                    wordIndex = current.wordIndex,
                    distanceToPrevious = distance,
                    proximityLevel = proximityLevel
                )
            )
        }

        val total = matches.size
        val effectiveMinDist = if (minDistance == Int.MAX_VALUE) 0 else minDistance

        val diagnosticMessage = when {
            criticalCount > 0 -> "⚠️ Alta redundancia: Se repite $criticalCount veces a menos de 40 palabras."
            moderateCount > 0 -> "ℹ️ Proximidad moderada: $moderateCount repeticiones en el mismo párrafo."
            total > 1 -> "✓ Densidad equilibrada: $total apariciones distribuidas con buen espacio."
            else -> "✓ Aparición única en el documento."
        }

        val suggestedAction = when {
            criticalCount > 0 -> "Reemplaza las ocurrencias cercanas usando el carrusel de sinónimos inferior."
            moderateCount > 0 -> "Revisa si puedes alternar con un sinónimo para dar mayor variedad léxica."
            total > 8 -> "Palabra recurrente. Verifica si es un término temático o un vicio de estilo."
            else -> "Uso adecuado del término."
        }

        val report = StyleRadarReport(
            query = cleanQuery,
            stem = queryStem,
            totalMatches = total,
            criticalCount = criticalCount,
            moderateCount = moderateCount,
            minWordDistance = effectiveMinDist,
            diagnosticMessage = diagnosticMessage,
            suggestedAction = suggestedAction
        )

        return Pair(matches, report)
    }

    /**
     * Búsqueda literal por subcadena para casos donde no intervenga el stemmer de Lucene.
     */
    private fun findLiteralMatches(fullText: String, query: String, wordsPerPage: Int): List<SearchMatch> {
        val matches = mutableListOf<SearchMatch>()
        var startIndex = 0
        var matchIndex = 0
        while (startIndex < fullText.length) {
            val found = fullText.indexOf(query, startIndex, ignoreCase = true)
            if (found == -1) break

            val end = found + query.length
            val snippetStart = (found - 20).coerceAtLeast(0)
            val snippetEnd = (end + 20).coerceAtMost(fullText.length)
            val snippet = "...${fullText.substring(snippetStart, snippetEnd).replace("\n", " ").trim()}..."

            // Estimación de índice de palabra por conteo de espacios antes del offset
            val wordsBefore = fullText.substring(0, found).split(Regex("\\s+")).size
            val pageIndex = (wordsBefore / wordsPerPage.coerceAtLeast(100)).coerceAtLeast(0)

            matches.add(
                SearchMatch(
                    index = matchIndex++,
                    startOffset = found,
                    endOffset = end,
                    pageIndex = pageIndex,
                    matchedWord = fullText.substring(found, end),
                    snippet = snippet,
                    wordIndex = wordsBefore,
                    distanceToPrevious = -1,
                    proximityLevel = ProximityLevel.NORMAL
                )
            )
            startIndex = end
        }
        return matches
    }
}
