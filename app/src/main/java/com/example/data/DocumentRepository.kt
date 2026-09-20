package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * DocumentRepository: Capa de repositorio que abstrae el acceso a datos.
 * 
 * Garantiza que las operaciones de lectura y escritura se ejecuten de manera segura
 * en el hilo de fondo (Dispatchers.IO) utilizando Kotlin Coroutines, evitando cualquier
 * sobrecarga en el hilo principal de la interfaz gráfica.
 */
class DocumentRepository(private val documentDao: DocumentDao) {

    /**
     * Flujo reactivo con la lista de todos los documentos, optimizado en Dispatchers.IO.
     */
    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()
        .flowOn(Dispatchers.IO)

    /**
     * Obtiene el flujo de un documento específico por su ID.
     */
    fun getDocumentById(id: Long): Flow<DocumentEntity?> {
        return documentDao.getDocumentById(id).flowOn(Dispatchers.IO)
    }

    /**
     * Obtiene una sola vez el documento por su ID de manera suspendida.
     */
    suspend fun getDocumentByIdOnce(id: Long): DocumentEntity? {
        return withContext(Dispatchers.IO) {
            documentDao.getDocumentByIdOnce(id)
        }
    }

    /**
     * Inserta un nuevo documento en la base de datos.
     */
    suspend fun insertDocument(document: DocumentEntity): Long {
        return withContext(Dispatchers.IO) {
            documentDao.insertDocument(document)
        }
    }

    /**
     * Actualiza los datos de un documento existente.
     */
    suspend fun updateDocument(document: DocumentEntity) {
        withContext(Dispatchers.IO) {
            documentDao.updateDocument(document)
        }
    }

    /**
     * Elimina un documento específico.
     */
    suspend fun deleteDocument(document: DocumentEntity) {
        withContext(Dispatchers.IO) {
            documentDao.deleteDocument(document)
        }
    }

    /**
     * Elimina un documento por su ID numérico.
     */
    suspend fun deleteDocumentById(id: Long) {
        withContext(Dispatchers.IO) {
            documentDao.deleteDocumentById(id)
        }
    }
}
