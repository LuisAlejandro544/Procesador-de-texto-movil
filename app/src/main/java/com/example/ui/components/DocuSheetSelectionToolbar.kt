package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material.icons.outlined.VerticalAlignTop
import com.example.ui.MarkedSwapBlock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
 * Definición de un color de tinta físico para la paleta de PC.
 */
data class InkColorOption(
    val name: String,
    val hex: String,
    val color: Color
)

val DEFAULT_INK_PALETTE = listOf(
    InkColorOption("Negro Carbón", "#1E293B", Color(0xFF1E293B)),
    InkColorOption("Azul Real", "#1D4ED8", Color(0xFF1D4ED8)),
    InkColorOption("Rojo Borgoña", "#BE123C", Color(0xFFBE123C)),
    InkColorOption("Verde Botánico", "#047857", Color(0xFF047857)),
    InkColorOption("Violeta Imperial", "#7E22CE", Color(0xFF7E22CE)),
    InkColorOption("Ámbar Cálido", "#D97706", Color(0xFFD97706)),
    InkColorOption("Turquesa", "#0284C7", Color(0xFF0284C7)),
    InkColorOption("Coral Rosa", "#E11D48", Color(0xFFE11D48)),
    InkColorOption("Marrón Sepia", "#78350F", Color(0xFF78350F)),
    InkColorOption("Gris Grafito", "#475569", Color(0xFF475569))
)

/**
 * Barra contextual flotante / anclada de PC para texto seleccionado en DocuSheet.
 * Sustituye el menú del fabricante con opciones de PC:
 * - Copiar, Cortar, Pegar
 * - Cambio de tipografía directa para la selección (Serif, Sans, Mono, Cursive)
 * - Cambio de color de letra de la selección con paleta de tintas y selector libre
 * - Estilos rápidos (Negrita, Cursiva, Subrayado, Tachado)
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
    modifier: Modifier = Modifier
) {
    val selection = textFieldValue.selection
    val hasSelection = !selection.collapsed
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    var showFontMenu by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showSwapMenu by remember { mutableStateOf(false) }
    var showMoveMenu by remember { mutableStateOf(false) }
    var customHexInput by remember { mutableStateOf("") }
    var selectedInkHex by remember { mutableStateOf("#1D4ED8") }

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
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SwapHoriz,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val previewA = if (markedSwapBlock.text.length > 18) {
                                    markedSwapBlock.text.take(18) + "…"
                                } else {
                                    markedSwapBlock.text
                                }
                                Text(
                                    text = "Swap Activo: Bloque A: \"$previewA\". Elige Bloque B.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                TextButton(
                                    onClick = { onExecuteSwap?.invoke() },
                                    enabled = hasSelection,
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Intercambiar", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                }

                                IconButton(
                                    onClick = { onClearMarkedSwap?.invoke() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancelar swap",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
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

                    // ==========================================================
                    // --- 4. MODO INTERCAMBIAR (Swap / Transposición de PC) ---
                    // ==========================================================
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

                    // ==========================================================
                    // --- 5. MODO MOVER / ARRASTRAR (Move / Drag de PC) ---
                    // ==========================================================
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


                    // --- 4. CAMBIAR TIPOGRAFÍA DE SELECCIÓN ---
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

                    // --- 5. CAMBIAR COLOR DE LETRA DE SELECCIÓN ---
                    SelectionBarButton(
                        icon = Icons.Outlined.Palette,
                        label = "Color Tinta",
                        testTag = "btn_pc_color",
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = {
                            customHexInput = ""
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

                    // --- 6. FORMATOS RÁPIDOS ---
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
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Color de Letra (PC)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Elige el color de tinta para el texto seleccionado:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Muestra de color actual
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Muestra de texto: ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        val previewColor = try {
                            Color(android.graphics.Color.parseColor(selectedInkHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }
                        Text(
                            text = if (selectedText.length > 20) selectedText.take(20) + "..." else selectedText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = previewColor,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    // Paleta de colores predefinidos (Tinta física de alta calidad)
                    Text("Paleta de Tintas Clásicas:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DEFAULT_INK_PALETTE.chunked(5).forEach { rowColors ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                rowColors.forEach { ink ->
                                    val isSelected = selectedInkHex.equals(ink.hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(ink.color)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0x33000000),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                selectedInkHex = ink.hex
                                                customHexInput = ink.hex
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = ink.name,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Entrada para código hexadecimal libre
                    OutlinedTextField(
                        value = customHexInput,
                        onValueChange = { input ->
                            customHexInput = input
                            val clean = if (input.startsWith("#")) input else "#$input"
                            if (clean.matches(Regex("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})$"))) {
                                selectedInkHex = clean
                            }
                        },
                        label = { Text("Código HEX libre (ej: #BE123C)") },
                        placeholder = { Text("#1D4ED8") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalHex = if (selectedInkHex.startsWith("#")) selectedInkHex else "#$selectedInkHex"
                        applyTagToSelection(
                            textFieldValue = textFieldValue,
                            openTag = "[color:$finalHex]",
                            closeTag = "[/color]",
                            onValueChange = onValueChange
                        )
                        showColorDialog = false
                    }
                ) {
                    Text("Aplicar Color", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showColorDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Aplica una etiqueta de apertura y cierre al rango seleccionado actualmente en el TextFieldValue.
 */
private fun applyTagToSelection(
    textFieldValue: TextFieldValue,
    openTag: String,
    closeTag: String,
    onValueChange: (TextFieldValue) -> Unit
) {
    val selection = textFieldValue.selection
    val min = selection.min.coerceIn(0, textFieldValue.text.length)
    val max = selection.max.coerceIn(0, textFieldValue.text.length)

    if (min == max) {
        // No hay texto seleccionado: inserta las etiquetas y coloca el cursor en medio
        val newText = textFieldValue.text.substring(0, min) + openTag + closeTag + textFieldValue.text.substring(max)
        val newCursor = min + openTag.length
        onValueChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursor)
            )
        )
    } else {
        // Envolver el texto seleccionado con las etiquetas
        val selectedText = textFieldValue.text.substring(min, max)
        val replacement = "$openTag$selectedText$closeTag"
        val newText = textFieldValue.text.substring(0, min) + replacement + textFieldValue.text.substring(max)
        val newCursor = min + replacement.length
        onValueChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursor)
            )
        )
    }
}

/**
 * Botón individual de la barra contextual de PC con target táctil mínimo de 48x48 dp.
 */
@Composable
private fun SelectionBarButton(
    icon: ImageVector,
    label: String,
    testTag: String,
    accentColor: Color? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .size(width = 54.dp, height = 50.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accentColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            maxLines = 1
        )
    }
}
