package com.example.ui.components.paper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renderiza diagramas de nodos y grafos de flujo en la hoja de DocuSheet.
 * Soporta alineación (izquierda, centro, derecha) y disposición horizontal o vertical.
 */
@Composable
fun SheetNodesBlock(
    nodes: List<String>,
    align: String,
    layout: String,
    fillColorHex: String,
    strokeColorHex: String,
    defaultFontFamily: FontFamily,
    modifier: Modifier = Modifier
) {
    if (nodes.isEmpty()) return

    val fillCol = parseColorSafe(fillColorHex, Color(0xFFF1F5F9))
    val strokeCol = parseColorSafe(strokeColorHex, Color(0xFF2563EB))

    val boxAlignment = when (align.lowercase()) {
        "center" -> Alignment.Center
        "right" -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }

    val isVertical = layout.equals("vertical", ignoreCase = true)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = boxAlignment
    ) {
        if (isVertical) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                nodes.forEachIndexed { index, nodeText ->
                    NodeItemCard(
                        index = index + 1,
                        text = nodeText,
                        fillColor = fillCol,
                        strokeColor = strokeCol,
                        fontFamily = defaultFontFamily
                    )

                    if (index < nodes.size - 1) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Conector de flujo",
                            tint = strokeCol,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        } else {
            // Horizontal con scroll fluido si excede el ancho de la hoja
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                nodes.forEachIndexed { index, nodeText ->
                    NodeItemCard(
                        index = index + 1,
                        text = nodeText,
                        fillColor = fillCol,
                        strokeColor = strokeCol,
                        fontFamily = defaultFontFamily
                    )

                    if (index < nodes.size - 1) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Conector de flujo",
                            tint = strokeCol,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeItemCard(
    index: Int,
    text: String,
    fillColor: Color,
    strokeColor: Color,
    fontFamily: FontFamily
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = fillColor),
        modifier = Modifier
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .border(1.5.dp, strokeColor, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicador de nodo circular
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(strokeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$index",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = text,
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color(0xFF1E293B)
            )
        }
    }
}
