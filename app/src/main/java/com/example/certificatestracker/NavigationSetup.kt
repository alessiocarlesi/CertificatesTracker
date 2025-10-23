// filename: NavigationSetup.kt
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
        // 🔹 Schermata principale
        composable("certificates") {
            CertificatesScreen(viewModel = viewModel, navController = navController)
        }

        // 🔹 Schermata di riepilogo bonus mensili
        composable("summary") {
            val certificates = viewModel.certificates.collectAsState().value
            MonthlySummaryScreen(certificates = certificates)
        }

        composable("apilogs") {
            ApiLogsScreen(viewModel = viewModel)
        }

    }
}
