// filename: MainActivity.kt
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔹 Ottieni il database in modo sicuro (restore se corrotto)
        val safeDb = CertificatesDatabase.getDatabase(applicationContext)

        // 🔹 Ottieni DAO dal database sicuro
        dao = safeDb.certificatesDao()
        apiUsageDao = ApiUsageDatabase.getDatabase(applicationContext).apiUsageDao() // separato dal backup principale

        // 🔹 Crea ViewModel con entrambi i DAO
        certificatesViewModel = ViewModelProvider(
            this,
            CertificatesViewModelFactory(dao, apiUsageDao)
        )[CertificatesViewModel::class.java]

        // 🔹 Composable UI
        setContent {
            MaterialTheme {
                Surface {
                    CertificatesScreen(certificatesViewModel)
                }
            }
        }

        // 🔹 Aggiorna tutti i prezzi all’avvio
        CoroutineScope(Dispatchers.Main).launch {
            certificatesViewModel.certificates.value.forEach { cert ->
                certificatesViewModel.fetchAndUpdatePrice(cert.isin)
            }
        }

        // 🔹 Salva subito un backup aggiornato del DB
        DatabaseBackupHelper.backupDatabase(this)
    }
}
