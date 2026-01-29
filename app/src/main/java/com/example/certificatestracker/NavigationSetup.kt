// filename: app/src/main/java/com/example/certificatestracker/NavigationSetup.kt
package com.example.certificatestracker

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState

@Composable
fun AppNavigation(viewModel: CertificatesViewModel) {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = "certificates") {

        // 🔹 Schermata principale (Lista/Dettaglio)
        composable("certificates") {
            CertificatesScreen(viewModel = viewModel, navController = navController)
        }

        // 🔹 Schermata di riepilogo bonus mensili (Tabella)
        composable("summary") {
            // Passiamo il viewModel completo per permettere il recupero delle date di acquisto
            MonthlySummaryScreen(viewModel = viewModel)
        }

        // 🔹 Schermata Log API
        composable("apilogs") {
            ApiLogsScreen(viewModel = viewModel)
        }
    }
}