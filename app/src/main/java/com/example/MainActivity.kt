package com.example

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
 * Configura el soporte de pantalla completa (Edge-to-Edge), inicializa el tema
 * y arranca el grafo de navegación conectando el ViewModel central.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val viewModel: DocumentViewModel = viewModel()
                DocuSheetNavGraph(
                    navController = navController,
                    viewModel = viewModel
                )
            }
        }
    }
}
