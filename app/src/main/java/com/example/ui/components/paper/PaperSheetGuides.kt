package com.example.ui.components.paper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Regla milimétrica graduada de cabecera de página de procesador de texto.
 */
@Composable
fun PageRuler(
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

/**
 * Guías en forma de "L" en las esquinas que delimitan los márgenes del papel físico.
 */
@Composable
fun CornerMarginGuide(
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

/**
 * Separador visual que indica el final de una hoja y el inicio de la siguiente en vista cascada.
 */
@Composable
fun CascadePageDivider(
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
