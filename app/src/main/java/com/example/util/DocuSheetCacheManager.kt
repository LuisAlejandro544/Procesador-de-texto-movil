package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.UUID

/**
 * Estadísticas cuantitativas del estado de almacenamiento en caché y archivos temporales.
 */
data class CacheStats(
    val exportCacheSizeBytes: Long = 0L,
    val exportFilesCount: Int = 0,
    val imagesCacheSizeBytes: Long = 0L,
    val imagesFilesCount: Int = 0,
    val orphanImagesCount: Int = 0,
    val orphanImagesSizeBytes: Long = 0L,
    val coilCacheSizeBytes: Long = 0L,
    val totalReclaimableBytes: Long = 0L
) {
    val totalReclaimableFormatted: String
        get() = formatBytes(totalReclaimableBytes)

    val totalCacheFormatted: String
        get() = formatBytes(exportCacheSizeBytes + imagesCacheSizeBytes + coilCacheSizeBytes)

    val imagesCacheFormatted: String
        get() = formatBytes(imagesCacheSizeBytes)

    val exportsCacheFormatted: String
        get() = formatBytes(exportCacheSizeBytes)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 KB"
            val kb = bytes / 1024.0
            if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
            val mb = kb / 1024.0
            return String.format(Locale.US, "%.2f MB", mb)
        }
    }
}

/**
 * DocuSheetCacheManager: Motor de gestión inteligente de memoria y almacenamiento en caché.
 *
 * Responsabilidades:
 * 1. Importación y compresión balanceada de imágenes desde la galería del teléfono (Android Photo Picker).
 * 2. Almacenamiento persistente local en `filesDir/doc_images` para evitar expiración de permisos de URIs.
 * 3. Configuración optimizada de Coil con límites estrictos de memoria RAM (25%) y caché de disco (50 MB).
 * 4. Detección y purga de imágenes huérfanas (archivos que ya no están referenciados por ningún documento de Room).
 * 5. Limpieza automática o manual de archivos temporales generados al exportar PDF y Markdown.
 */
object DocuSheetCacheManager {

    private const val MAX_IMAGE_DIMENSION = 1920
    private const val JPEG_QUALITY = 86

    /**
     * Inicializa Coil globalmente con una política inteligente de memoria y disco.
     */
    fun initializeCoil(context: Context) {
        val imageLoader = ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "coil_image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50 MB
                    .build()
            }
            .crossfade(true)
            .build()

        Coil.setImageLoader(imageLoader)
    }

    /**
     * Guarda y optimiza de forma permanente una imagen seleccionada de la galería en el almacenamiento
     * interno privado de DocuSheet (filesDir/doc_images).
     * 
     * Ventajas:
     * - Utiliza una única apertura de stream para garantizar compatibilidad con todos los proveedores de contenido (Photo Picker, Google Fotos, etc.).
     * - Protege la imagen de expiración de URI temporal de Android.
     * - Retorna la ruta absoluta local en el dispositivo.
     */
    suspend fun saveImageFromUri(context: Context, sourceUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val imagesDir = File(context.filesDir, "doc_images").apply { mkdirs() }
                val uniqueId = UUID.randomUUID().toString().take(8)
                val fileName = "docusheet_img_${System.currentTimeMillis()}_$uniqueId.jpg"
                val destinationFile = File(imagesDir, fileName)

                // 1. Copiar directamente los bytes del ContentProvider al archivo local en almacenamiento privado
                val streamOpened = context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    FileOutputStream(destinationFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    true
                } ?: false

                if (!streamOpened || !destinationFile.exists() || destinationFile.length() == 0L) {
                    destinationFile.delete()
                    return@withContext null
                }

                // 2. Si la foto es gigante (> 4 MB), decodificarla y optimizarla a tamaño balanceado
                if (destinationFile.length() > 4L * 1024 * 1024) {
                    try {
                        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(destinationFile.absolutePath, boundsOpts)
                        var sampleSize = 1
                        while ((boundsOpts.outWidth / sampleSize) > MAX_IMAGE_DIMENSION || (boundsOpts.outHeight / sampleSize) > MAX_IMAGE_DIMENSION) {
                            sampleSize *= 2
                        }
                        if (sampleSize > 1) {
                            val decodeOpts = BitmapFactory.Options().apply {
                                inSampleSize = sampleSize
                                inPreferredConfig = Bitmap.Config.ARGB_8888
                            }
                            val sampledBitmap = BitmapFactory.decodeFile(destinationFile.absolutePath, decodeOpts)
                            if (sampledBitmap != null) {
                                val tempOptimized = File(imagesDir, "temp_$fileName")
                                FileOutputStream(tempOptimized).use { out ->
                                    sampledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                                }
                                sampledBitmap.recycle()
                                if (tempOptimized.exists() && tempOptimized.length() > 0) {
                                    destinationFile.delete()
                                    tempOptimized.renameTo(destinationFile)
                                } else {
                                    tempOptimized.delete()
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Si falla la re-compresión, se mantiene el archivo original copiado
                    }
                }

                destinationFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Audita el estado actual de la caché y el almacenamiento de medios de la aplicación.
     */
    suspend fun getCacheStats(context: Context, activeDocumentContents: List<String>): CacheStats {
        return withContext(Dispatchers.IO) {
            val exportsDir = File(context.cacheDir, "exports")
            val exportFiles = exportsDir.listFiles() ?: emptyArray()
            val exportSizeBytes = exportFiles.sumOf { it.length() }

            val imagesDir = File(context.filesDir, "doc_images")
            val imageFiles = imagesDir.listFiles() ?: emptyArray()
            val imagesSizeBytes = imageFiles.sumOf { it.length() }

            val coilDir = File(context.cacheDir, "coil_image_cache")
            val coilFiles = coilDir.listFiles() ?: emptyArray()
            val coilSizeBytes = coilFiles.sumOf { it.length() }

            // Identificar imágenes huérfanas (no mencionadas en ningún documento de Room)
            val allContentsJoined = activeDocumentContents.joinToString("\n")
            var orphanCount = 0
            var orphanBytes = 0L

            for (file in imageFiles) {
                val fileName = file.name
                val filePath = file.absolutePath
                val isReferenced = allContentsJoined.contains(fileName) || allContentsJoined.contains(filePath)
                if (!isReferenced) {
                    orphanCount++
                    orphanBytes += file.length()
                }
            }

            val totalReclaimable = exportSizeBytes + orphanBytes

            CacheStats(
                exportCacheSizeBytes = exportSizeBytes,
                exportFilesCount = exportFiles.size,
                imagesCacheSizeBytes = imagesSizeBytes,
                imagesFilesCount = imageFiles.size,
                orphanImagesCount = orphanCount,
                orphanImagesSizeBytes = orphanBytes,
                coilCacheSizeBytes = coilSizeBytes,
                totalReclaimableBytes = totalReclaimable
            )
        }
    }

    /**
     * Elimina archivos temporales de exportación (PDF y Markdown).
     */
    suspend fun cleanExportCache(context: Context): Long {
        return withContext(Dispatchers.IO) {
            val exportsDir = File(context.cacheDir, "exports")
            val files = exportsDir.listFiles() ?: emptyArray()
            var freedBytes = 0L
            for (file in files) {
                val len = file.length()
                if (file.delete()) {
                    freedBytes += len
                }
            }
            freedBytes
        }
    }

    /**
     * Elimina únicamente las imágenes huérfanas que ya no están referenciadas por ningún documento.
     */
    suspend fun cleanOrphanImages(context: Context, activeDocumentContents: List<String>): Long {
        return withContext(Dispatchers.IO) {
            val imagesDir = File(context.filesDir, "doc_images")
            val files = imagesDir.listFiles() ?: emptyArray()
            val allContentsJoined = activeDocumentContents.joinToString("\n")
            val now = System.currentTimeMillis()
            val gracePeriodMillis = 2 * 60 * 60 * 1000L // 2 horas de gracia para borradores en curso
            var freedBytes = 0L

            for (file in files) {
                val fileName = file.name
                val filePath = file.absolutePath
                val isReferenced = allContentsJoined.contains(fileName) || allContentsJoined.contains(filePath)
                val isRecent = (now - file.lastModified()) < gracePeriodMillis
                if (!isReferenced && !isRecent) {
                    val len = file.length()
                    if (file.delete()) {
                        freedBytes += len
                    }
                }
            }
            freedBytes
        }
    }

    /**
     * Limpia todo el almacenamiento prescindible (exportaciones y archivos huérfanos).
     */
    suspend fun cleanAllReclaimable(context: Context, activeDocumentContents: List<String>): Long {
        return withContext(Dispatchers.IO) {
            val freedExports = cleanExportCache(context)
            val freedOrphans = cleanOrphanImages(context, activeDocumentContents)
            freedExports + freedOrphans
        }
    }

    /**
     * Poda silenciosa automática de archivos temporales antiguos (más de 48 horas) o si la caché supera 40 MB.
     */
    suspend fun autoPruneIfExceeded(context: Context, activeDocumentContents: List<String>) {
        withContext(Dispatchers.IO) {
            try {
                val exportsDir = File(context.cacheDir, "exports")
                val files = exportsDir.listFiles() ?: emptyArray()
                val now = System.currentTimeMillis()
                val maxAgeMillis = 48 * 60 * 60 * 1000L // 48 horas

                var totalExportBytes = 0L
                for (f in files) {
                    totalExportBytes += f.length()
                    if (now - f.lastModified() > maxAgeMillis) {
                        f.delete()
                    }
                }

                // Si aún supera los 30 MB, eliminar los más viejos
                if (totalExportBytes > 30L * 1024 * 1024) {
                    cleanExportCache(context)
                }

                // Limpiar huérfanas periódicamente
                cleanOrphanImages(context, activeDocumentContents)
            } catch (e: Exception) {
                // Falla silenciosa para no interrumpir el flujo del usuario
            }
        }
    }
}
