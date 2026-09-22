package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.ProximityLevel
import com.example.util.SearchMatch
import com.example.util.StyleRadarReport

/**
 * DocuSheetSearchRadarBar: Barra de herramientas táctil para Buscador de PC,
 * Radar de Redundancia (Análisis de Proximidad con Apache Lucene) y Carrusel
 * de Sinónimos Offline en un solo toque.
 *
 * Características para teléfono móvil:
 * - Botones táctiles de mínimo 48x48 dp.
 * - Contador de posición en vivo (ej. "3 / 14").
 * - Insignia de advertencia de redundancia y densidad léxica con colores de alerta.
 * - Carrusel deslizable horizontal de alternativas de sinónimos listos para sustituir.
 * - Modo expandible de Reemplazo individual y masivo ("Reemplazar Todo").
 */
@Composable
fun DocuSheetSearchRadarBar(
    searchQuery: String,
    replaceQuery: String,
    matches: List<SearchMatch>,
    currentIndex: Int,
    report: StyleRadarReport?,
    synonyms: List<String>,
    isReplaceExpanded: Boolean,
    isAnalyzing: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onReplaceQueryChange: (String) -> Unit,
    onNextMatch: () -> Unit,
    onPreviousMatch: () -> Unit,
    onToggleReplace: () -> Unit,
    onReplaceCurrent: () -> Unit,
    onReplaceAll: () -> Unit,
    onApplySynonym: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("docusheet_search_radar_bar"),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // ==================================================================
            // Fila 1: Campo de Búsqueda, Navegación Carrusel (← / →), Contador y Cierre
            // ==================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Campo de texto de búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_word_input"),
                    placeholder = {
                        Text(
                            text = "Buscar en el documento...",
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar palabra",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpiar término de búsqueda",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Contador de posición estilo PC ("1 / 5")
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    val countText = if (matches.isNotEmpty()) {
                        "${currentIndex + 1}/${matches.size}"
                    } else if (searchQuery.isNotBlank()) {
                        "0/0"
                    } else {
                        "—"
                    }
                    Text(
                        text = countText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (matches.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }

                // Botón Anterior (mínimo 48x48 dp)
                IconButton(
                    onClick = onPreviousMatch,
                    enabled = matches.isNotEmpty(),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("search_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Coincidencia anterior"
                    )
                }

                // Botón Siguiente (mínimo 48x48 dp)
                IconButton(
                    onClick = onNextMatch,
                    enabled = matches.isNotEmpty(),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("search_next_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Coincidencia siguiente"
                    )
                }

                // Alternar Reemplazo estilo PC
                IconButton(
                    onClick = onToggleReplace,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("search_toggle_replace_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FindReplace,
                        contentDescription = "Mostrar barra de reemplazar",
                        tint = if (isReplaceExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Botón Cerrar (mínimo 48x48 dp)
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("search_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar buscador"
                    )
                }
            }

            // ==================================================================
            // Fila 2 (Opcional): Barra de Reemplazo estilo PC
            // ==================================================================
            AnimatedVisibility(
                visible = isReplaceExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = replaceQuery,
                            onValueChange = onReplaceQueryChange,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("replace_word_input"),
                            placeholder = {
                                Text(
                                    text = "Reemplazar con...",
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        FilledTonalButton(
                            onClick = onReplaceCurrent,
                            enabled = matches.isNotEmpty() && replaceQuery.isNotEmpty(),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("replace_one_button"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = ButtonDefaults.ContentPadding
                        ) {
                            Text("Reemplazar", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        OutlinedButton(
                            onClick = onReplaceAll,
                            enabled = matches.isNotEmpty() && replaceQuery.isNotEmpty(),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("replace_all_button"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = ButtonDefaults.ContentPadding
                        ) {
                            Text("Todos", fontSize = 12.sp)
                        }
                    }
                }
            }

            // ==================================================================
            // Fila 3: Radar de Estilo y Detección de Redundancia de Proximidad
            // ==================================================================
            if (report != null && matches.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                val isCritical = report.criticalCount > 0
                val isModerate = report.moderateCount > 0 && !isCritical

                val badgeBackground = when {
                    isCritical -> Color(0xFFFFEBEE)
                    isModerate -> Color(0xFFFFF8E1)
                    else -> Color(0xFFE8F5E9)
                }

                val badgeBorder = when {
                    isCritical -> Color(0xFFEF5350)
                    isModerate -> Color(0xFFFFB74D)
                    else -> Color(0xFF81C784)
                }

                val textColor = when {
                    isCritical -> Color(0xFFC62828)
                    isModerate -> Color(0xFFE65100)
                    else -> Color(0xFF2E7D32)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCritical || isModerate) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Alerta de redundancia de estilo",
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = report.diagnosticMessage,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = report.suggestedAction,
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // ==================================================================
            // Fila 4: Carrusel de Sinónimos Offline en un solo toque
            // ==================================================================
            if (synonyms.isNotEmpty() && matches.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sinónimos:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("synonyms_carousel_row"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(synonyms) { synonym ->
                            Surface(
                                onClick = { onApplySynonym(synonym) },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .heightIn(min = 36.dp)
                                    .testTag("synonym_chip_$synonym")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = synonym,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
