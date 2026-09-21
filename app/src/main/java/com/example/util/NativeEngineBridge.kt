package com.example.util

import android.util.Log

/**
 * NativeEngineBridge: Puente de enlace entre la capa de Kotlin/Compose y los núcleos
 * de alto rendimiento en C++20 y Rust.
 * 
 * Funcionalidad:
 * - Carga de forma segura y tolerante a fallos la biblioteca compartida nativa 'docusheet_core'.
 * - Expone los métodos nativos (JNI) para la medición tipográfica y paginación rápida.
 * - Proporciona estado informativo sobre si el motor nativo se encuentra enlazado.
 */
object NativeEngineBridge {
    private const val TAG = "NativeEngineBridge"
    private var isNativeLoaded = false
    private var isRustLoaded = false

    init {
        // 1. Cargar biblioteca de alto rendimiento en Rust (docusheet_rust)
        try {
            System.loadLibrary("docusheet_rust")
            isRustLoaded = true
            Log.i(TAG, "Biblioteca nativa 'docusheet_rust' cargada exitosamente.")
        } catch (e: Throwable) {
            isRustLoaded = false
            Log.w(TAG, "Biblioteca de Rust en preparación o enlazada estáticamente: ${e.message}")
        }

        // 2. Cargar biblioteca central en C++20 (docusheet_core)
        try {
            System.loadLibrary("docusheet_core")
            isNativeLoaded = true
            Log.i(TAG, "Biblioteca nativa 'docusheet_core' (C++20) cargada exitosamente.")
        } catch (e: Throwable) {
            isNativeLoaded = false
            Log.w(TAG, "Motor nativo en preparación: ${e.message}")
        }
    }

    /**
     * Retorna verdadero si la biblioteca compilada en C++20 está activa en memoria.
     */
    fun isAvailable(): Boolean = isNativeLoaded

    /**
     * Retorna verdadero si la biblioteca compilada en Rust está activa en memoria.
     */
    fun isRustAvailable(): Boolean = isRustLoaded

    /**
     * Versión reportada por el núcleo en C++20.
     */
    external fun getNativeEngineVersion(): String

    /**
     * Versión reportada por el núcleo en Rust.
     */
    external fun getRustEngineVersion(): String

    /**
     * Retorna la versión del motor Rust de forma segura con fallback.
     */
    fun getRustEngineVersionSafe(): String {
        return if (isNativeLoaded) {
            try {
                getRustEngineVersion()
            } catch (e: UnsatisfiedLinkError) {
                "DocuSheet Rust Core (Fallback Mode)"
            }
        } else {
            "DocuSheet Rust Core (Offline)"
        }
    }

    /**
     * Calcula la distribución de espaciado inter-palabras para justificado real en C++20.
     */
    external fun computeJustifiedSpacing(lineText: String, targetWidthPt: Float): FloatArray

    /**
     * Calcula la distribución métrica y anchos de columnas para Tablas y Cuadrículas
     * Editoriales de acuerdo al ancho imprimible de la hoja física.
     */
    external fun computeTableColumnWidths(numColumns: Int, printableWidthPt: Float): FloatArray

    /**
     * Método seguro para el cálculo de anchos de columna de tablas con fallback en Kotlin.
     */
    fun computeTableColumnWidthsSafe(numColumns: Int, printableWidthPt: Float): FloatArray {
        if (numColumns <= 0 || printableWidthPt <= 0f) return FloatArray(0)
        return if (isNativeLoaded) {
            try {
                val widths = computeTableColumnWidths(numColumns, printableWidthPt)
                if (widths.isNotEmpty()) widths else fallbackTableWidths(numColumns, printableWidthPt)
            } catch (e: UnsatisfiedLinkError) {
                fallbackTableWidths(numColumns, printableWidthPt)
            }
        } else {
            fallbackTableWidths(numColumns, printableWidthPt)
        }
    }

    private fun fallbackTableWidths(numColumns: Int, printableWidthPt: Float): FloatArray {
        val colWidth = printableWidthPt / numColumns.toFloat()
        return FloatArray(numColumns) { colWidth }
    }

    /**
     * Método seguro con fallback si la biblioteca nativa aún no está enlazada.
     */
    fun computeSpacingSafe(lineText: String, targetWidthPt: Float): FloatArray {
        return if (isNativeLoaded) {
            try {
                computeJustifiedSpacing(lineText, targetWidthPt)
            } catch (e: UnsatisfiedLinkError) {
                FloatArray(0)
            }
        } else {
            FloatArray(0)
        }
    }

    // ==========================================================================
    // Motor de Transposición e Intercambio de Texto (PC Text Engine en C++20)
    // ==========================================================================

    /**
     * Intercambia dos rangos de texto en el documento utilizando el motor nativo C++20.
     */
    external fun swapTextRangesNative(
        fullText: String,
        startA: Int,
        endA: Int,
        startB: Int,
        endB: Int
    ): TextOperationResult?

    /**
     * Mueve un rango de texto a una posición de destino dentro del documento con C++20.
     */
    external fun moveTextRangeNative(
        fullText: String,
        start: Int,
        end: Int,
        targetPosition: Int
    ): TextOperationResult?

    /**
     * Intercambia el párrafo actual con el anterior (swapUp=true) o posterior (swapUp=false).
     */
    external fun swapParagraphNative(
        fullText: String,
        cursorStart: Int,
        cursorEnd: Int,
        swapUp: Boolean
    ): TextOperationResult?

    /**
     * Intercambia dos rangos de texto con ejecución en C++20 y respaldo tolerante a fallos.
     */
    fun swapTextRangesSafe(
        fullText: String,
        startA: Int,
        endA: Int,
        startB: Int,
        endB: Int
    ): TextOperationResult {
        if (isNativeLoaded) {
            try {
                val res = swapTextRangesNative(fullText, startA, endA, startB, endB)
                if (res != null) return res
            } catch (e: Throwable) {
                Log.w(TAG, "Excepción en swapTextRangesNative, usando fallback: ${e.message}")
            }
        }
        return fallbackSwapRanges(fullText, startA, endA, startB, endB)
    }

    /**
     * Mueve un rango de texto con ejecución en C++20 y respaldo tolerante a fallos.
     */
    fun moveTextRangeSafe(
        fullText: String,
        start: Int,
        end: Int,
        targetPosition: Int
    ): TextOperationResult {
        if (isNativeLoaded) {
            try {
                val res = moveTextRangeNative(fullText, start, end, targetPosition)
                if (res != null) return res
            } catch (e: Throwable) {
                Log.w(TAG, "Excepción en moveTextRangeNative, usando fallback: ${e.message}")
            }
        }
        return fallbackMoveRange(fullText, start, end, targetPosition)
    }

    /**
     * Intercambia el párrafo actual con el anterior o posterior con respaldo tolerante a fallos.
     */
    fun swapParagraphSafe(
        fullText: String,
        cursorStart: Int,
        cursorEnd: Int,
        swapUp: Boolean
    ): TextOperationResult {
        if (isNativeLoaded) {
            try {
                val res = swapParagraphNative(fullText, cursorStart, cursorEnd, swapUp)
                if (res != null) return res
            } catch (e: Throwable) {
                Log.w(TAG, "Excepción en swapParagraphNative, usando fallback: ${e.message}")
            }
        }
        return fallbackSwapParagraph(fullText, cursorStart, cursorEnd, swapUp)
    }

    // --------------------------------------------------------------------------
    // Algoritmos de Respaldo en Kotlin puro
    // --------------------------------------------------------------------------

    private fun fallbackSwapRanges(
        fullText: String,
        startA: Int,
        endA: Int,
        startB: Int,
        endB: Int
    ): TextOperationResult {
        val len = fullText.length
        val aS = minOf(startA, endA).coerceIn(0, len)
        val aE = maxOf(startA, endA).coerceIn(0, len)
        val bS = minOf(startB, endB).coerceIn(0, len)
        val bE = maxOf(startB, endB).coerceIn(0, len)

        if (aS < bE && aE > bS) {
            return TextOperationResult(fullText, aS, aE, false, "Los bloques seleccionados no pueden solaparse")
        }

        val aFirst = aS <= bS
        val firstS = if (aFirst) aS else bS
        val firstE = if (aFirst) aE else bE
        val secondS = if (aFirst) bS else aS
        val secondE = if (aFirst) bE else aE

        val sb = StringBuilder()
        sb.append(fullText.substring(0, firstS))
        sb.append(fullText.substring(secondS, secondE))
        sb.append(fullText.substring(firstE, secondS))
        sb.append(fullText.substring(firstS, firstE))
        sb.append(fullText.substring(secondE))

        val newText = sb.toString()
        val newStart = if (aFirst) {
            val bLen = secondE - secondS
            val midLen = secondS - firstE
            firstS + bLen + midLen
        } else {
            firstS
        }
        val aLen = aE - aS
        return TextOperationResult(newText, newStart, newStart + aLen, true, "Bloques intercambiados (Fallback)")
    }

    private fun fallbackMoveRange(
        fullText: String,
        start: Int,
        end: Int,
        targetPosition: Int
    ): TextOperationResult {
        val len = fullText.length
        val s = minOf(start, end).coerceIn(0, len)
        val e = maxOf(start, end).coerceIn(0, len)
        val target = targetPosition.coerceIn(0, len)

        if (s == e || (target in s..e)) {
            return TextOperationResult(fullText, s, e, true, "Sin cambios de posición")
        }

        val block = fullText.substring(s, e)
        val sb = StringBuilder()
        val (newStart, newEnd) = if (target < s) {
            sb.append(fullText.substring(0, target))
            sb.append(block)
            sb.append(fullText.substring(target, s))
            sb.append(fullText.substring(e))
            Pair(target, target + block.length)
        } else {
            sb.append(fullText.substring(0, s))
            sb.append(fullText.substring(e, target))
            sb.append(block)
            sb.append(fullText.substring(target))
            Pair(target - block.length, target)
        }

        return TextOperationResult(sb.toString(), newStart, newEnd, true, "Texto reubicado (Fallback)")
    }

    private fun fallbackSwapParagraph(
        fullText: String,
        cursorStart: Int,
        cursorEnd: Int,
        swapUp: Boolean
    ): TextOperationResult {
        if (fullText.isEmpty()) {
            return TextOperationResult("", 0, 0, false, "Documento vacío")
        }

        val len = fullText.length
        val cS = minOf(cursorStart, cursorEnd).coerceIn(0, len)
        val cE = maxOf(cursorStart, cursorEnd).coerceIn(0, len)

        var currStart = cS
        while (currStart > 0 && fullText[currStart - 1] != '\n') {
            currStart--
        }

        var currEnd = cE
        while (currEnd < len && fullText[currEnd] != '\n') {
            currEnd++
        }

        return if (swapUp) {
            if (currStart == 0) {
                return TextOperationResult(fullText, cS, cE, false, "Ya es el primer párrafo")
            }
            val prevEnd = currStart - 1
            var prevStart = prevEnd
            while (prevStart > 0 && fullText[prevStart - 1] != '\n') {
                prevStart--
            }
            fallbackSwapRanges(fullText, currStart, currEnd, prevStart, prevEnd)
        } else {
            if (currEnd >= len) {
                return TextOperationResult(fullText, cS, cE, false, "Ya es el último párrafo")
            }
            val nextStart = currEnd + 1
            var nextEnd = nextStart
            while (nextEnd < len && fullText[nextEnd] != '\n') {
                nextEnd++
            }
            fallbackSwapRanges(fullText, currStart, currEnd, nextStart, nextEnd)
        }
    }
}

