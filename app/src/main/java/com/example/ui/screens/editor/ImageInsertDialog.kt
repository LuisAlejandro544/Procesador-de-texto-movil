package com.example.ui.screens.editor

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.util.CacheStats
import com.example.util.DocuSheetCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Diálogo modal para inserción de imágenes con ajuste de hoja (Layout & Wrap),
 * selección directa desde la galería de fotos del móvil y compresión optimizada en caché.
 */
@Composable
fun ImageInsertDialog(
    context: Context,
    onDismiss: () -> Unit,
    onInsert: (uri: String, wrapMode: String, caption: String) -> Unit
) {
    var imageUrlInput by remember { mutableStateOf("") }
    var imageCaptionInput by remember { mutableStateOf("") }
    var imageWrapMode by remember { mutableStateOf("full") }
    var isProcessingGalleryImage by remember { mutableStateOf(false) }
    var selectedImageFile by remember { mutableStateOf<File?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (_: Exception) {}

            isProcessingGalleryImage = true
            coroutineScope.launch(Dispatchers.IO) {
                val savedFilePath = DocuSheetCacheManager.saveImageFromUri(context, uri)
                withContext(Dispatchers.Main) {
                    isProcessingGalleryImage = false
                    if (savedFilePath != null) {
                        val file = File(savedFilePath)
                        selectedImageFile = file
                        imageUrlInput = savedFilePath
                        Toast.makeText(context, "Imagen guardada permanentemente: ${CacheStats.formatBytes(file.length())}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No se pudo procesar la imagen seleccionada", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Insertar Imagen con Ajuste", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        galleryPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isProcessingGalleryImage) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Comprimiendo y optimizando...")
                    } else {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Elegir de la Galería del Móvil", fontWeight = FontWeight.SemiBold)
                    }
                }

                if (imageUrlInput.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val imageSource: Any = if (imageUrlInput.startsWith("/")) File(imageUrlInput) else imageUrlInput
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageSource)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Vista previa",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (imageUrlInput.startsWith("/")) "Imagen de Galería (Caché optimizada)" else "Enlace Web",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (imageUrlInput.startsWith("/")) {
                                        val f = File(imageUrlInput)
                                        "${f.name.take(18)} (${CacheStats.formatBytes(f.length())})"
                                    } else {
                                        imageUrlInput.take(24) + "..."
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Ajuste de Hoja (Layout & Wrap):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "full" to "Ancho",
                        "center" to "Centro",
                        "left" to "Izq.",
                        "right" to "Der."
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = imageWrapMode == mode,
                            onClick = { imageWrapMode = mode },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = imageUrlInput,
                    onValueChange = { imageUrlInput = it },
                    label = { Text("Ruta en caché o URL web") },
                    placeholder = { Text("https://ejemplo.com/grafico.png") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = imageCaptionInput,
                    onValueChange = { imageCaptionInput = it },
                    label = { Text("Pie de foto (opcional)") },
                    placeholder = { Text("Ej: Figura 1. Esquema conceptual") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalUrl = imageUrlInput.trim().ifEmpty {
                        "https://images.unsplash.com/photo-1455390582262-044cdead277a?w=800"
                    }
                    onInsert(
                        finalUrl,
                        imageWrapMode,
                        imageCaptionInput.trim()
                    )
                },
                enabled = !isProcessingGalleryImage
            ) {
                Text("Insertar en Hoja", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
