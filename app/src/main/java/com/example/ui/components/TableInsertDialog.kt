package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.ViewCompact
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * TableInsertDialog: Diálogo táctil y visual para la configuración e inserción
 * de Tablas y Cuadrículas Editoriales en DocuSheet.
 * 
 * Permite seleccionar el número de columnas (1..6) y filas (1..10), activar fila
 * de encabezado y elegir entre cuatro estilos tipográficos editoriales.
 */
@Composable
fun TableInsertDialog(
    onDismiss: () -> Unit,
    onConfirm: (rows: Int, cols: Int, hasHeader: Boolean, style: String) -> Unit
) {
    var cols by remember { mutableIntStateOf(3) }
    var rows by remember { mutableIntStateOf(3) }
    var hasHeader by remember { mutableStateOf(true) }
    var selectedStyle by remember { mutableStateOf("classic") }

    val styles = listOf(
        TableStyleOption("classic", "Clásica", Icons.Outlined.GridView, "Bordes y celdas completos"),
        TableStyleOption("editorial", "Editorial", Icons.Outlined.ViewDay, "Líneas horizontales sin cortes verticales"),
        TableStyleOption("striped", "Rayada", Icons.Outlined.TableChart, "Filas alternadas sombreadas"),
        TableStyleOption("compact", "Compacta", Icons.Outlined.ViewCompact, "Alta densidad de datos")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.TableChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "Insertar Tabla Editorial",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Selector de Columnas
                NumberSelectorRow(
                    label = "Columnas",
                    value = cols,
                    min = 1,
                    max = 6,
                    onValueChange = { cols = it },
                    tagPrefix = "cols"
                )

                // Selector de Filas
                NumberSelectorRow(
                    label = "Filas",
                    value = rows,
                    min = 1,
                    max = 10,
                    onValueChange = { rows = it },
                    tagPrefix = "rows"
                )

                // Interruptor de Encabezado
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Fila de Encabezados",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Diferenciar la primera fila con negrita y fondo",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        Switch(
                            checked = hasHeader,
                            onCheckedChange = { hasHeader = it },
                            modifier = Modifier.testTag("switch_table_header")
                        )
                    }
                }

                // Selector de Estilo Editorial
                Text(
                    text = "Estilo de la Cuadrícula",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    styles.forEach { styleOpt ->
                        val isSelected = selectedStyle == styleOpt.id
                        val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(8.dp))
                                .background(bgColor)
                                .clickable { selectedStyle = styleOpt.id }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = styleOpt.icon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = styleOpt.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = styleOpt.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Vista Previa Clara y Realista sobre Hoja Física
                val styleTitle = styles.first { it.id == selectedStyle }.title
                val totalCells = cols * rows
                
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Vista Previa en Hoja:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$cols col × $rows fil ($totalCells celdas) • $styleTitle",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Cuadrícula gráfica interactiva de alta legibilidad
                        TableLiveSheetPreview(
                            cols = cols,
                            rows = minOf(3, rows),
                            hasHeader = hasHeader,
                            style = selectedStyle
                        )

                        Text(
                            text = if (rows > 3) "Mostrando primeras 3 filas en vista previa de la hoja." else "Las celdas generadas son editables con texto enriquecido en la hoja.",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(rows, cols, hasHeader, selectedStyle) },
                modifier = Modifier.testTag("btn_confirm_insert_table")
            ) {
                Text("Insertar en Hoja", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun NumberSelectorRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    tagPrefix: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { if (value > min) onValueChange(value - 1) },
                enabled = value > min,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_dec_$tagPrefix")
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Reducir $label")
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(width = 44.dp, height = 36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$value",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            IconButton(
                onClick = { if (value < max) onValueChange(value + 1) },
                enabled = value < max,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_inc_$tagPrefix")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Aumentar $label")
            }
        }
    }
}

/**
 * Renderizador de vista previa nítida de la tabla física con contenido de muestra legible.
 */
@Composable
private fun TableLiveSheetPreview(
    cols: Int,
    rows: Int,
    hasHeader: Boolean,
    style: String
) {
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val paperColor = Color(0xFFFAF8F5) // Simulación de papel marfil claro
    val cellPadding = if (style == "compact") 4.dp else 7.dp

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = paperColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.4f)),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .then(
                    if (style != "editorial") {
                        Modifier.border(1.dp, borderColor, RoundedCornerShape(4.dp))
                    } else {
                        Modifier
                    }
                )
        ) {
            for (r in 0 until rows) {
                val isHeaderRow = (r == 0 && hasHeader)
                val isEven = (r % 2 == 0)

                val rowBg = when {
                    isHeaderRow -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    style == "striped" && isEven -> Color(0xFFE2E8F0).copy(alpha = 0.5f)
                    else -> Color.Transparent
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                        .then(
                            when {
                                isHeaderRow && style == "editorial" -> Modifier.border(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                r < rows - 1 && style == "editorial" -> Modifier.border(
                                    width = 0.8.dp,
                                    color = borderColor.copy(alpha = 0.4f)
                                )
                                r < rows - 1 -> Modifier.border(
                                    width = 0.5.dp,
                                    color = borderColor
                                )
                                else -> Modifier
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (c in 0 until cols) {
                        val cellText = if (isHeaderRow) "Col ${c + 1}" else "Dato ${r + 1},${c + 1}"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (c < cols - 1 && style != "editorial") {
                                        Modifier.border(0.5.dp, borderColor)
                                    } else Modifier
                                )
                                .padding(vertical = cellPadding, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cellText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = if (style == "compact") 9.sp else 10.5.sp,
                                    fontWeight = if (isHeaderRow) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isHeaderRow) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color(0xFF1E293B)
                                    },
                                    textAlign = TextAlign.Center
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class TableStyleOption(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val description: String
)
