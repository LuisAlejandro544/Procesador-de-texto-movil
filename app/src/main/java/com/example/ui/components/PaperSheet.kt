package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * PaperSheet: Representación visual y táctil de una o múltiples hojas de papel de procesador de texto.
 * 
 * Funcionalidades clave:
 * - Soporte para Paginación Continua en Cascada (Múltiples Hojas físicas apiladas)
 * - Regla horizontal superior con marcas graduadas en centímetros
 * - Marcas guía de recorte/márgenes en las cuatro esquinas (L-corners estilo PC)
 * - Texturas de papel: Blanco, Marfil, Rayado, Cuadriculado, Sepia, Carbón
 * - Tipografías: Serif, Sans-Serif, Monospace y Cursive (Caligráfica)
 * - Renderizado de formato estructurado: Títulos (#, ##, ###), Citas (>), Viñetas (•), Listas y Tareas ([ ], [x])
 */
@Composable
fun PaperSheet(
    content: String,
    onContentChange: (String) -> Unit,
    title: String,
    paperType: String,
    fontStyle: String,
    fontSize: Int,
    lineSpacing: Float,
    marginStyle: String,
    isReadOnly: Boolean,
    modifier: Modifier = Modifier,
    isCascadeMode: Boolean = true,
    onAddPage: (() -> Unit)? = null
) {
    // Configuración de colores del papel
    val (paperBgColor, paperTextColor, paperBorderColor, gridLineColor) = when (paperType) {
        "IVORY" -> Quadruple(
            Color(0xFFFAF7F0), // Marfil cálido
            Color(0xFF2B2620), // Tinta marrón oscuro
            Color(0xFFE4DEC8),
            Color(0x1F7A6A4E)
        )
        "LINED" -> Quadruple(
            Color(0xFFFCFDFD), // Blanco suave con rayas
            Color(0xFF1E293B),
            Color(0xFFE2E8F0),
            Color(0x3360A5FA) // Azul tenue para líneas
        )
        "GRID" -> Quadruple(
            Color(0xFFFAFBFC), // Cuadriculado milimetrado tenue
            Color(0xFF0F172A),
            Color(0xFFCBD5E1),
            Color(0x243B82F6)
        )
        "SEPIA" -> Quadruple(
            Color(0xFFF5EEDC), // Papiro sepia vintage
            Color(0xFF382A1D),
            Color(0xFFDAC7AB),
            Color(0x1F6B4E2E)
        )
        "DARK" -> Quadruple(
            Color(0xFF1E2124), // Modo noche tipo carbón
            Color(0xFFF1F5F9),
            Color(0xFF33383F),
            Color(0x1FFFFFFF)
        )
        else -> Quadruple(
            Color(0xFFFFFFFF), // Blanco clásico de oficina
            Color(0xFF1A1A1A), // Tinta negra suave
            Color(0xFFE2E8F0),
            Color(0x1F000000)
        )
    }

    // Configuración de familia tipográfica ampliada
    val selectedFontFamily = when (fontStyle) {
        "SANS_SERIF" -> FontFamily.SansSerif
        "MONOSPACE" -> FontFamily.Monospace
        "CURSIVE" -> FontFamily.Cursive
        else -> FontFamily.Serif
    }

    // Márgenes laterales de la hoja
    val horizontalMargin: Dp = when (marginStyle) {
        "NARROW" -> 16.dp
        "WIDE" -> 36.dp
        else -> 24.dp
    }

    val calculatedLineHeight = (fontSize * lineSpacing).sp

    if (isCascadeMode) {
        // --- Modo Cascada Continua (Múltiples Hojas de Papel) ---
        val pages = remember(content) {
            partitionIntoPages(content)
        }
        val totalPages = pages.size

        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            pages.forEachIndexed { index, pageContent ->
                val pageNumber = index + 1

                SingleSheetCard(
                    pageText = pageContent,
                    onPageTextChange = { newPageText ->
                        val updatedPages = pages.toMutableList()
                        updatedPages[index] = newPageText
                        onContentChange(joinPages(updatedPages))
                    },
                    pageNumber = pageNumber,
                    totalPages = totalPages,
                    title = title,
                    paperBgColor = paperBgColor,
                    paperTextColor = paperTextColor,
                    paperBorderColor = paperBorderColor,
                    gridLineColor = gridLineColor,
                    paperType = paperType,
                    selectedFontFamily = selectedFontFamily,
                    fontSize = fontSize,
                    calculatedLineHeight = calculatedLineHeight,
                    horizontalMargin = horizontalMargin,
                    isReadOnly = isReadOnly,
                    showRuler = (pageNumber == 1) // Regla principal en la primera hoja
                )

                // Separador visual de escritorio entre hojas
                if (pageNumber < totalPages) {
                    CascadePageDivider(
                        currentPage = pageNumber,
                        nextPage = pageNumber + 1
                    )
                }
            }

            // Botón de salto / adición de nueva hoja física
            onAddPage?.let { addAction ->
                OutlinedButton(
                    onClick = addAction,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .testTag("add_page_cascade_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Añadir Hoja Siguiente", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    } else {
        // --- Modo Hoja Única ---
        SingleSheetCard(
            pageText = content,
            onPageTextChange = onContentChange,
            pageNumber = 1,
            totalPages = 1,
            title = title,
            paperBgColor = paperBgColor,
            paperTextColor = paperTextColor,
            paperBorderColor = paperBorderColor,
            gridLineColor = gridLineColor,
            paperType = paperType,
            selectedFontFamily = selectedFontFamily,
            fontSize = fontSize,
            calculatedLineHeight = calculatedLineHeight,
            horizontalMargin = horizontalMargin,
            isReadOnly = isReadOnly,
            showRuler = true,
            modifier = modifier
        )
    }
}

/**
 * Renderiza una hoja de papel individual completa (portada o subsiguiente) con todas sus guías y reglas.
 */
@Composable
private fun SingleSheetCard(
    pageText: String,
    onPageTextChange: (String) -> Unit,
    pageNumber: Int,
    totalPages: Int,
    title: String,
    paperBgColor: Color,
    paperTextColor: Color,
    paperBorderColor: Color,
    gridLineColor: Color,
    paperType: String,
    selectedFontFamily: FontFamily,
    fontSize: Int,
    calculatedLineHeight: androidx.compose.ui.unit.TextUnit,
    horizontalMargin: Dp,
    isReadOnly: Boolean,
    showRuler: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(2.dp),
                ambientColor = Color(0x33000000),
                spotColor = Color(0x44000000)
            )
            .background(paperBgColor, RoundedCornerShape(2.dp))
            .border(1.dp, paperBorderColor, RoundedCornerShape(2.dp))
            .drawBehind {
                if (paperType == "LINED") {
                    val lineSpacingPx = 32.dp.toPx()
                    val startY = if (showRuler) 80.dp.toPx() else 60.dp.toPx()
                    var y = startY
                    while (y < size.height - 40.dp.toPx()) {
                        drawLine(
                            color = gridLineColor,
                            start = Offset(horizontalMargin.toPx(), y),
                            end = Offset(size.width - horizontalMargin.toPx(), y),
                            strokeWidth = 1.dp.toPx()
                        )
                        y += lineSpacingPx
                    }
                } else if (paperType == "GRID") {
                    val gridSizePx = 20.dp.toPx()
                    var x = horizontalMargin.toPx()
                    while (x < size.width - horizontalMargin.toPx()) {
                        drawLine(
                            color = gridLineColor,
                            start = Offset(x, 40.dp.toPx()),
                            end = Offset(x, size.height - 30.dp.toPx()),
                            strokeWidth = 0.8f
                        )
                        x += gridSizePx
                    }
                    var y = 40.dp.toPx()
                    while (y < size.height - 30.dp.toPx()) {
                        drawLine(
                            color = gridLineColor,
                            start = Offset(horizontalMargin.toPx(), y),
                            end = Offset(size.width - horizontalMargin.toPx(), y),
                            strokeWidth = 0.8f
                        )
                        y += gridSizePx
                    }
                }
            }
    ) {
        // Regla horizontal si corresponde
        if (showRuler) {
            PageRuler(
                paperBorderColor = paperBorderColor,
                accentColor = paperTextColor.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Cabecera de la hoja y marcas guía superiores
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalMargin, vertical = 12.dp)
        ) {
            CornerMarginGuide(
                color = paperTextColor.copy(alpha = 0.25f),
                isTop = true,
                isLeft = true,
                modifier = Modifier.align(Alignment.TopStart)
            )

            Text(
                text = if (pageNumber == 1) title.ifBlank { "Documento sin título" }.uppercase() else "DOCUSHEET — HOJA $pageNumber",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    color = paperTextColor.copy(alpha = 0.4f)
                ),
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            )

            CornerMarginGuide(
                color = paperTextColor.copy(alpha = 0.25f),
                isTop = true,
                isLeft = false,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }

        // Cuerpo de escritura
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 450.dp)
                .padding(horizontal = horizontalMargin, vertical = 8.dp)
        ) {
            if (isReadOnly) {
                // Modo Lectura con renderizado enriquecido de párrafos, títulos y listas
                FormattedSheetContent(
                    text = if (pageText.isEmpty()) "Hoja en blanco" else pageText,
                    fontFamily = selectedFontFamily,
                    baseFontSize = fontSize,
                    lineHeight = calculatedLineHeight,
                    textColor = if (pageText.isEmpty()) paperTextColor.copy(alpha = 0.4f) else paperTextColor,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Modo Edición interactivo fluido
                BasicTextField(
                    value = pageText,
                    onValueChange = onPageTextChange,
                    textStyle = TextStyle(
                        fontFamily = selectedFontFamily,
                        fontSize = fontSize.sp,
                        lineHeight = calculatedLineHeight,
                        color = paperTextColor,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(if (paperType == "DARK") Color(0xFF60A5FA) else Color(0xFF1D4ED8)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 450.dp)
                        .testTag("paper_editor_field_$pageNumber"),
                    decorationBox = { innerTextField ->
                        if (pageText.isEmpty()) {
                            Text(
                                text = "Escribe aquí tu texto...",
                                style = TextStyle(
                                    fontFamily = selectedFontFamily,
                                    fontSize = fontSize.sp,
                                    lineHeight = calculatedLineHeight,
                                    color = paperTextColor.copy(alpha = 0.35f)
                                )
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        // Pie de página con numeración formal de hoja
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalMargin, vertical = 14.dp)
        ) {
            CornerMarginGuide(
                color = paperTextColor.copy(alpha = 0.25f),
                isTop = false,
                isLeft = true,
                modifier = Modifier.align(Alignment.BottomStart)
            )

            Text(
                text = if (totalPages > 1) "— Página $pageNumber de $totalPages —" else "— Página 1 —",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = paperTextColor.copy(alpha = 0.45f)
                ),
                modifier = Modifier.align(Alignment.Center)
            )

            CornerMarginGuide(
                color = paperTextColor.copy(alpha = 0.25f),
                isTop = false,
                isLeft = false,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

/**
 * Renderiza el texto de lectura con formato estructurado (Títulos #, ##, Citas >, Viñetas y Negritas).
 */
@Composable
private fun FormattedSheetContent(
    text: String,
    fontFamily: FontFamily,
    baseFontSize: Int,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val lines = remember(text) { text.split("\n") }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { line ->
            when {
                // Título H1
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# ").trim(),
                        style = TextStyle(
                            fontFamily = fontFamily,
                            fontSize = (baseFontSize + 7).sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            lineHeight = (baseFontSize + 11).sp
                        ),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                // Subtítulo H2
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## ").trim(),
                        style = TextStyle(
                            fontFamily = fontFamily,
                            fontSize = (baseFontSize + 4).sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            lineHeight = (baseFontSize + 8).sp
                        ),
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                // Apartado H3
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### ").trim(),
                        style = TextStyle(
                            fontFamily = fontFamily,
                            fontSize = (baseFontSize + 2).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            lineHeight = (baseFontSize + 5).sp
                        ),
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                // Cita destacada
                line.startsWith("> ") -> {
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
                            text = line.removePrefix("> ").trim(),
                            style = TextStyle(
                                fontFamily = fontFamily,
                                fontSize = baseFontSize.sp,
                                fontStyle = FontStyle.Italic,
                                color = textColor.copy(alpha = 0.85f),
                                lineHeight = lineHeight
                            )
                        )
                    }
                }
                // Tareas con casillas [ ] o [x]
                line.startsWith("[ ] ") -> {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = line.removePrefix("[ ] ").trim(),
                            style = TextStyle(fontFamily = fontFamily, fontSize = baseFontSize.sp, color = textColor)
                        )
                    }
                }
                line.startsWith("[x] ") || line.startsWith("[X] ") -> {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = line.substring(4).trim(),
                            style = TextStyle(
                                fontFamily = fontFamily,
                                fontSize = baseFontSize.sp,
                                color = textColor.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                }
                // Párrafo normal con soporte de negritas sencillas
                else -> {
                    val annotatedString = buildAnnotatedString {
                        var currentIndex = 0
                        val boldPattern = Regex("\\*\\*(.*?)\\*\\*")
                        val matches = boldPattern.findAll(line)
                        for (match in matches) {
                            if (match.range.first > currentIndex) {
                                append(line.substring(currentIndex, match.range.first))
                            }
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(match.groupValues[1])
                            }
                            currentIndex = match.range.last + 1
                        }
                        if (currentIndex < line.length) {
                            append(line.substring(currentIndex))
                        }
                    }
                    val finalAnnotated = if (annotatedString.isEmpty()) buildAnnotatedString { append(" ") } else annotatedString
                    Text(
                        text = finalAnnotated,
                        style = TextStyle(
                            fontFamily = fontFamily,
                            fontSize = baseFontSize.sp,
                            lineHeight = lineHeight,
                            color = textColor
                        )
                    )
                }
            }
        }
    }
}

/**
 * Separador visual que indica el final de una hoja y el inicio de la siguiente en vista cascada.
 */
@Composable
private fun CascadePageDivider(
    currentPage: Int,
    nextPage: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 1.dp,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = "Hoja $currentPage  ▼  Hoja $nextPage",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )
    }
}

/**
 * Divide el texto en hojas lógicas para la vista en cascada.
 */
private fun partitionIntoPages(content: String): List<String> {
    if (content.isBlank()) return listOf("")
    val explicitPages = content.split(Regex("\\n\\[--- Salto de Página ---\\]\\n|\\n---\\n"))
    val result = mutableListOf<String>()
    val charLimitPerPage = 1400

    for (part in explicitPages) {
        if (part.length <= charLimitPerPage) {
            result.add(part)
        } else {
            // Dividir respetando párrafos
            val paragraphs = part.split("\n\n")
            var currentBuffer = StringBuilder()
            for (p in paragraphs) {
                if (currentBuffer.length + p.length > charLimitPerPage && currentBuffer.isNotEmpty()) {
                    result.add(currentBuffer.toString().trimEnd())
                    currentBuffer = StringBuilder()
                }
                if (currentBuffer.isNotEmpty()) currentBuffer.append("\n\n")
                currentBuffer.append(p)
            }
            if (currentBuffer.isNotEmpty()) {
                result.add(currentBuffer.toString())
            }
        }
    }
    return if (result.isEmpty()) listOf("") else result
}

/**
 * Vuelve a unir las páginas en un único flujo de texto con saltos de página.
 */
private fun joinPages(pages: List<String>): String {
    return pages.joinToString(separator = "\n[--- Salto de Página ---]\n")
}

@Composable
private fun PageRuler(
    paperBorderColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .height(18.dp)
            .background(paperBorderColor.copy(alpha = 0.3f))
    ) {
        val totalWidth = size.width
        val step = totalWidth / 16f

        drawLine(
            color = paperBorderColor,
            start = Offset(0f, size.height),
            end = Offset(totalWidth, size.height),
            strokeWidth = 1.dp.toPx()
        )

        for (i in 0..16) {
            val x = i * step
            val isMajorTick = i % 4 == 0
            val tickHeight = if (isMajorTick) size.height * 0.7f else size.height * 0.35f

            drawLine(
                color = accentColor,
                start = Offset(x, size.height - tickHeight),
                end = Offset(x, size.height),
                strokeWidth = if (isMajorTick) 1.2.dp.toPx() else 0.8.dp.toPx()
            )
        }
    }
}

@Composable
private fun CornerMarginGuide(
    color: Color,
    isTop: Boolean,
    isLeft: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.width(10.dp).height(10.dp)) {
        val length = 8.dp.toPx()
        val thickness = 1.dp.toPx()

        if (isTop && isLeft) {
            drawLine(color, Offset(0f, 0f), Offset(length, 0f), thickness)
            drawLine(color, Offset(0f, 0f), Offset(0f, length), thickness)
        } else if (isTop && !isLeft) {
            drawLine(color, Offset(size.width, 0f), Offset(size.width - length, 0f), thickness)
            drawLine(color, Offset(size.width, 0f), Offset(size.width, length), thickness)
        } else if (!isTop && isLeft) {
            drawLine(color, Offset(0f, size.height), Offset(length, size.height), thickness)
            drawLine(color, Offset(0f, size.height), Offset(0f, size.height - length), thickness)
        } else {
            drawLine(color, Offset(size.width, size.height), Offset(size.width - length, size.height), thickness)
            drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - length), thickness)
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
