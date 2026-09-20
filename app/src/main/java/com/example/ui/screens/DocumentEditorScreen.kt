package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.outlined.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DocumentViewModel
import com.example.ui.components.PaperSheet
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
    val editorTitle by viewModel.editorTitle.collectAsStateWithLifecycle()
    val saveStatus by viewModel.saveStatus.collectAsStateWithLifecycle()
    val isReadOnly by viewModel.isReadOnlyMode.collectAsStateWithLifecycle()
    val pageZoom by viewModel.pageZoom.collectAsStateWithLifecycle()
    val isCascadeMode by viewModel.isCascadeMode.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()

    var showExportMenu by remember { mutableStateOf(false) }

    val wordCount = viewModel.getWordCount(editorContent)
    val charCount = viewModel.getCharCount(editorContent)
    val estimatedPages = viewModel.getEstimatedPages(editorContent)
    val readingTime = viewModel.getReadingTimeMinutes(editorContent)

    val doc = activeDoc

    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
                                text = { Text("Compartir Texto Plano") },
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
            // Barra de estado estilo procesador de PC en el pie de pantalla
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                            isReadOnly = isReadOnly,
                            isCascadeMode = isCascadeMode,
                            onAddPage = { viewModel.insertPageBreak() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
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
