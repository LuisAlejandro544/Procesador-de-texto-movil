package com.example.data.macro

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * MacroEntity: Modela una macro o plantilla automatizada en DocuSheet.
 * 
 * Permite a los usuarios insertar bloques estructurados de texto con variables dinámicas
 * calculadas al vuelo ({FECHA}, {HORA}, {TITULO}, {AUTOR}, {FORMATO_HOJA}, etc.).
 * 
 * Diseñado para interacción táctil en smartphones sin teclado físico, acelerando
 * la redacción de correspondencia, actas, contratos, tablas y citas con un solo toque.
 */
@Entity(tableName = "macros")
data class MacroEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val triggerKeyword: String = "",
    val category: String = "GENERAL",
    val templateContent: String,
    val isPredefined: Boolean = false,
    val iconName: String = "description"
)
