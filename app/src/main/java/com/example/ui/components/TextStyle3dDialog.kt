package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatColorText
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.paper.parseColorSafe
import com.example.ui.components.paper.parseFontWeightSafe

/**
 * Diálogo interactivo táctil para configurar y aplicar:
 * 1. Color de letra (paleta viva + código hexadecimal)
 * 2. Grosor tipográfico (Fino, Normal, Medio, Semi-negrita, Negrita, Extra-negrita)
 * 3. Color y Efecto 3D (sombra estereoscópica, relieve profundo y color de relieve)
 */
@Composable
fun TextStyle3dDialog(
    selectedTextPreview: String,
    onDismiss: () -> Unit,
    onApplyStyle: (generatedTagPrefix: String, generatedTagSuffix: String) -> Unit
) {
    // Paleta de colores predeterminada
    val presetColors = listOf(
        "#1E293B" to "Negro",
        "#1D4ED8" to "Azul",
        "#BE123C" to "Rojo",
        "#047857" to "Verde",
        "#7E22CE" to "Púrpura",
        "#D97706" to "Ámbar",
        "#EA580C" to "Naranja",
        "#0284C7" to "Cian",
        "#E11D48" to "Rosa",
        "#78350F" to "Sepia"
    )

    // Paleta de sombras 3D
    val preset3dShadows = listOf(
        "#0F172A" to "Carbón",
        "#1E3A8A" to "Azul Noche",
        "#881337" to "Vino",
        "#064E3B" to "Bosque",
        "#581C87" to "Violeta",
        "#78350F" to "Bronce",
        "#000000" to "Negro Puro"
    )

    // Opciones de grosor
    val weightOptions = listOf(
        "light" to "Fino",
        "normal" to "Normal",
        "medium" to "Medio",
        "semibold" to "Seminegrita",
        "bold" to "Negrita",
        "black" to "Extranegrita"
    )

    var selectedColorHex by remember { mutableStateOf("#1D4ED8") }
    var customHexInput by remember { mutableStateOf("") }
    var selectedWeightKey by remember { mutableStateOf("bold") }

    var is3dEnabled by remember { mutableStateOf(false) }
    var selected3dShadowHex by remember { mutableStateOf("#0F172A") }
    var threeDDepth by remember { mutableStateOf("normal") } // "subtle", "normal", "deep"

    val sampleText = if (selectedTextPreview.isNotBlank()) selectedTextPreview.take(40) else "Texto de Prueba 3D"

    // Resolver colores y fuentes para la vista previa
    val effectiveColor = if (customHexInput.trim().startsWith("#") && customHexInput.trim().length in 4..9) {
        parseColorSafe(customHexInput.trim(), Color.Black)
    } else {
        parseColorSafe(selectedColorHex, Color.Black)
    }

    val effectiveWeight = parseFontWeightSafe(selectedWeightKey)
    val effectiveShadowColor = parseColorSafe(selected3dShadowHex, Color(0xFF0F172A))

    val shadowOffset = when (threeDDepth) {
        "subtle" -> Offset(2f, 2f)
        "deep" -> Offset(6f, 6f)
        else -> Offset(3.8f, 3.8f)
    }
    val shadowBlur = when (threeDDepth) {
        "subtle" -> 1f
        "deep" -> 3f
        else -> 1.8f
    }

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
                Text(
                    text = "Color, Grosor y Efecto 3D",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // --- 1. VISTA PREVIA EN TIEMPO REAL ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "VISTA PREVIA EN VIVO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = sampleText,
                            style = TextStyle(
                                color = effectiveColor,
                                fontWeight = effectiveWeight,
                                fontSize = 22.sp,
                                shadow = if (is3dEnabled) {
                                    Shadow(
                                        color = effectiveShadowColor.copy(alpha = 0.9f),
                                        offset = shadowOffset,
                                        blurRadius = shadowBlur
                                    )
                                } else null
                            ),
                            maxLines = 2
                        )
                    }
                }

                // --- 2. PALETA DE COLOR DE LETRA ---
                Text(
                    text = "1. Color de las Letras",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { (hex, name) ->
                        val col = parseColorSafe(hex, Color.Black)
                        val isSelected = selectedColorHex.equals(hex, ignoreCase = true) && customHexInput.isBlank()

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedColorHex = hex
                                    customHexInput = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = name,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = customHexInput,
                    onValueChange = { customHexInput = it },
                    label = { Text("O escribe código Hex (ej. #2563EB)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(thickness = 0.5.dp)

                // --- 3. GROSOR TIPOGRÁFICO ---
                Text(
                    text = "2. Grosor Tipográfico",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    weightOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedWeightKey == key,
                            onClick = { selectedWeightKey = key },
                            label = {
                                Text(
                                    text = label,
                                    fontWeight = parseFontWeightSafe(key),
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                HorizontalDivider(thickness = 0.5.dp)

                // --- 4. EFECTO Y COLOR 3D ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Activar Efecto 3D en Letras",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Switch(
                        checked = is3dEnabled,
                        onCheckedChange = { is3dEnabled = it }
                    )
                }

                if (is3dEnabled) {
                    Text(
                        text = "Color de Sombra/Relieve 3D:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        preset3dShadows.forEach { (hex, name) ->
                            val col = parseColorSafe(hex, Color.Black)
                            val isSelected = selected3dShadowHex.equals(hex, ignoreCase = true)

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { selected3dShadowHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = name,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Profundidad del Relieve 3D:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("subtle" to "Sutil", "normal" to "Clásica", "deep" to "Profunda").forEach { (key, label) ->
                            FilterChip(
                                selected = threeDDepth == key,
                                onClick = { threeDDepth = key },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalColor = if (customHexInput.trim().startsWith("#")) customHexInput.trim() else selectedColorHex

                    val prefixBuilder = StringBuilder()
                    val suffixBuilder = StringBuilder()

                    // 1. Color de letra
                    prefixBuilder.append("[color:$finalColor]")
                    suffixBuilder.insert(0, "[/color]")

                    // 2. Grosor
                    if (selectedWeightKey != "normal") {
                        prefixBuilder.append("[weight:$selectedWeightKey]")
                        suffixBuilder.insert(0, "[/weight]")
                    }

                    // 3. 3D
                    if (is3dEnabled) {
                        prefixBuilder.append("[3d:$selected3dShadowHex,$finalColor]")
                        suffixBuilder.insert(0, "[/3d]")
                    }

                    onApplyStyle(prefixBuilder.toString(), suffixBuilder.toString())
                },
                modifier = Modifier.testTag("apply_style_3d_btn")
            ) {
                Text("Aplicar a la Hoja", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
