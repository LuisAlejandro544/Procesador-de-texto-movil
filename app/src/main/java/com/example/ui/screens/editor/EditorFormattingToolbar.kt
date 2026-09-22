package com.example.ui.screens.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatAlignLeft
import androidx.compose.material.icons.automirrored.outlined.FormatAlignRight
import androidx.compose.material.icons.automirrored.outlined.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Checklist
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
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Subscript
import androidx.compose.material.icons.outlined.Superscript
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Barra horizontal deslizante de herramientas de formato estructurado y tipográfico para el editor.
 */
@Composable
fun EditorFormattingToolbar(
    isVisible: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onInsertH1: () -> Unit,
    onInsertH2: () -> Unit,
    onInsertH3: () -> Unit,
    onInsertQuote: () -> Unit,
    onInsertBold: () -> Unit,
    onInsertItalic: () -> Unit,
    onInsertUnderline: () -> Unit,
    onInsertStrikethrough: () -> Unit,
    onInsertSubscript: () -> Unit,
    onInsertSuperscript: () -> Unit,
    onInsertAlignmentTag: (String) -> Unit,
    onSetGlobalAlignment: (String) -> Unit,
    onInsertFontTag: (String) -> Unit,
    onSetGlobalFont: (String) -> Unit,
    onOpenImageDialog: () -> Unit,
    onOpenTableDialog: () -> Unit,
    isCursorInTable: Boolean = false,
    onEditCurrentTable: (() -> Unit)? = null,
    onInsertBullet: () -> Unit,
    onInsertNumberedList: () -> Unit,
    onInsertChecklist: () -> Unit,
    onInsertPageBreak: () -> Unit,
    onInsertIndent: () -> Unit,
    onInsertDate: () -> Unit,
    onOpenMacros: () -> Unit,
    onInsertDivider: () -> Unit,
    onCycleZoom: () -> Unit,
    onOpenColor3dDialog: () -> Unit = {},
    onOpenShapeNodeDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAlignMenu by remember { mutableStateOf(false) }
    var showFontMenu by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
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
                    onClick = onUndo,
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
                    onClick = onRedo,
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

                // Títulos
                QuickToolButton(icon = Icons.Outlined.Title, label = "H1 Título", onClick = onInsertH1)
                QuickToolButton(icon = Icons.Outlined.TextFields, label = "H2 Subtítulo", onClick = onInsertH2)
                QuickToolButton(icon = Icons.Outlined.TextFields, label = "H3 Apartado", onClick = onInsertH3)
                QuickToolButton(icon = Icons.Outlined.FormatQuote, label = "Cita", onClick = onInsertQuote)

                // Estilos de texto
                QuickToolButton(icon = Icons.Outlined.FormatBold, label = "Negrita", onClick = onInsertBold)
                QuickToolButton(icon = Icons.Outlined.FormatItalic, label = "Cursiva", onClick = onInsertItalic)
                QuickToolButton(icon = Icons.Outlined.FormatUnderlined, label = "Subrayado", onClick = onInsertUnderline)
                QuickToolButton(icon = Icons.Outlined.FormatStrikethrough, label = "Tachado", onClick = onInsertStrikethrough)
                QuickToolButton(icon = Icons.Outlined.Subscript, label = "Subíndice", onClick = onInsertSubscript)
                QuickToolButton(icon = Icons.Outlined.Superscript, label = "Superíndice", onClick = onInsertSuperscript)
                QuickToolButton(icon = Icons.Outlined.Palette, label = "Color / 3D", onClick = onOpenColor3dDialog)

                // Alineación
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
                                onInsertAlignmentTag("left")
                                showAlignMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Párrafo: Centrado", fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Outlined.FormatAlignCenter, contentDescription = null) },
                            onClick = {
                                onInsertAlignmentTag("center")
                                showAlignMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Párrafo: Derecha", fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.FormatAlignRight, contentDescription = null) },
                            onClick = {
                                onInsertAlignmentTag("right")
                                showAlignMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Párrafo: Justificado Real", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Outlined.FormatAlignJustify, contentDescription = null) },
                            onClick = {
                                onInsertAlignmentTag("justify")
                                showAlignMenu = false
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Documento: Izquierda") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.FormatAlignLeft, contentDescription = null) },
                            onClick = {
                                onSetGlobalAlignment("LEFT")
                                showAlignMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Documento: Centrado") },
                            leadingIcon = { Icon(Icons.Outlined.FormatAlignCenter, contentDescription = null) },
                            onClick = {
                                onSetGlobalAlignment("CENTER")
                                showAlignMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Documento: Derecha") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.FormatAlignRight, contentDescription = null) },
                            onClick = {
                                onSetGlobalAlignment("RIGHT")
                                showAlignMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Documento: Justificado Real", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Outlined.FormatAlignJustify, contentDescription = null) },
                            onClick = {
                                onSetGlobalAlignment("JUSTIFY")
                                showAlignMenu = false
                            }
                        )
                    }
                }

                // Tipografía
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
                                onInsertFontTag("serif")
                                showFontMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Fragmento: Sans-Serif (Moderna)") },
                            onClick = {
                                onInsertFontTag("sans")
                                showFontMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Fragmento: Monospace (Máquina)") },
                            onClick = {
                                onInsertFontTag("mono")
                                showFontMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Fragmento: Caligráfica (Manuscrita)") },
                            onClick = {
                                onInsertFontTag("cursive")
                                showFontMenu = false
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Documento Global: Serif") },
                            onClick = {
                                onSetGlobalFont("SERIF")
                                showFontMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Documento Global: Sans-Serif") },
                            onClick = {
                                onSetGlobalFont("SANS_SERIF")
                                showFontMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Documento Global: Monospace") },
                            onClick = {
                                onSetGlobalFont("MONOSPACE")
                                showFontMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Documento Global: Caligráfica") },
                            onClick = {
                                onSetGlobalFont("CURSIVE")
                                showFontMenu = false
                            }
                        )
                    }
                }

                // Elementos Multimedia y Estructuras
                QuickToolButton(icon = Icons.Outlined.Image, label = "Insertar Imagen", onClick = onOpenImageDialog)
                QuickToolButton(icon = Icons.Outlined.Category, label = "Figuras / Nodos", onClick = onOpenShapeNodeDialog)

                if (isCursorInTable && onEditCurrentTable != null) {
                    QuickToolButton(
                        icon = Icons.Outlined.TableChart,
                        label = "Editar Tabla",
                        onClick = onEditCurrentTable,
                        isHighlighted = true
                    )
                    QuickToolButton(
                        icon = Icons.Outlined.GridView,
                        label = "+ Nueva",
                        onClick = onOpenTableDialog
                    )
                } else {
                    QuickToolButton(
                        icon = Icons.Outlined.GridView,
                        label = "Insertar Tabla",
                        onClick = onOpenTableDialog
                    )
                }

                // Listas y bloques
                QuickToolButton(icon = Icons.Outlined.FormatListBulleted, label = "Viñeta", onClick = onInsertBullet)
                QuickToolButton(icon = Icons.Outlined.FormatListNumbered, label = "1. 2. Lista", onClick = onInsertNumberedList)
                QuickToolButton(icon = Icons.Outlined.Checklist, label = "[ ] Tarea", onClick = onInsertChecklist)

                // Página y disposición
                QuickToolButton(icon = Icons.Outlined.NoteAdd, label = "Nueva Hoja", onClick = onInsertPageBreak)
                QuickToolButton(icon = Icons.AutoMirrored.Outlined.FormatIndentIncrease, label = "Sangría", onClick = onInsertIndent)
                QuickToolButton(icon = Icons.Outlined.Today, label = "Fecha", onClick = onInsertDate)

                // Macros y Separador
                QuickToolButton(icon = Icons.Outlined.AutoAwesome, label = "Macros", onClick = onOpenMacros)
                QuickToolButton(icon = Icons.Outlined.HorizontalRule, label = "Separador", onClick = onInsertDivider)

                // Zoom
                IconButton(
                    onClick = onCycleZoom,
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
}

@Composable
private fun QuickToolButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isHighlighted: Boolean = false
) {
    FilterChip(
        selected = isHighlighted,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium
            )
        },
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
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}
