package com.example.ui

import android.app.Application
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DocumentEntity
import com.example.data.DocumentRepository
import com.example.data.macro.MacroEntity
import com.example.data.macro.MacroRepository
import com.example.data.synonym.ThesaurusRepository
import com.example.ui.components.ActiveTableData
import com.example.ui.delegates.DocumentStatsCalculator
import com.example.ui.delegates.MacroWorkflowDelegate
import com.example.ui.delegates.SearchAndRadarDelegate
import com.example.ui.delegates.TextEditingHistoryManager
import com.example.util.CacheStats
import com.example.util.DocuSheetCacheManager
import com.example.util.PageFormat
import com.example.util.SearchMatch
import com.example.util.StyleRadarEngine
import com.example.util.StyleRadarReport
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
 * Arquitectura Modularizada:
 * - Persistencia reactiva de documentos mediante corrutinas (Room)
 * - Guardado automático diferido para fluidez al escribir
 * - Delegado de historial y transposición de PC ([TextEditingHistoryManager])
 * - Delegado de buscador, radar de estilo y sinónimos ([SearchAndRadarDelegate])
 * - Delegado de macros y automatizaciones dinámicas ([MacroWorkflowDelegate])
 * - Calculador puro de estadísticas cuantitativas ([DocumentStatsCalculator])
 */
class DocumentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DocumentRepository
    private val thesaurusRepository: ThesaurusRepository
    private val macroRepository: MacroRepository
    private val styleRadarEngine = StyleRadarEngine()

    // --- Delegados Especializados de Negocio ---
    private val historyManager = TextEditingHistoryManager()
    private val searchDelegate: SearchAndRadarDelegate
    private val macroDelegate: MacroWorkflowDelegate

    val allDocuments: StateFlow<List<DocumentEntity>>

    // ==========================================================================
    // Estado del Sistema de Macros y Plantillas Automatizadas
    // ==========================================================================
    val allMacros: StateFlow<List<MacroEntity>>
    val isMacroSheetVisible: StateFlow<Boolean>
    val selectedMacroCategory: StateFlow<String>

    // ==========================================================================
    // Estado del Buscador Avanzado, Radar de Redundancia y Carrusel de Sinónimos
    // ==========================================================================
    val isEditorSearchVisible: StateFlow<Boolean>
    val editorSearchQuery: StateFlow<String>
    val editorReplaceQuery: StateFlow<String>
    val isReplaceBarExpanded: StateFlow<Boolean>
    val editorSearchMatches: StateFlow<List<SearchMatch>>
    val currentSearchMatchIndex: StateFlow<Int>
    val currentStyleRadarReport: StateFlow<StyleRadarReport?>
    val currentSynonyms: StateFlow<List<String>>
    val isAnalyzingStyle: StateFlow<Boolean>

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

    // Historial para Deshacer / Rehacer conectado al delegado
    val canUndo: StateFlow<Boolean> = historyManager.canUndo
    val canRedo: StateFlow<Boolean> = historyManager.canRedo
    val markedSwapBlock: StateFlow<MarkedSwapBlock?> = historyManager.markedSwapBlock

    // Mensaje informativo temporal de operaciones de edición de PC
    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage = _userFeedbackMessage.asStateFlow()

    // Estado reactivo para el Asistente Visual de Edición de Tablas
    private val _activeEditingTable = MutableStateFlow<ActiveTableData?>(null)
    val activeEditingTable: StateFlow<ActiveTableData?> = _activeEditingTable.asStateFlow()

    // Estadísticas cuantitativas de almacenamiento en caché y optimización
    private val _cacheStats = MutableStateFlow(CacheStats())
    val cacheStats = _cacheStats.asStateFlow()

    private var autoSaveJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = DocumentRepository(database.documentDao())
        thesaurusRepository = ThesaurusRepository(database.synonymDao())
        macroRepository = MacroRepository(database.macroDao())

        searchDelegate = SearchAndRadarDelegate(viewModelScope, thesaurusRepository, styleRadarEngine)
        macroDelegate = MacroWorkflowDelegate(viewModelScope, macroRepository)

        // Enlace reactivo a los flujos del delegado de búsqueda
        isEditorSearchVisible = searchDelegate.isEditorSearchVisible
        editorSearchQuery = searchDelegate.editorSearchQuery
        editorReplaceQuery = searchDelegate.editorReplaceQuery
        isReplaceBarExpanded = searchDelegate.isReplaceBarExpanded
        editorSearchMatches = searchDelegate.editorSearchMatches
        currentSearchMatchIndex = searchDelegate.currentSearchMatchIndex
        currentStyleRadarReport = searchDelegate.currentStyleRadarReport
        currentSynonyms = searchDelegate.currentSynonyms
        isAnalyzingStyle = searchDelegate.isAnalyzingStyle

        // Enlace reactivo a los flujos del delegado de macros
        isMacroSheetVisible = macroDelegate.isMacroSheetVisible
        selectedMacroCategory = macroDelegate.selectedMacroCategory

        allDocuments = repository.allDocuments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allMacros = macroRepository.allMacros.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch(Dispatchers.IO) {
            thesaurusRepository.ensureInitialized()
            DocuSheetCacheManager.autoPruneIfExceeded(application, emptyList())
            refreshCacheStats()
        }
    }

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
                historyManager.reset(it.content)
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
            historyManager.recordContentChange(_editorContent.value)
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
        val oldText = _editorContent.value
        _editorTextFieldValue.value = newValue
        if (newValue.text != oldText) {
            onContentChanged(newValue.text)
            // Si el usuario escribe y termina un disparador con ':' o espacio, expandirlo automáticamente
            if (newValue.text.length > oldText.length) {
                val cursor = newValue.selection.start.coerceIn(0, newValue.text.length)
                if (cursor > 0) {
                    val lastChar = newValue.text[cursor - 1]
                    if (lastChar == ':' || lastChar == ' ' || lastChar == '\n') {
                        checkAndExpandMacroTrigger()
                    }
                }
            }
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
    // Operaciones de Arrastrar/Mover e Intercambiar Texto (Delegado de Historial)
    // ==========================================================================

    fun markCurrentSelectionForSwap() {
        val msg = historyManager.markCurrentSelectionForSwap(_editorTextFieldValue.value)
        _userFeedbackMessage.value = msg
    }

    fun clearMarkedSwapBlock() {
        historyManager.clearMarkedSwapBlock()
        _userFeedbackMessage.value = "Intercambio cancelado"
    }

    fun executeSwapWithMarkedBlock(): Boolean {
        return historyManager.executeSwapWithMarkedBlock(
            textFieldValue = _editorTextFieldValue.value,
            onSuccess = { newTfv, msg ->
                _editorTextFieldValue.value = newTfv
                onContentChanged(newTfv.text)
                _userFeedbackMessage.value = msg
            },
            onError = { msg ->
                _userFeedbackMessage.value = msg
            }
        )
    }

    fun swapParagraphUp() {
        historyManager.swapParagraph(
            textFieldValue = _editorTextFieldValue.value,
            swapUp = true,
            onSuccess = { newTfv, msg ->
                _editorTextFieldValue.value = newTfv
                onContentChanged(newTfv.text)
                _userFeedbackMessage.value = msg
            },
            onError = { msg -> _userFeedbackMessage.value = msg }
        )
    }

    fun swapParagraphDown() {
        historyManager.swapParagraph(
            textFieldValue = _editorTextFieldValue.value,
            swapUp = false,
            onSuccess = { newTfv, msg ->
                _editorTextFieldValue.value = newTfv
                onContentChanged(newTfv.text)
                _userFeedbackMessage.value = msg
            },
            onError = { msg -> _userFeedbackMessage.value = msg }
        )
    }

    fun moveSelectionToStart() {
        historyManager.moveSelection(
            textFieldValue = _editorTextFieldValue.value,
            targetPosition = 0,
            label = "al inicio",
            onSuccess = { newTfv, msg ->
                _editorTextFieldValue.value = newTfv
                onContentChanged(newTfv.text)
                _userFeedbackMessage.value = msg
            },
            onError = { msg -> _userFeedbackMessage.value = msg }
        )
    }

    fun moveSelectionToEnd() {
        historyManager.moveSelection(
            textFieldValue = _editorTextFieldValue.value,
            targetPosition = _editorTextFieldValue.value.text.length,
            label = "al final",
            onSuccess = { newTfv, msg ->
                _editorTextFieldValue.value = newTfv
                onContentChanged(newTfv.text)
                _userFeedbackMessage.value = msg
            },
            onError = { msg -> _userFeedbackMessage.value = msg }
        )
    }

    fun moveSelectionToTarget(targetPosition: Int) {
        historyManager.moveSelection(
            textFieldValue = _editorTextFieldValue.value,
            targetPosition = targetPosition,
            label = "en la nueva posición",
            onSuccess = { newTfv, msg ->
                _editorTextFieldValue.value = newTfv
                onContentChanged(newTfv.text)
                _userFeedbackMessage.value = msg
            },
            onError = { msg -> _userFeedbackMessage.value = msg }
        )
    }

    fun swapSelectionWithClipboard(clipboardText: String): String {
        return historyManager.swapSelectionWithClipboard(
            textFieldValue = _editorTextFieldValue.value,
            clipboardText = clipboardText,
            onSuccess = { newTfv, msg ->
                _editorTextFieldValue.value = newTfv
                onContentChanged(newTfv.text)
                _userFeedbackMessage.value = msg
            }
        )
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
        val previous = historyManager.undo(_editorContent.value)
        if (previous != null) {
            _editorContent.value = previous
            _editorTextFieldValue.value = TextFieldValue(previous, TextRange(previous.length))
            scheduleAutoSave()
        }
    }

    /**
     * Rehace el texto deshecho.
     */
    fun redo() {
        val next = historyManager.redo(_editorContent.value)
        if (next != null) {
            _editorContent.value = next
            _editorTextFieldValue.value = TextFieldValue(next, TextRange(next.length))
            scheduleAutoSave()
        }
    }

    fun toggleReadOnlyMode() {
        _isReadOnlyMode.value = !_isReadOnlyMode.value
    }

    fun cyclePageZoom() {
        _pageZoom.value = when (_pageZoom.value) {
            1.0f -> 1.15f
            1.15f -> 0.85f
            else -> 1.0f
        }
    }

    fun toggleCascadeMode() {
        _isCascadeMode.value = !_isCascadeMode.value
    }

    fun setDailyGoal(goal: Int) {
        _dailyGoalWords.value = goal
    }

    // --- Formatos Rápidos de Redacción Editorial ---

    fun insertHeading1() {
        val current = _editorContent.value
        val prefix = if (current.isEmpty() || current.endsWith("\n")) "" else "\n\n"
        onContentChanged(current + prefix + "# TÍTULO PRINCIPAL\n")
    }

    fun insertHeading2() {
        val current = _editorContent.value
        val prefix = if (current.isEmpty() || current.endsWith("\n")) "" else "\n\n"
        onContentChanged(current + prefix + "## Subtítulo o Sección\n")
    }

    fun insertHeading3() {
        val current = _editorContent.value
        val prefix = if (current.isEmpty() || current.endsWith("\n")) "" else "\n\n"
        onContentChanged(current + prefix + "### Apartado temático\n")
    }

    fun insertQuote() {
        val current = _editorContent.value
        val prefix = if (current.isEmpty() || current.endsWith("\n")) "" else "\n\n"
        onContentChanged(current + prefix + "> \"Cita reflexiva o idea central a destacar...\"\n")
    }

    fun insertNumberedList() {
        val current = _editorContent.value
        val prefix = if (current.isEmpty() || current.endsWith("\n")) "" else "\n"
        onContentChanged(current + prefix + "1. Primer elemento\n2. Segundo elemento\n")
    }

    fun insertChecklist() {
        val current = _editorContent.value
        val prefix = if (current.isEmpty() || current.endsWith("\n")) "" else "\n"
        onContentChanged(current + prefix + "[ ] Tarea pendiente\n")
    }

    fun insertBold() {
        val current = _editorContent.value
        onContentChanged(current + "**texto en negrita**")
    }

    fun insertItalic() {
        val current = _editorContent.value
        onContentChanged(current + "*texto en cursiva*")
    }

    fun insertUnderline() {
        val current = _editorContent.value
        onContentChanged(current + "<u>texto subrayado</u>")
    }

    fun insertStrikethrough() {
        val current = _editorContent.value
        onContentChanged(current + "~~texto tachado~~")
    }

    fun insertSubscript() {
        val current = _editorContent.value
        onContentChanged(current + "<sub>2</sub>")
    }

    fun insertSuperscript() {
        val current = _editorContent.value
        onContentChanged(current + "<sup>2</sup>")
    }

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
     * Aplica estilo de color, grosor o efecto 3D a la selección o en la posición del cursor.
     */
    fun applyTextStyle3d(prefixTag: String, suffixTag: String) {
        val currentVal = _editorTextFieldValue.value
        val text = currentVal.text
        val selStart = currentVal.selection.min.coerceIn(0, text.length)
        val selEnd = currentVal.selection.max.coerceIn(0, text.length)

        if (selStart != selEnd) {
            val selected = text.substring(selStart, selEnd)
            val newText = text.substring(0, selStart) + prefixTag + selected + suffixTag + text.substring(selEnd)
            val newCursor = selStart + prefixTag.length + selected.length + suffixTag.length
            onTextFieldValueChange(TextFieldValue(newText, TextRange(newCursor)))
        } else {
            val placeholder = "Texto 3D"
            val newText = text.substring(0, selStart) + prefixTag + placeholder + suffixTag + text.substring(selEnd)
            val selStartNew = selStart + prefixTag.length
            val selEndNew = selStartNew + placeholder.length
            onTextFieldValueChange(TextFieldValue(newText, TextRange(selStartNew, selEndNew)))
        }
        saveImmediately()
    }

    /**
     * Inserta un bloque estructurado (Figura geométrica o Diagrama de nodos) en el cursor.
     */
    fun insertBlockAtCursor(blockText: String) {
        val currentVal = _editorTextFieldValue.value
        val text = currentVal.text
        val selStart = currentVal.selection.min.coerceIn(0, text.length)
        val selEnd = currentVal.selection.max.coerceIn(0, text.length)

        val prefix = if (selStart == 0 || (selStart > 0 && text[selStart - 1] == '\n')) "" else "\n"
        val suffix = if (selEnd < text.length && text[selEnd] == '\n') "" else "\n"
        val fullInsert = prefix + blockText.trim() + suffix

        val newText = text.substring(0, selStart) + fullInsert + text.substring(selEnd)
        val newCursor = selStart + fullInsert.length
        onTextFieldValueChange(TextFieldValue(newText, TextRange(newCursor)))
        saveImmediately()
    }

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

        if (style != "classic") {
            sb.append("[table:$style]\n")
        }

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
                selection = TextRange(newCursor)
            )
        )
    }

    /**
     * Verifica rápidamente si la posición del cursor se encuentra dentro de un bloque de tabla.
     */
    fun isCursorInTable(): Boolean {
        return findTableAtCursor() != null
    }

    /**
     * Detecta si la posición actual del cursor o selección se encuentra dentro de una tabla.
     */
    fun findTableAtCursor(): ActiveTableData? {
        val tfv = _editorTextFieldValue.value
        val text = tfv.text
        if (text.isEmpty()) return null

        val cursor = tfv.selection.start.coerceIn(0, text.length)

        // Caso 1: Tabla explícita con directiva [table:style] ... [/table]
        val openTagIndex = text.lastIndexOf("[table:", cursor)
        if (openTagIndex != -1) {
            val closeTagIndex = text.indexOf("[/table]", openTagIndex)
            if (closeTagIndex != -1) {
                val fullEndIndex = (closeTagIndex + "[/table]".length).coerceAtMost(text.length)
                if (cursor in openTagIndex..fullEndIndex) {
                    val tableBlock = text.substring(openTagIndex, fullEndIndex)
                    return parseTableBlock(tableBlock, openTagIndex, fullEndIndex)
                }
            }
        }

        // Caso 2: Tabla Markdown delimitada por líneas con '|'
        val lines = text.lines()
        var charCount = 0
        var cursorLineIdx = -1
        for ((idx, line) in lines.withIndex()) {
            val lineEnd = charCount + line.length
            if (cursor in charCount..lineEnd) {
                cursorLineIdx = idx
                break
            }
            charCount = lineEnd + 1 // Salto de línea '\n'
        }

        if (cursorLineIdx != -1 && lines[cursorLineIdx].trim().startsWith("|")) {
            var startLine = cursorLineIdx
            while (startLine > 0 && lines[startLine - 1].trim().startsWith("|")) {
                startLine--
            }
            var endLine = cursorLineIdx
            while (endLine < lines.size - 1 && lines[endLine + 1].trim().startsWith("|")) {
                endLine++
            }

            var startChar = 0
            for (i in 0 until startLine) {
                startChar += lines[i].length + 1
            }
            var endChar = startChar
            for (i in startLine..endLine) {
                endChar += lines[i].length + (if (i < lines.size - 1) 1 else 0)
            }

            val tableBlock = text.substring(startChar, endChar.coerceAtMost(text.length))
            return parseTableBlock(tableBlock, startChar, endChar)
        }

        return null
    }

    /**
     * Parsea un bloque de texto que contiene una tabla y lo estructura en ActiveTableData.
     */
    private fun parseTableBlock(block: String, startIndex: Int, endIndex: Int): ActiveTableData? {
        val lines = block.lines()
        var style = "classic"
        val rowLines = mutableListOf<String>()

        for (l in lines) {
            val trimmed = l.trim()
            if (trimmed.startsWith("[table:") && trimmed.endsWith("]")) {
                style = trimmed.removePrefix("[table:").removeSuffix("]").trim().ifEmpty { "classic" }
            } else if (trimmed == "[/table]") {
                // etiqueta de cierre
            } else if (trimmed.startsWith("|")) {
                rowLines.add(trimmed)
            }
        }

        if (rowLines.isEmpty()) return null

        var hasHeader = false
        val rows = mutableListOf<List<String>>()

        var i = 0
        while (i < rowLines.size) {
            val current = rowLines[i]
            val isSeparator = current.contains("---") && current.replace("|", "").replace("-", "").replace(":", "").isBlank()

            if (isSeparator) {
                if (rows.isNotEmpty()) {
                    hasHeader = true
                }
                i++
                continue
            }

            val cells = current
                .removePrefix("|")
                .removeSuffix("|")
                .split("|")
                .map { it.trim() }

            if (cells.isNotEmpty()) {
                rows.add(cells)
            }
            i++
        }

        if (rows.isEmpty()) return null

        return ActiveTableData(
            startIndex = startIndex,
            endIndex = endIndex,
            style = style,
            hasHeader = hasHeader,
            rows = rows
        )
    }

    /**
     * Abre el editor visual de tablas si el cursor se encuentra en una tabla existente.
     * Retorna verdadero si se abrió el editor, o falso si no hay tabla bajo el cursor.
     */
    fun openTableEditor(): Boolean {
        val table = findTableAtCursor()
        if (table != null) {
            _activeEditingTable.value = table
            return true
        }
        return false
    }

    /**
     * Cierra el editor visual de tablas.
     */
    fun closeTableEditor() {
        _activeEditingTable.value = null
    }

    /**
     * Aplica los cambios realizados en el Editor Visual de Tablas directamente sobre el documento.
     */
    fun saveTableChanges(updated: ActiveTableData) {
        val sb = StringBuilder()
        if (updated.style != "classic") {
            sb.append("[table:${updated.style}]\n")
        }

        for ((index, row) in updated.rows.withIndex()) {
            sb.append("|")
            for (cell in row) {
                val cleanCell = cell.replace("\n", " ").replace("|", "\\|")
                sb.append(" $cleanCell |")
            }
            sb.append("\n")

            if (index == 0 && updated.hasHeader) {
                sb.append("|")
                for (i in row.indices) {
                    sb.append("---|")
                }
                sb.append("\n")
            }
        }

        if (updated.style != "classic") {
            sb.append("[/table]\n")
        }

        val newTableText = sb.toString()
        val currentText = _editorTextFieldValue.value.text

        val safeStart = updated.startIndex.coerceIn(0, currentText.length)
        val safeEnd = updated.endIndex.coerceIn(safeStart, currentText.length)

        val newFullText = currentText.substring(0, safeStart) + newTableText + currentText.substring(safeEnd)
        val newCursor = safeStart + newTableText.length

        onTextFieldValueChange(
            TextFieldValue(
                text = newFullText,
                selection = TextRange(newCursor)
            )
        )
        saveImmediately()
        _activeEditingTable.value = null
    }

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

    fun insertAlignmentTag(alignment: String) = insertAlignmentBlock(alignment)

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

    fun insertFontTag(fontStyle: String) = insertFontBlock(fontStyle)

    fun updateGlobalAlignment(newAlignment: String) {
        updatePageSettings(alignment = newAlignment)
    }

    fun setGlobalAlignment(newAlignment: String) = updateGlobalAlignment(newAlignment)

    fun updateGlobalFont(newFont: String) {
        updatePageSettings(fontStyle = newFont)
    }

    fun insertPageBreak() {
        val current = _editorContent.value
        val prefix = if (current.isEmpty() || current.endsWith("\n")) "" else "\n"
        onContentChanged(current + prefix + "\n[--- Salto de Página ---]\n\n")
    }

    fun insertBullet() {
        val current = _editorContent.value
        val insertion = "\n• "
        onContentChanged(if (current.isEmpty()) "• " else current + insertion)
    }

    fun insertIndent() {
        val current = _editorContent.value
        val indent = "    "
        onContentChanged(current + indent)
    }

    fun insertCurrentDate() {
        val formatter = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        val dateString = formatter.format(Date())
        val current = _editorContent.value
        val separator = if (current.endsWith("\n") || current.isEmpty()) "" else "\n"
        onContentChanged(current + separator + dateString + "\n")
    }

    fun insertDivider() {
        val current = _editorContent.value
        val divider = "\n────────────────────────────\n"
        onContentChanged(current + divider)
    }

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

    fun updatePageFormat(pageSize: String, customWordsPerPage: Int? = null) {
        val format = PageFormat.fromId(pageSize)
        val targetWords = customWordsPerPage ?: format.defaultWordsLimit
        updatePageSettings(pageSize = format.id, wordsPerPage = targetWords)
    }

    fun updateWordsPerPageLimit(wordsLimit: Int) {
        val safeWords = wordsLimit.coerceIn(50, 1500)
        updatePageSettings(pageSize = "CUSTOM", wordsPerPage = safeWords)
    }

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
     * Importa un archivo externo (.docx, .rtf, .tex, .md, .txt) desde el almacenamiento,
     * lo guarda como nuevo documento en Room y notifica el nuevo ID para abrirlo.
     */
    fun importDocument(
        context: android.content.Context,
        uri: android.net.Uri,
        onSuccess: (Long, String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imported = com.example.util.DocumentImporter.importFromUri(context, uri)
                val newDoc = DocumentEntity(
                    title = imported.title,
                    content = imported.content,
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
                withContext(Dispatchers.Main) {
                    onSuccess(newId, imported.formatName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Error al importar el documento")
                }
            }
        }
    }

    /**
     * Importa un archivo externo e inserta su contenido en la posición activa del editor.
     */
    fun importIntoCurrentDocument(
        context: android.content.Context,
        uri: android.net.Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imported = com.example.util.DocumentImporter.importFromUri(context, uri)
                withContext(Dispatchers.Main) {
                    val current = _editorContent.value
                    val separator = if (current.isBlank() || current.endsWith("\n")) "" else "\n\n"
                    onContentChanged(current + separator + imported.content)
                    saveImmediately()
                    onSuccess(imported.formatName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Error al importar el archivo")
                }
            }
        }
    }

    fun deleteDocument(document: DocumentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDocument(document)
            if (_activeDocument.value?.id == document.id) {
                _activeDocument.value = null
            }
        }
    }

    // --- Estadísticas y Métricas (Delegadas a DocumentStatsCalculator) ---

    fun getWordCount(text: String): Int = DocumentStatsCalculator.getWordCount(text)

    fun getCharCount(text: String): Int = DocumentStatsCalculator.getCharCount(text)

    fun getEstimatedPages(text: String): Int = DocumentStatsCalculator.getEstimatedPages(text)

    fun getParagraphCount(text: String): Int = DocumentStatsCalculator.getParagraphCount(text)

    fun getSentenceCount(text: String): Int = DocumentStatsCalculator.getSentenceCount(text)

    fun getReadingTimeMinutes(text: String): Int = DocumentStatsCalculator.getReadingTimeMinutes(text)

    fun getSpeakingTimeMinutes(text: String): Int = DocumentStatsCalculator.getSpeakingTimeMinutes(text)

    // ==========================================================================
    // Módulo: Buscador Avanzado, Radar de Redundancia y Carrusel de Sinónimos
    // ==========================================================================

    fun openEditorSearch(initialWord: String? = null) {
        val wordsPerPage = _activeDocument.value?.wordsPerPage ?: 350
        searchDelegate.openSearch(
            initialWord = initialWord,
            currentTfv = _editorTextFieldValue.value,
            wordsPerPage = wordsPerPage,
            onHighlightMatch = { highlightAndSelectMatch(it) }
        )
    }

    fun closeEditorSearch() {
        searchDelegate.closeSearch()
    }

    fun toggleReplaceBar() {
        searchDelegate.toggleReplaceBar()
    }

    fun onEditorSearchQueryChanged(newQuery: String) {
        val wordsPerPage = _activeDocument.value?.wordsPerPage ?: 350
        searchDelegate.onSearchQueryChanged(
            newQuery = newQuery,
            content = _editorContent.value,
            wordsPerPage = wordsPerPage,
            onHighlightMatch = { highlightAndSelectMatch(it) }
        )
    }

    fun onEditorReplaceQueryChanged(newReplace: String) {
        searchDelegate.onReplaceQueryChanged(newReplace)
    }

    fun goToNextMatch() {
        searchDelegate.goToNextMatch { highlightAndSelectMatch(it) }
    }

    fun goToPreviousMatch() {
        searchDelegate.goToPreviousMatch { highlightAndSelectMatch(it) }
    }

    private fun highlightAndSelectMatch(match: SearchMatch) {
        val text = _editorContent.value
        val start = match.startOffset.coerceIn(0, text.length)
        val end = match.endOffset.coerceIn(0, text.length)
        if (start <= end) {
            _editorTextFieldValue.value = _editorTextFieldValue.value.copy(
                selection = TextRange(start, end)
            )
        }
    }

    fun applySynonymToCurrentMatch(synonym: String) {
        val wordsPerPage = _activeDocument.value?.wordsPerPage ?: 350
        searchDelegate.applySynonym(
            synonym = synonym,
            currentText = _editorContent.value,
            wordsPerPage = wordsPerPage,
            onContentUpdated = { newText, newTfv, msg ->
                onContentChanged(newText)
                _editorTextFieldValue.value = newTfv
                _userFeedbackMessage.value = msg
            },
            onHighlightMatch = { highlightAndSelectMatch(it) }
        )
    }

    fun replaceCurrentMatch() {
        val wordsPerPage = _activeDocument.value?.wordsPerPage ?: 350
        searchDelegate.replaceCurrent(
            currentText = _editorContent.value,
            wordsPerPage = wordsPerPage,
            onContentUpdated = { newText, newTfv, msg ->
                onContentChanged(newText)
                _editorTextFieldValue.value = newTfv
                _userFeedbackMessage.value = msg
            },
            onHighlightMatch = { highlightAndSelectMatch(it) }
        )
    }

    fun replaceAllMatches() {
        val wordsPerPage = _activeDocument.value?.wordsPerPage ?: 350
        searchDelegate.replaceAll(
            currentText = _editorContent.value,
            wordsPerPage = wordsPerPage,
            onContentUpdated = { newText, newTfv, msg ->
                onContentChanged(newText)
                _editorTextFieldValue.value = newTfv
                _userFeedbackMessage.value = msg
            },
            onHighlightMatch = { highlightAndSelectMatch(it) }
        )
    }

    // ==========================================================================
    // Operaciones del Sistema de Macros y Plantillas Automatizadas
    // ==========================================================================

    fun setMacroSheetVisible(visible: Boolean) {
        macroDelegate.setMacroSheetVisible(visible)
    }

    fun selectMacroCategory(category: String) {
        macroDelegate.selectMacroCategory(category)
    }

    fun executeMacro(macro: MacroEntity, clipboardText: String = "") {
        val activeDoc = _activeDocument.value
        macroDelegate.executeMacro(
            macro = macro,
            clipboardText = clipboardText,
            editorTitle = _editorTitle.value,
            activeDocTitle = activeDoc?.title,
            pageSize = activeDoc?.pageSize,
            currentContent = _editorContent.value,
            currentTfv = _editorTextFieldValue.value,
            onContentUpdated = { newText, newTfv, msg ->
                onContentChanged(newText)
                _editorTextFieldValue.value = newTfv
                _userFeedbackMessage.value = msg
            }
        )
    }

    fun createCustomMacro(
        name: String,
        description: String,
        triggerKeyword: String,
        category: String,
        templateContent: String,
        iconName: String = "description"
    ) {
        macroDelegate.createCustomMacro(
            name = name,
            description = description,
            triggerKeyword = triggerKeyword,
            category = category,
            templateContent = templateContent,
            iconName = iconName,
            onMessage = { _userFeedbackMessage.value = it }
        )
    }

    fun deleteCustomMacro(macro: MacroEntity) {
        macroDelegate.deleteCustomMacro(macro) {
            _userFeedbackMessage.value = it
        }
    }

    fun checkAndExpandMacroTrigger(clipboardText: String = ""): Boolean {
        val activeDoc = _activeDocument.value
        return macroDelegate.checkAndExpandMacroTrigger(
            allMacros = allMacros.value,
            clipboardText = clipboardText,
            editorTitle = _editorTitle.value,
            activeDocTitle = activeDoc?.title,
            pageSize = activeDoc?.pageSize,
            currentContent = _editorContent.value,
            currentTfv = _editorTextFieldValue.value,
            onContentUpdated = { newText, newTfv, msg ->
                onContentChanged(newText)
                _editorTextFieldValue.value = newTfv
                _userFeedbackMessage.value = msg
            }
        )
    }
}
