package com.example.ui.components

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.FormatAlignLeft
import androidx.compose.material.icons.outlined.FormatAlignRight
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.paper.SheetNodesBlock
import com.example.ui.components.paper.SheetShapeBlock
import com.example.ui.components.paper.parseColorSafe

/**
 * Diálogo interactivo táctil para insertar Figuras Geométricas y Nodos tipo PC:
 * - Selección de figura (rectángulo, círculo, triángulo, rombo, estrella, flecha, llamada)
 * - Tamaño paramétrico (ancho y alto ajustables con preajustes rápidos o sliders)
 * - Ubicación / Dónde estará (Izquierda, Centro, Derecha o Ancho Completo)
 * - Paletas de relleno y contorno
 * - Diagramas de nodos conectores de flujo (horizontal o vertical)
 */
@Composable
fun InsertShapeOrNodeDialog(
    onDismiss: () -> Unit,
    onInsertText: (textToInsert: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Figuras, 1 = Nodos

    // --- ESTADO PARA FIGURAS GEOMÉTRICAS ---
    val shapeTypes = listOf(
        "rect" to "Rectángulo",
        "circle" to "Círculo",
        "triangle" to "Triángulo",
        "diamond" to "Rombo",
        "star" to "Estrella",
        "arrow" to "Flecha",
        "callout" to "Llamada"
    )
    var selectedShapeType by remember { mutableStateOf("rect") }
    var shapeWidth by remember { mutableFloatStateOf(240f) }
    var shapeHeight by remember { mutableFloatStateOf(95f) }
    var shapeAlign by remember { mutableStateOf("center") } // left, center, right, full
    var shapeFillColor by remember { mutableStateOf("#DBEAFE") }
    var shapeStrokeColor by remember { mutableStateOf("#2563EB") }
    var shapeBorderWidth by remember { mutableIntStateOf(2) }
    var shapeCornerRadius by remember { mutableIntStateOf(10) }
    var shapeText by remember { mutableStateOf("Título o Nota") }

    // --- ESTADO PARA DIAGRAMA DE NODOS ---
    val defaultNodes = remember { mutableStateListOf("Inicio", "Proceso Clave", "Finalización") }
    var newNodeInput by remember { mutableStateOf("") }
    var nodeAlign by remember { mutableStateOf("center") }
    var nodeLayout by remember { mutableStateOf("horizontal") } // horizontal, vertical
    var nodeFillColor by remember { mutableStateOf("#EFF6FF") }
    var nodeStrokeColor by remember { mutableStateOf("#2563EB") }

    // Paletas de colores rápidos
    val fillColors = listOf(
        "#DBEAFE" to "Azul",
        "#DCFCE7" to "Verde",
        "#FEE2E2" to "Rojo",
        "#FEF3C7" to "Ámbar",
        "#F3E8FF" to "Púrpura",
        "#FFEDD5" to "Naranja",
        "#F1F5F9" to "Gris",
        "#FFFFFF" to "Blanco"
    )

    val strokeColors = listOf(
        "#1D4ED8" to "Azul",
        "#16A34A" to "Verde",
        "#DC2626" to "Rojo",
        "#D97706" to "Ámbar",
        "#9333EA" to "Púrpura",
        "#EA580C" to "Naranja",
        "#334155" to "Pizarra",
        "#000000" to "Negro"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (selectedTab == 0) Icons.Outlined.Category else Icons.Outlined.Hub,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedTab == 0) "Insertar Figura Geométrica" else "Insertar Diagrama de Nodos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // PESTAÑAS DE MODO
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Figuras", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Outlined.Category, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Nodos", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Outlined.Hub, contentDescription = null) }
                    )
                }

                if (selectedTab == 0) {
                    // ==========================================
                    // MODO FIGURAS GEOMÉTRICAS
                    // ==========================================

                    // 1. Vista previa en tiempo real
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "VISTA PREVIA DE LA FIGURA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            SheetShapeBlock(
                                type = selectedShapeType,
                                widthDp = shapeWidth.toInt(),
                                heightDp = shapeHeight.toInt(),
                                align = shapeAlign,
                                fillColorHex = shapeFillColor,
                                strokeColorHex = shapeStrokeColor,
                                borderWidthDp = shapeBorderWidth,
                                cornerRadiusDp = shapeCornerRadius,
                                text = shapeText,
                                defaultFontFamily = FontFamily.Default
                            )
                        }
                    }

                    // 2. Tipo de Figura
                    Text(
                        text = "1. Tipo de Figura",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        shapeTypes.forEach { (typeKey, label) ->
                            FilterChip(
                                selected = selectedShapeType == typeKey,
                                onClick = { selectedShapeType = typeKey },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }

                    // 3. Ubicación (Dónde estará en la hoja)
                    Text(
                        text = "2. Ubicación en la Hoja",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("left", "Izquierda", Icons.Outlined.FormatAlignLeft),
                            Triple("center", "Centro", Icons.Outlined.FormatAlignCenter),
                            Triple("right", "Derecha", Icons.Outlined.FormatAlignRight),
                            Triple("full", "Ancho Total", Icons.Outlined.Category)
                        ).forEach { (alignKey, label, icon) ->
                            FilterChip(
                                selected = shapeAlign == alignKey,
                                onClick = { shapeAlign = alignKey },
                                label = { Text(label, fontSize = 11.sp) },
                                leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }

                    // 4. Tamaño (Ancho y Alto)
                    Text(
                        text = "3. Tamaño: Ancho (${shapeWidth.toInt()} dp) x Alto (${shapeHeight.toInt()} dp)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Preajustes de tamaño
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple(160f, 70f, "Pequeño"),
                            Triple(240f, 95f, "Mediano"),
                            Triple(320f, 130f, "Grande")
                        ).forEach { (w, h, label) ->
                            FilterChip(
                                selected = shapeWidth == w && shapeHeight == h,
                                onClick = {
                                    shapeWidth = w
                                    shapeHeight = h
                                },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Text("Ajuste fino de Ancho:", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = shapeWidth,
                        onValueChange = { shapeWidth = it },
                        valueRange = 100f..360f
                    )

                    Text("Ajuste fino de Alto:", style = MaterialTheme.typography.labelSmall)
                    Slider(
                        value = shapeHeight,
                        onValueChange = { shapeHeight = it },
                        valueRange = 40f..200f
                    )

                    // 5. Colores de Relleno y Trazo
                    Text(
                        text = "4. Color de Relleno y Borde",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text("Relleno:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fillColors.forEach { (hex, _) ->
                            val col = parseColorSafe(hex, Color.LightGray)
                            val isSelected = shapeFillColor.equals(hex, true)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { shapeFillColor = hex }
                            )
                        }
                    }

                    Text("Borde / Contorno:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        strokeColors.forEach { (hex, _) ->
                            val col = parseColorSafe(hex, Color.Black)
                            val isSelected = shapeStrokeColor.equals(hex, true)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { shapeStrokeColor = hex }
                            )
                        }
                    }

                    // 6. Texto en la figura
                    OutlinedTextField(
                        value = shapeText,
                        onValueChange = { shapeText = it },
                        label = { Text("Texto dentro de la figura (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                } else {
                    // ==========================================
                    // MODO DIAGRAMA DE NODOS
                    // ==========================================

                    // 1. Vista previa en tiempo real
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "VISTA PREVIA DEL FLUJO DE NODOS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            SheetNodesBlock(
                                nodes = defaultNodes.toList(),
                                align = nodeAlign,
                                layout = nodeLayout,
                                fillColorHex = nodeFillColor,
                                strokeColorHex = nodeStrokeColor,
                                defaultFontFamily = FontFamily.Default
                            )
                        }
                    }

                    // 2. Disposición y Alineación
                    Text(
                        text = "1. Orientación del Flujo",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "horizontal" to "Horizontal (➔)",
                            "vertical" to "Vertical (↓)"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = nodeLayout == key,
                                onClick = { nodeLayout = key },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }

                    Text(
                        text = "2. Ubicación en la Hoja",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "left" to "Izquierda",
                            "center" to "Centro",
                            "right" to "Derecha"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = nodeAlign == key,
                                onClick = { nodeAlign = key },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }

                    // 3. Gestión interactiva de Nodos
                    Text(
                        text = "3. Secuencia de Nodos (${defaultNodes.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newNodeInput,
                            onValueChange = { newNodeInput = it },
                            label = { Text("Nuevo nodo / paso") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newNodeInput.isNotBlank()) {
                                    defaultNodes.add(newNodeInput.trim())
                                    newNodeInput = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Añadir nodo")
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        defaultNodes.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${index + 1}. $item",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (defaultNodes.size > 1) {
                                    IconButton(
                                        onClick = { defaultNodes.removeAt(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Eliminar",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Colores
                    Text(
                        text = "4. Paleta de Color",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text("Relleno de cápsula:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fillColors.forEach { (hex, _) ->
                            val col = parseColorSafe(hex, Color.LightGray)
                            val isSelected = nodeFillColor.equals(hex, true)
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { nodeFillColor = hex }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTab == 0) {
                        // Generar bloque de figura
                        val shapeDirective = buildString {
                            append("[shape:type=$selectedShapeType")
                            append(",w=${shapeWidth.toInt()}")
                            append(",h=${shapeHeight.toInt()}")
                            append(",align=$shapeAlign")
                            append(",fill=$shapeFillColor")
                            append(",stroke=$shapeStrokeColor")
                            append(",border=$shapeBorderWidth")
                            append(",radius=$shapeCornerRadius")
                            if (shapeText.isNotBlank()) {
                                append(",text=\"${shapeText.trim()}\"")
                            }
                            append("]")
                        }
                        onInsertText("\n$shapeDirective\n")
                    } else {
                        // Generar bloque de diagrama de nodos
                        val nodesDirective = buildString {
                            append("[nodes:align=$nodeAlign,layout=$nodeLayout,fill=$nodeFillColor,stroke=$nodeStrokeColor]\n")
                            append(defaultNodes.joinToString(" -> "))
                            append("\n[/nodes]")
                        }
                        onInsertText("\n$nodesDirective\n")
                    }
                },
                modifier = Modifier.testTag("insert_shape_node_confirm_btn")
            ) {
                Text(
                    text = if (selectedTab == 0) "Insertar Figura" else "Insertar Nodos",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
