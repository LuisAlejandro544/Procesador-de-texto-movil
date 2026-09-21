package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
@Database(entities = [DocumentEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE documents ADD COLUMN pageSize TEXT NOT NULL DEFAULT 'A4'")
                database.execSQL("ALTER TABLE documents ADD COLUMN wordsPerPage INTEGER NOT NULL DEFAULT 350")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "docusheet_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
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
                    content = """Bienvenido a DocuSheet, tu procesador de texto editorial para móvil con la potencia de un procesador de escritorio.

# TÍTULO EDITORIAL EN DOCUSHEET

Aquí puedes redactar cartas, ensayos, contratos y borradores con herramientas profesionales de PC adaptadas para tu teléfono:

• Justificado Real de Párrafos: El texto se distribuye de margen a margen con armonía tipográfica.
• Formato Enriquecido: Ahora cuentas con **negrita**, *cursiva*, <u>subrayado formal</u> y ~~texto tachado~~.
• Fórmulas y Citas: Notaciones con subíndices (H<sub>2</sub>O) y superíndices (X<sup>2</sup> o 1<sup>er</sup> puesto).
• Tipografías Dinámicas: Puedes alternar entre [font:serif]Serif clásica[/font], [font:sans]Sans moderna[/font], [font:mono]Monospace de máquina[/font] o [font:cursive]Caligrafía[/font] en cualquier momento.
• Inserción de Imágenes: Con ajuste de hoja (Layout & Wrap) centrado o a ancho completo.

> "La escritura precisa sobre papel físico ahora cabe en la palma de tu mano."

¡Comienza a escribir sobre la hoja o usa la barra de herramientas superior para explorar todos los formatos!""",
                    paperType = "WHITE",
                    fontStyle = "SERIF",
                    fontSize = 16,
                    lineSpacing = 1.5f,
                    marginStyle = "NORMAL",
                    alignment = "JUSTIFY",
                    pageSize = "A4",
                    wordsPerPage = 350,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                dao.insertDocument(initialDocument)
            }
        }
    }
}
