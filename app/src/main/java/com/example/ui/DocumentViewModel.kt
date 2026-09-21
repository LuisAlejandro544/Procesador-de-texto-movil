package com.example.ui

import android.app.Application
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DocumentEntity
import com.example.data.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.util.NativeEngineBridge
import com.example.util.TextOperationResult
import com.example.util.DocuSheetCacheManager
import com.example.util.CacheStats
import com.example.util.PageFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bloque fijado temporalmente para la operación de Intercambio de 2 bloques (Swap A ⇄ B).
 */
data class MarkedSwapBlock(
    val text: String,
    val start: Int,
    val end: Int
)

/**
 * DocumentViewModel: Administrador de estado y lógica de negocio para el procesador de texto.
 * 
 * Gestiona:
 * - Persistencia reactiva de documentos mediante corrutinas
 * - Guardado automático diferido para fluidez al escribir
 * - Pila de Deshacer (Undo) y Rehacer (Redo)
 * - Estadísticas en vivo de la hoja (palabras, caracteres, tiempo de lectura)
 * - Modos de visualización (Edición / Lectura) y niveles de zoom de la hoja
 */
class DocumentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DocumentRepository
    val allDocuments: StateFlow<List<DocumentEntity>>

    // Filtro de búsqueda en la biblioteca de documentos
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Documento activo en el editor
    private val _activeDocument = MutableStateFlow<DocumentEntity?>(null)
    val activeDocument = _activeDocument.asStateFlow()

    // Contenido en edición en tiempo real
    private val _editorContent = MutableStateFlow("")
    val editorContent = _editorContent.asStateFlow()

    // Estado del campo de texto con cursor y selección activa de PC
    private val _editorTextFieldValue = MutableStateFlow(TextFieldValue(""))
    val editorTextFieldValue = _editorTextFieldValue.asStateFlow()

    // Título en edición en tiempo real
    private val _editorTitle = MutableStateFlow("")
    val editorTitle = _editorTitle.asStateFlow()

    // Estado de guardado ("Guardado", "Guardando...", "Cambios pendientes")
    private val _saveStatus = MutableStateFlow("Guardado")
    val saveStatus = _saveStatus.asStateFlow()

    // Modo de vista: false = Modo Edición, true = Modo Lectura de Hoja
    private val _isReadOnlyMode = MutableStateFlow(false)
    val isReadOnlyMode = _isReadOnlyMode.asStateFlow()

    // Escala de zoom de la hoja (0.85f, 1.0f, 1.15f)
    private val _pageZoom = MutableStateFlow(1.0f)
    val pageZoom = _pageZoom.asStateFlow()

    // Modo de visualización: Hoja en cascada continua (múltiples hojas físicas)
    private val _isCascadeMode = MutableStateFlow(true)
    val isCascadeMode = _isCascadeMode.asStateFlow()

    // Meta diaria de redacción en palabras
    private val _dailyGoalWords = MutableStateFlow(500)
    val dailyGoalWords = _dailyGoalWords.asStateFlow()

    // Historial para Deshacer / Rehacer
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo = _canRedo.asStateFlow()

    // Bloque marcado para intercambio entre 2 selecciones (Swap Bloque A ⇄ Bloque B)
    private val _markedSwapBlock = MutableStateFlow<MarkedSwapBlock?>(null)
    val markedSwapBlock = _markedSwapBlock.asStateFlow()

    // Mensaje informativo temporal de operaciones de edición de PC
    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage = _userFeedbackMessage.asStateFlow()

    // Estadísticas cuantitativas de almacenamiento en caché y optimización
    private val _cacheStats = MutableStateFlow(CacheStats())
    val cacheStats = _cacheStats.asStateFlow()

    fun clearFeedbackMessage() {
        _userFeedbackMessage.value = null
    }

    fun refreshCacheStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val contents = allDocuments.value.map { it.content }
            val stats = DocuSheetCacheManager.getCacheStats(getApplication(), contents)
            _cacheStats.value = stats
        }
    }

    fun cleanCache(onComplete: ((String) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val contents = allDocuments.value.map { it.content }
            val freedBytes = DocuSheetCacheManager.cleanAllReclaimable(getApplication(), contents)
            refreshCacheStats()
            val freedFormatted = CacheStats.formatBytes(freedBytes)
            viewModelScope.launch(Dispatchers.Main) {
                _userFeedbackMessage.value = "Se han liberado $freedFormatted de memoria caché y archivos temporales."
                onComplete?.invoke("Se han liberado $freedFormatted de memoria caché y archivos temporales.")
            }
        }
    }

    private var autoSaveJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = DocumentRepository(database.documentDao())

        allDocuments = repository.allDocuments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch(Dispatchers.IO) {
            DocuSheetCacheManager.autoPruneIfExceeded(application, emptyList())
            refreshCacheStats()
        }
    }

    /**
     * Documentos filtrados por el término de búsqueda.
     */
    val filteredDocuments: StateFlow<List<DocumentEntity>> = combine(
        allDocuments,
        _searchQuery
    ) { docs, query ->
        if (query.isBlank()) {
            docs
        } else {
            docs.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    /**
     * Carga un documento en el editor por su ID.
     */
    fun loadDocument(docId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val doc = repository.getDocumentByIdOnce(docId)
            doc?.let {
                _activeDocument.value = it
                _editorTitle.value = it.title
                _editorContent.value = it.content
                _editorTextFieldValue.value = TextFieldValue(it.content, TextRange(it.content.length))
                undoStack.clear()
                redoStack.clear()
                undoStack.add(it.content)
                _canUndo.value = false
                _canRedo.value = false
                _saveStatus.value = "Guardado"

                // Detección y migración automática de imágenes temporales content:// a almacenamiento privado permanente
                if (it.content.contains("content://")) {
                    val contentUriRegex = Regex("""!\[([^\]]*)\]\((content://[^)]+)\)""")
                    val matches = contentUriRegex.findAll(it.content).toList()
                    if (matches.isNotEmpty()) {
                        var migratedContent = it.content
                        for (match in matches) {
                            val rawUri = match.groupValues[2]
                            try {
                                val permanentPath = DocuSheetCacheManager.saveImageFromUri(getApplication(), android.net.Uri.parse(rawUri))
                                if (permanentPath != null) {
                                    migratedContent = migratedContent.replace(rawUri, permanentPath)
                                }
                            } catch (_: Exception) {}
                        }
                        if (migratedContent != it.content) {
                            _editorContent.value = migratedContent
                            _editorTextFieldValue.value = TextFieldValue(migratedContent, TextRange(migratedContent.length))
                            saveCurrentDocumentInternal()
                        }
                    }
                }
            }
        }
    }

    /**
     * Actualiza el contenido de la hoja e inicia un guardado automático con retardo.
     */
    fun onContentChanged(newContent: String) {
        if (newContent != _editorContent.value) {
            // Guardar en pila de deshacer si hubo cambio sustancial
            if (undoStack.isEmpty() || undoStack.last() != _editorContent.value) {
                undoStack.add(_editorContent.value)
                if (undoStack.size > 50) undoStack.removeAt(0)
                redoStack.clear()
                _canUndo.value = true
                _canRedo.value = false
            }

            _editorContent.value = newContent
            if (_editorTextFieldValue.value.text != newContent) {
                _editorTextFieldValue.value = TextFieldValue(newContent, TextRange(newContent.length))
            }
            _saveStatus.value = "Guardando..."
            scheduleAutoSave()
        }
    }

    /**
     * Actualiza el TextFieldValue manteniendo el cursor y la selección activa de PC.
     */
    fun onTextFieldValueChange(newValue: TextFieldValue) {
        _editorTextFieldValue.value = newValue
        if (newValue.text != _editorContent.value) {
            onContentChanged(newValue.text)
        }
    }

    /**
     * Deselecciona el texto activo posicionando el cursor al final de la selección.
     */
     fun clearSelection() {
         val current = _editorTextFieldValue.value
         val cursor = current.selection.end
         _editorTextFieldValue.value = current.copy(selection = TextRange(cursor))
     }

    // ==========================================================================
    // Operaciones de Arrastrar/Mover e Intercambiar Texto (Modos estilo PC)
    // ==========================================================================

    /**
     * Fija el tramo de texto actualmente seleccionado como "Bloque A" para intercambiarlo
     * con una futura selección ("Bloque B").
     */
    fun markCurrentSelectionForSwap() {
        val current = _editorTextFieldValue.value
        val min = current.selection.min.coerceIn(0, current.text.length)
        val max = current.selection.max.coerceIn(0, current.text.length)
        if (min < max) {
            val selected = current.text.substring(min, max)
            _markedSwapBlock.value = MarkedSwapBlock(selected, min, max)
            _userFeedbackMessage.value = "Bloque A fijado (${selected.length} letras). Selecciona el Bloque B para intercambiar."
        } else {
            _userFeedbackMessage.value = "Selecciona primero un texto para fijar como Bloque A."
        }
    }

    /**
     * Cancela la fijación del Bloque A para intercambio.
     */
    fun clearMarkedSwapBlock() {
        _markedSwapBlock.value = null
        _userFeedbackMessage.value = "Intercambio cancelado"
    }

    /**
     * Ejecuta el intercambio atómico entre el Bloque A fijado y la selección actual (Bloque B).
     */
    fun executeSwapWithMarkedBlock(): Boolean {
        val blockA = _markedSwapBlock.value ?: return false
        val current = _editorTextFieldValue.value
        val bStart = current.selection.min.coerceIn(0, current.text.length)
        val bEnd = current.selection.max.coerceIn(0, current.text.length)

        if (bStart == bEnd) {
            _userFeedbackMessage.value = "Selecciona el segundo bloque de texto (Bloque B) para intercambiar."
            return false
        }

        val result = NativeEngineBridge.swapTextRangesSafe(
            fullText = current.text,
            startA = blockA.start,
            endA = blockA.end,
            startB = bStart,
            endB = bEnd
        )

        if (result.success) {
            _markedSwapBlock.value = null
            _editorTextFieldValue.value = TextFieldValue(
                text = result.newText,
                selection = TextRange(result.newSelectionStart, result.newSelectionEnd)
            )
            onContentChanged(result.newText)
            _userFeedbackMessage.value = "¡Intercambio realizado con éxito!"
            return true
        } else {
            _userFeedbackMessage.value = result.message
            return false
        }
    }

    /**
     * Intercambia el párrafo actual donde está el cursor/selección con el párrafo anterior.
     */
    fun swapParagraphUp() {
        val current = _editorTextFieldValue.value
        val result = NativeEngineBridge.swapParagraphSafe(
            fullText = current.text,
            cursorStart = current.selection.min,
            cursorEnd = current.selection.max,
            swapUp = true
        )
        if (result.success) {
            _editorTextFieldValue.value = TextFieldValue(
                text = result.newText,
                selection = TextRange(result.newSelectionStart, result.newSelectionEnd)
            )
            onContentChanged(result.newText)
            _userFeedbackMessage.value = "Párrafo intercambiado con el anterior"
        } else {
            _userFeedbackMessage.value = result.message
        }
    }

    /**
     * Intercambia el párrafo actual donde está el cursor/selección con el párrafo posterior.
     */
    fun swapParagraphDown() {
        val current = _editorTextFieldValue.value
        val result = NativeEngineBridge.swapParagraphSafe(
            fullText = current.text,
            cursorStart = current.selection.min,
            cursorEnd = current.selection.max,
            swapUp = false
        )
        if (result.success) {
            _editorTextFieldValue.value = TextFieldValue(
                text = result.newText,
                selection = TextRange(result.newSelectionStart, result.newSelectionEnd)
            )
            onContentChanged(result.newText)
            _userFeedbackMessage.value = "Párrafo intercambiado con el siguiente"
        } else {
            _userFeedbackMessage.value = result.message
        }
    }

    /**
     * Mueve el texto seleccionado al inicio absoluto del documento.
     */
    fun moveSelectionToStart() {
        val current = _editorTextFieldValue.value
        val s = current.selection.min
        val e = current.selection.max
        if (s == e) {
            _userFeedbackMessage.value = "Selecciona texto para mover al inicio"
            return
        }

        val result = NativeEngineBridge.moveTextRangeSafe(
            fullText = current.text,
            start = s,
            end = e,
            targetPosition = 0
        )
        if (result.success) {
            _editorTextFieldValue.value = TextFieldValue(
                text = result.newText,
                selection = TextRange(result.newSelectionStart, result.newSelectionEnd)
            )
            onContentChanged(result.newText)
            _userFeedbackMessage.value = "Texto reubicado al inicio"
        } else {
            _userFeedbackMessage.value = result.message
        }
    }

    /**
     * Mueve el texto seleccionado al final absoluto del documento.
     */
    fun moveSelectionToEnd() {
        val current = _editorTextFieldValue.value
        val s = current.selection.min
        val e = current.selection.max
        if (s == e) {
            _userFeedbackMessage.value = "Selecciona texto para mover al final"
            return
        }

        val result = NativeEngineBridge.moveTextRangeSafe(
            fullText = current.text,
            start = s,
            end = e,
            targetPosition = current.text.length
        )
        if (result.success) {
            _editorTextFieldValue.value = TextFieldValue(
                text = result.newText,
                selection = TextRange(result.newSelectionStart, result.newSelectionEnd)
            )
            onContentChanged(result.newText)
            _userFeedbackMessage.value = "Texto reubicado al final"
        } else {
            _userFeedbackMessage.value = result.message
        }
    }

    /**
     * Mueve el texto seleccionado hacia una posición arbitraria (targetPosition).
     */
    fun moveSelectionToTarget(targetPosition: Int) {
        val current = _editorTextFieldValue.value
        val s = current.selection.min
        val e = current.selection.max
        if (s == e) return

        val result = NativeEngineBridge.moveTextRangeSafe(
            fullText = current.text,
            start = s,
            end = e,
            targetPosition = targetPosition
        )
        if (result.success) {
            _editorTextFieldValue.value = TextFieldValue(
                text = result.newText,
                selection = TextRange(result.newSelectionStart, result.newSelectionEnd)
            )
            onContentChanged(result.newText)
            _userFeedbackMessage.value = "Texto reubicado en la nueva posición"
        } else {
            _userFeedbackMessage.value = result.message
        }
    }

    /**
     * Intercambia el texto seleccionado actualmente con el texto del portapapeles.
     */
    fun swapSelectionWithClipboard(clipboardText: String): String {
        val current = _editorTextFieldValue.value
        val min = current.selection.min.coerceIn(0, current.text.length)
        val max = current.selection.max.coerceIn(0, current.text.length)
        if (min == max) return ""

        val originalSelected = current.text.substring(min, max)
        val newText = current.text.replaceRange(min, max, clipboardText)
        val newCursor = min + clipboardText.length

        _editorTextFieldValue.value = TextFieldValue(
            text = newText,
            selection = TextRange(min, newCursor)
        )
        onContentChanged(newText)
        _userFeedbackMessage.value = "Texto intercambiado con el portapapeles"
        return originalSelected
    }


    /**
     * Actualiza el título del documento e inicia guardado automático.
     */
    fun onTitleChanged(newTitle: String) {
        _editorTitle.value = newTitle
        _saveStatus.value = "Guardando..."
        scheduleAutoSave()
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(600) // Debounce para escritura fluida
            saveCurrentDocumentInternal()
        }
    }

    private suspend fun saveCurrentDocumentInternal() {
        val current = _activeDocument.value ?: return
        val updated = current.copy(
            title = _editorTitle.value.ifBlank { "Documento sin título" },
            content = _editorContent.value,
            updatedAt = System.currentTimeMillis()
        )
        repository.updateDocument(updated)
        _activeDocument.value = updated
        _saveStatus.value = "Guardado"
    }

    /**
     * Fuerza el guardado inmediato en base de datos.
     */
    fun saveImmediately() {
        autoSaveJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            saveCurrentDocumentInternal()
        }
    }

    /**
     * Deshace el último cambio textual.
     */
    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(_editorContent.value)
            _editorContent.value = previous
            _canUndo.value = undoStack.isNotEmpty()
            _canRedo.value = true
            scheduleAutoSave()
        }
    }

    /**
     * Rehace el texto deshecho.
     */
    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(_editorContent.value)
            _editorContent.value = next
            _canUndo.value = true
            _canRedo.value = redoStack.isNotEmpty()
            scheduleAutoSave()
        }
    }

    /**
     * Alterna entre modo lectura y modo edición de la hoja.
     */
    fun toggleReadOnlyMode() {
        _isReadOnlyMode.value = !_isReadOnlyMode.value
    }

    /**
     * Cambia el nivel de zoom de la hoja de texto.
     */
    fun cyclePageZoom() {
        _pageZoom.value = when (_pageZoom.value) {
            1.0f -> 1.15f
            1.15f -> 0.85f
            else -> 1.0f
        }
    }

    /**
     * Alterna entre modo de cascada continua (múltiples hojas) y hoja única.
     */
    fun toggleCascadeMode() {
        _isCascadeMode.value = !_isCascadeMode.value
    }

    /**
     * Define la meta diaria de palabras.
     */
    fun setDailyGoal(goal: Int) {
        _dailyGoalWords.value = goal
    }

    /**
     * Inserta un título principal (H1).
     */
    fun insertHeading1() {
        val current = _editorContent.value
        val prefix = if (current.isEmpty() || current.endsWith("\n")) "" else "\n\n"
        onContentChanged(current + prefix + "# TÍTULO PRINCIPAL\n")
    }

    /**
     * Inserta un subtítulo o sección (H2).
     */
    fun insertHeading2() {
        val current = _editorContent.value
        val prefix = if (current.isEmpty() || current.endsWith("\n")) "" else "\n\n"
        onContentChanged(current + prefix + "## Subtítulo o Sección\n")
    }

    /**
     * Inserta un sub-párrafo o apartado (H3).
     */
    fun insertHeading3() {
        val current = _editorContent.value
        val prefix = if (current.isEmpty() || current.endsWith("\n")) "" else "\n\n"
        onContentChanged(current + prefix + "### Apartado temático\n")
    }

    /**
     * Inserta un bloque de cita destacada.
     */
    fun insertQuote() {
        val current = _editorContent.value
        val prefix = if (current.isEmpty() || current.endsWith("\n")) "" else "\n\n"
        onContentChanged(current + prefix + "> \"Cita reflexiva o idea central a destacar...\"\n")
    }

    /**
     * Inserta una lista numerada (1, 2, 3).
     */
    fun insertNumberedList() {
        val current = _editorContent.value
        val prefix = if (current.isEmpty() || current.endsWith("\n")) "" else "\n"
        onContentChanged(current + prefix + "1. Primer elemento\n2. Segundo elemento\n")
    }

    /**
     * Inserta una tarea con casilla interactiva.
     */
    fun insertChecklist() {
        val current = _editorContent.value
        val prefix = if (current.isEmpty() || current.endsWith("\n")) "" else "\n"
        onContentChanged(current + prefix + "[ ] Tarea pendiente\n")
    }

    /**
     * Aplica o inserta texto en negrita.
     */
    fun insertBold() {
        val current = _editorContent.value
        onContentChanged(current + "**texto en negrita**")
    }

    /**
     * Aplica o inserta texto en cursiva.
     */
    fun insertItalic() {
        val current = _editorContent.value
        onContentChanged(current + "*texto en cursiva*")
    }

    /**
     * Aplica o inserta texto subrayado.
     */
    fun insertUnderline() {
        val current = _editorContent.value
        onContentChanged(current + "<u>texto subrayado</u>")
    }

    /**
     * Aplica o inserta texto tachado.
     */
    fun insertStrikethrough() {
        val current = _editorContent.value
        onContentChanged(current + "~~texto tachado~~")
    }

    /**
     * Inserta una notación con subíndice (ej. fórmulas químicas H2O).
     */
    fun insertSubscript() {
        val current = _editorContent.value
        onContentChanged(current + "<sub>2</sub>")
    }

    /**
     * Inserta una notación con superíndice (ej. exponentes X2 o fechas 1ro).
     */
    fun insertSuperscript() {
        val current = _editorContent.value
        onContentChanged(current + "<sup>2</sup>")
    }

    /**
     * Inserta una imagen con directiva de ajuste de hoja (Layout & Wrap).
     * @param uri URI o ruta de la imagen local.
     * @param wrapMode Modo de ajuste: "full" (ancho completo), "center" (centrada), "left" (izquierda), "right" (derecha).
     * @param caption Pie de foto explicativo opcional.
     */
    fun insertImage(uri: String, wrapMode: String = "full", caption: String = "") {
        val tfv = _editorTextFieldValue.value
        val current = tfv.text
        val selStart = tfv.selection.min.coerceIn(0, current.length)
        val selEnd = tfv.selection.max.coerceIn(0, current.length)

        val prefix = if (selStart == 0 || (selStart > 0 && current[selStart - 1] == '\n')) "" else "\n"
        val suffix = if (selEnd < current.length && current[selEnd] == '\n') "" else "\n"
        val imageDirective = "$prefix![wrap:$wrapMode,$caption]($uri)$suffix"

        val newText = current.substring(0, selStart) + imageDirective + current.substring(selEnd)
        val newCursor = selStart + imageDirective.length
        onTextFieldValueChange(TextFieldValue(newText, TextRange(newCursor)))
        saveImmediately()

        // Si la URI es un content:// de Android, migrarlo inmediatamente a un archivo permanente en almacenamiento privado
        if (uri.startsWith("content://")) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val permanentPath = DocuSheetCacheManager.saveImageFromUri(getApplication(), android.net.Uri.parse(uri))
                    if (permanentPath != null) {
                        val latest = _editorContent.value
                        val updated = latest.replace(uri, permanentPath)
                        if (updated != latest) {
                            withContext(Dispatchers.Main) {
                                onContentChanged(updated)
                                saveImmediately()
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Inserta una Tabla o Cuadrícula Editorial con formato estructurado compatible con Markdown.
     * Soporta los estilos: "classic", "editorial", "striped", "compact".
     * Calcula dinámicamente celdas, encabezados y posición del cursor.
     */
    fun insertTable(
        rows: Int = 3,
        cols: Int = 3,
        hasHeader: Boolean = true,
        style: String = "classic"
    ) {
        val safeRows = maxOf(1, minOf(15, rows))
        val safeCols = maxOf(1, minOf(8, cols))
        val sb = StringBuilder()

        val currentVal = _editorTextFieldValue.value
        val currentText = currentVal.text
        val selStart = currentVal.selection.min
        val selEnd = currentVal.selection.max

        val prefix = if (selStart == 0 || (selStart > 0 && currentText[selStart - 1] == '\n')) "" else "\n"
        sb.append(prefix)

        // Etiqueta de estilo editorial si difiere del estándar
        if (style != "classic") {
            sb.append("[table:$style]\n")
        }

        // Fila de encabezados
        if (hasHeader) {
            sb.append("|")
            for (c in 1..safeCols) {
                sb.append(" Encabezado $c |")
            }
            sb.append("\n|")
            for (c in 1..safeCols) {
                sb.append("---|")
            }
            sb.append("\n")
        }

        // Filas de datos
        for (r in 1..safeRows) {
            sb.append("|")
            for (c in 1..safeCols) {
                sb.append(" Dato $r,$c |")
            }
            sb.append("\n")
        }

        if (style != "classic") {
            sb.append("[/table]\n")
        } else {
            sb.append("\n")
        }

        val tableString = sb.toString()
        val newText = if (selStart >= 0 && selEnd <= currentText.length) {
            currentText.substring(0, selStart) + tableString + currentText.substring(selEnd)
        } else {
            currentText + tableString
        }

        val newCursor = (if (selStart >= 0) selStart else currentText.length) + tableString.length
        onTextFieldValueChange(
            TextFieldValue(
                text = newText,
                selection = androidx.compose.ui.text.TextRange(newCursor)
            )
        )
    }

    /**
     * Inserta un bloque con alineación de párrafo específica (Izquierda, Centrado, Derecha, Justificado).
     * Si hay texto seleccionado, lo envuelve con [align:x]...[/align].
     * Si no hay texto seleccionado, envuelve la línea actual alrededor del cursor.
     */
    fun insertAlignmentBlock(alignment: String) {
        val alignLower = when (alignment.lowercase()) {
            "center", "centrado" -> "center"
            "right", "derecha" -> "right"
            "justify", "justificado" -> "justify"
            else -> "left"
        }
        val tfv = _editorTextFieldValue.value
        val text = tfv.text
        val sel = tfv.selection

        val openTag = "[align:$alignLower]"
        val closeTag = "[/align]"

        if (sel.min != sel.max) {
            val min = sel.min.coerceIn(0, text.length)
            val max = sel.max.coerceIn(0, text.length)
            val selected = text.substring(min, max)
            val newText = text.substring(0, min) + openTag + selected + closeTag + text.substring(max)
            val newCursor = min + openTag.length + selected.length + closeTag.length
            onTextFieldValueChange(TextFieldValue(newText, TextRange(newCursor)))
        } else {
            val cursor = sel.start.coerceIn(0, text.length)
            val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
            val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
            val currentLine = text.substring(lineStart, lineEnd)

            if (currentLine.isNotBlank()) {
                val newText = text.substring(0, lineStart) + openTag + currentLine + closeTag + text.substring(lineEnd)
                val newCursor = lineStart + openTag.length + currentLine.length + closeTag.length
                onTextFieldValueChange(TextFieldValue(newText, TextRange(newCursor)))
            } else {
                val newText = text.substring(0, cursor) + openTag + closeTag + text.substring(cursor)
                val newCursor = cursor + openTag.length
                onTextFieldValueChange(TextFieldValue(newText, TextRange(newCursor)))
            }
        }
        saveImmediately()
    }

    /** Alias conveniente para insertAlignmentBlock */
    fun insertAlignmentTag(alignment: String) = insertAlignmentBlock(alignment)

    /**
     * Inserta un tramo de texto con una tipografía específica (serif, sans, mono, cursive).
     * Si hay texto seleccionado, lo envuelve con [font:x]...[/font].
     * Si no hay texto seleccionado, inserta las etiquetas con el cursor posicionado en su interior.
     */
    fun insertFontBlock(fontStyle: String) {
        val fontLower = when (fontStyle.uppercase()) {
            "SANS_SERIF", "SANS" -> "sans"
            "MONOSPACE", "MONO" -> "mono"
            "CURSIVE" -> "cursive"
            else -> "serif"
        }
        val tfv = _editorTextFieldValue.value
        val text = tfv.text
        val sel = tfv.selection

        val openTag = "[font:$fontLower]"
        val closeTag = "[/font]"

        if (sel.min != sel.max) {
            val min = sel.min.coerceIn(0, text.length)
            val max = sel.max.coerceIn(0, text.length)
            val selected = text.substring(min, max)
            val newText = text.substring(0, min) + openTag + selected + closeTag + text.substring(max)
            val newCursor = min + openTag.length + selected.length + closeTag.length
            onTextFieldValueChange(TextFieldValue(newText, TextRange(newCursor)))
        } else {
            val cursor = sel.start.coerceIn(0, text.length)
            val newText = text.substring(0, cursor) + openTag + closeTag + text.substring(cursor)
            val newCursor = cursor + openTag.length
            onTextFieldValueChange(TextFieldValue(newText, TextRange(newCursor)))
        }
        saveImmediately()
    }

    /** Alias conveniente para insertFontBlock */
    fun insertFontTag(fontStyle: String) = insertFontBlock(fontStyle)

    /**
     * Actualiza la alineación predeterminada de todo el documento (Alineación Cuádruple).
     */
    fun updateGlobalAlignment(newAlignment: String) {
        updatePageSettings(alignment = newAlignment)
    }

    /** Alias conveniente para updateGlobalAlignment */
    fun setGlobalAlignment(newAlignment: String) = updateGlobalAlignment(newAlignment)

    /**
     * Actualiza la tipografía global predeterminada de todo el documento.
     */
    fun updateGlobalFont(newFont: String) {
        updatePageSettings(fontStyle = newFont)
    }

    /**
     * Inserta un salto de página físico explícito.
     */
    fun insertPageBreak() {
        val current = _editorContent.value
        val prefix = if (current.isEmpty() || current.endsWith("\n")) "" else "\n"
        onContentChanged(current + prefix + "\n[--- Salto de Página ---]\n\n")
    }

    /**
     * Inserta una viñeta al inicio de la línea o en el cursor.
     */
    fun insertBullet() {
        val current = _editorContent.value
        val insertion = "\n• "
        onContentChanged(if (current.isEmpty()) "• " else current + insertion)
    }

    /**
     * Inserta una sangría tipo tabulador.
     */
    fun insertIndent() {
        val current = _editorContent.value
        val indent = "    "
        onContentChanged(current + indent)
    }

    /**
     * Inserta la fecha actual formateada en español.
     */
    fun insertCurrentDate() {
        val formatter = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        val dateString = formatter.format(Date())
        val current = _editorContent.value
        val separator = if (current.endsWith("\n") || current.isEmpty()) "" else "\n"
        onContentChanged(current + separator + dateString + "\n")
    }

    /**
     * Inserta una línea divisoria de página.
     */
    fun insertDivider() {
        val current = _editorContent.value
        val divider = "\n────────────────────────────\n"
        onContentChanged(current + divider)
    }

    /**
     * Actualiza la configuración de estilo de la hoja (papel, fuente, tamaño, interlineado, margen, alineación, tamaño de hoja y palabras por hoja).
     */
    fun updatePageSettings(
        paperType: String? = null,
        fontStyle: String? = null,
        fontSize: Int? = null,
        lineSpacing: Float? = null,
        marginStyle: String? = null,
        alignment: String? = null,
        pageSize: String? = null,
        wordsPerPage: Int? = null
    ) {
        val current = _activeDocument.value ?: return
        val updated = current.copy(
            paperType = paperType ?: current.paperType,
            fontStyle = fontStyle ?: current.fontStyle,
            fontSize = fontSize ?: current.fontSize,
            lineSpacing = lineSpacing ?: current.lineSpacing,
            marginStyle = marginStyle ?: current.marginStyle,
            alignment = alignment ?: current.alignment,
            pageSize = pageSize ?: current.pageSize,
            wordsPerPage = wordsPerPage ?: current.wordsPerPage,
            updatedAt = System.currentTimeMillis()
        )
        _activeDocument.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDocument(updated)
        }
    }

    /**
     * Cambia el formato de hoja (A4, Carta, Legal, A5, Personalizado) y sincroniza su capacidad de palabras.
     */
    fun updatePageFormat(pageSize: String, customWordsPerPage: Int? = null) {
        val format = PageFormat.fromId(pageSize)
        val targetWords = customWordsPerPage ?: format.defaultWordsLimit
        updatePageSettings(pageSize = format.id, wordsPerPage = targetWords)
    }

    /**
     * Ajusta el límite de palabras por hoja física para el modo de auto-desborde a nuevas hojas.
     */
    fun updateWordsPerPageLimit(wordsLimit: Int) {
        val safeWords = wordsLimit.coerceIn(50, 1500)
        updatePageSettings(pageSize = "CUSTOM", wordsPerPage = safeWords)
    }

    /**
     * Crea un nuevo documento en blanco o con plantilla.
     */
    fun createNewDocument(templateTitle: String = "Nueva Hoja", initialContent: String = "", onCreated: (Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val newDoc = DocumentEntity(
                title = templateTitle,
                content = initialContent,
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
            val newId = repository.insertDocument(newDoc)
            viewModelScope.launch(Dispatchers.Main) {
                onCreated(newId)
            }
        }
    }

    /**
     * Duplica un documento existente.
     */
    fun duplicateDocument(document: DocumentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val copy = document.copy(
                id = 0,
                title = "${document.title} (Copia)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertDocument(copy)
        }
    }

    /**
     * Elimina un documento por su entidad.
     */
    fun deleteDocument(document: DocumentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDocument(document)
            if (_activeDocument.value?.id == document.id) {
                _activeDocument.value = null
            }
        }
    }

    /**
     * Calcula estadísticas de palabras del texto.
     */
    fun getWordCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    }

    /**
     * Calcula estadísticas de caracteres del texto.
     */
    fun getCharCount(text: String): Int {
        return text.length
    }

    /**
     * Calcula la cantidad de páginas estimadas (aprox. 300 palabras por página estándar).
     */
    fun getEstimatedPages(text: String): Int {
        val words = getWordCount(text)
        return maxOf(1, (words / 300) + (if (words % 300 > 0) 1 else 0))
    }

    /**
     * Calcula la cantidad de párrafos del documento.
     */
    fun getParagraphCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.split("\n\n", "\n").count { it.trim().isNotEmpty() }
    }

    /**
     * Calcula la cantidad de oraciones aproximadas.
     */
    fun getSentenceCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.split(Regex("[.!?]+\\s*")).count { it.trim().isNotEmpty() }
    }

    /**
     * Calcula el tiempo estimado de lectura en minutos (a 200 palabras por minuto).
     */
    fun getReadingTimeMinutes(text: String): Int {
        val words = getWordCount(text)
        return maxOf(1, (words + 199) / 200)
    }

    /**
     * Calcula el tiempo estimado de discurso oral en minutos (a 130 palabras por minuto).
     */
    fun getSpeakingTimeMinutes(text: String): Int {
        val words = getWordCount(text)
        return maxOf(1, (words + 129) / 130)
    }
}
