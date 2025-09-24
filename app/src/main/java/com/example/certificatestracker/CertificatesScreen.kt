package com.example.certificatestracker

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CertificatesScreen(viewModel: CertificatesViewModel) {
    val certificates by viewModel.certificates.collectAsState(initial = emptyList())

    var currentIndex by remember { mutableStateOf(0) }
    var newIsin by remember { mutableStateOf("") }
    var newUnderlying by remember { mutableStateOf("") }
    var newStrike by remember { mutableStateOf("") }
    var newBarrier by remember { mutableStateOf("") }
    var newBonus by remember { mutableStateOf("") }
    var newAutocall by remember { mutableStateOf("") }

    // Stato per evidenziare aggiornamenti recenti
    val recentlyUpdated = remember { mutableStateMapOf<String, Boolean>() }

    // Coroutine scope per onClick
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {

        // Navigazione tra certificati
        if (certificates.isNotEmpty()) {
            val cert = certificates.getOrNull(currentIndex)
            cert?.let {

                val textColor = if (recentlyUpdated[it.isin] == true) Color(0xFF008000) else Color.Black

                Text(
                    text = "ISIN: ${it.isin}\nSottostante: ${it.underlyingName} - Prezzo: ${it.lastPrice} EUR\nStrike: ${it.strike}\nBarrier: ${it.barrier}\nBonus: ${it.bonusLevel}\nAutocall: ${it.autocallLevel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    Button(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0
                    ) { Text("<") }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = { if (currentIndex < certificates.size - 1) currentIndex++ },
                        enabled = currentIndex < certificates.size - 1
                    ) { Text(">") }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.deleteCertificate(it.isin) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancella questo certificato") }
            }
        } else {
            Text("Nessun certificato inserito")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Campi di inserimento
        OutlinedTextField(
            value = newIsin,
            onValueChange = { newIsin = it },
            label = { Text("ISIN") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = newUnderlying,
            onValueChange = { newUnderlying = it },
            label = { Text("Sottostante") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = newStrike,
            onValueChange = { newStrike = it },
            label = { Text("Strike") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = newBarrier,
            onValueChange = { newBarrier = it },
            label = { Text("Barrier") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = newBonus,
            onValueChange = { newBonus = it },
            label = { Text("Bonus") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = newAutocall,
            onValueChange = { newAutocall = it },
            label = { Text("Autocall Level") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Pulsante per aggiungere certificato
        Button(
            onClick = {
                if (newIsin.isNotEmpty()) {
                    val strikeVal = newStrike.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val barrierVal = newBarrier.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val bonusVal = newBonus.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val autocallVal = newAutocall.replace(',', '.').toDoubleOrNull() ?: 0.0

                    viewModel.addCertificate(
                        isin = newIsin,
                        underlyingName = newUnderlying,
                        strike = strikeVal,
                        barrier = barrierVal,
                        bonusLevel = bonusVal,
                        autocallLevel = autocallVal
                    )

                    newIsin = ""
                    newUnderlying = ""
                    newStrike = ""
                    newBarrier = ""
                    newBonus = ""
                    newAutocall = ""

                    currentIndex = certificates.size
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Aggiungi certificato") }

        Spacer(modifier = Modifier.height(8.dp))

        // Pulsante per aggiornare tutti i prezzi
        Button(
            onClick = {
                certificates.forEach { cert ->
                    scope.launch {
                        viewModel.fetchAndUpdatePrice(cert.isin, "e1e60f41a11968b889595584e0a6c310")
                        recentlyUpdated[cert.isin] = true
                        delay(2000)
                        recentlyUpdated[cert.isin] = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Aggiorna tutti i prezzi") }
    }
}
