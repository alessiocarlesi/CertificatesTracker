package com.example.certificatestracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel

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

                // Composable principale
                CertificatesScreen(viewModel = certificatesViewModel)
            }
        }
    }
}
