package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.ViewCompact
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Modelo de datos estructurado de una tabla activa en edición.
 */
data class ActiveTableData(
    val startIndex: Int,
    val endIndex: Int,
    val style: String,
    val hasHeader: Boolean,
    val rows: List<List<String>>
)

/**
 * TableEditorDialog: Asistente visual y táctil para editar tablas existentes cómodamente en el teléfono.
 * 
 * Permite modificar celdas individuales, agregar o remover filas y columnas,
 * alternar fila de encabezados y cambiar el estilo editorial sin lidiar con la sintaxis de Markdown.
 */
@Composable
fun TableEditorDialog(
    initialData: ActiveTableData,
    onDismiss: () -> Unit,
    onSave: (ActiveTableData) -> Unit
) {
    var selectedStyle by remember { mutableStateOf(initialData.style) }
    var hasHeader by remember { mutableStateOf(initialData.hasHeader) }

    // Representación mutable de la cuadrícula de celdas
    val rowsData = remember {
        mutableStateListOf<MutableList<String>>().apply {
            if (initialData.rows.isEmpty()) {
                add(mutableListOf("Columna 1", "Columna 2"))
                add(mutableListOf("Dato 1", "Dato 2"))
            } else {
                initialData.rows.forEach { row ->
                    add(row.toMutableList())
                }
            }
        }
    }

    val currentCols = if (rowsData.isNotEmpty()) rowsData.maxOf { it.size } else 1

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // --- Cabecera del diálogo ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TableChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Editor Visual de Tabla",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${rowsData.size} filas × $currentCols columnas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar editor de tabla"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- Barra de Estilos Editoriales ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val styles = listOf(
                        Pair("classic", "Clásica"),
                        Pair("editorial", "Editorial"),
                        Pair("striped", "Rayada"),
                        Pair("compact", "Compacta")
                    )

                    styles.forEach { (styleKey, styleLabel) ->
                        FilterChip(
                            selected = selectedStyle == styleKey,
                            onClick = { selectedStyle = styleKey },
                            label = { Text(styleLabel, fontSize = 12.sp) },
                            leadingIcon = if (selectedStyle == styleKey) {
                                { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- Barra de Herramientas de Edición de Cuadrícula (Filas y Columnas) ---
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Agregar Fila
                        OutlinedButton(
                            onClick = {
                                val newRow = MutableList(currentCols) { "" }
                                rowsData.add(newRow)
                            },
                            contentPadding = ButtonDefaults.ContentPadding,
                            modifier = Modifier.heightIn(min = 40.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fila", fontSize = 12.sp)
                        }

                        // Eliminar Fila
                        OutlinedButton(
                            onClick = {
                                if (rowsData.size > 1) {
                                    rowsData.removeAt(rowsData.size - 1)
                                }
                            },
                            enabled = rowsData.size > 1,
                            contentPadding = ButtonDefaults.ContentPadding,
                            modifier = Modifier.heightIn(min = 40.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fila", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Agregar Columna
                        OutlinedButton(
                            onClick = {
                                if (currentCols < 8) {
                                    for (i in 0 until rowsData.size) {
                                        rowsData[i].add("")
                                    }
                                }
                            },
                            enabled = currentCols < 8,
                            contentPadding = ButtonDefaults.ContentPadding,
                            modifier = Modifier.heightIn(min = 40.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Columna", fontSize = 12.sp)
                        }

                        // Eliminar Columna
                        OutlinedButton(
                            onClick = {
                                if (currentCols > 1) {
                                    for (i in 0 until rowsData.size) {
                                        if (rowsData[i].isNotEmpty()) {
                                            rowsData[i].removeAt(rowsData[i].size - 1)
                                        }
                                    }
                                }
                            },
                            enabled = currentCols > 1,
                            contentPadding = ButtonDefaults.ContentPadding,
                            modifier = Modifier.heightIn(min = 40.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Columna", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Encabezados
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Encabezado:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = hasHeader,
                                onCheckedChange = { hasHeader = it },
                                modifier = Modifier.testTag("switch_table_editor_header")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- Cuadrícula interactiva de Celdas Táctiles ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                ) {
                    val verticalScroll = rememberScrollState()
                    val horizontalScroll = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(verticalScroll)
                            .horizontalScroll(horizontalScroll)
                            .padding(8.dp)
                    ) {
                        rowsData.forEachIndexed { rowIndex, rowList ->
                            val isHeaderRow = rowIndex == 0 && hasHeader

                            Row(
                                modifier = Modifier
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Etiqueta lateral de fila
                                Surface(
                                    color = if (isHeaderRow) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = if (isHeaderRow) "H" else "$rowIndex",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isHeaderRow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Celdas de la fila
                                for (colIndex in 0 until currentCols) {
                                    val cellValue = rowList.getOrNull(colIndex) ?: ""

                                    val cellBgColor = when {
                                        isHeaderRow -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                        selectedStyle == "striped" && rowIndex % 2 == 1 -> MaterialTheme.colorScheme.surfaceContainerHigh
                                        else -> MaterialTheme.colorScheme.surface
                                    }

                                    Surface(
                                        color = cellBgColor,
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isHeaderRow) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                        ),
                                        modifier = Modifier
                                            .width(130.dp)
                                            .height(44.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            BasicTextField(
                                                value = cellValue,
                                                onValueChange = { newValue ->
                                                    while (rowList.size <= colIndex) {
                                                        rowList.add("")
                                                    }
                                                    rowList[colIndex] = newValue
                                                },
                                                singleLine = true,
                                                textStyle = TextStyle(
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isHeaderRow) FontWeight.Bold else FontWeight.Normal,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    textAlign = if (isHeaderRow) TextAlign.Center else TextAlign.Start
                                                ),
                                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                                decorationBox = { innerTextField ->
                                                    if (cellValue.isEmpty()) {
                                                        Text(
                                                            text = if (isHeaderRow) "Col ${colIndex + 1}" else "Celda",
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                                        )
                                                    }
                                                    innerTextField()
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- Botones de Confirmación y Cancelación ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            val sanitizedRows = rowsData.map { row ->
                                (0 until currentCols).map { c ->
                                    row.getOrNull(c) ?: ""
                                }
                            }
                            val updated = initialData.copy(
                                style = selectedStyle,
                                hasHeader = hasHeader,
                                rows = sanitizedRows
                            )
                            onSave(updated)
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .heightIn(min = 48.dp)
                            .testTag("save_table_changes_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aplicar a la Hoja", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
