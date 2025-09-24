package com.example.certificatestracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                // Ottieni DAO dal database
                val dao = CertificatesDatabase.getDatabase(application).certificatesDao()

                // Crea il ViewModel con la factory
                val certificatesViewModel: CertificatesViewModel = viewModel(
                    factory = CertificatesViewModelFactory(dao)
                )

                // 🔹 LaunchedEffect per chiamare fetchAndUpdatePrice all'avvio
                LaunchedEffect(Unit) {
                    certificatesViewModel.fetchAndUpdatePrice("ISP.MI", "e1e60f41a11968b889595584e0a6c310")
                }



                // Composable principale
                CertificatesScreen(viewModel = certificatesViewModel)

                // 🔹 ESEMPIO TEMPORANEO SOLO PER DEBUG
                // Chiamata al fetcher per verificare il prezzo di chiusura
                certificatesViewModel.fetchLatestCloseForCertificate(
                    symbol = "ISP.MI",
                    apiKey = "e1e60f41a11968b889595584e0a6c310"
                )
            }
        }
    }
}
