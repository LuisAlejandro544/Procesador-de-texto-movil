package com.example.ui.components.selection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Botón individual de la barra contextual de PC con target táctil ergonómico (mínimo 48x48 dp).
 */
@Composable
fun SelectionBarButton(
    icon: ImageVector,
    label: String,
    testTag: String,
    accentColor: Color? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .size(width = 54.dp, height = 50.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accentColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            maxLines = 1
        )
    }
}

/**
 * Aplica una etiqueta de apertura y cierre al rango seleccionado actualmente en el TextFieldValue.
 */
fun applyTagToSelection(
    textFieldValue: TextFieldValue,
    openTag: String,
    closeTag: String,
    onValueChange: (TextFieldValue) -> Unit
) {
    val selection = textFieldValue.selection
    val min = selection.min.coerceIn(0, textFieldValue.text.length)
    val max = selection.max.coerceIn(0, textFieldValue.text.length)

    if (min == max) {
        // No hay texto seleccionado: inserta las etiquetas y coloca el cursor en medio
        val newText = textFieldValue.text.substring(0, min) + openTag + closeTag + textFieldValue.text.substring(max)
        val newCursor = min + openTag.length
        onValueChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursor)
            )
        )
    } else {
        // Envolver el texto seleccionado con las etiquetas
        val selectedText = textFieldValue.text.substring(min, max)
        val replacement = "$openTag$selectedText$closeTag"
        val newText = textFieldValue.text.substring(0, min) + replacement + textFieldValue.text.substring(max)
        val newCursor = min + replacement.length
        onValueChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursor)
            )
        )
    }
}
