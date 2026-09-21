package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatAlignLeft
import androidx.compose.material.icons.automirrored.outlined.FormatAlignRight
import androidx.compose.material.icons.automirrored.outlined.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.FormatAlignJustify
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Subscript
import androidx.compose.material.icons.outlined.Superscript
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DocumentViewModel
import com.example.ui.components.DocuSheetPcSelectionBar
import com.example.ui.components.PaperSheet
import com.example.ui.components.TableInsertDialog
import com.example.util.DocumentExporter

/**
 * DocumentEditorScreen: Pantalla principal de escritura en hoja de papel física.
 * 
 * Recrea el entorno de un procesador de texto de PC con:
 * - Área de trabajo tipo escritorio con hojas flotantes en cascada continua
 * - Barra superior con título editable, guardado automático y menú de exportación (.pdf / .md)
 * - Barra de herramientas de formato estructurado: Títulos (H1, H2, H3), Citas, Negritas, Cursivas,
 *   Viñetas, Listas numeradas, Tareas, Saltos de página y Sangrías
 * - Barra de estado en el borde inferior con estadísticas en vivo (páginas, palabras, tiempo de lectura y zoom)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    var showExportMenu by remember { mutableStateOf(false) }
    var showAlignMenu by remember { mutableStateOf(false) }
    var showFontMenu by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }
    var showTableDialog by remember { mutableStateOf(false) }
    var imageUrlInput by remember { mutableStateOf("") }
    var imageCaptionInput by remember { mutableStateOf("") }
    var imageWrapMode by remember { mutableStateOf("full") }

    val wordCount = viewModel.getWordCount(editorContent)
    val charCount = viewModel.getCharCount(editorContent)
    val estimatedPages = viewModel.getEstimatedPages(editorContent)
    val readingTime = viewModel.getReadingTimeMinutes(editorContent)

    val doc = activeDoc

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.saveImmediately()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Volver a documentos"
                        )
                    }
                },
                title = {
                    Column {
                        // Campo editable para el título del documento
                        BasicTextField(
                            value = editorTitle,
                            onValueChange = { viewModel.onTitleChanged(it) },
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_title_input"),
                            decorationBox = { innerTextField ->
                                if (editorTitle.isEmpty()) {
                                    Text(
                                        text = "Título del documento...",
                                        style = TextStyle(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )

                        // Indicador de guardado automático
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            if (saveStatus == "Guardado") {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = saveStatus,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    color = if (saveStatus == "Guardado") Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                },
                actions = {
                    // Alternar modo de hojas en cascada continua
                    IconButton(
                        onClick = { viewModel.toggleCascadeMode() },
                        modifier = Modifier.testTag("toggle_cascade_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Layers,
                            contentDescription = if (isCascadeMode) "Modo Cascada (Múltiples Hojas)" else "Modo Hoja Única",
                            tint = if (isCascadeMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Alternar modo edición / lectura
                    IconButton(
                        onClick = { viewModel.toggleReadOnlyMode() },
                        modifier = Modifier.testTag("toggle_read_only_button")
                    ) {
                        Icon(
                            imageVector = if (isReadOnly) Icons.Outlined.Edit else Icons.Outlined.Visibility,
                            contentDescription = if (isReadOnly) "Cambiar a modo edición" else "Cambiar a modo lectura"
                        )
                    }

                    // Botón para acceder a configuración de la hoja
                    IconButton(
                        onClick = {
                            viewModel.saveImmediately()
                            onNavigateToSettings(docId)
                        },
                        modifier = Modifier.testTag("open_sheet_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Ajustes de la hoja de papel"
                        )
                    }

                    // Menú de Exportación (.pdf / .md / compartir)
                    Box {
                        IconButton(
                            onClick = { showExportMenu = true },
                            modifier = Modifier.testTag("export_document_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.UploadFile,
                                contentDescription = "Exportar documento"
                            )
                        }

                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Exportar como PDF Digital (.pdf)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.PictureAsPdf,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showExportMenu = false
                                    viewModel.saveImmediately()
                                    val uri = DocumentExporter.exportToPdf(
                                        context = context,
                                        title = editorTitle,
                                        content = editorContent,
                                        fontStyle = doc?.fontStyle ?: "SERIF"
                                    )
                                    if (uri != null) {
                                        DocumentExporter.shareExportedFile(
                                            context = context,
                                            uri = uri,
                                            mimeType = "application/pdf",
                                            title = "$editorTitle.pdf"
                                        )
                                    } else {
                                        Toast.makeText(context, "Error al generar el PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Exportar como Markdown (.md)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                },
                                onClick = {
                                    showExportMenu = false
                                    viewModel.saveImmediately()
                                    val uri = DocumentExporter.exportToMarkdown(
                                        context = context,
                                        title = editorTitle,
                                        content = editorContent
                                    )
                                    if (uri != null) {
                                        DocumentExporter.shareExportedFile(
                                            context = context,
                                            uri = uri,
                                            mimeType = "text/markdown",
                                            title = "$editorTitle.md"
                                        )
                                    } else {
                                        Toast.makeText(context, "Error al generar el archivo .md", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Exportar como HTML Editorial (.html)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Language,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7)
                                    )
                                },
                                onClick = {
                                    showExportMenu = false
                                    viewModel.saveImmediately()
                                    val uri = DocumentExporter.exportToHtml(
                                        context = context,
                                        title = editorTitle,
                                        content = editorContent,
                                        fontStyle = doc?.fontStyle ?: "SERIF"
                                    )
                                    if (uri != null) {
                                        DocumentExporter.shareExportedFile(
                                            context = context,
                                            uri = uri,
                                            mimeType = "text/html",
                                            title = "$editorTitle.html"
                                        )
                                    } else {
                                        Toast.makeText(context, "Error al generar el HTML", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Exportar como Documento Texto (.txt)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.TextSnippet,
                                        contentDescription = null,
                                        tint = Color(0xFF059669)
                                    )
                                },
                                onClick = {
                                    showExportMenu = false
                                    viewModel.saveImmediately()
                                    val uri = DocumentExporter.exportToPlainText(
                                        context = context,
                                        title = editorTitle,
                                        content = editorContent
                                    )
                                    if (uri != null) {
                                        DocumentExporter.shareExportedFile(
                                            context = context,
                                            uri = uri,
                                            mimeType = "text/plain",
                                            title = "$editorTitle.txt"
                                        )
                                    } else {
                                        Toast.makeText(context, "Error al generar el archivo .txt", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Compartir Texto Rápido") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Share,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showExportMenu = false
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, editorTitle)
                                        putExtra(Intent.EXTRA_TEXT, editorContent)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Compartir texto"))
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            // Barra de estado estilo procesador de PC en el pie de pantalla (ocultada fluidamente cuando el teclado está activo)
            val isImeVisible = WindowInsets.isImeVisible
            AnimatedVisibility(
                visible = !isImeVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pág. 1 de $estimatedPages",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "$wordCount palabras",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.outline
                                )
                            )
                            Text(
                                text = "$readingTime min lectura",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Text(
                            text = "${(pageZoom * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
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
            AnimatedVisibility(
                visible = !isReadOnly,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Deshacer
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = canUndo,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Undo,
                                contentDescription = "Deshacer",
                                tint = if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Rehacer
                        IconButton(
                            onClick = { viewModel.redo() },
                            enabled = canRedo,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Redo,
                                contentDescription = "Rehacer",
                                tint = if (canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // Título H1
                        QuickToolButton(
                            icon = Icons.Outlined.Title,
                            label = "H1 Título",
                            onClick = { viewModel.insertHeading1() }
                        )

                        // Subtítulo H2
                        QuickToolButton(
                            icon = Icons.Outlined.TextFields,
                            label = "H2 Subtítulo",
                            onClick = { viewModel.insertHeading2() }
                        )

                        // Apartado H3
                        QuickToolButton(
                            icon = Icons.Outlined.TextFields,
                            label = "H3 Apartado",
                            onClick = { viewModel.insertHeading3() }
                        )

                        // Cita destacada
                        QuickToolButton(
                            icon = Icons.Outlined.FormatQuote,
                            label = "Cita",
                            onClick = { viewModel.insertQuote() }
                        )

                        // Negrita
                        QuickToolButton(
                            icon = Icons.Outlined.FormatBold,
                            label = "Negrita",
                            onClick = { viewModel.insertBold() }
                        )

                        // Cursiva
                        QuickToolButton(
                            icon = Icons.Outlined.FormatItalic,
                            label = "Cursiva",
                            onClick = { viewModel.insertItalic() }
                        )

                        // Subrayado
                        QuickToolButton(
                            icon = Icons.Outlined.FormatUnderlined,
                            label = "Subrayado",
                            onClick = { viewModel.insertUnderline() }
                        )

                        // Tachado
                        QuickToolButton(
                            icon = Icons.Outlined.FormatStrikethrough,
                            label = "Tachado",
                            onClick = { viewModel.insertStrikethrough() }
                        )

                        // Subíndice
                        QuickToolButton(
                            icon = Icons.Outlined.Subscript,
                            label = "Subíndice",
                            onClick = { viewModel.insertSubscript() }
                        )

                        // Superíndice
                        QuickToolButton(
                            icon = Icons.Outlined.Superscript,
                            label = "Superíndice",
                            onClick = { viewModel.insertSuperscript() }
                        )

                        // Alineación Cuádruple con Justificado Real (Párrafo o Global)
                        Box {
                            QuickToolButton(
                                icon = Icons.Outlined.FormatAlignJustify,
                                label = "Alineación",
                                onClick = { showAlignMenu = true }
                            )
                            DropdownMenu(
                                expanded = showAlignMenu,
                                onDismissRequest = { showAlignMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Párrafo: Izquierda", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.FormatAlignLeft, contentDescription = null) },
                                    onClick = {
                                        viewModel.insertAlignmentTag("left")
                                        showAlignMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Párrafo: Centrado", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = { Icon(Icons.Outlined.FormatAlignCenter, contentDescription = null) },
                                    onClick = {
                                        viewModel.insertAlignmentTag("center")
                                        showAlignMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Párrafo: Derecha", fontWeight = FontWeight.SemiBold) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.FormatAlignRight, contentDescription = null) },
                                    onClick = {
                                        viewModel.insertAlignmentTag("right")
                                        showAlignMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Párrafo: Justificado Real", fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Outlined.FormatAlignJustify, contentDescription = null) },
                                    onClick = {
                                        viewModel.insertAlignmentTag("justify")
                                        showAlignMenu = false
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Documento: Izquierda") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.FormatAlignLeft, contentDescription = null) },
                                    onClick = {
                                        viewModel.setGlobalAlignment("LEFT")
                                        showAlignMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Documento: Centrado") },
                                    leadingIcon = { Icon(Icons.Outlined.FormatAlignCenter, contentDescription = null) },
                                    onClick = {
                                        viewModel.setGlobalAlignment("CENTER")
                                        showAlignMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Documento: Derecha") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.FormatAlignRight, contentDescription = null) },
                                    onClick = {
                                        viewModel.setGlobalAlignment("RIGHT")
                                        showAlignMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Documento: Justificado Real", fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Outlined.FormatAlignJustify, contentDescription = null) },
                                    onClick = {
                                        viewModel.setGlobalAlignment("JUSTIFY")
                                        showAlignMenu = false
                                    }
                                )
                            }
                        }

                        // Selector de Tipografía (Inline o Global)
                        Box {
                            QuickToolButton(
                                icon = Icons.Outlined.FontDownload,
                                label = "Tipografía",
                                onClick = { showFontMenu = true }
                            )
                            DropdownMenu(
                                expanded = showFontMenu,
                                onDismissRequest = { showFontMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Fragmento: Serif (Editorial)") },
                                    onClick = {
                                        viewModel.insertFontTag("serif")
                                        showFontMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Fragmento: Sans-Serif (Moderna)") },
                                    onClick = {
                                        viewModel.insertFontTag("sans")
                                        showFontMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Fragmento: Monospace (Máquina)") },
                                    onClick = {
                                        viewModel.insertFontTag("mono")
                                        showFontMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Fragmento: Caligráfica (Manuscrita)") },
                                    onClick = {
                                        viewModel.insertFontTag("cursive")
                                        showFontMenu = false
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Documento Global: Serif") },
                                    onClick = {
                                        viewModel.updatePageSettings(fontStyle = "SERIF")
                                        showFontMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Documento Global: Sans-Serif") },
                                    onClick = {
                                        viewModel.updatePageSettings(fontStyle = "SANS_SERIF")
                                        showFontMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Documento Global: Monospace") },
                                    onClick = {
                                        viewModel.updatePageSettings(fontStyle = "MONOSPACE")
                                        showFontMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Documento Global: Caligráfica") },
                                    onClick = {
                                        viewModel.updatePageSettings(fontStyle = "CURSIVE")
                                        showFontMenu = false
                                    }
                                )
                            }
                        }

                        // Inserción de Imágenes con Ajuste de Hoja (Layout & Wrap)
                        QuickToolButton(
                            icon = Icons.Outlined.Image,
                            label = "Insertar Imagen",
                            onClick = {
                                imageUrlInput = ""
                                imageCaptionInput = ""
                                imageWrapMode = "full"
                                showImageDialog = true
                            }
                        )

                        // Inserción de Tablas y Cuadrículas Editoriales
                        QuickToolButton(
                            icon = Icons.Outlined.GridView,
                            label = "Insertar Tabla",
                            onClick = {
                                showTableDialog = true
                            }
                        )

                        // Insertar viñeta
                        QuickToolButton(
                            icon = Icons.Outlined.FormatListBulleted,
                            label = "Viñeta",
                            onClick = { viewModel.insertBullet() }
                        )

                        // Lista numerada
                        QuickToolButton(
                            icon = Icons.Outlined.FormatListNumbered,
                            label = "1. 2. Lista",
                            onClick = { viewModel.insertNumberedList() }
                        )

                        // Lista de tareas
                        QuickToolButton(
                            icon = Icons.Outlined.Checklist,
                            label = "[ ] Tarea",
                            onClick = { viewModel.insertChecklist() }
                        )

                        // Salto de Página (Nueva Hoja física)
                        QuickToolButton(
                            icon = Icons.Outlined.NoteAdd,
                            label = "Nueva Hoja",
                            onClick = { viewModel.insertPageBreak() }
                        )

                        // Sangría
                        QuickToolButton(
                            icon = Icons.AutoMirrored.Outlined.FormatIndentIncrease,
                            label = "Sangría",
                            onClick = { viewModel.insertIndent() }
                        )

                        // Fecha
                        QuickToolButton(
                            icon = Icons.Outlined.Today,
                            label = "Fecha",
                            onClick = { viewModel.insertCurrentDate() }
                        )

                        // Separador
                        QuickToolButton(
                            icon = Icons.Outlined.HorizontalRule,
                            label = "Separador",
                            onClick = { viewModel.insertDivider() }
                        )

                        // Zoom
                        IconButton(
                            onClick = { viewModel.cyclePageZoom() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ZoomIn,
                                contentDescription = "Zoom de página",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // --- Barra de Selección Contextual Estilo PC (Reemplazo del selector del sistema) ---
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
                }
            )

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
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // --- Diálogo para Inserción de Imágenes con Ajuste de Hoja (Layout & Wrap) ---
    if (showImageDialog) {
        AlertDialog(
            onDismissRequest = { showImageDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Insertar Imagen con Ajuste", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Ajuste de Hoja (Layout & Wrap):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "full" to "Ancho",
                            "center" to "Centro",
                            "left" to "Izq.",
                            "right" to "Der."
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = imageWrapMode == mode,
                                onClick = { imageWrapMode = mode },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = imageUrlInput,
                        onValueChange = { imageUrlInput = it },
                        label = { Text("URL o ruta de la imagen") },
                        placeholder = { Text("https://ejemplo.com/grafico.png") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = imageCaptionInput,
                        onValueChange = { imageCaptionInput = it },
                        label = { Text("Pie de foto (opcional)") },
                        placeholder = { Text("Ej: Figura 1. Esquema conceptual") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalUrl = imageUrlInput.trim().ifEmpty {
                            "https://images.unsplash.com/photo-1455390582262-044cdead277a?w=800"
                        }
                        viewModel.insertImage(
                            uri = finalUrl,
                            wrapMode = imageWrapMode,
                            caption = imageCaptionInput.trim()
                        )
                        showImageDialog = false
                    }
                ) {
                    Text("Insertar en Hoja", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImageDialog = false }) {
                    Text("Cancelar")
                }
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
}

@Composable
private fun QuickToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
        },
        shape = RoundedCornerShape(16.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
