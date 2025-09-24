package com.example.certificatestracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

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

                // Coroutine scope per aggiornamenti
                val scope = rememberCoroutineScope()

                // 🔹 Aggiornamento prezzi automatico all’avvio
                LaunchedEffect(certificatesViewModel) {
                    certificatesViewModel.certificates.collect { certList ->
                        certList.forEach { cert ->
                            scope.launch {
                                certificatesViewModel.fetchAndUpdatePrice(cert.isin, "e1e60f41a11968b889595584e0a6c310")
                            }
                        }
                    }
                }

                // Composable principale
                CertificatesScreen(viewModel = certificatesViewModel)
            }
        }
    }
}
