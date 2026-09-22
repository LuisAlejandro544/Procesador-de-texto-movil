package com.example.ui.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MarkedSwapBlock
import com.example.ui.components.selection.SelectionBarButton
import com.example.ui.components.selection.SelectionInkPaletteDialog
import com.example.ui.components.selection.SelectionSwapBanner
import com.example.ui.components.selection.applyTagToSelection

// Re-exportamos los tipos de tinta para compatibilidad total
typealias InkColorOption = com.example.ui.components.selection.InkColorOption
val DEFAULT_INK_PALETTE = com.example.ui.components.selection.DEFAULT_INK_PALETTE

/**
 * DocuSheetDisabledSystemToolbar:
 * Deshabilita el menú flotante del selector del fabricante de Android (One UI, MIUI, ColorOS, etc.)
 * para que DocuSheet utilice su propia barra contextual de edición y cursor de PC.
 */
class DocuSheetDisabledSystemToolbar : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden

    override fun hide() {
        // No-op: El menú del sistema se mantiene oculto
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        // Anulamos la llamada del sistema Android para no invocar el menú nativo del fabricante
    }
}

/**
 * Barra contextual flotante / anclada de PC para texto seleccionado en DocuSheet.
 * Sustituye el menú del fabricante con opciones de PC:
 * - Copiar, Cortar, Pegar
 * - Cambio de tipografía directa para la selección (Serif, Sans, Mono, Cursive)
 * - Cambio de color de letra de la selección con paleta de tintas y selector libre
 * - Estilos rápidos (Negrita, Cursiva, Subrayado, Tachado)
 * - Modo Swap A ⇄ B de párrafos y oraciones
 * - Modo Desplazar al inicio o al final
 * - Botones táctiles amplios (mínimo 48x48 dp) y diseño de escritorio
 */
@Composable
fun DocuSheetPcSelectionBar(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onDismissSelection: () -> Unit,
    markedSwapBlock: MarkedSwapBlock? = null,
    onMarkForSwap: (() -> Unit)? = null,
    onExecuteSwap: (() -> Unit)? = null,
    onClearMarkedSwap: (() -> Unit)? = null,
    onSwapUp: (() -> Unit)? = null,
    onSwapDown: (() -> Unit)? = null,
    onMoveToStart: (() -> Unit)? = null,
    onMoveToEnd: (() -> Unit)? = null,
    onSwapWithClipboard: ((String) -> Unit)? = null,
    onSearchAndSynonyms: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val selection = textFieldValue.selection
    val hasSelection = !selection.collapsed
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    var showFontMenu by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showSwapMenu by remember { mutableStateOf(false) }
    var showMoveMenu by remember { mutableStateOf(false) }

    val selectedText = if (hasSelection) {
        val min = selection.min.coerceIn(0, textFieldValue.text.length)
        val max = selection.max.coerceIn(0, textFieldValue.text.length)
        textFieldValue.text.substring(min, max)
    } else ""

    AnimatedVisibility(
        visible = hasSelection || markedSwapBlock != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("pc_selection_bar")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 6.dp)
            ) {
                // Banner activo si hay un Bloque A fijado para transposición atómica (Swap A ⇄ B)
                if (markedSwapBlock != null) {
                    SelectionSwapBanner(
                        markedSwapBlock = markedSwapBlock,
                        hasSelection = hasSelection,
                        onExecuteSwap = { onExecuteSwap?.invoke() },
                        onClearMarkedSwap = { onClearMarkedSwap?.invoke() }
                    )
                }

                // Fila superior informativa de PC con botón de cerrar selección
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val wordCount = selectedText.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
                    Text(
                        text = if (hasSelection) {
                            "Selección de PC: $wordCount palabra(s), ${selectedText.length} caract."
                        } else {
                            "Modo Intercambio de PC activo"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    IconButton(
                        onClick = onDismissSelection,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("dismiss_selection_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Deseleccionar",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                // Fila de herramientas de escritorio con scroll horizontal fluido
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // --- 1. COPIAR ---
                    SelectionBarButton(
                        icon = Icons.Outlined.ContentCopy,
                        label = "Copiar",
                        testTag = "btn_pc_copy",
                        onClick = {
                            if (selectedText.isNotEmpty()) {
                                clipboardManager.setText(AnnotatedString(selectedText))
                            }
                        }
                    )

                    // --- 2. CORTAR ---
                    SelectionBarButton(
                        icon = Icons.Outlined.ContentCut,
                        label = "Cortar",
                        testTag = "btn_pc_cut",
                        onClick = {
                            if (selectedText.isNotEmpty()) {
                                clipboardManager.setText(AnnotatedString(selectedText))
                                val min = selection.min.coerceIn(0, textFieldValue.text.length)
                                val max = selection.max.coerceIn(0, textFieldValue.text.length)
                                val newText = textFieldValue.text.removeRange(min, max)
                                onValueChange(
                                    TextFieldValue(
                                        text = newText,
                                        selection = TextRange(min)
                                    )
                                )
                            }
                        }
                    )

                    // --- 3. PEGAR ---
                    SelectionBarButton(
                        icon = Icons.Outlined.ContentPaste,
                        label = "Pegar",
                        testTag = "btn_pc_paste",
                        onClick = {
                            val clipText = clipboardManager.getText()?.text ?: ""
                            if (clipText.isNotEmpty()) {
                                val min = selection.min.coerceIn(0, textFieldValue.text.length)
                                val max = selection.max.coerceIn(0, textFieldValue.text.length)
                                val newText = textFieldValue.text.replaceRange(min, max, clipText)
                                val newCursor = min + clipText.length
                                onValueChange(
                                    TextFieldValue(
                                        text = newText,
                                        selection = TextRange(newCursor)
                                    )
                                )
                            }
                        }
                    )

                    // Separador visual
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    // --- BUSCAR / SINÓNIMOS / RADAR DE ESTILO ---
                    if (onSearchAndSynonyms != null && selectedText.isNotEmpty()) {
                        SelectionBarButton(
                            icon = Icons.Outlined.Search,
                            label = "Sinónimos/Radar",
                            testTag = "btn_pc_search_synonyms",
                            onClick = {
                                onSearchAndSynonyms(selectedText.trim())
                            }
                        )

                        // Separador visual
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }

                    // --- 4. MODO INTERCAMBIAR (Swap / Transposición de PC) ---
                    Box {
                        SelectionBarButton(
                            icon = Icons.Outlined.SwapVert,
                            label = "Intercambiar",
                            testTag = "btn_pc_swap_menu",
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            onClick = { showSwapMenu = true }
                        )

                        DropdownMenu(
                            expanded = showSwapMenu,
                            onDismissRequest = { showSwapMenu = false }
                        ) {
                            if (markedSwapBlock == null) {
                                DropdownMenuItem(
                                    text = { Text("Fijar como Bloque A (Swap A ⇄ B)") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    onClick = {
                                        showSwapMenu = false
                                        onMarkForSwap?.invoke()
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Completar Intercambio con Bloque B") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    enabled = hasSelection,
                                    onClick = {
                                        showSwapMenu = false
                                        onExecuteSwap?.invoke()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Cancelar Intercambio Activo") },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Close, contentDescription = null)
                                    },
                                    onClick = {
                                        showSwapMenu = false
                                        onClearMarkedSwap?.invoke()
                                    }
                                )
                            }

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text("Intercambiar Párrafo Arriba (Swap ↑)") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.ArrowUpward, contentDescription = null)
                                },
                                onClick = {
                                    showSwapMenu = false
                                    onSwapUp?.invoke()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Intercambiar Párrafo Abajo (Swap ↓)") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.ArrowDownward, contentDescription = null)
                                },
                                onClick = {
                                    showSwapMenu = false
                                    onSwapDown?.invoke()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Intercambiar con Portapapeles") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.ContentPaste, contentDescription = null)
                                },
                                onClick = {
                                    showSwapMenu = false
                                    val clipText = clipboardManager.getText()?.text ?: ""
                                    onSwapWithClipboard?.invoke(clipText)
                                }
                            )
                        }
                    }

                    // Botón rápido de Swap Arriba
                    SelectionBarButton(
                        icon = Icons.Outlined.ArrowUpward,
                        label = "Swap ↑",
                        testTag = "btn_pc_swap_up",
                        onClick = { onSwapUp?.invoke() }
                    )

                    // Botón rápido de Swap Abajo
                    SelectionBarButton(
                        icon = Icons.Outlined.ArrowDownward,
                        label = "Swap ↓",
                        testTag = "btn_pc_swap_down",
                        onClick = { onSwapDown?.invoke() }
                    )

                    // Separador visual
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    // --- 5. MODO MOVER / ARRASTRAR (Move / Drag de PC) ---
                    Box {
                        SelectionBarButton(
                            icon = Icons.Outlined.OpenWith,
                            label = "Mover",
                            testTag = "btn_pc_move_menu",
                            accentColor = MaterialTheme.colorScheme.secondary,
                            onClick = { showMoveMenu = true }
                        )

                        DropdownMenu(
                            expanded = showMoveMenu,
                            onDismissRequest = { showMoveMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Mover al Inicio de la Hoja (Top)") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.VerticalAlignTop, contentDescription = null)
                                },
                                onClick = {
                                    showMoveMenu = false
                                    onMoveToStart?.invoke()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Mover al Final de la Hoja (Bottom)") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.VerticalAlignBottom, contentDescription = null)
                                },
                                onClick = {
                                    showMoveMenu = false
                                    onMoveToEnd?.invoke()
                                }
                            )
                        }
                    }

                    // Botón rápido Mover al Inicio
                    SelectionBarButton(
                        icon = Icons.Outlined.VerticalAlignTop,
                        label = "Al Inicio",
                        testTag = "btn_pc_move_start",
                        onClick = { onMoveToStart?.invoke() }
                    )

                    // Botón rápido Mover al Final
                    SelectionBarButton(
                        icon = Icons.Outlined.VerticalAlignBottom,
                        label = "Al Final",
                        testTag = "btn_pc_move_end",
                        onClick = { onMoveToEnd?.invoke() }
                    )

                    // Separador visual
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    // --- 6. CAMBIAR TIPOGRAFÍA DE SELECCIÓN ---
                    Box {
                        SelectionBarButton(
                            icon = Icons.Outlined.FontDownload,
                            label = "Fuente",
                            testTag = "btn_pc_font",
                            onClick = { showFontMenu = true }
                        )

                        DropdownMenu(
                            expanded = showFontMenu,
                            onDismissRequest = { showFontMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Serif (Editorial)", fontFamily = FontFamily.Serif) },
                                onClick = {
                                    applyTagToSelection(textFieldValue, "[font:serif]", "[/font]", onValueChange)
                                    showFontMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sans-Serif (Moderna)", fontFamily = FontFamily.SansSerif) },
                                onClick = {
                                    applyTagToSelection(textFieldValue, "[font:sans]", "[/font]", onValueChange)
                                    showFontMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Monospace (Máquina)", fontFamily = FontFamily.Monospace) },
                                onClick = {
                                    applyTagToSelection(textFieldValue, "[font:mono]", "[/font]", onValueChange)
                                    showFontMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Caligráfica (Cursive)", fontFamily = FontFamily.Cursive) },
                                onClick = {
                                    applyTagToSelection(textFieldValue, "[font:cursive]", "[/font]", onValueChange)
                                    showFontMenu = false
                                }
                            )
                        }
                    }

                    // --- 7. CAMBIAR COLOR DE LETRA DE SELECCIÓN ---
                    SelectionBarButton(
                        icon = Icons.Outlined.Palette,
                        label = "Color Tinta",
                        testTag = "btn_pc_color",
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = {
                            showColorDialog = true
                        }
                    )

                    // Separador visual
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    // --- 8. FORMATOS RÁPIDOS ---
                    SelectionBarButton(
                        icon = Icons.Outlined.FormatBold,
                        label = "Negrita",
                        testTag = "btn_pc_bold",
                        onClick = {
                            applyTagToSelection(textFieldValue, "**", "**", onValueChange)
                        }
                    )

                    SelectionBarButton(
                        icon = Icons.Outlined.FormatItalic,
                        label = "Cursiva",
                        testTag = "btn_pc_italic",
                        onClick = {
                            applyTagToSelection(textFieldValue, "*", "*", onValueChange)
                        }
                    )

                    SelectionBarButton(
                        icon = Icons.Outlined.FormatUnderlined,
                        label = "Subrayado",
                        testTag = "btn_pc_underline",
                        onClick = {
                            applyTagToSelection(textFieldValue, "<u>", "</u>", onValueChange)
                        }
                    )

                    SelectionBarButton(
                        icon = Icons.Outlined.FormatStrikethrough,
                        label = "Tachado",
                        testTag = "btn_pc_strikethrough",
                        onClick = {
                            applyTagToSelection(textFieldValue, "~~", "~~", onValueChange)
                        }
                    )
                }
            }
        }
    }

    // Diálogo de selección de color de tinta para la selección
    if (showColorDialog) {
        SelectionInkPaletteDialog(
            selectedText = selectedText,
            textFieldValue = textFieldValue,
            onValueChange = onValueChange,
            onDismiss = { showColorDialog = false }
        )
    }
}
