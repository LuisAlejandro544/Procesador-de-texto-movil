package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DocumentViewModel
import com.example.ui.components.DocuSheetMacroBottomSheet
import com.example.ui.components.DocuSheetPcSelectionBar
import com.example.ui.components.DocuSheetSearchRadarBar
import com.example.ui.components.PaperSheet
import com.example.ui.components.TableInsertDialog
import com.example.ui.screens.editor.EditorFormattingToolbar
import com.example.ui.screens.editor.EditorStatusBar
import com.example.ui.screens.editor.EditorTopBar
import com.example.ui.screens.editor.ImageInsertDialog

/**
 * DocumentEditorScreen: Pantalla principal de escritura en hoja de papel física.
 * 
 * Recrea el entorno de un procesador de texto de PC con:
 * - Área de trabajo tipo escritorio con hojas flotantes en cascada continua ([PaperSheet])
 * - Barra superior modularizada ([EditorTopBar]) con título editable, guardado automático y exportación
 * - Barra de herramientas de formato estructurado ([EditorFormattingToolbar])
 * - Barra de estado en el borde inferior ([EditorStatusBar]) con métricas cuantitativas
 * - Diálogos especializados de inserción ([ImageInsertDialog], [TableInsertDialog])
 * - Barra de selección contextual de PC ([DocuSheetPcSelectionBar]) y panel de búsqueda/radar ([DocuSheetSearchRadarBar])
 * - Panel modal de automatizaciones ([DocuSheetMacroBottomSheet])
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentEditorScreen(
    docId: Long,
    viewModel: DocumentViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: (Long) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(docId) {
        viewModel.loadDocument(docId)
    }

    val activeDoc by viewModel.activeDocument.collectAsStateWithLifecycle()
    val editorContent by viewModel.editorContent.collectAsStateWithLifecycle()
    val editorTextFieldValue by viewModel.editorTextFieldValue.collectAsStateWithLifecycle()
    val editorTitle by viewModel.editorTitle.collectAsStateWithLifecycle()
    val saveStatus by viewModel.saveStatus.collectAsStateWithLifecycle()
    val isReadOnly by viewModel.isReadOnlyMode.collectAsStateWithLifecycle()
    val pageZoom by viewModel.pageZoom.collectAsStateWithLifecycle()
    val isCascadeMode by viewModel.isCascadeMode.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val markedSwapBlock by viewModel.markedSwapBlock.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.userFeedbackMessage.collectAsStateWithLifecycle()

    // Estados del Buscador Avanzado, Radar de Redundancia y Carrusel de Sinónimos
    val isEditorSearchVisible by viewModel.isEditorSearchVisible.collectAsStateWithLifecycle()
    val editorSearchQuery by viewModel.editorSearchQuery.collectAsStateWithLifecycle()
    val editorReplaceQuery by viewModel.editorReplaceQuery.collectAsStateWithLifecycle()
    val isReplaceBarExpanded by viewModel.isReplaceBarExpanded.collectAsStateWithLifecycle()
    val editorSearchMatches by viewModel.editorSearchMatches.collectAsStateWithLifecycle()
    val currentSearchMatchIndex by viewModel.currentSearchMatchIndex.collectAsStateWithLifecycle()
    val currentStyleRadarReport by viewModel.currentStyleRadarReport.collectAsStateWithLifecycle()
    val currentSynonyms by viewModel.currentSynonyms.collectAsStateWithLifecycle()
    val isAnalyzingStyle by viewModel.isAnalyzingStyle.collectAsStateWithLifecycle()

    val isMacroSheetVisible by viewModel.isMacroSheetVisible.collectAsStateWithLifecycle()
    val allMacros by viewModel.allMacros.collectAsStateWithLifecycle()
    val selectedMacroCategory by viewModel.selectedMacroCategory.collectAsStateWithLifecycle()
    val macroSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    var showImageDialog by remember { mutableStateOf(false) }
    var showTableDialog by remember { mutableStateOf(false) }

    val wordCount = viewModel.getWordCount(editorContent)
    val estimatedPages = viewModel.getEstimatedPages(editorContent)
    val readingTime = viewModel.getReadingTimeMinutes(editorContent)

    val doc = activeDoc

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            EditorTopBar(
                context = context,
                title = editorTitle,
                onTitleChange = { viewModel.onTitleChanged(it) },
                saveStatus = saveStatus,
                isCascadeMode = isCascadeMode,
                onToggleCascadeMode = { viewModel.toggleCascadeMode() },
                isReadOnly = isReadOnly,
                onToggleReadOnly = { viewModel.toggleReadOnlyMode() },
                isSearchVisible = isEditorSearchVisible,
                onToggleSearch = {
                    if (isEditorSearchVisible) {
                        viewModel.closeEditorSearch()
                    } else {
                        viewModel.openEditorSearch()
                    }
                },
                onOpenSettings = {
                    viewModel.saveImmediately()
                    onNavigateToSettings(docId)
                },
                onNavigateBack = {
                    viewModel.saveImmediately()
                    onNavigateBack()
                },
                onBeforeExport = { viewModel.saveImmediately() },
                fontStyle = doc?.fontStyle ?: "SERIF",
                content = editorContent
            )
        },
        bottomBar = {
            EditorStatusBar(
                estimatedPages = estimatedPages,
                wordCount = wordCount,
                readingTimeMinutes = readingTime,
                pageZoom = pageZoom
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            // --- Barra de Herramientas de Escritura y Formato Rápido ---
            EditorFormattingToolbar(
                isVisible = !isReadOnly,
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onInsertH1 = { viewModel.insertHeading1() },
                onInsertH2 = { viewModel.insertHeading2() },
                onInsertH3 = { viewModel.insertHeading3() },
                onInsertQuote = { viewModel.insertQuote() },
                onInsertBold = { viewModel.insertBold() },
                onInsertItalic = { viewModel.insertItalic() },
                onInsertUnderline = { viewModel.insertUnderline() },
                onInsertStrikethrough = { viewModel.insertStrikethrough() },
                onInsertSubscript = { viewModel.insertSubscript() },
                onInsertSuperscript = { viewModel.insertSuperscript() },
                onInsertAlignmentTag = { viewModel.insertAlignmentTag(it) },
                onSetGlobalAlignment = { viewModel.setGlobalAlignment(it) },
                onInsertFontTag = { viewModel.insertFontTag(it) },
                onSetGlobalFont = { viewModel.updatePageSettings(fontStyle = it) },
                onOpenImageDialog = { showImageDialog = true },
                onOpenTableDialog = { showTableDialog = true },
                onInsertBullet = { viewModel.insertBullet() },
                onInsertNumberedList = { viewModel.insertNumberedList() },
                onInsertChecklist = { viewModel.insertChecklist() },
                onInsertPageBreak = { viewModel.insertPageBreak() },
                onInsertIndent = { viewModel.insertIndent() },
                onInsertDate = { viewModel.insertCurrentDate() },
                onOpenMacros = { viewModel.setMacroSheetVisible(true) },
                onInsertDivider = { viewModel.insertDivider() },
                onCycleZoom = { viewModel.cyclePageZoom() }
            )

            // --- Barra de Selección Contextual Estilo PC ---
            DocuSheetPcSelectionBar(
                textFieldValue = editorTextFieldValue,
                onValueChange = { viewModel.onTextFieldValueChange(it) },
                onDismissSelection = { viewModel.clearSelection() },
                markedSwapBlock = markedSwapBlock,
                onMarkForSwap = { viewModel.markCurrentSelectionForSwap() },
                onExecuteSwap = { viewModel.executeSwapWithMarkedBlock() },
                onClearMarkedSwap = { viewModel.clearMarkedSwapBlock() },
                onSwapUp = { viewModel.swapParagraphUp() },
                onSwapDown = { viewModel.swapParagraphDown() },
                onMoveToStart = { viewModel.moveSelectionToStart() },
                onMoveToEnd = { viewModel.moveSelectionToEnd() },
                onSwapWithClipboard = { clipText ->
                    val oldText = viewModel.swapSelectionWithClipboard(clipText)
                    if (oldText.isNotEmpty()) {
                        clipboardManager.setText(AnnotatedString(oldText))
                    }
                },
                onSearchAndSynonyms = { word ->
                    viewModel.openEditorSearch(word)
                }
            )

            // --- Panel Flotante: Buscador Avanzado, Radar de Redundancia y Carrusel de Sinónimos Offline ---
            AnimatedVisibility(
                visible = isEditorSearchVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                DocuSheetSearchRadarBar(
                    searchQuery = editorSearchQuery,
                    replaceQuery = editorReplaceQuery,
                    matches = editorSearchMatches,
                    currentIndex = currentSearchMatchIndex,
                    report = currentStyleRadarReport,
                    synonyms = currentSynonyms,
                    isReplaceExpanded = isReplaceBarExpanded,
                    isAnalyzing = isAnalyzingStyle,
                    onSearchQueryChange = { viewModel.onEditorSearchQueryChanged(it) },
                    onReplaceQueryChange = { viewModel.onEditorReplaceQueryChanged(it) },
                    onNextMatch = { viewModel.goToNextMatch() },
                    onPreviousMatch = { viewModel.goToPreviousMatch() },
                    onToggleReplace = { viewModel.toggleReplaceBar() },
                    onReplaceCurrent = { viewModel.replaceCurrentMatch() },
                    onReplaceAll = { viewModel.replaceAllMatches() },
                    onApplySynonym = { viewModel.applySynonymToCurrentMatch(it) },
                    onClose = { viewModel.closeEditorSearch() },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // --- Área de Trabajo tipo Escritorio donde reposa la Hoja ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                doc?.let { d ->
                    Box(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .scale(pageZoom)
                    ) {
                        PaperSheet(
                            content = editorContent,
                            onContentChange = { viewModel.onContentChanged(it) },
                            title = editorTitle,
                            paperType = d.paperType,
                            fontStyle = d.fontStyle,
                            fontSize = d.fontSize,
                            lineSpacing = d.lineSpacing,
                            marginStyle = d.marginStyle,
                            alignment = d.alignment,
                            isReadOnly = isReadOnly,
                            isCascadeMode = isCascadeMode,
                            onAddPage = { viewModel.insertPageBreak() },
                            textFieldValue = editorTextFieldValue,
                            onTextFieldValueChange = { viewModel.onTextFieldValueChange(it) },
                            pageSize = d.pageSize,
                            wordsPerPageLimit = d.wordsPerPage,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // --- Diálogo para Inserción de Imágenes con Ajuste de Hoja (Layout & Wrap) ---
    if (showImageDialog) {
        ImageInsertDialog(
            context = context,
            onDismiss = { showImageDialog = false },
            onInsert = { uri, wrapMode, caption ->
                viewModel.insertImage(uri, wrapMode, caption)
                showImageDialog = false
            }
        )
    }

    // Diálogo interactivo táctil para Configurar e Insertar Tablas Editoriales
    if (showTableDialog) {
        TableInsertDialog(
            onDismiss = { showTableDialog = false },
            onConfirm = { rows, cols, hasHeader, style ->
                viewModel.insertTable(
                    rows = rows,
                    cols = cols,
                    hasHeader = hasHeader,
                    style = style
                )
                showTableDialog = false
            }
        )
    }

    // Panel modal táctil de Macros y Automatizaciones
    if (isMacroSheetVisible) {
        DocuSheetMacroBottomSheet(
            sheetState = macroSheetState,
            macros = allMacros,
            selectedCategory = selectedMacroCategory,
            onCategorySelected = { viewModel.selectMacroCategory(it) },
            onExecuteMacro = { macro, clip ->
                viewModel.executeMacro(macro, clip)
            },
            onCreateMacro = { name, desc, trigger, cat, content ->
                viewModel.createCustomMacro(
                    name = name,
                    description = desc,
                    triggerKeyword = trigger,
                    category = cat,
                    templateContent = content
                )
            },
            onDeleteMacro = { macro ->
                viewModel.deleteCustomMacro(macro)
            },
            onDismiss = {
                viewModel.setMacroSheetVisible(false)
            }
        )
    }
}
