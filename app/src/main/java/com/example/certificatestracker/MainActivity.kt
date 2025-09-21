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
                val db = CertificatesDatabase.getDatabase(application)
                val dao = db.certificatesDao()
                val factory = CertificatesViewModelFactory(dao)
                val certificatesViewModel: CertificatesViewModel = viewModel(factory = factory)

                CertificatesScreen(viewModel = certificatesViewModel)
            }
        }
    }
}
