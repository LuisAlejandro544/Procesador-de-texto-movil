package com.example.ui.delegates

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.synonym.ThesaurusRepository
import com.example.util.SearchMatch
import com.example.util.StyleRadarEngine
import com.example.util.StyleRadarReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Delegado especializado para el Buscador Avanzado, Radar de Redundancia y Carrusel de Sinónimos.
 * 
 * Gestiona el análisis estilístico en segundo plano mediante Apache Lucene y Thesaurus,
 * permitiendo la navegación de coincidencias, reemplazos atómicos y sustitución léxica de sinónimos.
 */
class SearchAndRadarDelegate(
    private val scope: CoroutineScope,
    private val thesaurusRepository: ThesaurusRepository,
    private val styleRadarEngine: StyleRadarEngine = StyleRadarEngine()
) {

    private val _isEditorSearchVisible = MutableStateFlow(false)
    val isEditorSearchVisible: StateFlow<Boolean> = _isEditorSearchVisible.asStateFlow()

    private val _editorSearchQuery = MutableStateFlow("")
    val editorSearchQuery: StateFlow<String> = _editorSearchQuery.asStateFlow()

    private val _editorReplaceQuery = MutableStateFlow("")
    val editorReplaceQuery: StateFlow<String> = _editorReplaceQuery.asStateFlow()

    private val _isReplaceBarExpanded = MutableStateFlow(false)
    val isReplaceBarExpanded: StateFlow<Boolean> = _isReplaceBarExpanded.asStateFlow()

    private val _editorSearchMatches = MutableStateFlow<List<SearchMatch>>(emptyList())
    val editorSearchMatches: StateFlow<List<SearchMatch>> = _editorSearchMatches.asStateFlow()

    private val _currentSearchMatchIndex = MutableStateFlow(0)
    val currentSearchMatchIndex: StateFlow<Int> = _currentSearchMatchIndex.asStateFlow()

    private val _currentStyleRadarReport = MutableStateFlow<StyleRadarReport?>(null)
    val currentStyleRadarReport: StateFlow<StyleRadarReport?> = _currentStyleRadarReport.asStateFlow()

    private val _currentSynonyms = MutableStateFlow<List<String>>(emptyList())
    val currentSynonyms: StateFlow<List<String>> = _currentSynonyms.asStateFlow()

    private val _isAnalyzingStyle = MutableStateFlow(false)
    val isAnalyzingStyle: StateFlow<Boolean> = _isAnalyzingStyle.asStateFlow()

    fun openSearch(
        initialWord: String?,
        currentTfv: TextFieldValue,
        onHighlightMatch: (SearchMatch) -> Unit,
        wordsPerPage: Int
    ) {
        _isEditorSearchVisible.value = true
        val sel = currentTfv.selection
        val txt = currentTfv.text
        val target = initialWord ?: run {
            val selection = if (!sel.collapsed && sel.min < txt.length && sel.max <= txt.length) {
                txt.substring(sel.min, sel.max).trim()
            } else ""
            if (selection.isNotEmpty() && !selection.contains("\n")) selection else ""
        }

        if (target.isNotEmpty()) {
            _editorSearchQuery.value = target
            onSearchQueryChanged(target, txt, wordsPerPage, onHighlightMatch)
        } else if (_editorSearchQuery.value.isNotEmpty()) {
            onSearchQueryChanged(_editorSearchQuery.value, txt, wordsPerPage, onHighlightMatch)
        }
    }

    fun closeSearch() {
        _isEditorSearchVisible.value = false
        _isReplaceBarExpanded.value = false
        _editorSearchMatches.value = emptyList()
        _currentStyleRadarReport.value = null
        _currentSynonyms.value = emptyList()
    }

    fun toggleReplaceBar() {
        _isReplaceBarExpanded.value = !_isReplaceBarExpanded.value
    }

    fun onSearchQueryChanged(
        newQuery: String,
        content: String,
        wordsPerPage: Int,
        onHighlightMatch: (SearchMatch) -> Unit
    ) {
        _editorSearchQuery.value = newQuery
        if (newQuery.isBlank()) {
            _editorSearchMatches.value = emptyList()
            _currentSearchMatchIndex.value = 0
            _currentStyleRadarReport.value = null
            _currentSynonyms.value = emptyList()
            return
        }
        executeSearchAnalysis(newQuery, content, wordsPerPage, onHighlightMatch)
    }

    fun onReplaceQueryChanged(newReplace: String) {
        _editorReplaceQuery.value = newReplace
    }

    fun executeSearchAnalysis(
        query: String,
        content: String,
        wordsPerPage: Int,
        onHighlightMatch: (SearchMatch) -> Unit
    ) {
        scope.launch(Dispatchers.Default) {
            _isAnalyzingStyle.value = true
            val (matches, report) = styleRadarEngine.analyzeSearch(content, query, wordsPerPage)

            _editorSearchMatches.value = matches
            _currentStyleRadarReport.value = report

            if (matches.isNotEmpty()) {
                val safeIndex = _currentSearchMatchIndex.value.coerceIn(0, matches.size - 1)
                _currentSearchMatchIndex.value = safeIndex
                val targetMatch = matches[safeIndex]
                onHighlightMatch(targetMatch)
                fetchSynonymsForWord(targetMatch.matchedWord, report?.stem)
            } else {
                _currentSearchMatchIndex.value = 0
                _currentSynonyms.value = emptyList()
            }
            _isAnalyzingStyle.value = false
        }
    }

    fun goToNextMatch(onHighlightMatch: (SearchMatch) -> Unit) {
        val matches = _editorSearchMatches.value
        if (matches.isEmpty()) return

        val nextIndex = (_currentSearchMatchIndex.value + 1) % matches.size
        _currentSearchMatchIndex.value = nextIndex
        val match = matches[nextIndex]
        onHighlightMatch(match)
        fetchSynonymsForWord(match.matchedWord, _currentStyleRadarReport.value?.stem)
    }

    fun goToPreviousMatch(onHighlightMatch: (SearchMatch) -> Unit) {
        val matches = _editorSearchMatches.value
        if (matches.isEmpty()) return

        val prevIndex = if (_currentSearchMatchIndex.value - 1 < 0) matches.size - 1 else _currentSearchMatchIndex.value - 1
        _currentSearchMatchIndex.value = prevIndex
        val match = matches[prevIndex]
        onHighlightMatch(match)
        fetchSynonymsForWord(match.matchedWord, _currentStyleRadarReport.value?.stem)
    }

    private fun fetchSynonymsForWord(word: String, stem: String? = null) {
        scope.launch(Dispatchers.IO) {
            val list = thesaurusRepository.getSynonyms(word, stem)
            _currentSynonyms.value = list
        }
    }

    fun applySynonym(
        synonym: String,
        currentText: String,
        wordsPerPage: Int,
        onContentUpdated: (String, TextFieldValue, String) -> Unit,
        onHighlightMatch: (SearchMatch) -> Unit
    ) {
        val matches = _editorSearchMatches.value
        val currentIndex = _currentSearchMatchIndex.value
        if (matches.isEmpty() || currentIndex !in matches.indices) return

        val targetMatch = matches[currentIndex]
        val start = targetMatch.startOffset.coerceIn(0, currentText.length)
        val end = targetMatch.endOffset.coerceIn(0, currentText.length)

        if (start < end) {
            val originalWord = currentText.substring(start, end)
            val formattedSynonym = if (originalWord.firstOrNull()?.isUpperCase() == true) {
                synonym.replaceFirstChar { it.uppercase() }
            } else {
                synonym
            }

            val newText = currentText.substring(0, start) + formattedSynonym + currentText.substring(end)
            val newTfv = TextFieldValue(
                text = newText,
                selection = TextRange(start + formattedSynonym.length)
            )
            val msg = "Sustituido: «$originalWord» → «$formattedSynonym»"
            onContentUpdated(newText, newTfv, msg)
            executeSearchAnalysis(_editorSearchQuery.value, newText, wordsPerPage, onHighlightMatch)
        }
    }

    fun replaceCurrent(
        currentText: String,
        wordsPerPage: Int,
        onContentUpdated: (String, TextFieldValue, String) -> Unit,
        onHighlightMatch: (SearchMatch) -> Unit
    ) {
        val replacement = _editorReplaceQuery.value
        val matches = _editorSearchMatches.value
        val currentIndex = _currentSearchMatchIndex.value
        if (matches.isEmpty() || currentIndex !in matches.indices) return

        val targetMatch = matches[currentIndex]
        val start = targetMatch.startOffset.coerceIn(0, currentText.length)
        val end = targetMatch.endOffset.coerceIn(0, currentText.length)

        if (start <= end) {
            val originalWord = currentText.substring(start, end)
            val newText = currentText.substring(0, start) + replacement + currentText.substring(end)
            val newTfv = TextFieldValue(
                text = newText,
                selection = TextRange(start + replacement.length)
            )
            val msg = "Reemplazado: «$originalWord» → «$replacement»"
            onContentUpdated(newText, newTfv, msg)
            executeSearchAnalysis(_editorSearchQuery.value, newText, wordsPerPage, onHighlightMatch)
        }
    }

    fun replaceAll(
        currentText: String,
        wordsPerPage: Int,
        onContentUpdated: (String, TextFieldValue, String) -> Unit,
        onHighlightMatch: (SearchMatch) -> Unit
    ) {
        val query = _editorSearchQuery.value
        val replacement = _editorReplaceQuery.value
        if (query.isBlank() || currentText.isBlank()) return

        val matches = _editorSearchMatches.value
        if (matches.isEmpty()) return

        val count = matches.size
        val newText = currentText.replace(query, replacement, ignoreCase = true)
        if (newText != currentText) {
            val newTfv = TextFieldValue(newText, TextRange(newText.length))
            val msg = "Se han reemplazado $count coincidencias de «$query»."
            onContentUpdated(newText, newTfv, msg)
            executeSearchAnalysis(query, newText, wordsPerPage, onHighlightMatch)
        }
    }
}
