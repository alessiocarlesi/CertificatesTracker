package com.example.certificatestracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.certificatestracker.api.ApiProvider

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: CertificatesViewModel
    private lateinit var usageRepository: ApiUsageRepository
    private lateinit var dao: CertificatesDao
    private lateinit var apiUsageDao: ApiUsageDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Creazione del database con fallback distruttivo (utile in sviluppo)
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "certificates_db"
        )
            .fallbackToDestructiveMigration() // cancella dati vecchi se mismatch di versione
            .build()

        // Ottieni i DAO
        dao = db.certificatesDao()
        apiUsageDao = db.apiUsageDao()

        // Inizializza il repository di API usage
        usageRepository = ApiUsageRepository(apiUsageDao)

        // Inizializza il ViewModel con la factory aggiornata
        val factory = CertificatesViewModelFactory(dao, usageRepository)
        viewModel = ViewModelProvider(this, factory)[CertificatesViewModel::class.java]

        setContent {
            // Passa ViewModel e repository allo screen
            CertificatesScreen(
                viewModel = viewModel,
                usageRepository = usageRepository
            )
        }
    }
}
