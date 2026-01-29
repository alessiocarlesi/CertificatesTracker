// filename: app/src/main/java/com/example/certificatestracker/MainActivity.kt
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
    private lateinit var insertionDao: CertificateInsertionDao // 🔹 Nuovo DAO per le date di acquisto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inizializzazione Database Certificati (Anagrafica principale)
        val safeDb = CertificatesDatabase.getDatabase(applicationContext)
        dao = safeDb.certificatesDao()

        // 2. Inizializzazione Database Utilizzo API
        apiUsageDao = ApiUsageDatabase.getDatabase(applicationContext).apiUsageDao()

        // 3. Inizializzazione Database Inserzioni (Date di acquisto separate)
        // Questo database protegge le tue date di inserimento se resetti l'anagrafica
        val insertionDb = InsertionDatabase.getDatabase(applicationContext)
        insertionDao = insertionDb.insertionDao()

        // 4. Creazione ViewModel tramite Factory aggiornata con 3 parametri
        certificatesViewModel = ViewModelProvider(
            this,
            CertificatesViewModelFactory(dao, apiUsageDao, insertionDao)
        )[CertificatesViewModel::class.java]

        // UI
        setContent {
            MaterialTheme {
                Surface {
                    AppNavigation(certificatesViewModel)
                }
            }
        }

        // Caricamento iniziale e aggiornamento prezzi
        CoroutineScope(Dispatchers.Main).launch {
            // Sincronizziamo le date di inserimento prima di aggiornare i prezzi
            certificatesViewModel.refreshInsertionDates()

            // Aggiorna i prezzi di tutti i certificati presenti
            certificatesViewModel.certificates.value.forEach { cert ->
                certificatesViewModel.fetchAndUpdatePrice(cert.isin)
            }
        }

        // Backup automatico del database principale
        DatabaseBackupHelper.backupDatabase(this)
    }
}