package com.example.ui.components

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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.paper.CascadePageDivider
import com.example.ui.components.paper.CornerMarginGuide
import com.example.ui.components.paper.DocuSheetRichVisualTransformation
import com.example.ui.components.paper.FormattedSheetContent
import com.example.ui.components.paper.PageRuler
import com.example.ui.components.paper.PageSlice
import com.example.ui.components.paper.PaperSheetPaginator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Función pública de compatibilidad para conteo de palabras.
 */
fun countWords(text: String): Int = PaperSheetPaginator.countWords(text)

/**
 * PaperSheet: Representación visual y táctil de una o múltiples hojas de papel de procesador de texto.
 * 
 * Arquitectura Modularizada:
 * - Paginación y partición de flujo continuo ([PaperSheetPaginator])
 * - Resaltado de sintaxis enriquecido con mapeo 1:1 de cursor ([DocuSheetRichVisualTransformation])
 * - Regla superior, divisores de cascada y guías de márgenes L ([PageRuler], [CascadePageDivider], [CornerMarginGuide])
 * - Renderizado en bloques editoriales ([FormattedSheetContent])
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
    onTextFieldValueChange: ((TextFieldValue) -> Unit)? = null,
    pageSize: String = "A4",
    wordsPerPageLimit: Int = 350
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
        val pageSlices = remember(content, wordsPerPageLimit) {
            PaperSheetPaginator.calculatePageSlices(content, wordsPerPageLimit)
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
                    showRuler = (pageNumber == 1),
                    alignment = alignment,
                    pageSize = pageSize,
                    wordsPerPageLimit = wordsPerPageLimit,
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

                if (pageNumber < totalPages) {
                    CascadePageDivider(
                        currentPage = pageNumber,
                        nextPage = pageNumber + 1
                    )
                }
            }

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
            pageSize = pageSize,
            wordsPerPageLimit = wordsPerPageLimit,
            modifier = modifier,
            textFieldValue = textFieldValue,
            onTextFieldValueChange = onTextFieldValueChange
        )
    }
}

/**
 * Renderiza una hoja de papel individual completa con guías, regla y cuerpo de texto.
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
    pageSize: String = "A4",
    wordsPerPageLimit: Int = 350,
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
        if (showRuler) {
            PageRuler(
                paperBorderColor = paperBorderColor,
                accentColor = paperTextColor.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            )
        }

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 450.dp)
                .padding(horizontal = horizontalMargin, vertical = 8.dp)
        ) {
            if (isReadOnly) {
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
                CompositionLocalProvider(LocalTextToolbar provides remember { DocuSheetDisabledSystemToolbar() }) {
                    val effectiveTfv = textFieldValue ?: remember(pageText) { TextFieldValue(pageText) }
                    val effectiveOnValueChange: (TextFieldValue) -> Unit = onTextFieldValueChange ?: { onPageTextChange(it.text) }

                    LaunchedEffect(isImeVisible) {
                        if (isImeVisible && effectiveTfv.selection.start in 0..effectiveTfv.text.length) {
                            delay(120)
                            bringIntoViewRequester.bringIntoView()
                        }
                    }

                    val editorTextAlign = if (textAlignment == TextAlign.Justify) TextAlign.Start else textAlignment
                    val richTransformation = remember(paperTextColor) { DocuSheetRichVisualTransformation(paperTextColor) }

                    BasicTextField(
                        value = effectiveTfv,
                        onValueChange = effectiveOnValueChange,
                        visualTransformation = richTransformation,
                        textStyle = TextStyle(
                            fontFamily = selectedFontFamily,
                            fontSize = fontSize.sp,
                            lineHeight = calculatedLineHeight,
                            color = paperTextColor,
                            fontWeight = FontWeight.Normal,
                            textAlign = editorTextAlign
                        ),
                        cursorBrush = SolidColor(if (paperType == "DARK") Color(0xFF60A5FA) else Color(0xFF1D4ED8)),
                        onTextLayout = { textLayoutResult ->
                            val sel = effectiveTfv.selection
                            if (sel.start in 0..effectiveTfv.text.length) {
                                val cursorIndex = sel.start.coerceIn(0, effectiveTfv.text.length)
                                val cursorRect = textLayoutResult.getCursorRect(cursorIndex)
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
                                        textAlign = editorTextAlign
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }

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

            val pageWords = remember(pageText) { countWords(pageText) }
            val isOverLimit = pageWords >= wordsPerPageLimit

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (totalPages > 1) "— Página $pageNumber de $totalPages —" else "— Página 1 —",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = paperTextColor.copy(alpha = 0.55f)
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$pageWords / $wordsPerPageLimit palabras ($pageSize)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = if (isOverLimit) FontWeight.Bold else FontWeight.Normal,
                        color = if (isOverLimit) {
                            if (paperType == "DARK") Color(0xFFF87171) else Color(0xFFDC2626)
                        } else {
                            paperTextColor.copy(alpha = 0.4f)
                        }
                    )
                )
            }

            CornerMarginGuide(
                color = paperTextColor.copy(alpha = 0.25f),
                isTop = false,
                isLeft = false,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}
