package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.macro.MacroDao
import com.example.data.macro.MacroEntity
import com.example.data.macro.MacroSeedData
import com.example.data.synonym.SynonymDao
import com.example.data.synonym.SynonymEntity
import com.example.data.synonym.ThesaurusSeedData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AppDatabase: Base de datos Room principal de la aplicación.
 * 
 * Gestiona la tabla de documentos, el diccionario local de sinónimos offline (Thesaurus),
 * y el catálogo de macros y plantillas automatizadas con variables dinámicas.
 * Inicializa un documento de bienvenida para que el usuario pueda escribir y probar
 * el procesador de texto desde el primer momento.
 */
@Database(
    entities = [DocumentEntity::class, SynonymEntity::class, MacroEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao
    abstract fun synonymDao(): SynonymDao
    abstract fun macroDao(): MacroDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE documents ADD COLUMN pageSize TEXT NOT NULL DEFAULT 'A4'")
                database.execSQL("ALTER TABLE documents ADD COLUMN wordsPerPage INTEGER NOT NULL DEFAULT 350")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS macros (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        triggerKeyword TEXT NOT NULL,
                        category TEXT NOT NULL,
                        templateContent TEXT NOT NULL,
                        isPredefined INTEGER NOT NULL,
                        iconName TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "docusheet_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_4_5)
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
                        populateInitialThesaurus(database.synonymDao())
                        populateInitialMacros(database.macroDao())
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        // Verifica que el tesauro esté poblado si la base ya existía
                        if (database.synonymDao().countEntries() == 0) {
                            populateInitialThesaurus(database.synonymDao())
                        }
                        // Verifica que las macros iniciales estén pobladas
                        if (database.macroDao().countMacros() == 0) {
                            populateInitialMacros(database.macroDao())
                        }
                    }
                }
            }

            private suspend fun populateInitialThesaurus(dao: SynonymDao) {
                dao.insertAll(ThesaurusSeedData.INITIAL_ENTRIES)
            }

            private suspend fun populateInitialMacros(dao: MacroDao) {
                dao.insertAll(MacroSeedData.INITIAL_MACROS)
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
