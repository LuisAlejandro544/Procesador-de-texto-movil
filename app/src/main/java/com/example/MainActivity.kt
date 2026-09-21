package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.ui.DocumentViewModel
import com.example.ui.navigation.DocuSheetNavGraph
import com.example.ui.theme.MyApplicationTheme

/**
 * MainActivity: Punto de entrada principal de la aplicación DocuSheet.
 * 
 * Configura el soporte de pantalla completa (Edge-to-Edge), inicializa el tema,
 * detecta y procesa intents de "Abrir con" o "Compartir con" para archivos PDF entrantes,
 * y arranca el grafo de navegación conectando el ViewModel central.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialPdfUri = extractPdfUriFromIntent(intent)

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val viewModel: DocumentViewModel = viewModel()
                DocuSheetNavGraph(
                    navController = navController,
                    viewModel = viewModel,
                    initialPdfUri = initialPdfUri
                )
            }
        }
    }

    /**
     * Extrae el URI seguro del archivo PDF cuando la app es invocada desde "Abrir con"
     * o mediante el menú "Compartir" de otra aplicación.
     */
    private fun extractPdfUriFromIntent(intent: Intent?): Uri? {
        if (intent == null) return null
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_VIEW == action) {
            val data = intent.data
            if (data != null) return data
        } else if (Intent.ACTION_SEND == action) {
            if (type != null && type.contains("pdf", ignoreCase = true)) {
                val streamUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                }
                if (streamUri != null) return streamUri
            }
        }
        return null
    }
}
