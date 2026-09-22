package com.example.ui.components.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Definición de un color de tinta físico para la paleta de PC.
 */
data class InkColorOption(
    val name: String,
    val hex: String,
    val color: Color
)

val DEFAULT_INK_PALETTE = listOf(
    InkColorOption("Negro Carbón", "#1E293B", Color(0xFF1E293B)),
    InkColorOption("Azul Real", "#1D4ED8", Color(0xFF1D4ED8)),
    InkColorOption("Rojo Borgoña", "#BE123C", Color(0xFFBE123C)),
    InkColorOption("Verde Botánico", "#047857", Color(0xFF047857)),
    InkColorOption("Violeta Imperial", "#7E22CE", Color(0xFF7E22CE)),
    InkColorOption("Ámbar Cálido", "#D97706", Color(0xFFD97706)),
    InkColorOption("Turquesa", "#0284C7", Color(0xFF0284C7)),
    InkColorOption("Coral Rosa", "#E11D48", Color(0xFFE11D48)),
    InkColorOption("Marrón Sepia", "#78350F", Color(0xFF78350F)),
    InkColorOption("Gris Grafito", "#475569", Color(0xFF475569))
)

/**
 * Diálogo interactivo táctil para seleccionar un color de tinta de la paleta clásica
 * o especificar un código HEX personalizado para aplicar al texto seleccionado.
 */
@Composable
fun SelectionInkPaletteDialog(
    selectedText: String,
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedInkHex by remember { mutableStateOf("#1D4ED8") }
    var customHexInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Color de Letra (PC)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Elige el color de tinta para el texto seleccionado:",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Muestra de color actual
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Muestra de texto: ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    val previewColor = try {
                        Color(android.graphics.Color.parseColor(selectedInkHex))
                    } catch (_: Exception) {
                        MaterialTheme.colorScheme.primary
                    }
                    Text(
                        text = if (selectedText.length > 20) selectedText.take(20) + "..." else selectedText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = previewColor,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                // Paleta de colores predefinidos (Tinta física de alta calidad)
                Text("Paleta de Tintas Clásicas:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DEFAULT_INK_PALETTE.chunked(5).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowColors.forEach { ink ->
                                val isSelected = selectedInkHex.equals(ink.hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(ink.color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0x33000000),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            selectedInkHex = ink.hex
                                            customHexInput = ink.hex
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = ink.name,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Entrada para código hexadecimal libre
                OutlinedTextField(
                    value = customHexInput,
                    onValueChange = { input ->
                        customHexInput = input
                        val clean = if (input.startsWith("#")) input else "#$input"
                        if (clean.matches(Regex("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})$"))) {
                            selectedInkHex = clean
                        }
                    },
                    label = { Text("Código HEX libre (ej: #BE123C)") },
                    placeholder = { Text("#1D4ED8") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalHex = if (selectedInkHex.startsWith("#")) selectedInkHex else "#$selectedInkHex"
                    applyTagToSelection(
                        textFieldValue = textFieldValue,
                        openTag = "[color:$finalHex]",
                        closeTag = "[/color]",
                        onValueChange = onValueChange
                    )
                    onDismiss()
                }
            ) {
                Text("Aplicar Color", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
