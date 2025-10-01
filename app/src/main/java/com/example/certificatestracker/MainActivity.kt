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

        // 🔹 Ottieni DAO
        dao = CertificatesDatabase.getDatabase(applicationContext).certificatesDao()
        apiUsageDao = ApiUsageDatabase.getDatabase(applicationContext).apiUsageDao()

        // 🔹 Crea ViewModel con entrambi i DAO
        certificatesViewModel = ViewModelProvider(
            this,
            CertificatesViewModelFactory(dao, apiUsageDao)
        )[CertificatesViewModel::class.java]

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
    }
}
