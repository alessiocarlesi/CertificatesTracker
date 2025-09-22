package com.example.certificatestracker

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CertificatesScreen(viewModel: CertificatesViewModel) {
    val certificates by viewModel.certificates.collectAsState()

    var newIsin by remember { mutableStateOf("") }
    var newUnderlying by remember { mutableStateOf("") }
    var newStrike by remember { mutableStateOf("") }
    var newBarrier by remember { mutableStateOf("") }
    var newBonus by remember { mutableStateOf("") }
    var newAutocall by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        certificates.forEach { cert ->
            Text(
                text = "ISIN: ${cert.isin} | Sottostante: ${cert.underlyingName} | Strike: ${cert.strike} | Barrier: ${cert.barrier} | Bonus: ${cert.bonusLevel} | Autocall: ${cert.autocallLevel} | LastPrice: ${cert.lastPrice}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input campi
        OutlinedTextField(value = newIsin, onValueChange = { newIsin = it }, label = { Text("ISIN") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = newUnderlying, onValueChange = { newUnderlying = it }, label = { Text("Sottostante") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = newStrike, onValueChange = { newStrike = it }, label = { Text("Strike") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = newBarrier, onValueChange = { newBarrier = it }, label = { Text("Barrier") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = newBonus, onValueChange = { newBonus = it }, label = { Text("Bonus") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = newAutocall, onValueChange = { newAutocall = it }, label = { Text("Autocall Level") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(8.dp))

        // Bottone Aggiungi
        Button(onClick = {
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
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Aggiungi certificato")
        }
    }
}
