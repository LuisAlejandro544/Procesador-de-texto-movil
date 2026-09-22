package com.example.ui.components.paper

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TableSheetBlock

sealed class SheetBlock {
    data class Paragraph(val rawLine: String) : SheetBlock()
    data class Image(val uri: String, val wrapMode: String, val caption: String) : SheetBlock()
    data class Table(val lines: List<String>, val style: String) : SheetBlock()
    data class Shape(
        val type: String,
        val widthDp: Int,
        val heightDp: Int,
        val align: String,
        val fillColorHex: String,
        val strokeColorHex: String,
        val borderWidthDp: Int,
        val cornerRadiusDp: Int,
        val text: String
    ) : SheetBlock()
    data class Nodes(
        val nodes: List<String>,
        val align: String,
        val layout: String,
        val fillColorHex: String,
        val strokeColorHex: String
    ) : SheetBlock()
}

/**
 * Renderiza el texto de lectura con formato estructurado, ajuste de hoja (Layout & Wrap de imágenes),
 * alineación de párrafo cuádruple con justificado real y estilos tipográficos avanzados.
 */
@Composable
fun FormattedSheetContent(
    text: String,
    fontFamily: FontFamily,
    baseFontSize: Int,
    lineHeight: TextUnit,
    textColor: Color,
    docAlignment: String = "LEFT",
    borderColor: Color = Color.LightGray,
    modifier: Modifier = Modifier
) {
    val defaultTextAlign = when (docAlignment.uppercase()) {
        "JUSTIFY" -> TextAlign.Justify
        "CENTER" -> TextAlign.Center
        "RIGHT" -> TextAlign.Right
        else -> TextAlign.Left
    }

    // Estructuración de líneas en bloques (Párrafos, Imágenes y Tablas Editoriales)
    val blocks = remember(text) {
        val lines = text.split("\n")
        val result = mutableListOf<SheetBlock>()
        var i = 0

        while (i < lines.size) {
            val rawLine = lines[i]
            val trimmedLine = rawLine.trim()

            // 1. Bloque de tabla explícito con estilo: [table:style] ... [/table]
            if (trimmedLine.startsWith("[table:") && trimmedLine.endsWith("]")) {
                val style = trimmedLine.removePrefix("[table:").removeSuffix("]").trim()
                val tableLines = mutableListOf<String>()
                i++
                while (i < lines.size && lines[i].trim() != "[/table]") {
                    if (lines[i].trim().startsWith("|")) {
                        tableLines.add(lines[i])
                    }
                    i++
                }
                result.add(SheetBlock.Table(tableLines, style.ifEmpty { "classic" }))
                i++
                continue
            }

            // 2. Bloque de tabla Markdown estándar implícito (líneas consecutivas que inician con '|')
            if (trimmedLine.startsWith("|") && trimmedLine.endsWith("|") && trimmedLine.length > 2) {
                val tableLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith("|") && lines[i].trim().endsWith("|")) {
                    tableLines.add(lines[i])
                    i++
                }
                result.add(SheetBlock.Table(tableLines, "classic"))
                continue
            }

            // 3. Bloque de imagen con ajuste
            val wrapImageRegex = Regex("""^!\[(?:wrap:(full|center|left|right)(?:,([^\]]*))?)?\]\(([^)]+)\)""")
            val simpleImageRegex = Regex("""^!\[([^\]]*)\]\(([^)]+)\)""")

            val wrapMatch = wrapImageRegex.find(trimmedLine)
            val simpleMatch = if (wrapMatch == null) simpleImageRegex.find(trimmedLine) else null

            if (wrapMatch != null) {
                val wrapMode = wrapMatch.groupValues[1].ifEmpty { "full" }
                val caption = wrapMatch.groupValues[2]
                val uri = wrapMatch.groupValues[3]
                result.add(SheetBlock.Image(uri, wrapMode, caption))
                i++
                continue
            } else if (simpleMatch != null) {
                val caption = simpleMatch.groupValues[1]
                val uri = simpleMatch.groupValues[2]
                result.add(SheetBlock.Image(uri, "full", caption))
                i++
                continue
            }

            // 4. Bloque de figura geométrica: [shape:...]
            if (trimmedLine.startsWith("[shape:", ignoreCase = true) && trimmedLine.endsWith("]")) {
                val paramsStr = trimmedLine.removePrefix("[shape:").removeSuffix("]").trim()
                result.add(parseShapeDirective(paramsStr))
                i++
                continue
            }

            // 5. Bloque de diagrama de nodos: [nodes...] ... [/nodes]
            if (trimmedLine.startsWith("[nodes", ignoreCase = true) && trimmedLine.endsWith("]")) {
                val header = trimmedLine.removePrefix("[nodes").removeSuffix("]").trim()
                val paramsStr = if (header.startsWith(":")) header.removePrefix(":") else ""
                val nodeLines = mutableListOf<String>()
                i++
                while (i < lines.size && lines[i].trim() != "[/nodes]") {
                    val nl = lines[i].trim()
                    if (nl.isNotEmpty()) {
                        nodeLines.add(nl)
                    }
                    i++
                }
                result.add(parseNodesDirective(paramsStr, nodeLines))
                i++
                continue
            }

            // 6. Párrafo estándar
            result.add(SheetBlock.Paragraph(rawLine))
            i++
        }
        result
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (block in blocks) {
            when (block) {
                is SheetBlock.Shape -> {
                    SheetShapeBlock(
                        type = block.type,
                        widthDp = block.widthDp,
                        heightDp = block.heightDp,
                        align = block.align,
                        fillColorHex = block.fillColorHex,
                        strokeColorHex = block.strokeColorHex,
                        borderWidthDp = block.borderWidthDp,
                        cornerRadiusDp = block.cornerRadiusDp,
                        text = block.text,
                        defaultFontFamily = fontFamily
                    )
                }
                is SheetBlock.Nodes -> {
                    SheetNodesBlock(
                        nodes = block.nodes,
                        align = block.align,
                        layout = block.layout,
                        fillColorHex = block.fillColorHex,
                        strokeColorHex = block.strokeColorHex,
                        defaultFontFamily = fontFamily
                    )
                }
                is SheetBlock.Table -> {
                    TableSheetBlock(
                        tableLines = block.lines,
                        style = block.style,
                        fontFamily = fontFamily,
                        baseFontSize = baseFontSize,
                        textColor = textColor,
                        borderColor = borderColor
                    )
                }
                is SheetBlock.Image -> {
                    SheetImageBlock(
                        uri = block.uri,
                        wrapMode = block.wrapMode,
                        caption = block.caption,
                        borderColor = borderColor,
                        textColor = textColor
                    )
                }
                is SheetBlock.Paragraph -> {
                    val rawLine = block.rawLine
                    val trimmedLine = rawLine.trim()

                    // Comprobar directiva de alineación específica para este párrafo: [align:xxx]...[/align]
                    val alignRegex = Regex("""\[align:(left|center|right|justify)\](.*?)\[/align\]""", RegexOption.DOT_MATCHES_ALL)
                    val alignMatch = alignRegex.find(trimmedLine)

                    val (effectiveLine, effectiveAlign) = if (alignMatch != null) {
                        val mode = alignMatch.groupValues[1].lowercase()
                        val content = trimmedLine.replace(alignMatch.value, alignMatch.groupValues[2]).trim()
                        val align = when (mode) {
                            "justify" -> TextAlign.Justify
                            "center" -> TextAlign.Center
                            "right" -> TextAlign.Right
                            else -> TextAlign.Left
                        }
                        Pair(content, align)
                    } else {
                        val align = if (defaultTextAlign == TextAlign.Justify) {
                            val words = rawLine.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }
                            if (words.size < 7 || rawLine.length < 45) TextAlign.Start else TextAlign.Justify
                        } else {
                            defaultTextAlign
                        }
                        Pair(rawLine, align)
                    }

                    when {
                        // Título H1
                        effectiveLine.startsWith("# ") -> {
                            Text(
                                text = effectiveLine.removePrefix("# ").trim(),
                                style = TextStyle(
                                    fontFamily = fontFamily,
                                    fontSize = (baseFontSize + 7).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    lineHeight = (baseFontSize + 11).sp,
                                    textAlign = effectiveAlign
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        // Subtítulo H2
                        effectiveLine.startsWith("## ") -> {
                            Text(
                                text = effectiveLine.removePrefix("## ").trim(),
                                style = TextStyle(
                                    fontFamily = fontFamily,
                                    fontSize = (baseFontSize + 4).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    lineHeight = (baseFontSize + 8).sp,
                                    textAlign = effectiveAlign
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, bottom = 2.dp)
                            )
                        }
                        // Apartado H3
                        effectiveLine.startsWith("### ") -> {
                            Text(
                                text = effectiveLine.removePrefix("### ").trim(),
                                style = TextStyle(
                                    fontFamily = fontFamily,
                                    fontSize = (baseFontSize + 2).sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor,
                                    lineHeight = (baseFontSize + 5).sp,
                                    textAlign = effectiveAlign
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 2.dp)
                            )
                        }
                        // Cita destacada
                        effectiveLine.startsWith("> ") -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(22.dp)
                                        .background(textColor.copy(alpha = 0.4f), RoundedCornerShape(1.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = effectiveLine.removePrefix("> ").trim(),
                                    style = TextStyle(
                                        fontFamily = fontFamily,
                                        fontSize = baseFontSize.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = textColor.copy(alpha = 0.85f),
                                        lineHeight = lineHeight,
                                        textAlign = effectiveAlign
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        // Tareas con casillas [ ] o [x]
                        effectiveLine.startsWith("[ ] ") -> {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    tint = textColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = effectiveLine.removePrefix("[ ] ").trim(),
                                    style = TextStyle(fontFamily = fontFamily, fontSize = baseFontSize.sp, color = textColor)
                                )
                            }
                        }
                        effectiveLine.startsWith("[x] ") || effectiveLine.startsWith("[X] ") -> {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = effectiveLine.substring(4).trim(),
                                    style = TextStyle(
                                        fontFamily = fontFamily,
                                        fontSize = baseFontSize.sp,
                                        color = textColor.copy(alpha = 0.5f),
                                        textDecoration = TextDecoration.LineThrough
                                    )
                                )
                            }
                        }
                        // Separador horizontal
                        effectiveLine.startsWith("───") || effectiveLine.startsWith("---") -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .height(1.dp)
                                    .background(textColor.copy(alpha = 0.2f))
                            )
                        }
                        // Párrafo normal con soporte de texto enriquecido completo
                        else -> {
                            val annotatedString = parseRichInlineText(
                                text = effectiveLine,
                                defaultFontFamily = fontFamily,
                                baseFontSize = baseFontSize,
                                defaultColor = textColor
                            )
                            Text(
                                text = annotatedString,
                                style = TextStyle(
                                    fontFamily = fontFamily,
                                    fontSize = baseFontSize.sp,
                                    lineHeight = lineHeight,
                                    color = textColor,
                                    textAlign = effectiveAlign
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Parsea los parámetros de directiva de figura: [shape:type=rect,w=220,h=100,align=center,fill=#DBEAFE,stroke=#2563EB,border=2,radius=10,text="Mi Figura"]
 */
fun parseShapeDirective(paramsStr: String): SheetBlock.Shape {
    var type = "rect"
    var w = 220
    var h = 90
    var align = "center"
    var fill = "#DBEAFE"
    var stroke = "#2563EB"
    var border = 2
    var radius = 10
    var text = ""

    val tokens = paramsStr.split(",")
    for (t in tokens) {
        val trimmed = t.trim()
        if (trimmed.isEmpty()) continue
        if (trimmed.contains("=")) {
            val key = trimmed.substringBefore("=").trim().lowercase()
            val value = trimmed.substringAfter("=").trim().removeSurrounding("\"").removeSurrounding("'")
            when (key) {
                "type" -> type = value.lowercase()
                "w", "width" -> w = value.toIntOrNull() ?: 220
                "h", "height" -> h = value.toIntOrNull() ?: 90
                "align" -> align = value.lowercase()
                "fill" -> fill = value
                "stroke" -> stroke = value
                "border" -> border = value.toIntOrNull() ?: 2
                "radius" -> radius = value.toIntOrNull() ?: 10
                "text" -> text = value
            }
        } else {
            val lower = trimmed.lowercase()
            if (lower in listOf("rect", "circle", "oval", "triangle", "diamond", "rombo", "star", "arrow", "callout")) {
                type = lower
            }
        }
    }

    return SheetBlock.Shape(
        type = type,
        widthDp = w,
        heightDp = h,
        align = align,
        fillColorHex = fill,
        strokeColorHex = stroke,
        borderWidthDp = border,
        cornerRadiusDp = radius,
        text = text
    )
}

/**
 * Parsea los parámetros y las líneas conectadas de un diagrama de nodos.
 */
fun parseNodesDirective(paramsStr: String, lines: List<String>): SheetBlock.Nodes {
    var align = "center"
    var layout = "horizontal"
    var fill = "#F1F5F9"
    var stroke = "#2563EB"

    val tokens = paramsStr.split(",")
    for (t in tokens) {
        val trimmed = t.trim()
        if (trimmed.isEmpty()) continue
        if (trimmed.contains("=")) {
            val key = trimmed.substringBefore("=").trim().lowercase()
            val value = trimmed.substringAfter("=").trim().removeSurrounding("\"").removeSurrounding("'")
            when (key) {
                "align" -> align = value.lowercase()
                "layout" -> layout = value.lowercase()
                "fill" -> fill = value
                "stroke" -> stroke = value
            }
        }
    }

    val nodesList = mutableListOf<String>()
    for (l in lines) {
        val parts = l.split("->").map { it.trim() }.filter { it.isNotEmpty() }
        nodesList.addAll(parts)
    }

    return SheetBlock.Nodes(
        nodes = if (nodesList.isEmpty()) listOf("Paso 1", "Paso 2") else nodesList,
        align = align,
        layout = layout,
        fillColorHex = fill,
        strokeColorHex = stroke
    )
}

