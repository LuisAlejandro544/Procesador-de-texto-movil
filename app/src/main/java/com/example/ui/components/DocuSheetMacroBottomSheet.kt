package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.macro.MacroEntity
import com.example.util.MacroEngine

/**
 * DocuSheetMacroBottomSheet: Panel modal táctil optimizado para smartphones
 * que permite explorar, ejecutar y crear macros y plantillas automatizadas
 * con variables dinámicas ({FECHA}, {HORA}, {TITULO}, etc.).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DocuSheetMacroBottomSheet(
    sheetState: SheetState,
    macros: List<MacroEntity>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onExecuteMacro: (MacroEntity, String) -> Unit,
    onCreateMacro: (String, String, String, String, String) -> Unit,
    onDeleteMacro: (MacroEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentTabIndex by remember { mutableIntStateOf(0) }

    // Estado del formulario de creación de nueva macro
    var newMacroName by remember { mutableStateOf("") }
    var newMacroDescription by remember { mutableStateOf("") }
    var newMacroTrigger by remember { mutableStateOf("") }
    var newMacroCategory by remember { mutableStateOf("MIS MACROS") }
    var newMacroContent by remember { mutableStateOf("") }
    var showVariableHelperDialog by remember { mutableStateOf(false) }

    val categories = listOf("TODAS", "TRABAJO", "CORRESPONDENCIA", "ACADÉMICO", "TABLAS", "LEGAL", "REPORTES", "ESTILO", "MIS MACROS")

    val filteredMacros = remember(macros, selectedCategory) {
        if (selectedCategory == "TODAS") {
            macros
        } else {
            macros.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    // Obtener contenido seguro del portapapeles para inyección de {PORTAPAPELES}
    fun getClipboardContent(): String {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        modifier = Modifier
            .fillMaxHeight(0.92f)
            .testTag("macro_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // --- Encabezado Táctil del Centro de Macros ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Macros y Automatizaciones",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${macros.size} plantillas disponibles • Inserción a 1 toque",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_macro_sheet_btn")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Cerrar panel",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Pestañas Principales: Catálogo vs Crear
            PrimaryTabRow(
                selectedTabIndex = currentTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = currentTabIndex == 0,
                    onClick = { currentTabIndex = 0 },
                    text = { Text("Catálogo (${macros.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Outlined.Widgets, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = currentTabIndex == 1,
                    onClick = { currentTabIndex = 1 },
                    text = { Text("Crear Macro", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (currentTabIndex == 0) {
                // ==============================================================
                // PESTAÑA 0: CATÁLOGO DE MACROS CON FILTRO DE CATEGORÍAS
                // ==============================================================
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Carrusel horizontal de Categorías táctiles
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        categories.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { onCategorySelected(category) },
                                label = { Text(category, style = MaterialTheme.typography.labelMedium) },
                                leadingIcon = if (selectedCategory == category) {
                                    {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    // Lista de tarjetas de macros
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            // Banner informativo del atajo rápido
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Consejo táctil: También puedes escribir el atajo (ej. :acta:) directamente en el texto para expandir la macro.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        items(filteredMacros, key = { it.id }) { macro ->
                            MacroCardItem(
                                macro = macro,
                                onExecute = {
                                    val clip = getClipboardContent()
                                    onExecuteMacro(macro, clip)
                                    onDismiss()
                                },
                                onDelete = { onDeleteMacro(macro) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            } else {
                // ==============================================================
                // PESTAÑA 1: CONSTRUCTOR DE MACROS CON VARIABLES DINÁMICAS
                // ==============================================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Construye tu propia plantilla automatizada",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = newMacroName,
                        onValueChange = { newMacroName = it },
                        label = { Text("Nombre de la macro (Ej. Minuta Semanal)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newMacroTrigger,
                            onValueChange = { newMacroTrigger = it },
                            label = { Text("Atajo (Ej. :minuta:)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = newMacroCategory,
                            onValueChange = { newMacroCategory = it },
                            label = { Text("Categoría") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = newMacroDescription,
                        onValueChange = { newMacroDescription = it },
                        label = { Text("Descripción corta") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // --- Paleta de Variables Dinámicas Táctiles ---
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Toca una variable para insertarla en la plantilla:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Botones táctiles de variables dinámicas (FlowRow)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MacroEngine.AVAILABLE_VARIABLES.forEach { variable ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier
                                            .clickable {
                                                newMacroContent += variable.token
                                            }
                                    ) {
                                        Text(
                                            text = variable.token,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Campo de Texto de la Plantilla
                    OutlinedTextField(
                        value = newMacroContent,
                        onValueChange = { newMacroContent = it },
                        label = { Text("Contenido de la Plantilla (Admite Markdown, tablas y variables)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp),
                        maxLines = 10
                    )

                    // Botón de Guardado Táctil Grande (Mínimo 48dp)
                    Button(
                        onClick = {
                            if (newMacroName.isNotBlank() && newMacroContent.isNotBlank()) {
                                onCreateMacro(
                                    newMacroName,
                                    newMacroDescription,
                                    newMacroTrigger,
                                    newMacroCategory,
                                    newMacroContent
                                )
                                // Reiniciar formulario y volver a catálogo
                                newMacroName = ""
                                newMacroDescription = ""
                                newMacroTrigger = ""
                                newMacroContent = ""
                                currentTabIndex = 0
                            }
                        },
                        enabled = newMacroName.isNotBlank() && newMacroContent.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_custom_macro_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Guardar Macro Automatizada",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

/**
 * Tarjeta individual para mostrar los detalles y acciones de una macro.
 */
@Composable
private fun MacroCardItem(
    macro: MacroEntity,
    onExecute: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("macro_item_${macro.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getMacroIcon(macro.iconName, macro.category),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = macro.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = macro.category,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (macro.triggerKeyword.isNotBlank()) {
                                Text(
                                    text = "• Atajo: ${macro.triggerKeyword}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                if (!macro.isPredefined) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Eliminar macro",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (macro.description.isNotBlank()) {
                Text(
                    text = macro.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Vista previa compacta de la plantilla
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = macro.templateContent.trim(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(8.dp)
                )
            }

            // Botón de inserción táctil completo (Mínimo 48dp)
            Button(
                onClick = onExecute,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("execute_macro_${macro.id}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Insertar en la Hoja",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

private fun getMacroIcon(iconName: String, category: String): ImageVector {
    return when (iconName) {
        "groups" -> Icons.Outlined.Groups
        "mail" -> Icons.Outlined.Mail
        "assignment" -> Icons.AutoMirrored.Outlined.Assignment
        "format_quote" -> Icons.Outlined.FormatQuote
        "table_chart" -> Icons.Outlined.TableChart
        "gavel" -> Icons.Outlined.Gavel
        "auto_awesome" -> Icons.Outlined.AutoAwesome
        "insights" -> Icons.Outlined.Insights
        else -> when (category.uppercase()) {
            "TRABAJO" -> Icons.Outlined.Groups
            "CORRESPONDENCIA" -> Icons.Outlined.Mail
            "ACADÉMICO" -> Icons.Outlined.FormatQuote
            "TABLAS" -> Icons.Outlined.TableChart
            "LEGAL" -> Icons.Outlined.Gavel
            "REPORTES" -> Icons.Outlined.Insights
            else -> Icons.Outlined.Description
        }
    }
}
