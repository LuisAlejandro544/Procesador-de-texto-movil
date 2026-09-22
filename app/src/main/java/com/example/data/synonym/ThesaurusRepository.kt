package com.example.data.synonym

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ThesaurusRepository: Repositorio para el Diccionario de Sinónimos Local y Offline.
 *
 * Se encarga de:
 * 1. Inicializar y precargar el catálogo offline en Room si la tabla está vacía.
 * 2. Buscar sinónimos exactos o por similitud de raíz lematizada.
 * 3. Ejecutar todas las operaciones en el hilo de fondo (Dispatchers.IO) para no bloquear la UI.
 */
class ThesaurusRepository(
    private val synonymDao: SynonymDao
) {

    /**
     * Asegura que la base de datos de sinónimos contenga el catálogo precompilado.
     */
    suspend fun ensureInitialized() = withContext(Dispatchers.IO) {
        val count = synonymDao.countEntries()
        if (count == 0) {
            synonymDao.insertAll(ThesaurusSeedData.INITIAL_ENTRIES)
        }
    }

    /**
     * Obtiene una lista de sinónimos para una palabra dada.
     * Si no encuentra una coincidencia exacta, busca por la raíz lematizada.
     *
     * @param word Palabra a consultar (ej: "importante", "problema").
     * @param stem Raíz lematizada calculada por el motor de Lucene (opcional).
     * @return Lista limpia de sinónimos ordenados.
     */
    suspend fun getSynonyms(word: String, stem: String? = null): List<String> = withContext(Dispatchers.IO) {
        val cleanWord = word.trim().lowercase()
        if (cleanWord.isEmpty()) return@withContext emptyList()

        // 1. Búsqueda exacta
        val exactMatch = synonymDao.getSynonymsByWord(cleanWord)
        if (exactMatch != null) {
            return@withContext exactMatch.getSynonymsList()
        }

        // 2. Búsqueda por raíz (stem) si está disponible
        if (!stem.isNullOrBlank()) {
            val stemMatches = synonymDao.getSynonymsByStem(stem)
            val combined = stemMatches.flatMap { it.getSynonymsList() }.distinct()
            if (combined.isNotEmpty()) {
                return@withContext combined
            }
        }

        // 3. Fallback de búsqueda directa en el catálogo estático en caso de que aún no se haya volcado a disco
        val fallback = ThesaurusSeedData.INITIAL_ENTRIES.firstOrNull {
            it.word.equals(cleanWord, ignoreCase = true) || (stem != null && it.stem.equals(stem, ignoreCase = true))
        }
        return@withContext fallback?.getSynonymsList() ?: emptyList()
    }
}
