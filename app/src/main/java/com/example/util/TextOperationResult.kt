package com.example.util

/**
 * TextOperationResult: Representa el resultado de una operación de bajo nivel
 * de movimiento, transposición o intercambio de texto ejecutada por los motores
 * nativos en C++20 y Rust de DocuSheet.
 *
 * @property newText El texto completo del documento tras la operación de permutación.
 * @property newSelectionStart Índice inicial recomendado para el cursor o selección activa.
 * @property newSelectionEnd Índice final recomendado para el cursor o selección activa.
 * @property success Verdadero si la operación se completó exitosamente sin colisiones.
 * @property message Mensaje informativo o descripción de la acción realizada.
 */
data class TextOperationResult(
    val newText: String,
    val newSelectionStart: Int,
    val newSelectionEnd: Int,
    val success: Boolean,
    val message: String = ""
)
