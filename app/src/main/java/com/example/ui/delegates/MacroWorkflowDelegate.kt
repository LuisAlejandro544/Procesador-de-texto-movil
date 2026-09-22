package com.example.ui.delegates

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.macro.MacroEntity
import com.example.data.macro.MacroRepository
import com.example.util.MacroEngine
import com.example.util.MacroExecutionContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Delegado especializado para el Sistema de Macros, Variables Dinámicas y Automatizaciones.
 * 
 * Gestiona el panel inferior modal, filtrado por categorías, ejecución con expansión de
 * variables dinámicas ({FECHA}, {TITULO}, etc.), persistencia de macros personalizadas
 * y detección reactiva de disparadores (:acta:) al escribir.
 */
class MacroWorkflowDelegate(
    private val scope: CoroutineScope,
    private val macroRepository: MacroRepository
) {

    private val _isMacroSheetVisible = MutableStateFlow(false)
    val isMacroSheetVisible: StateFlow<Boolean> = _isMacroSheetVisible.asStateFlow()

    private val _selectedMacroCategory = MutableStateFlow("TODAS")
    val selectedMacroCategory: StateFlow<String> = _selectedMacroCategory.asStateFlow()

    fun setMacroSheetVisible(visible: Boolean) {
        _isMacroSheetVisible.value = visible
    }

    fun selectMacroCategory(category: String) {
        _selectedMacroCategory.value = category
    }

    /**
     * Ejecuta una macro evaluando sus variables dinámicas e insertando el resultado en la hoja activa.
     */
    fun executeMacro(
        macro: MacroEntity,
        clipboardText: String,
        editorTitle: String,
        activeDocTitle: String?,
        pageSize: String?,
        currentContent: String,
        currentTfv: TextFieldValue,
        onContentUpdated: (String, TextFieldValue, String) -> Unit
    ) {
        val fullText = currentTfv.text
        val selMin = currentTfv.selection.min.coerceIn(0, fullText.length)
        val selMax = currentTfv.selection.max.coerceIn(0, fullText.length)
        val selectedText = if (selMin < selMax) fullText.substring(selMin, selMax) else ""

        val context = MacroExecutionContext(
            documentTitle = editorTitle.ifBlank { activeDocTitle ?: "Documento Sin Título" },
            authorName = "Redactor DocuSheet",
            pageFormat = pageSize ?: "A4",
            currentPage = 1,
            totalPages = DocumentStatsCalculator.getEstimatedPages(currentContent),
            totalWords = DocumentStatsCalculator.getWordCount(currentContent),
            selectedText = selectedText,
            clipboardText = clipboardText
        )

        val expandedText = MacroEngine.evaluateTemplate(macro.templateContent, context)

        val newText = if (selMin < selMax) {
            fullText.substring(0, selMin) + expandedText + fullText.substring(selMax)
        } else {
            val cursor = currentTfv.selection.start.coerceIn(0, fullText.length)
            fullText.substring(0, cursor) + expandedText + fullText.substring(cursor)
        }

        val newCursorPos = if (selMin < selMax) {
            selMin + expandedText.length
        } else {
            currentTfv.selection.start.coerceIn(0, fullText.length) + expandedText.length
        }

        val newTfv = TextFieldValue(
            text = newText,
            selection = TextRange(newCursorPos)
        )
        val msg = "Macro aplicada: «${macro.name}»"
        onContentUpdated(newText, newTfv, msg)
    }

    /**
     * Guarda una macro personalizada creada por el usuario con variables dinámicas.
     */
    fun createCustomMacro(
        name: String,
        description: String,
        triggerKeyword: String,
        category: String,
        templateContent: String,
        iconName: String = "description",
        onMessage: (String) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            val cleanTrigger = if (triggerKeyword.isNotBlank()) {
                val trimmed = triggerKeyword.trim()
                if (!trimmed.startsWith(":")) ":$trimmed:" else trimmed
            } else {
                ""
            }
            val newMacro = MacroEntity(
                name = name.trim().ifBlank { "Macro Personalizada" },
                description = description.trim().ifBlank { "Plantilla creada por el usuario" },
                triggerKeyword = cleanTrigger,
                category = category.trim().ifBlank { "MIS MACROS" },
                templateContent = templateContent,
                isPredefined = false,
                iconName = iconName
            )
            macroRepository.insertMacro(newMacro)
            onMessage("Macro «${newMacro.name}» guardada")
        }
    }

    /**
     * Elimina una macro personalizada (las predefinidas del sistema están protegidas).
     */
    fun deleteCustomMacro(macro: MacroEntity, onMessage: (String) -> Unit) {
        scope.launch(Dispatchers.IO) {
            if (!macro.isPredefined) {
                macroRepository.deleteMacro(macro)
                onMessage("Macro «${macro.name}» eliminada")
            } else {
                onMessage("Las macros predefinidas del sistema no pueden eliminarse")
            }
        }
    }

    /**
     * Detecta y expande un disparador de macro inmediatamente anterior al cursor (ej. ":acta:").
     */
    fun checkAndExpandMacroTrigger(
        allMacros: List<MacroEntity>,
        clipboardText: String,
        editorTitle: String,
        activeDocTitle: String?,
        pageSize: String?,
        currentContent: String,
        currentTfv: TextFieldValue,
        onContentUpdated: (String, TextFieldValue, String) -> Unit
    ): Boolean {
        val text = currentTfv.text
        val cursor = currentTfv.selection.start.coerceIn(0, text.length)
        val textBefore = text.substring(0, cursor)

        val triggerMatch = MacroEngine.detectTriggerBeforeCursor(textBefore) ?: return false
        val (trigger, length) = triggerMatch

        val matchingMacro = allMacros.find { it.triggerKeyword.equals(trigger, ignoreCase = true) } ?: return false

        val startOfTrigger = cursor - length
        val context = MacroExecutionContext(
            documentTitle = editorTitle.ifBlank { activeDocTitle ?: "Documento Sin Título" },
            authorName = "Redactor DocuSheet",
            pageFormat = pageSize ?: "A4",
            currentPage = 1,
            totalPages = DocumentStatsCalculator.getEstimatedPages(currentContent),
            totalWords = DocumentStatsCalculator.getWordCount(currentContent),
            selectedText = "",
            clipboardText = clipboardText
        )

        val expanded = MacroEngine.evaluateTemplate(matchingMacro.templateContent, context)
        val newText = text.substring(0, startOfTrigger) + expanded + text.substring(cursor)
        val newCursor = startOfTrigger + expanded.length

        val newTfv = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
        val msg = "Disparador «$trigger» expandido"
        onContentUpdated(newText, newTfv, msg)
        return true
    }
}
