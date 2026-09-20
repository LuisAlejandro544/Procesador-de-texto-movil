package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AppDatabase: Base de datos Room principal de la aplicación.
 * 
 * Gestiona la tabla de documentos e inicializa un documento de bienvenida
 * para que el usuario pueda escribir y probar el procesador de texto desde el primer momento.
 */
@Database(entities = [DocumentEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "docusheet_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialDocument(database.documentDao())
                    }
                }
            }

            private suspend fun populateInitialDocument(dao: DocumentDao) {
                val initialDocument = DocumentEntity(
                    title = "Mi Primera Hoja",
                    content = """Bienvenido a DocuSheet, tu procesador de texto móvil diseñado para ofrecer la sensación auténtica de escribir en una hoja de papel de escritorio.

Aquí puedes redactar ensayos, notas personales, cartas o borradores con total comodidad:

• Hoja realista: Diseñada con márgenes de página, elevación tridimensional y marcas guía de esquina.
• Regla de medidas: Inspirada en los procesadores de texto de PC.
• Personalización completa: Elige entre papel Blanco, Marfil cálido, Cuadriculado, Rayado o Sepia.
• Contador en tiempo real: Monitorea tus palabras y caracteres mientras escribes.
• Guardado automático: Tu contenido se resguarda de forma segura en almacenamiento local.

¡Comienza a escribir directamente sobre esta hoja o pulsa el botón de opciones para ajustar el formato a tu gusto!""",
                    paperType = "WHITE",
                    fontStyle = "SERIF",
                    fontSize = 16,
                    lineSpacing = 1.5f,
                    marginStyle = "NORMAL",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                dao.insertDocument(initialDocument)
            }
        }
    }
}
