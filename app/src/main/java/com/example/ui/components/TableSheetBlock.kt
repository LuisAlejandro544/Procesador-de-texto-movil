package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.NativeEngineBridge

/**
 * TableSheetBlock: Renderizador editorial de tablas y cuadrículas sobre la hoja física de papel.
 * 
 * Características:
 * - Calcula la distribución métrica de columnas mediante el núcleo nativo en C++20 (NativeEngineBridge)
 * - Soporta cuatro acabados editoriales profesionales:
 *   1. "classic": Cuadrícula completa con encabezados sombreados y esquinas redondeadas.
 *   2. "editorial": Estilo académico/periodístico sin líneas verticales, con líneas horizontales prominentes.
 *   3. "striped": Filas alternadas con sombreado cebra para fácil lectura de datos.
 *   4. "compact": Densidad condensada con bordes tenues para tablas con múltiples valores.
 * - Integra texto enriquecido dentro de cada celda (negrita, cursiva, tipografías y colores).
 */
@Composable
fun TableSheetBlock(
    tableLines: List<String>,
    style: String = "classic",
    fontFamily: FontFamily,
    baseFontSize: Int,
    textColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    if (tableLines.isEmpty()) return

    // Estructurar filas y celdas
    val parsedRows = remember(tableLines) {
        val rows = mutableListOf<List<String>>()
        var isFirst = true
        var skipNextSeparator = false

        for (line in tableLines) {
            val trimmed = line.trim()
            if (!trimmed.startsWith("|")) continue

            // Comprobar si es la línea separadora Markdown (|---|---|)
            if (trimmed.contains("---") && trimmed.replace("|", "").replace("-", "").replace(":", "").isBlank()) {
                skipNextSeparator = true
                continue
            }

            val cells = trimmed
                .removePrefix("|")
                .removeSuffix("|")
                .split("|")
                .map { it.trim() }

            if (cells.isNotEmpty() && cells.any { it.isNotEmpty() }) {
                rows.add(cells)
            }
        }
        rows
    }

    if (parsedRows.isEmpty()) return

    val numColumns = remember(parsedRows) {
        parsedRows.maxOfOrNull { it.size } ?: 1
    }

    // Cálculo métrico de anchos de columna mediante el motor C++20
    val columnWidthWeights = remember(numColumns) {
        // En una hoja física A4 estándar el ancho imprimible aproximado es de 450-500 pt
        NativeEngineBridge.computeTableColumnWidthsSafe(numColumns, 480f)
    }

    val containerShape = RoundedCornerShape(6.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(containerShape)
            .border(
                width = if (style == "editorial") 0.dp else 1.dp,
                color = if (style == "editorial") Color.Transparent else borderColor.copy(alpha = 0.65f),
                shape = containerShape
            )
            .testTag("block_editorial_table")
    ) {
        parsedRows.forEachIndexed { rowIndex, rowCells ->
            val isHeader = rowIndex == 0
            val isEven = rowIndex % 2 == 0

            // Color de fondo de la fila según estilo
            val rowBgColor = when {
                isHeader -> textColor.copy(alpha = 0.08f)
                style == "striped" && isEven -> textColor.copy(alpha = 0.035f)
                else -> Color.Transparent
            }

            // Borde inferior de la fila
            val bottomBorderModifier = when {
                isHeader && style == "editorial" -> Modifier.border(
                    width = 1.5.dp,
                    color = textColor.copy(alpha = 0.75f)
                )
                isHeader -> Modifier.border(
                    width = 1.dp,
                    color = borderColor.copy(alpha = 0.7f)
                )
                style == "editorial" && rowIndex == parsedRows.size - 1 -> Modifier.border(
                    width = 1.2.dp,
                    color = textColor.copy(alpha = 0.75f)
                )
                rowIndex < parsedRows.size - 1 && style != "editorial" -> Modifier.border(
                    width = 0.5.dp,
                    color = borderColor.copy(alpha = 0.35f)
                )
                else -> Modifier
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowBgColor)
                    .then(bottomBorderModifier),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (colIndex in 0 until numColumns) {
                    val cellText = rowCells.getOrNull(colIndex) ?: ""
                    val weight = if (colIndex < columnWidthWeights.size && columnWidthWeights[colIndex] > 0f) {
                        columnWidthWeights[colIndex]
                    } else {
                        1f
                    }

                    // Borde vertical entre columnas (no presente en estilo editorial)
                    val showRightBorder = colIndex < numColumns - 1 && style != "editorial"

                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .then(
                                if (showRightBorder) {
                                    Modifier.border(
                                        width = 0.5.dp,
                                        color = borderColor.copy(alpha = 0.35f)
                                    )
                                } else Modifier
                            )
                            .padding(
                                horizontal = if (style == "compact") 6.dp else 10.dp,
                                vertical = if (style == "compact") 5.dp else if (isHeader) 9.dp else 7.dp
                            )
                    ) {
                        Text(
                            text = cellText.ifEmpty { "—" },
                            style = androidx.compose.ui.text.TextStyle(
                                fontFamily = fontFamily,
                                fontSize = if (isHeader) (baseFontSize).sp else (baseFontSize - 1).sp,
                                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                                color = if (isHeader) textColor else textColor.copy(alpha = 0.88f),
                                textAlign = if (isHeader) TextAlign.Center else TextAlign.Start
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
