package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DocumentDao: Interfaz de acceso a datos para operaciones SQLite mediante Room.
 * 
 * Utiliza Kotlin Coroutines y Flow para emitir cambios en tiempo real
 * hacia la interfaz de usuario sin bloquear el hilo principal de la aplicación.
 */
@Dao
interface DocumentDao {

    /**
     * Obtiene el listado completo de documentos ordenados por fecha de última modificación descendente.
     */
    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    /**
     * Obtiene el flujo reactivo de un documento específico por su ID.
     */
    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    fun getDocumentById(id: Long): Flow<DocumentEntity?>

    /**
     * Obtiene de forma única (one-shot) un documento por su ID.
     */
    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentByIdOnce(id: Long): DocumentEntity?

    /**
     * Inserta un nuevo documento y retorna el ID autogenerado.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    /**
     * Actualiza un documento existente.
     */
    @Update
    suspend fun updateDocument(document: DocumentEntity)

    /**
     * Elimina un documento específico.
     */
    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    /**
     * Elimina un documento por su ID.
     */
    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)
}
