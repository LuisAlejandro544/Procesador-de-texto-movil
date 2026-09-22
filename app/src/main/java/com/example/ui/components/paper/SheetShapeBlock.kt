package com.example.ui.components.paper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renderiza figuras geométricas vectoriales de alta precisión en la hoja de papel físico:
 * Rectángulos, círculos, triángulos, diamantes/rombos, estrellas, flechas y llamadas.
 */
@Composable
fun SheetShapeBlock(
    type: String,
    widthDp: Int,
    heightDp: Int,
    align: String,
    fillColorHex: String,
    strokeColorHex: String,
    borderWidthDp: Int,
    cornerRadiusDp: Int,
    text: String,
    defaultFontFamily: FontFamily,
    modifier: Modifier = Modifier
) {
    val boxAlignment = when (align.lowercase()) {
        "center" -> Alignment.Center
        "right" -> Alignment.CenterEnd
        "full" -> Alignment.Center
        else -> Alignment.CenterStart
    }

    val fillCol = parseColorSafe(fillColorHex, Color(0xFFDBEAFE))
    val strokeCol = parseColorSafe(strokeColorHex, Color(0xFF1D4ED8))
    val isFullWidth = align.equals("full", ignoreCase = true)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = boxAlignment
    ) {
        val shapeWidth = if (isFullWidth) Modifier.fillMaxWidth() else Modifier.width(widthDp.dp)
        val shapeHeight = Modifier.height(heightDp.dp)

        Box(
            modifier = Modifier
                .then(shapeWidth)
                .then(shapeHeight)
                .wrapContentSize(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .then(shapeWidth)
                    .then(shapeHeight)
            ) {
                val w = size.width
                val h = size.height
                val strokeWidthPx = borderWidthDp.dp.toPx().coerceAtLeast(1f)

                when (type.lowercase()) {
                    "circle", "oval" -> {
                        drawOval(
                            color = fillCol,
                            size = Size(w, h)
                        )
                        if (borderWidthDp > 0) {
                            drawOval(
                                color = strokeCol,
                                size = Size(w - strokeWidthPx, h - strokeWidthPx),
                                topLeft = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f),
                                style = Stroke(width = strokeWidthPx)
                            )
                        }
                    }
                    "triangle" -> {
                        val path = Path().apply {
                            moveTo(w / 2f, strokeWidthPx)
                            lineTo(w - strokeWidthPx, h - strokeWidthPx)
                            lineTo(strokeWidthPx, h - strokeWidthPx)
                            close()
                        }
                        drawPath(path, color = fillCol, style = Fill)
                        if (borderWidthDp > 0) {
                            drawPath(path, color = strokeCol, style = Stroke(width = strokeWidthPx))
                        }
                    }
                    "diamond", "rombo" -> {
                        val path = Path().apply {
                            moveTo(w / 2f, strokeWidthPx)
                            lineTo(w - strokeWidthPx, h / 2f)
                            lineTo(w / 2f, h - strokeWidthPx)
                            lineTo(strokeWidthPx, h / 2f)
                            close()
                        }
                        drawPath(path, color = fillCol, style = Fill)
                        if (borderWidthDp > 0) {
                            drawPath(path, color = strokeCol, style = Stroke(width = strokeWidthPx))
                        }
                    }
                    "star" -> {
                        val path = Path().apply {
                            val centerX = w / 2f
                            val centerY = h / 2f
                            val outerRadius = (minOf(w, h) / 2f) - strokeWidthPx
                            val innerRadius = outerRadius * 0.45f
                            val points = 5
                            var angle = -Math.PI / 2.0
                            val angleStep = Math.PI / points

                            for (step in 0 until points * 2) {
                                val r = if (step % 2 == 0) outerRadius else innerRadius
                                val x = centerX + (cos(angle) * r).toFloat()
                                val y = centerY + (sin(angle) * r).toFloat()
                                if (step == 0) moveTo(x, y) else lineTo(x, y)
                                angle += angleStep
                            }
                            close()
                        }
                        drawPath(path, color = fillCol, style = Fill)
                        if (borderWidthDp > 0) {
                            drawPath(path, color = strokeCol, style = Stroke(width = strokeWidthPx))
                        }
                    }
                    "arrow" -> {
                        val path = Path().apply {
                            val arrowHeadWidth = w * 0.35f
                            val stemHeight = h * 0.45f
                            val stemTop = (h - stemHeight) / 2f
                            val stemBottom = stemTop + stemHeight

                            moveTo(strokeWidthPx, stemTop)
                            lineTo(w - arrowHeadWidth, stemTop)
                            lineTo(w - arrowHeadWidth, strokeWidthPx)
                            lineTo(w - strokeWidthPx, h / 2f)
                            lineTo(w - arrowHeadWidth, h - strokeWidthPx)
                            lineTo(w - arrowHeadWidth, stemBottom)
                            lineTo(strokeWidthPx, stemBottom)
                            close()
                        }
                        drawPath(path, color = fillCol, style = Fill)
                        if (borderWidthDp > 0) {
                            drawPath(path, color = strokeCol, style = Stroke(width = strokeWidthPx))
                        }
                    }
                    "callout" -> {
                        val radiusPx = cornerRadiusDp.dp.toPx()
                        val tailHeight = h * 0.22f
                        val mainH = h - tailHeight
                        val path = Path().apply {
                            addRoundRect(
                                androidx.compose.ui.geometry.RoundRect(
                                    left = strokeWidthPx,
                                    top = strokeWidthPx,
                                    right = w - strokeWidthPx,
                                    bottom = mainH,
                                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                                )
                            )
                            // Pico inferior del globo
                            moveTo(w * 0.25f, mainH)
                            lineTo(w * 0.18f, h - strokeWidthPx)
                            lineTo(w * 0.42f, mainH)
                        }
                        drawPath(path, color = fillCol, style = Fill)
                        if (borderWidthDp > 0) {
                            drawPath(path, color = strokeCol, style = Stroke(width = strokeWidthPx))
                        }
                    }
                    else -> {
                        // "rect" por defecto
                        val radiusPx = cornerRadiusDp.dp.toPx()
                        drawRoundRect(
                            color = fillCol,
                            size = Size(w, h),
                            cornerRadius = CornerRadius(radiusPx, radiusPx)
                        )
                        if (borderWidthDp > 0) {
                            drawRoundRect(
                                color = strokeCol,
                                topLeft = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f),
                                size = Size(w - strokeWidthPx, h - strokeWidthPx),
                                cornerRadius = CornerRadius(radiusPx, radiusPx),
                                style = Stroke(width = strokeWidthPx)
                            )
                        }
                    }
                }
            }

            if (text.isNotBlank()) {
                val textColor = if (fillColorHex.equals("#FFFFFF", true) || fillColorHex.contains("F", true)) {
                    Color(0xFF1E293B)
                } else {
                    Color(0xFF0F172A)
                }

                Text(
                    text = text,
                    fontFamily = defaultFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(if (isFullWidth) 0.85f else 0.9f)
                )
            }
        }
    }
}
