package com.example.ui

import android.app.Application
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private var autoSaveJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = DocumentRepository(database.documentDao())

        allDocuments = repository.allDocuments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
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
                undoStack.clear()
                redoStack.clear()
                undoStack.add(it.content)
                _canUndo.value = false
                _canRedo.value = false
                _saveStatus.value = "Guardado"
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
            _saveStatus.value = "Guardando..."
            scheduleAutoSave()
        }
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
     * Actualiza la configuración de estilo de la hoja (papel, fuente, tamaño, interlineado, margen).
     */
    fun updatePageSettings(
        paperType: String? = null,
        fontStyle: String? = null,
        fontSize: Int? = null,
        lineSpacing: Float? = null,
        marginStyle: String? = null
    ) {
        val current = _activeDocument.value ?: return
        val updated = current.copy(
            paperType = paperType ?: current.paperType,
            fontStyle = fontStyle ?: current.fontStyle,
            fontSize = fontSize ?: current.fontSize,
            lineSpacing = lineSpacing ?: current.lineSpacing,
            marginStyle = marginStyle ?: current.marginStyle,
            updatedAt = System.currentTimeMillis()
        )
        _activeDocument.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDocument(updated)
        }
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
