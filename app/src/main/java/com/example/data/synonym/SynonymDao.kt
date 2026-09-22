package com.example.data.synonym

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * SynonymDao: Objeto de Acceso a Datos (DAO) para el Diccionario de Sinónimos Offline.
 *
 * Proporciona métodos optimizados para consultar sinónimos exactos o por raíz lematizada,
 * permitiendo sustituciones instantáneas en el editor de texto sin demoras perceptibles.
 */
@Dao
interface SynonymDao {

    /**
     * Busca la entrada exacta de sinónimos para una palabra dada.
     */
    @Query("SELECT * FROM thesaurus_synonyms WHERE LOWER(word) = LOWER(:word) LIMIT 1")
    suspend fun getSynonymsByWord(word: String): SynonymEntity?

    /**
     * Busca coincidencias alternativas que compartan la misma raíz lematizada (stem).
     */
    @Query("SELECT * FROM thesaurus_synonyms WHERE stem = :stem LIMIT 3")
    suspend fun getSynonymsByStem(stem: String): List<SynonymEntity>

    /**
     * Cuenta cuántas entradas existen en la base de datos local para verificar si requiere precarga.
     */
    @Query("SELECT COUNT(*) FROM thesaurus_synonyms")
    suspend fun countEntries(): Int

    /**
     * Inserta masivamente el lote inicial del tesauro local offline.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(synonyms: List<SynonymEntity>)
}
