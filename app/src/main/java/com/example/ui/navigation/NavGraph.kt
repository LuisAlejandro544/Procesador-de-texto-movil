package com.example.ui.navigation

import androidx.compose.runtime.Composable
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

/**
 * NavGraph: Grafo central de navegación modular de DocuSheet.
 * 
 * Gestiona el enrutamiento entre las diferentes pantallas:
 * - "documents": Biblioteca de hojas y documentos
 * - "editor/{docId}": Editor de la hoja de texto activa
 * - "settings/{docId}": Configuración de formato y textura del papel
 * - "about": Pantalla de información, estadísticas y consejos de redacción
 */
@Composable
fun DocuSheetNavGraph(
    navController: NavHostController,
    viewModel: DocumentViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "documents"
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
                }
            )
        }
    }
}
