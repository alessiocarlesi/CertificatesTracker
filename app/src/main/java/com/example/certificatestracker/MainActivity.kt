package com.example.certificatestracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var certificatesViewModel: CertificatesViewModel
    private lateinit var dao: CertificatesDao
    private lateinit var apiUsageDao: ApiUsageDao
    private lateinit var insertionDao: CertificateInsertionDao
    private lateinit var underlyingPriceDao: UnderlyingPriceDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inizializzazione Database Certificati e Sottostanti (v14)
        val safeDb = CertificatesDatabase.getDatabase(applicationContext)
        dao = safeDb.certificatesDao()
        underlyingPriceDao = safeDb.underlyingPriceDao()

        // 2. Inizializzazione Database Utilizzo API
        apiUsageDao = ApiUsageDatabase.getDatabase(applicationContext).apiUsageDao()

        // 3. Inizializzazione Database Inserzioni
        val insertionDb = InsertionDatabase.getDatabase(applicationContext)
        insertionDao = insertionDb.insertionDao()

        // 4. Creazione ViewModel (Istanza UNICA per tutta l'app)
        certificatesViewModel = ViewModelProvider(
            this,
            CertificatesViewModelFactory(dao, apiUsageDao, insertionDao, underlyingPriceDao)
        )[CertificatesViewModel::class.java]

        // 5. Interfaccia UI - Passiamo il ViewModel alla navigazione
        setContent {
            MaterialTheme {
                Surface {
                    AppNavigation(certificatesViewModel)
                }
            }
        }

        // 6. Caricamento iniziale e aggiornamento prezzi all'avvio
        CoroutineScope(Dispatchers.Main).launch {
            // Sincronizziamo le date per i calcoli dei bonus
            certificatesViewModel.refreshInsertionDates()

            // Aggiornamento automatico dei prezzi per popolare i log all'avvio
            val lista = certificatesViewModel.certificates.value
            if (lista.isNotEmpty()) {
                lista.forEach { cert ->
                    certificatesViewModel.fetchAndUpdatePrice(cert.isin)
                }
            }
        }

        // 7. Backup automatico
        DatabaseBackupHelper.backupDatabase(this)
    }
}