package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.DocumentViewModel
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.DocumentEditorScreen
import com.example.ui.screens.DocumentListScreen
import com.example.ui.screens.DocumentSettingsScreen
import com.example.ui.screens.MacroManagerScreen
import com.example.ui.screens.PdfViewerScreen
import android.net.Uri

/**
 * NavGraph: Grafo central de navegación modular de DocuSheet.
 * 
 * Gestiona el enrutamiento entre las diferentes pantallas:
 * - "documents": Biblioteca de hojas y documentos
 * - "editor/{docId}": Editor de la hoja de texto activa
 * - "settings/{docId}": Configuración de formato y textura del papel
 * - "about": Pantalla de información, estadísticas y consejos de redacción
 * - "pdf_viewer": Visor nativo de alta resolución para documentos PDF ("Abrir con" o visualización local)
 */
@Composable
fun DocuSheetNavGraph(
    navController: NavHostController,
    viewModel: DocumentViewModel,
    initialPdfUri: Uri? = null,
    initialImportUri: Uri? = null
) {
    val context = LocalContext.current

    LaunchedEffect(initialImportUri) {
        if (initialImportUri != null) {
            viewModel.importDocument(
                context = context,
                uri = initialImportUri,
                onSuccess = { newId, _ ->
                    navController.navigate("editor/$newId")
                },
                onError = {
                    // Si falla la importación externa, permanece en la biblioteca
                }
            )
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (initialPdfUri != null) "pdf_viewer?uri=${Uri.encode(initialPdfUri.toString())}" else "documents"
    ) {
        // Pantalla 1: Biblioteca de Documentos
        composable(route = "documents") {
            DocumentListScreen(
                viewModel = viewModel,
                onNavigateToEditor = { docId ->
                    navController.navigate("editor/$docId")
                },
                onNavigateToSettings = { docId ->
                    navController.navigate("settings/$docId")
                },
                onNavigateToAbout = {
                    navController.navigate("about")
                },
                onOpenPdfUri = { uri ->
                    val encoded = Uri.encode(uri.toString())
                    navController.navigate("pdf_viewer?uri=$encoded")
                }
            )
        }

        // Pantalla 2: El Editor de Hoja de Texto
        composable(
            route = "editor/{docId}",
            arguments = listOf(
                navArgument("docId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
            DocumentEditorScreen(
                docId = docId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSettings = { id ->
                    navController.navigate("settings/$id")
                }
            )
        }

        // Pantalla 3: Ajustes de la Hoja
        composable(
            route = "settings/{docId}",
            arguments = listOf(
                navArgument("docId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
            DocumentSettingsScreen(
                docId = docId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Pantalla 4: Información y Estadísticas
        composable(route = "about") {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToMacros = {
                    navController.navigate("macros")
                }
            )
        }

        // Pantalla 5: Gestor Completo de Macros y Variables Dinámicas
        composable(route = "macros") {
            MacroManagerScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Pantalla 6: Visor Nativo de PDF de Alta Fidelidad
        composable(
            route = "pdf_viewer?uri={uri}",
            arguments = listOf(
                navArgument("uri") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val rawUri = backStackEntry.arguments?.getString("uri") ?: ""
            val parsedUri = if (rawUri.isNotBlank()) Uri.parse(Uri.decode(rawUri)) else Uri.EMPTY
            PdfViewerScreen(
                pdfUri = parsedUri,
                onNavigateBack = {
                    // Si vino directamente de "abrir con" y no hay backstack, ir a documents
                    if (!navController.popBackStack()) {
                        navController.navigate("documents") {
                            popUpTo(0)
                        }
                    }
                }
            )
        }
    }
}
