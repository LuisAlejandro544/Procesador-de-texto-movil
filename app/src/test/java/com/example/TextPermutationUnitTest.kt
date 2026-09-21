package com.example

import com.example.util.NativeEngineBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TextPermutationUnitTest:
 * Valida la lógica de transposición atómica (Swap) y desplazamiento (Move)
 * de texto estilo procesador de PC implementada en DocuSheet.
 */
class TextPermutationUnitTest {

    @Test
    fun testSwapRangesSafe() {
        val original = "Hola Mundo Lindo"
        // A = "Hola" (0..4)
        // B = "Lindo" (11..16)
        val result = NativeEngineBridge.swapTextRangesSafe(
            fullText = original,
            startA = 0,
            endA = 4,
            startB = 11,
            endB = 16
        )

        assertTrue(result.success)
        assertEquals("Lindo Mundo Hola", result.newText)
    }

    @Test
    fun testMoveRangeSafeToStart() {
        val original = "Primera linea\nSegunda linea"
        // Mover "Segunda linea" al inicio
        val result = NativeEngineBridge.moveTextRangeSafe(
            fullText = original,
            start = 14,
            end = 27,
            targetPosition = 0
        )

        assertTrue(result.success)
        assertEquals("Segunda lineaPrimera linea\n", result.newText)
    }

    @Test
    fun testMoveRangeSafeToEnd() {
        val original = "Parrafo uno\nParrafo dos"
        // Mover "Parrafo uno\n" al final
        val result = NativeEngineBridge.moveTextRangeSafe(
            fullText = original,
            start = 0,
            end = 12,
            targetPosition = original.length
        )

        assertTrue(result.success)
        assertEquals("Parrafo dosParrafo uno\n", result.newText)
    }

    @Test
    fun testSwapParagraphSafe() {
        val doc = "Primer parrafo\nSegundo parrafo\nTercer parrafo"
        // Párrafo del medio (cursor en "Segundo")
        val cursor = 20
        val result = NativeEngineBridge.swapParagraphSafe(
            fullText = doc,
            cursorStart = cursor,
            cursorEnd = cursor,
            swapUp = true
        )

        assertTrue(result.success)
        assertEquals("Segundo parrafo\nPrimer parrafo\nTercer parrafo", result.newText)
    }
}
