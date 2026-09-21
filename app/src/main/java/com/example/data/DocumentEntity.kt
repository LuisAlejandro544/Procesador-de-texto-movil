package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * DocumentEntity: Representa un documento dentro del procesador de texto.
 * 
 * Almacena el contenido textual así como las propiedades visuales y de formato
 * de la hoja de papel (tipo de papel, tipografía, tamaño de letra, espaciado entre líneas y márgenes).
 */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val paperType: String = "WHITE",      // WHITE, IVORY, LINED, GRID, SEPIA, DARK
    val fontStyle: String = "SERIF",      // SERIF, SANS_SERIF, MONOSPACE
    val fontSize: Int = 16,               // Tamaño en SP (14, 16, 18, 20, 24)
    val lineSpacing: Float = 1.5f,        // Multiplicador de interlineado (1.2f, 1.5f, 2.0f)
    val marginStyle: String = "NORMAL",   // NARROW, NORMAL, WIDE
    val alignment: String = "LEFT",       // LEFT, CENTER, RIGHT, JUSTIFY (Alineación Cuádruple con Justificado Real)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
