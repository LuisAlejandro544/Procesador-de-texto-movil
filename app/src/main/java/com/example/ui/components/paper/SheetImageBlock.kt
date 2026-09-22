package com.example.ui.components.paper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

/**
 * Renderiza una imagen incrustada en la hoja con ajuste de diseño físico (Layout & Wrap).
 */
@Composable
fun SheetImageBlock(
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
            val imageSource: Any = remember(uri) {
                if (uri.startsWith("/")) {
                    val f = File(uri)
                    if (f.exists()) f else uri
                } else {
                    uri
                }
            }
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageSource)
                    .crossfade(true)
                    .build(),
                contentDescription = caption.ifEmpty { "Imagen incrustada" },
                contentScale = if (wrapMode == "full") ContentScale.FillWidth else ContentScale.Fit,
                error = rememberVectorPainter(Icons.Outlined.BrokenImage),
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
