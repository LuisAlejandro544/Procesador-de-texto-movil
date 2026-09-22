package com.example.ui.delegates

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.ui.MarkedSwapBlock
import com.example.util.NativeEngineBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestor del historial de edición (Deshacer / Rehacer) y operaciones
 * atómicas de transposición y movimiento de texto al estilo PC.
 */
class TextEditingHistoryManager {

    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _markedSwapBlock = MutableStateFlow<MarkedSwapBlock?>(null)
    val markedSwapBlock: StateFlow<MarkedSwapBlock?> = _markedSwapBlock.asStateFlow()

    /**
     * Reinicia el historial con un contenido inicial.
     */
    fun reset(initialContent: String) {
        undoStack.clear()
        redoStack.clear()
        undoStack.add(initialContent)
        _canUndo.value = false
        _canRedo.value = false
        _markedSwapBlock.value = null
    }

    /**
     * Registra un cambio en el contenido del documento.
     */
    fun recordContentChange(currentContent: String) {
        if (undoStack.isEmpty() || undoStack.last() != currentContent) {
            undoStack.add(currentContent)
            if (undoStack.size > 50) undoStack.removeAt(0)
            redoStack.clear()
            _canUndo.value = true
            _canRedo.value = false
        }
    }

    /**
     * Deshace el último cambio textual y retorna el texto anterior, o null si la pila está vacía.
     */
    fun undo(currentContent: String): String? {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(currentContent)
            _canUndo.value = undoStack.isNotEmpty()
            _canRedo.value = true
            return previous
        }
        return null
    }

    /**
     * Rehace el texto deshecho y retorna el texto posterior, o null si la pila está vacía.
     */
    fun redo(currentContent: String): String? {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(currentContent)
            _canUndo.value = true
            _canRedo.value = redoStack.isNotEmpty()
            return next
        }
        return null
    }

    /**
     * Fija el tramo de texto actualmente seleccionado como "Bloque A" para intercambiarlo.
     * Retorna el mensaje para el usuario.
     */
    fun markCurrentSelectionForSwap(textFieldValue: TextFieldValue): String {
        val min = textFieldValue.selection.min.coerceIn(0, textFieldValue.text.length)
        val max = textFieldValue.selection.max.coerceIn(0, textFieldValue.text.length)
        return if (min < max) {
            val selected = textFieldValue.text.substring(min, max)
            _markedSwapBlock.value = MarkedSwapBlock(selected, min, max)
            "Bloque A fijado (${selected.length} letras). Selecciona el Bloque B para intercambiar."
        } else {
            "Selecciona primero un texto para fijar como Bloque A."
        }
    }

    /**
     * Cancela la fijación del Bloque A para intercambio.
     */
    fun clearMarkedSwapBlock() {
        _markedSwapBlock.value = null
    }

    /**
     * Ejecuta el intercambio atómico entre el Bloque A fijado y la selección actual (Bloque B).
     */
    fun executeSwapWithMarkedBlock(
        textFieldValue: TextFieldValue,
        onSuccess: (TextFieldValue, String) -> Unit,
        onError: (String) -> Unit
    ): Boolean {
        val blockA = _markedSwapBlock.value ?: return false
        val bStart = textFieldValue.selection.min.coerceIn(0, textFieldValue.text.length)
        val bEnd = textFieldValue.selection.max.coerceIn(0, textFieldValue.text.length)

        if (bStart == bEnd) {
            onError("Selecciona el segundo bloque de texto (Bloque B) para intercambiar.")
            return false
        }

        val result = NativeEngineBridge.swapTextRangesSafe(
            fullText = textFieldValue.text,
            startA = blockA.start,
            endA = blockA.end,
            startB = bStart,
            endB = bEnd
        )

        return if (result.success) {
            _markedSwapBlock.value = null
            val newTfv = TextFieldValue(
                text = result.newText,
                selection = TextRange(result.newSelectionStart, result.newSelectionEnd)
            )
            onSuccess(newTfv, "¡Intercambio realizado con éxito!")
            true
        } else {
            onError(result.message)
            false
        }
    }

    /**
     * Intercambia el párrafo actual con el anterior o el posterior.
     */
    fun swapParagraph(
        textFieldValue: TextFieldValue,
        swapUp: Boolean,
        onSuccess: (TextFieldValue, String) -> Unit,
        onError: (String) -> Unit
    ) {
        val result = NativeEngineBridge.swapParagraphSafe(
            fullText = textFieldValue.text,
            cursorStart = textFieldValue.selection.min,
            cursorEnd = textFieldValue.selection.max,
            swapUp = swapUp
        )
        if (result.success) {
            val newTfv = TextFieldValue(
                text = result.newText,
                selection = TextRange(result.newSelectionStart, result.newSelectionEnd)
            )
            val msg = if (swapUp) "Párrafo intercambiado con el anterior" else "Párrafo intercambiado con el siguiente"
            onSuccess(newTfv, msg)
        } else {
            onError(result.message)
        }
    }

    /**
     * Mueve el texto seleccionado al inicio, final o posición arbitraria.
     */
    fun moveSelection(
        textFieldValue: TextFieldValue,
        targetPosition: Int,
        label: String,
        onSuccess: (TextFieldValue, String) -> Unit,
        onError: (String) -> Unit
    ) {
        val s = textFieldValue.selection.min
        val e = textFieldValue.selection.max
        if (s == e) {
            onError("Selecciona texto para mover $label")
            return
        }

        val result = NativeEngineBridge.moveTextRangeSafe(
            fullText = textFieldValue.text,
            start = s,
            end = e,
            targetPosition = targetPosition
        )
        if (result.success) {
            val newTfv = TextFieldValue(
                text = result.newText,
                selection = TextRange(result.newSelectionStart, result.newSelectionEnd)
            )
            onSuccess(newTfv, "Texto reubicado $label")
        } else {
            onError(result.message)
        }
    }

    /**
     * Intercambia el texto seleccionado con el texto del portapapeles.
     */
    fun swapSelectionWithClipboard(
        textFieldValue: TextFieldValue,
        clipboardText: String,
        onSuccess: (TextFieldValue, String) -> Unit
    ): String {
        val min = textFieldValue.selection.min.coerceIn(0, textFieldValue.text.length)
        val max = textFieldValue.selection.max.coerceIn(0, textFieldValue.text.length)
        if (min == max) return ""

        val originalSelected = textFieldValue.text.substring(min, max)
        val newText = textFieldValue.text.replaceRange(min, max, clipboardText)
        val newCursor = min + clipboardText.length

        val newTfv = TextFieldValue(
            text = newText,
            selection = TextRange(min, newCursor)
        )
        onSuccess(newTfv, "Texto intercambiado con el portapapeles")
        return originalSelected
    }
}
