package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatAlignLeft
import androidx.compose.material.icons.automirrored.outlined.FormatAlignRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.FormatAlignJustify
import androidx.compose.material.icons.outlined.FormatLineSpacing
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Margin
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DocumentViewModel
import com.example.util.PageFormat

/**
 * DocumentSettingsScreen: Pantalla dedicada para configurar las propiedades de la hoja y el texto.
 * 
 * Permite cambiar de manera modular:
 * - Color y acabado de la hoja (Blanco, Marfil, Rayado, Cuadriculado, Sepia, Carbón)
 * - Familia tipográfica (Serif, Sans-Serif, Monospace)
 * - Tamaño de la fuente (14sp a 24sp)
 * - Interlineado de párrafo (1.2x, 1.5x, 2.0x)
 * - Márgenes laterales de la hoja (Estrecho, Normal, Amplio)
 * Incluye una vista previa en tiempo real de cómo luce el papel seleccionado.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DocumentSettingsScreen(
    docId: Long,
    viewModel: DocumentViewModel,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(docId) {
        viewModel.loadDocument(docId)
    }

    val activeDoc by viewModel.activeDocument.collectAsStateWithLifecycle()
    val doc = activeDoc ?: return

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Volver a la hoja"
                        )
                    }
                },
                title = {
                    Text(
                        text = "Ajustes de la Hoja",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_and_return_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Aplicar y volver a la Hoja", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- Vista previa interactiva de la hoja seleccionada ---
            Text(
                text = "VISTA PREVIA DE LA HOJA",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            )

            val previewBg = when (doc.paperType) {
                "IVORY" -> Color(0xFFFAF7F0)
                "LINED" -> Color(0xFFFCFDFD)
                "GRID" -> Color(0xFFFAFBFC)
                "SEPIA" -> Color(0xFFF5EEDC)
                "DARK" -> Color(0xFF1E2124)
                else -> Color(0xFFFFFFFF)
            }
            val previewText = if (doc.paperType == "DARK") Color(0xFFF1F5F9) else Color(0xFF1E293B)
            val previewFont = when (doc.fontStyle) {
                "SANS_SERIF" -> FontFamily.SansSerif
                "MONOSPACE" -> FontFamily.Monospace
                else -> FontFamily.Serif
            }

            val previewAlignment = when (doc.alignment.uppercase()) {
                "JUSTIFY" -> TextAlign.Justify
                "CENTER" -> TextAlign.Center
                "RIGHT" -> TextAlign.Right
                else -> TextAlign.Left
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(4.dp)),
                colors = CardDefaults.cardColors(containerColor = previewBg),
                shape = RoundedCornerShape(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "El arte de escribir con elegancia",
                        style = TextStyle(
                            fontFamily = previewFont,
                            fontSize = (doc.fontSize + 2).sp,
                            fontWeight = FontWeight.Bold,
                            color = previewText,
                            textAlign = previewAlignment
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Las palabras fluyen con naturalidad sobre el papel cuando el entorno está diseñado para inspirar concentración y claridad mental.",
                        style = TextStyle(
                            fontFamily = previewFont,
                            fontSize = doc.fontSize.sp,
                            lineHeight = (doc.fontSize * doc.lineSpacing).sp,
                            color = previewText,
                            textAlign = previewAlignment
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- Sección 1: Tipo de Papel ---
            SettingsSectionHeader(
                icon = Icons.Outlined.MenuBook,
                title = "Tipo y textura de papel"
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaperTypeOption(
                    title = "Blanco Clásico",
                    paperType = "WHITE",
                    colorPreview = Color(0xFFFFFFFF),
                    isSelected = doc.paperType == "WHITE",
                    onSelect = { viewModel.updatePageSettings(paperType = "WHITE") }
                )
                PaperTypeOption(
                    title = "Marfil Cálido",
                    paperType = "IVORY",
                    colorPreview = Color(0xFFFAF7F0),
                    isSelected = doc.paperType == "IVORY",
                    onSelect = { viewModel.updatePageSettings(paperType = "IVORY") }
                )
                PaperTypeOption(
                    title = "Rayado Cuaderno",
                    paperType = "LINED",
                    colorPreview = Color(0xFFE0F2FE),
                    isSelected = doc.paperType == "LINED",
                    onSelect = { viewModel.updatePageSettings(paperType = "LINED") }
                )
                PaperTypeOption(
                    title = "Cuadriculado",
                    paperType = "GRID",
                    colorPreview = Color(0xFFEFF6FF),
                    isSelected = doc.paperType == "GRID",
                    onSelect = { viewModel.updatePageSettings(paperType = "GRID") }
                )
                PaperTypeOption(
                    title = "Sepia Papiro",
                    paperType = "SEPIA",
                    colorPreview = Color(0xFFF5EEDC),
                    isSelected = doc.paperType == "SEPIA",
                    onSelect = { viewModel.updatePageSettings(paperType = "SEPIA") }
                )
                PaperTypeOption(
                    title = "Carbón Noche",
                    paperType = "DARK",
                    colorPreview = Color(0xFF1E2124),
                    isSelected = doc.paperType == "DARK",
                    onSelect = { viewModel.updatePageSettings(paperType = "DARK") }
                )
            }

            // --- Sección: Formato de Hoja y Capacidad (Paginación Automática) ---
            SettingsSectionHeader(
                icon = Icons.Outlined.AspectRatio,
                title = "Tamaño de Hoja y Paginación"
            )

            Text(
                text = "Formato físico del papel:",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PageFormat.entries.forEach { format ->
                    val isSelected = doc.pageSize == format.name
                    Surface(
                        modifier = Modifier
                            .clickable { viewModel.updatePageFormat(format.name) }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = format.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = "${format.dimensionsMm} • ~${format.defaultWordsLimit} pal.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Control de palabras por hoja
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Límite de palabras por hoja:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${doc.wordsPerPage} palabras",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = doc.wordsPerPage.toFloat(),
                        onValueChange = { viewModel.updateWordsPerPageLimit(it.toInt()) },
                        valueRange = 100f..800f,
                        steps = 13,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val current = doc.wordsPerPage
                                    if (current > 100) viewModel.updateWordsPerPageLimit((current - 50).coerceAtLeast(100))
                                },
                                modifier = Modifier.height(32.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Outlined.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("50", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val current = doc.wordsPerPage
                                    if (current < 800) viewModel.updateWordsPerPageLimit((current + 50).coerceAtMost(800))
                                },
                                modifier = Modifier.height(32.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("50", fontSize = 11.sp)
                            }
                        }

                        TextButton(
                            onClick = {
                                val standardWords = PageFormat.fromId(doc.pageSize).defaultWordsLimit
                                viewModel.updateWordsPerPageLimit(standardWords)
                            },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Estándar", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Al alcanzar este umbral de palabras, DocuSheet creará automáticamente una nueva hoja consecutiva (Pág. 2, 3...) de forma ininterrumpida.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            // --- Sección 2: Tipografía ---
            SettingsSectionHeader(
                icon = Icons.Outlined.FontDownload,
                title = "Familia Tipográfica"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TypographyOption(
                    label = "Serif",
                    sub = "Editorial",
                    fontFamily = FontFamily.Serif,
                    isSelected = doc.fontStyle == "SERIF",
                    modifier = Modifier.weight(1f),
                    onSelect = { viewModel.updatePageSettings(fontStyle = "SERIF") }
                )
                TypographyOption(
                    label = "Sans-Serif",
                    sub = "Moderna",
                    fontFamily = FontFamily.SansSerif,
                    isSelected = doc.fontStyle == "SANS_SERIF",
                    modifier = Modifier.weight(1f),
                    onSelect = { viewModel.updatePageSettings(fontStyle = "SANS_SERIF") }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TypographyOption(
                    label = "Monospace",
                    sub = "Máquina",
                    fontFamily = FontFamily.Monospace,
                    isSelected = doc.fontStyle == "MONOSPACE",
                    modifier = Modifier.weight(1f),
                    onSelect = { viewModel.updatePageSettings(fontStyle = "MONOSPACE") }
                )
                TypographyOption(
                    label = "Caligráfica",
                    sub = "Manuscrita",
                    fontFamily = FontFamily.Cursive,
                    isSelected = doc.fontStyle == "CURSIVE",
                    modifier = Modifier.weight(1f),
                    onSelect = { viewModel.updatePageSettings(fontStyle = "CURSIVE") }
                )
            }

            // --- Sección 3: Tamaño de Letra ---
            SettingsSectionHeader(
                icon = Icons.Outlined.FormatSize,
                title = "Tamaño de Fuente (SP)"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(14, 16, 18, 20, 24).forEach { size ->
                    FilterChip(
                        selected = doc.fontSize == size,
                        onClick = { viewModel.updatePageSettings(fontSize = size) },
                        label = { Text("${size}sp", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- Sección 4: Interlineado ---
            SettingsSectionHeader(
                icon = Icons.Outlined.FormatLineSpacing,
                title = "Espaciado entre líneas"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpacingOption(
                    label = "1.2x",
                    title = "Compacto",
                    isSelected = doc.lineSpacing == 1.2f,
                    modifier = Modifier.weight(1f),
                    onSelect = { viewModel.updatePageSettings(lineSpacing = 1.2f) }
                )
                SpacingOption(
                    label = "1.5x",
                    title = "Estándar",
                    isSelected = doc.lineSpacing == 1.5f,
                    modifier = Modifier.weight(1f),
                    onSelect = { viewModel.updatePageSettings(lineSpacing = 1.5f) }
                )
                SpacingOption(
                    label = "2.0x",
                    title = "Doble",
                    isSelected = doc.lineSpacing == 2.0f,
                    modifier = Modifier.weight(1f),
                    onSelect = { viewModel.updatePageSettings(lineSpacing = 2.0f) }
                )
            }

            // --- Sección 5: Márgenes de la Hoja ---
            SettingsSectionHeader(
                icon = Icons.Outlined.Margin,
                title = "Márgenes laterales de la hoja"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MarginOption(
                    label = "Estrecho",
                    sub = "16 dp",
                    isSelected = doc.marginStyle == "NARROW",
                    modifier = Modifier.weight(1f),
                    onSelect = { viewModel.updatePageSettings(marginStyle = "NARROW") }
                )
                MarginOption(
                    label = "Normal",
                    sub = "24 dp",
                    isSelected = doc.marginStyle == "NORMAL",
                    modifier = Modifier.weight(1f),
                    onSelect = { viewModel.updatePageSettings(marginStyle = "NORMAL") }
                )
                MarginOption(
                    label = "Amplio",
                    sub = "36 dp",
                    isSelected = doc.marginStyle == "WIDE",
                    modifier = Modifier.weight(1f),
                    onSelect = { viewModel.updatePageSettings(marginStyle = "WIDE") }
                )
            }

            // --- Sección 6: Alineación Cuádruple con Justificado Real ---
            SettingsSectionHeader(
                icon = Icons.Outlined.FormatAlignJustify,
                title = "Alineación de Párrafos (PC Engine)"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = doc.alignment == "LEFT",
                    onClick = { viewModel.updatePageSettings(alignment = "LEFT") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.FormatAlignLeft,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = { Text("Izquierda", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = doc.alignment == "CENTER",
                    onClick = { viewModel.updatePageSettings(alignment = "CENTER") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.FormatAlignCenter,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = { Text("Centro", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = doc.alignment == "RIGHT",
                    onClick = { viewModel.updatePageSettings(alignment = "RIGHT") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.FormatAlignRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = { Text("Derecha", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = doc.alignment == "JUSTIFY",
                    onClick = { viewModel.updatePageSettings(alignment = "JUSTIFY") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.FormatAlignJustify,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = { Text("Justificado", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PaperTypeOption(
    title: String,
    paperType: String,
    colorPreview: Color,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onSelect)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(colorPreview, RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TypographyOption(
    label: String,
    sub: String,
    fontFamily: FontFamily,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onSelect)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = TextStyle(fontFamily = fontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SpacingOption(
    label: String,
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onSelect)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MarginOption(
    label: String,
    sub: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onSelect)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = sub, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
