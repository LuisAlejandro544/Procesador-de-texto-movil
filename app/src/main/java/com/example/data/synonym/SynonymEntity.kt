package com.example.data.synonym

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * SynonymEntity: Entidad de persistencia local para el Diccionario de Sinónimos en Español (100% Offline).
 *
 * Cada registro almacena un lema o palabra normalizada (`word`), su raíz léxica (`stem`),
 * la lista de sinónimos separados por coma (`synonyms`) y su contexto o categoría gramatical (`category`).
 * Está completamente indexada para búsquedas ultra-rápidas en memoria local sin conexión a internet.
 *
 * @property id Identificador autoincremental de la entrada.
 * @property word Palabra o lema en minúsculas (ej: "importante", "hacer", "rápido").
 * @property stem Raíz lematizada obtenida mediante Apache Lucene SpanishLightStemmer (ej: "import", "hac", "rapid").
 * @property synonyms Lista de sinónimos directos y de alta afinidad separados por comas.
 * @property category Categoría gramatical o contexto (ej: "adjetivo", "verbo", "sustantivo").
 */
@Entity(
    tableName = "thesaurus_synonyms",
    indices = [
        Index(value = ["word"], unique = true),
        Index(value = ["stem"])
    ]
)
data class SynonymEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val word: String,
    val stem: String,
    val synonyms: String,
    val category: String = "general"
) {
    /**
     * Devuelve los sinónimos como una lista ordenada de cadenas limpias.
     */
    fun getSynonymsList(): List<String> {
        return synonyms.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
