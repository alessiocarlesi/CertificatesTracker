package com.example.certificatestracker

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CertificatesScreen(viewModel: CertificatesViewModel) {
    val certificates by viewModel.certificates.collectAsState(initial = emptyList())
    var currentIndex by remember { mutableStateOf(0) }

    // Campi input
    var newIsin by remember { mutableStateOf("") }
    var newUnderlying by remember { mutableStateOf("") }
    var newStrike by remember { mutableStateOf("") }
    var newBarrier by remember { mutableStateOf("") }
    var newBonus by remember { mutableStateOf("") }
    var newAutocall by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        if (certificates.isNotEmpty()) {
            val cert = certificates[currentIndex]

            Text(
                text = "ISIN: ${cert.isin}\n" +
                        "Sottostante: ${cert.underlyingName}\n" +
                        "Strike: ${cert.strike}\n" +
                        "Barrier: ${cert.barrier}\n" +
                        "Bonus: ${cert.bonusLevel}\n" +
                        "Autocall: ${cert.autocallLevel}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Button(
                    onClick = { if (currentIndex > 0) currentIndex-- },
                    enabled = currentIndex > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("< Precedente")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { if (currentIndex < certificates.lastIndex) currentIndex++ },
                    enabled = currentIndex < certificates.lastIndex,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Successivo >")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Button(
                    onClick = { viewModel.deleteCertificate(cert) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Elimina")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        // Precarica i dati nei campi input per la modifica
                        newIsin = cert.isin
                        newUnderlying = cert.underlyingName
                        newStrike = cert.strike.toString()
                        newBarrier = cert.barrier.toString()
                        newBonus = cert.bonusLevel.toString()
                        newAutocall = cert.autocallLevel.toString()
                        // Rimuovi il vecchio per reinserirlo aggiornato
                        viewModel.deleteCertificate(cert)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Modifica")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Input ISIN
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
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Aggiungi / Modifica certificato")
        }
    }
}
