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
    private lateinit var insertionDao: CertificateInsertionDao
    private lateinit var underlyingPriceDao: UnderlyingPriceDao // 🔹 DAO per prezzi persistenti v14

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inizializzazione Database Certificati e Sottostanti
        // Ora safeDb gestisce sia CertificatesDao che UnderlyingPriceDao
        val safeDb = CertificatesDatabase.getDatabase(applicationContext)
        dao = safeDb.certificatesDao()
        underlyingPriceDao = safeDb.underlyingPriceDao() // 🔹 Recupero istanza DAO v14

        // 2. Inizializzazione Database Utilizzo API
        apiUsageDao = ApiUsageDatabase.getDatabase(applicationContext).apiUsageDao()

        // 3. Inizializzazione Database Inserzioni (Date di acquisto separate)
        val insertionDb = InsertionDatabase.getDatabase(applicationContext)
        insertionDao = insertionDb.insertionDao()

        // 4. Creazione ViewModel tramite Factory aggiornata con 4 parametri
        // Passiamo tutti i motori necessari al ViewModel per funzionare offline
        certificatesViewModel = ViewModelProvider(
            this,
            CertificatesViewModelFactory(dao, apiUsageDao, insertionDao, underlyingPriceDao) // 🔹 Quarto parametro aggiunto
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

            // NOTA: Con la v14, i prezzi sottostanti vengono caricati automaticamente
            // nell'init del ViewModel dal database. Facciamo comunque un aggiornamento
            // per avere le quotazioni dell'ultimo minuto.
            certificatesViewModel.certificates.value.forEach { cert ->
                certificatesViewModel.fetchAndUpdatePrice(cert.isin)
            }
        }

        // Backup automatico del database principale
        DatabaseBackupHelper.backupDatabase(this)
    }
}