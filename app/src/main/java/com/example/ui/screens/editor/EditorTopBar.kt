package com.example.ui.screens.editor

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.DocumentExporter

/**
 * Barra superior del editor DocuSheet con título editable, estado de guardado,
 * alternador de vistas (cascada/hoja única, lectura/edición, búsqueda/radar) y menú de exportación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
    context: Context,
    title: String,
    onTitleChange: (String) -> Unit,
    saveStatus: String,
    isCascadeMode: Boolean,
    onToggleCascadeMode: () -> Unit,
    isReadOnly: Boolean,
    onToggleReadOnly: () -> Unit,
    isSearchVisible: Boolean,
    onToggleSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateBack: () -> Unit,
    onBeforeExport: () -> Unit,
    fontStyle: String,
    content: String,
    modifier: Modifier = Modifier
) {
    var showExportMenu by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = {
            IconButton(
                onClick = onNavigateBack,
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
                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
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
                        if (title.isEmpty()) {
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
            IconButton(
                onClick = onToggleCascadeMode,
                modifier = Modifier.testTag("toggle_cascade_mode_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Layers,
                    contentDescription = if (isCascadeMode) "Modo Cascada (Múltiples Hojas)" else "Modo Hoja Única",
                    tint = if (isCascadeMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onToggleReadOnly,
                modifier = Modifier.testTag("toggle_read_only_button")
            ) {
                Icon(
                    imageVector = if (isReadOnly) Icons.Outlined.Edit else Icons.Outlined.Visibility,
                    contentDescription = if (isReadOnly) "Cambiar a modo edición" else "Cambiar a modo lectura"
                )
            }

            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier.testTag("open_editor_search_radar_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Buscar y Radar de Redundancia",
                    tint = if (isSearchVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("open_sheet_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Ajustes de la hoja de papel"
                )
            }

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
                            onBeforeExport()
                            val uri = DocumentExporter.exportToPdf(
                                context = context,
                                title = title,
                                content = content,
                                fontStyle = fontStyle
                            )
                            if (uri != null) {
                                DocumentExporter.shareExportedFile(
                                    context = context,
                                    uri = uri,
                                    mimeType = "application/pdf",
                                    title = "$title.pdf"
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
                            onBeforeExport()
                            val uri = DocumentExporter.exportToMarkdown(
                                context = context,
                                title = title,
                                content = content
                            )
                            if (uri != null) {
                                DocumentExporter.shareExportedFile(
                                    context = context,
                                    uri = uri,
                                    mimeType = "text/markdown",
                                    title = "$title.md"
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
                            onBeforeExport()
                            val uri = DocumentExporter.exportToHtml(
                                context = context,
                                title = title,
                                content = content,
                                fontStyle = fontStyle
                            )
                            if (uri != null) {
                                DocumentExporter.shareExportedFile(
                                    context = context,
                                    uri = uri,
                                    mimeType = "text/html",
                                    title = "$title.html"
                                )
                            } else {
                                Toast.makeText(context, "Error al generar el HTML", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Exportar como Word (.docx)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = Color(0xFF2563EB)
                            )
                        },
                        onClick = {
                            showExportMenu = false
                            onBeforeExport()
                            val uri = DocumentExporter.exportToDocx(
                                context = context,
                                title = title,
                                content = content
                            )
                            if (uri != null) {
                                DocumentExporter.shareExportedFile(
                                    context = context,
                                    uri = uri,
                                    mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    title = "$title.docx"
                                )
                            } else {
                                Toast.makeText(context, "Error al generar el archivo .docx", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Exportar como Texto Enriquecido (.rtf)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.TextSnippet,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED)
                            )
                        },
                        onClick = {
                            showExportMenu = false
                            onBeforeExport()
                            val uri = DocumentExporter.exportToRtf(
                                context = context,
                                title = title,
                                content = content
                            )
                            if (uri != null) {
                                DocumentExporter.shareExportedFile(
                                    context = context,
                                    uri = uri,
                                    mimeType = "application/rtf",
                                    title = "$title.rtf"
                                )
                            } else {
                                Toast.makeText(context, "Error al generar el archivo .rtf", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Exportar como LaTeX Académico (.tex)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = null,
                                tint = Color(0xFF15803D)
                            )
                        },
                        onClick = {
                            showExportMenu = false
                            onBeforeExport()
                            val uri = DocumentExporter.exportToLatex(
                                context = context,
                                title = title,
                                content = content
                            )
                            if (uri != null) {
                                DocumentExporter.shareExportedFile(
                                    context = context,
                                    uri = uri,
                                    mimeType = "text/x-tex",
                                    title = "$title.tex"
                                )
                            } else {
                                Toast.makeText(context, "Error al generar el archivo .tex", Toast.LENGTH_SHORT).show()
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
                            onBeforeExport()
                            val uri = DocumentExporter.exportToPlainText(
                                context = context,
                                title = title,
                                content = content
                            )
                            if (uri != null) {
                                DocumentExporter.shareExportedFile(
                                    context = context,
                                    uri = uri,
                                    mimeType = "text/plain",
                                    title = "$title.txt"
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
                                putExtra(Intent.EXTRA_SUBJECT, title)
                                putExtra(Intent.EXTRA_TEXT, content)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir texto"))
                        }
                    )
                }
            }
        }
    )
}
