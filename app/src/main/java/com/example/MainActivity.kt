package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.ui.DocumentViewModel
import com.example.ui.navigation.DocuSheetNavGraph
import com.example.ui.theme.MyApplicationTheme

/**
 * Representa un archivo entrante recibido desde el sistema operativo o explorador de archivos.
 */
sealed class IncomingFile {
    data class Pdf(val uri: Uri) : IncomingFile()
    data class Document(val uri: Uri) : IncomingFile()
}

/**
 * MainActivity: Punto de entrada principal de la aplicación DocuSheet.
 * 
 * Configura el soporte de pantalla completa (Edge-to-Edge), inicializa el tema,
 * detecta y procesa intents de "Abrir con" o "Compartir con" para archivos PDF,
 * DOCX, RTF, Markdown y Texto Plano entrantes, y arranca el grafo de navegación.
 */
class MainActivity : ComponentActivity() {

    private var incomingFile by mutableStateOf<IncomingFile?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        incomingFile = extractIncomingFile(intent)

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val viewModel: DocumentViewModel = viewModel()
                val currentFile = incomingFile
                DocuSheetNavGraph(
                    navController = navController,
                    viewModel = viewModel,
                    initialPdfUri = (currentFile as? IncomingFile.Pdf)?.uri,
                    initialImportUri = (currentFile as? IncomingFile.Document)?.uri
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingFile = extractIncomingFile(intent)
    }

    /**
     * Extrae y clasifica el URI del archivo entrante cuando la app es invocada desde "Abrir con"
     * o mediante el menú "Compartir" de otra aplicación (PDF, DOCX, RTF, TXT, etc.).
     */
    private fun extractIncomingFile(intent: Intent?): IncomingFile? {
        if (intent == null) return null
        val action = intent.action
        val type = intent.type

        val uri: Uri? = when (action) {
            Intent.ACTION_VIEW, Intent.ACTION_EDIT -> intent.data
            Intent.ACTION_SEND -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                }
            }
            else -> null
        }

        if (uri == null) return null

        val isPdf = isPdfTarget(uri, type)
        return if (isPdf) IncomingFile.Pdf(uri) else IncomingFile.Document(uri)
    }

    private fun isPdfTarget(uri: Uri, mimeType: String?): Boolean {
        if (mimeType?.contains("pdf", ignoreCase = true) == true) return true
        val uriStr = uri.toString().lowercase()
        if (uriStr.endsWith(".pdf")) return true
        val path = uri.path?.lowercase() ?: ""
        if (path.endsWith(".pdf")) return true
        try {
            val type = contentResolver.getType(uri)
            if (type?.contains("pdf", ignoreCase = true) == true) return true
        } catch (_: Exception) {}
        return false
    }
}
