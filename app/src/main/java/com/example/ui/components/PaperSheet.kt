package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Representa una porción o hoja física dentro del flujo global del documento en Cascada.
 */
data class PageSlice(
    val index: Int,
    val text: String,
    val startOffset: Int,
    val endOffset: Int
)

sealed class SheetBlock {
    data class Paragraph(val rawLine: String) : SheetBlock()
    data class Image(val uri: String, val wrapMode: String, val caption: String) : SheetBlock()
    data class Table(val lines: List<String>, val style: String) : SheetBlock()
}

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
    alignment: String = "LEFT",
    onAddPage: (() -> Unit)? = null,
    textFieldValue: TextFieldValue? = null,
    onTextFieldValueChange: ((TextFieldValue) -> Unit)? = null
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
        val pageSlices = remember(content) {
            calculatePageSlices(content)
        }
        val totalPages = pageSlices.size

        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (slice in pageSlices) {
                val pageContent = slice.text
                val pageNumber = slice.index + 1

                // Sincronización atómica de selección y cursor entre la hoja local y el documento maestro
                val currentGlobalTfv = textFieldValue ?: TextFieldValue(content)
                val globalSel = currentGlobalTfv.selection

                val pageTfv = remember(currentGlobalTfv, slice) {
                    if (globalSel.collapsed) {
                        if (globalSel.start in slice.startOffset..slice.endOffset) {
                            val localCursor = (globalSel.start - slice.startOffset).coerceIn(0, pageContent.length)
                            TextFieldValue(text = pageContent, selection = TextRange(localCursor))
                        } else {
                            TextFieldValue(text = pageContent, selection = TextRange.Zero)
                        }
                    } else {
                        val selMin = globalSel.min
                        val selMax = globalSel.max
                        if (selMax > slice.startOffset && selMin < slice.endOffset) {
                            val localStart = (selMin - slice.startOffset).coerceIn(0, pageContent.length)
                            val localEnd = (selMax - slice.startOffset).coerceIn(0, pageContent.length)
                            TextFieldValue(text = pageContent, selection = TextRange(localStart, localEnd))
                        } else {
                            TextFieldValue(text = pageContent, selection = TextRange.Zero)
                        }
                    }
                }

                SingleSheetCard(
                    pageText = pageContent,
                    onPageTextChange = { newPageText ->
                        val before = content.substring(0, slice.startOffset.coerceAtMost(content.length))
                        val after = content.substring(slice.endOffset.coerceAtMost(content.length))
                        val newFullContent = before + newPageText + after
                        onContentChange(newFullContent)
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
                    showRuler = (pageNumber == 1), // Regla principal en la primera hoja
                    alignment = alignment,
                    textFieldValue = pageTfv,
                    onTextFieldValueChange = { newPageTfv ->
                        if (newPageTfv.text != pageContent) {
                            val before = content.substring(0, slice.startOffset.coerceAtMost(content.length))
                            val after = content.substring(slice.endOffset.coerceAtMost(content.length))
                            val newFullContent = before + newPageTfv.text + after
                            val newGlobalCursor = (slice.startOffset + newPageTfv.selection.start).coerceIn(0, newFullContent.length)
                            val newTfv = TextFieldValue(text = newFullContent, selection = TextRange(newGlobalCursor))
                            onTextFieldValueChange?.invoke(newTfv)
                            onContentChange(newFullContent)
                        } else {
                            val localSel = newPageTfv.selection
                            val newGlobalStart = (slice.startOffset + localSel.start).coerceIn(0, content.length)
                            val newGlobalEnd = (slice.startOffset + localSel.end).coerceIn(0, content.length)
                            val newTfv = currentGlobalTfv.copy(
                                text = content,
                                selection = TextRange(newGlobalStart, newGlobalEnd)
                            )
                            onTextFieldValueChange?.invoke(newTfv)
                        }
                    }
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
            alignment = alignment,
            modifier = modifier,
            textFieldValue = textFieldValue,
            onTextFieldValueChange = onTextFieldValueChange
        )
    }
}

/**
 * Calcula los límites exactos de caracteres de cada hoja dentro del documento global,
 * asegurando la sincronización atómica entre las hojas físicas y el TextFieldValue de PC.
 */
private fun calculatePageSlices(content: String): List<PageSlice> {
    if (content.isEmpty()) return listOf(PageSlice(0, "", 0, 0))
    val rawPages = partitionIntoPages(content)
    val slices = mutableListOf<PageSlice>()
    var searchStart = 0
    for ((index, pText) in rawPages.withIndex()) {
        val foundStart = if (pText.isNotEmpty()) {
            val idx = content.indexOf(pText, startIndex = searchStart)
            if (idx >= 0) idx else searchStart
        } else {
            searchStart
        }
        val foundEnd = (foundStart + pText.length).coerceAtMost(content.length)
        slices.add(PageSlice(index, pText, foundStart, foundEnd))
        searchStart = foundEnd
    }
    return if (slices.isEmpty()) listOf(PageSlice(0, "", 0, 0)) else slices
}

/**
 * Renderiza una hoja de papel individual completa (portada o subsiguiente) con todas sus guías y reglas.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
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
    alignment: String = "LEFT",
    modifier: Modifier = Modifier,
    textFieldValue: TextFieldValue? = null,
    onTextFieldValueChange: ((TextFieldValue) -> Unit)? = null
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val isImeVisible = WindowInsets.isImeVisible

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

        val textAlignment = when (alignment.uppercase()) {
            "JUSTIFY" -> TextAlign.Justify
            "CENTER" -> TextAlign.Center
            "RIGHT" -> TextAlign.Right
            else -> TextAlign.Left
        }

        // Cuerpo de escritura
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 450.dp)
                .padding(horizontal = horizontalMargin, vertical = 8.dp)
        ) {
            if (isReadOnly) {
                // Modo Lectura con renderizado enriquecido de párrafos, títulos, imágenes con wrap y estilos
                FormattedSheetContent(
                    text = if (pageText.isEmpty()) "Hoja en blanco" else pageText,
                    fontFamily = selectedFontFamily,
                    baseFontSize = fontSize,
                    lineHeight = calculatedLineHeight,
                    textColor = if (pageText.isEmpty()) paperTextColor.copy(alpha = 0.4f) else paperTextColor,
                    docAlignment = alignment,
                    borderColor = paperBorderColor,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Modo Edición interactivo fluido con soporte de alineación cuádruple en vivo
                // y selector del fabricante desactivado en favor del selector contextual propio de DocuSheet
                CompositionLocalProvider(LocalTextToolbar provides remember { DocuSheetDisabledSystemToolbar() }) {
                    val effectiveTfv = textFieldValue ?: remember(pageText) { TextFieldValue(pageText) }
                    val effectiveOnValueChange: (TextFieldValue) -> Unit = onTextFieldValueChange ?: { onPageTextChange(it.text) }

                    // Cuando el teclado se abre y el cursor está activo en esta hoja, asegurar vista inmediata
                    LaunchedEffect(isImeVisible) {
                        if (isImeVisible && effectiveTfv.selection.start in 0..effectiveTfv.text.length) {
                            delay(120)
                            bringIntoViewRequester.bringIntoView()
                        }
                    }

                    BasicTextField(
                        value = effectiveTfv,
                        onValueChange = effectiveOnValueChange,
                        textStyle = TextStyle(
                            fontFamily = selectedFontFamily,
                            fontSize = fontSize.sp,
                            lineHeight = calculatedLineHeight,
                            color = paperTextColor,
                            fontWeight = FontWeight.Normal,
                            textAlign = textAlignment
                        ),
                        cursorBrush = SolidColor(if (paperType == "DARK") Color(0xFF60A5FA) else Color(0xFF1D4ED8)),
                        onTextLayout = { textLayoutResult ->
                            val sel = effectiveTfv.selection
                            if (sel.start in 0..effectiveTfv.text.length) {
                                val cursorIndex = sel.start.coerceIn(0, effectiveTfv.text.length)
                                val cursorRect = textLayoutResult.getCursorRect(cursorIndex)
                                // Margen de confort visual vertical de 120px para no quedar nunca oculto bajo el teclado
                                val comfortableRect = cursorRect.copy(
                                    top = (cursorRect.top - 120f).coerceAtLeast(0f),
                                    bottom = cursorRect.bottom + 120f
                                )
                                coroutineScope.launch {
                                    bringIntoViewRequester.bringIntoView(comfortableRect)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 450.dp)
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .testTag("paper_editor_field_$pageNumber"),
                        decorationBox = { innerTextField ->
                            if (effectiveTfv.text.isEmpty()) {
                                Text(
                                    text = "Escribe aquí tu texto...",
                                    style = TextStyle(
                                        fontFamily = selectedFontFamily,
                                        fontSize = fontSize.sp,
                                        lineHeight = calculatedLineHeight,
                                        color = paperTextColor.copy(alpha = 0.35f),
                                        textAlign = textAlignment
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }
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
 * Renderiza una imagen incrustada en la hoja con ajuste de diseño físico (Layout & Wrap).
 */
@Composable
private fun SheetImageBlock(
    uri: String,
    wrapMode: String,
    caption: String,
    borderColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val alignment = when (wrapMode.lowercase()) {
        "center" -> Alignment.CenterHorizontally
        "left" -> Alignment.Start
        "right" -> Alignment.End
        else -> Alignment.CenterHorizontally
    }

    val widthModifier = when (wrapMode.lowercase()) {
        "left" -> Modifier.widthIn(max = 220.dp)
        "right" -> Modifier.widthIn(max = 220.dp)
        "center" -> Modifier.widthIn(max = 300.dp)
        else -> Modifier.fillMaxWidth()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = widthModifier
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(uri)
                    .crossfade(true)
                    .build(),
                contentDescription = caption.ifEmpty { "Imagen incrustada" },
                contentScale = if (wrapMode == "full") ContentScale.FillWidth else ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 280.dp)
            )
        }

        if (caption.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = caption,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    color = textColor.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

/**
 * Parsea texto enriquecido reconociendo negrita, cursiva, subrayado, tachado, subíndices, superíndices
 * y tipografías específicas inline ([font:serif], [font:sans], [font:mono], [font:cursive]).
 */
private fun parseRichInlineText(
    text: String,
    defaultFontFamily: FontFamily,
    baseFontSize: Int,
    defaultColor: Color
): androidx.compose.ui.text.AnnotatedString {
    if (text.isEmpty()) return buildAnnotatedString { append(" ") }

    val pattern = Regex(
        """(\*\*(.*?)\*\*)|(\*(.*?)\*)|(<u>(.*?)</u>)|(__(.*?)__)|(~~(.*?)~~)|(<s>(.*?)</s>)|(<sub>(.*?)</sub>)|(<sup>(.*?)</sup>)|(\[font:(serif|sans|mono|cursive)\](.*?)\[/font\])|(\[color:(#[0-9a-fA-F]{6}|#[0-9a-fA-F]{3}|[a-zA-Z]+)\](.*?)\[/color\])""",
        RegexOption.DOT_MATCHES_ALL
    )

    return buildAnnotatedString {
        var lastIndex = 0
        for (match in pattern.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1

            if (start > lastIndex) {
                append(text.substring(lastIndex, start))
            }

            val fullMatch = match.value
            when {
                // **negrita**
                fullMatch.startsWith("**") && fullMatch.endsWith("**") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(fullMatch.removePrefix("**").removeSuffix("**"))
                    }
                }
                // *cursiva*
                fullMatch.startsWith("*") && fullMatch.endsWith("*") && !fullMatch.startsWith("**") -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(fullMatch.removePrefix("*").removeSuffix("*"))
                    }
                }
                // <u>subrayado</u>
                fullMatch.startsWith("<u>") && fullMatch.endsWith("</u>") -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append(fullMatch.removePrefix("<u>").removeSuffix("</u>"))
                    }
                }
                // __subrayado__
                fullMatch.startsWith("__") && fullMatch.endsWith("__") -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append(fullMatch.removePrefix("__").removeSuffix("__"))
                    }
                }
                // ~~tachado~~
                fullMatch.startsWith("~~") && fullMatch.endsWith("~~") -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(fullMatch.removePrefix("~~").removeSuffix("~~"))
                    }
                }
                // <s>tachado</s>
                fullMatch.startsWith("<s>") && fullMatch.endsWith("</s>") -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(fullMatch.removePrefix("<s>").removeSuffix("</s>"))
                    }
                }
                // <sub>subíndice</sub>
                fullMatch.startsWith("<sub>") && fullMatch.endsWith("</sub>") -> {
                    withStyle(
                        SpanStyle(
                            baselineShift = BaselineShift.Subscript,
                            fontSize = (baseFontSize * 0.72f).sp
                        )
                    ) {
                        append(fullMatch.removePrefix("<sub>").removeSuffix("</sub>"))
                    }
                }
                // <sup>superíndice</sup>
                fullMatch.startsWith("<sup>") && fullMatch.endsWith("</sup>") -> {
                    withStyle(
                        SpanStyle(
                            baselineShift = BaselineShift.Superscript,
                            fontSize = (baseFontSize * 0.72f).sp
                        )
                    ) {
                        append(fullMatch.removePrefix("<sup>").removeSuffix("</sup>"))
                    }
                }
                // [font:xxx]texto[/font]
                fullMatch.startsWith("[font:") && fullMatch.endsWith("[/font]") -> {
                    val fontType = fullMatch.substringAfter("[font:").substringBefore("]").lowercase()
                    val innerText = fullMatch.substringAfter("]").removeSuffix("[/font]")
                    val fontFam = when (fontType) {
                        "sans" -> FontFamily.SansSerif
                        "mono" -> FontFamily.Monospace
                        "cursive" -> FontFamily.Cursive
                        else -> FontFamily.Serif
                    }
                    withStyle(SpanStyle(fontFamily = fontFam)) {
                        append(innerText)
                    }
                }
                // [color:xxx]texto[/color]
                fullMatch.startsWith("[color:") && fullMatch.endsWith("[/color]") -> {
                    val colorTag = fullMatch.substringAfter("[color:").substringBefore("]").trim()
                    val innerText = fullMatch.substringAfter("]").removeSuffix("[/color]")
                    val parsedColor = try {
                        val hex = if (colorTag.startsWith("#")) colorTag else when (colorTag.lowercase()) {
                            "black" -> "#1E293B"
                            "blue" -> "#1D4ED8"
                            "red" -> "#BE123C"
                            "green" -> "#047857"
                            "purple" -> "#7E22CE"
                            "amber", "yellow" -> "#D97706"
                            "cyan", "teal" -> "#0284C7"
                            "pink" -> "#E11D48"
                            "sepia", "brown" -> "#78350F"
                            "gray", "grey" -> "#475569"
                            else -> "#$colorTag"
                        }
                        Color(android.graphics.Color.parseColor(hex))
                    } catch (e: Exception) {
                        defaultColor
                    }
                    withStyle(SpanStyle(color = parsedColor)) {
                        append(innerText)
                    }
                }
                else -> {
                    append(fullMatch)
                }
            }
            lastIndex = end
        }

        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

/**
 * Renderiza el texto de lectura con formato estructurado, ajuste de hoja (Layout & Wrap de imágenes),
 * alineación de párrafo cuádruple con justificado real y estilos tipográficos avanzados.
 */
@Composable
private fun FormattedSheetContent(
    text: String,
    fontFamily: FontFamily,
    baseFontSize: Int,
    lineHeight: androidx.compose.ui.unit.TextUnit,
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

            // 4. Párrafo estándar
            result.add(SheetBlock.Paragraph(rawLine))
            i++
        }
        result
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (block in blocks) {
            when (block) {
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
                    val alignRegex = Regex("""^\[align:(left|center|right|justify)\](.*?)\[/align\]$""", RegexOption.DOT_MATCHES_ALL)
                    val alignMatch = alignRegex.find(trimmedLine)

                    val (effectiveLine, effectiveAlign) = if (alignMatch != null) {
                        val mode = alignMatch.groupValues[1].lowercase()
                        val content = alignMatch.groupValues[2]
                        val align = when (mode) {
                            "justify" -> TextAlign.Justify
                            "center" -> TextAlign.Center
                            "right" -> TextAlign.Right
                            else -> TextAlign.Left
                        }
                        Pair(content, align)
                    } else {
                        Pair(rawLine, defaultTextAlign)
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
