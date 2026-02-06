package com.example.certificatestracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch

@Composable
fun EditCertificateScreen(
    certificate: Certificate?,
    viewModel: CertificatesViewModel,
    onDone: () -> Unit
) {
    // 🔹 Dati Identificativi e Quantità
    var isin by remember { mutableStateOf(certificate?.isin ?: "") }
    var quantity by remember { mutableStateOf(certificate?.quantity?.toString() ?: "") }
    var purchasePrice by remember { mutableStateOf(certificate?.purchasePrice?.toString() ?: "") }

    // 🔹 I 6 SOTTOSTANTI
    var und1 by remember { mutableStateOf(certificate?.und1 ?: certificate?.underlyingName ?: "") }
    var s1 by remember { mutableStateOf(certificate?.und1Strike?.toString() ?: certificate?.strike?.toString() ?: "") }

    var und2 by remember { mutableStateOf(certificate?.und2 ?: "") }
    var s2 by remember { mutableStateOf(certificate?.und2Strike?.toString() ?: "") }

    var und3 by remember { mutableStateOf(certificate?.und3 ?: "") }
    var s3 by remember { mutableStateOf(certificate?.und3Strike?.toString() ?: "") }

    var und4 by remember { mutableStateOf(certificate?.und4 ?: "") }
    var s4 by remember { mutableStateOf(certificate?.und4Strike?.toString() ?: "") }

    var und5 by remember { mutableStateOf(certificate?.und5 ?: "") }
    var s5 by remember { mutableStateOf(certificate?.und5Strike?.toString() ?: "") }

    var und6 by remember { mutableStateOf(certificate?.und6 ?: "") }
    var s6 by remember { mutableStateOf(certificate?.und6Strike?.toString() ?: "") }

    // 🔹 PERCENTUALI STRATEGICHE (Uniche per ISIN)
    var bPerc by remember { mutableStateOf(certificate?.barrierPerc?.toString() ?: "") }
    var boPerc by remember { mutableStateOf(certificate?.bonusPerc?.toString() ?: "") }
    var auPerc by remember { mutableStateOf(certificate?.autocallPerc?.toString() ?: "") }

    // 🔹 Dati Cedole e Autocall
    var premio by remember { mutableStateOf(certificate?.premio?.toString() ?: "") }
    var bonusMonths by remember { mutableStateOf(certificate?.bonusMonths?.toString() ?: "") }
    var autocallMonths by remember { mutableStateOf(certificate?.autocallMonths?.toString() ?: "") }

    var rawNextBonus by remember { mutableStateOf(normalizeToShortRawDateForEdit(certificate?.nextbonus ?: "")) }
    var rawValAutocall by remember { mutableStateOf(normalizeToShortRawDateForEdit(certificate?.valautocall ?: "")) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Configurazione Certificato v13", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF1976D2))

        // --- SEZIONE 1: IDENTIFICAZIONE ---
        OutlinedTextField(value = isin, onValueChange = { isin = it.uppercase() }, label = { Text("ISIN") }, modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = quantity, onValueChange = { quantity = it.filter { ch -> ch.isDigit() } }, label = { Text("Quantità") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = purchasePrice, onValueChange = { purchasePrice = it.filter { it.isDigit() || it == '.' } }, label = { Text("Prezzo Acquisto") }, modifier = Modifier.weight(1f))
        }

        Divider(Modifier.padding(vertical = 8.dp))

        // --- SEZIONE 2: SOTTOSTANTI (PANIERE) ---
        Text("Paniere Sottostanti (Ticker Yahoo | Strike)", fontWeight = FontWeight.Bold, fontSize = 14.sp)

        @Composable
        fun rowUnd(ticker: String, onTChange: (String) -> Unit, strike: String, onSChange: (String) -> Unit, label: String) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = ticker, onValueChange = { onTChange(it.uppercase()) }, label = { Text(label) }, modifier = Modifier.weight(1.5f))
                OutlinedTextField(value = strike, onValueChange = { onSChange(it) }, label = { Text("Strike") }, modifier = Modifier.weight(1f))
            }
        }

        rowUnd(und1, { und1 = it }, s1, { s1 = it }, "Sottostante 1")
        rowUnd(und2, { und2 = it }, s2, { s2 = it }, "Sottostante 2")
        rowUnd(und3, { und3 = it }, s3, { s3 = it }, "Sottostante 3")

        // Mostriamo gli altri 3 solo se i primi sono pieni (per pulizia UI)
        if (und3.isNotEmpty()) {
            rowUnd(und4, { und4 = it }, s4, { s4 = it }, "Sottostante 4")
            rowUnd(und5, { und5 = it }, s5, { s5 = it }, "Sottostante 5")
            rowUnd(und6, { und6 = it }, s6, { s6 = it }, "Sottostante 6")
        }

        Divider(Modifier.padding(vertical = 8.dp))

        // --- SEZIONE 3: SOGLIE PERCENTUALI ---
        Text("Soglie di Riferimento (%)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = bPerc, onValueChange = { bPerc = it }, label = { Text("% Barriera") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = boPerc, onValueChange = { boPerc = it }, label = { Text("% Bonus") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = auPerc, onValueChange = { auPerc = it }, label = { Text("% Autocall") }, modifier = Modifier.weight(1f))
        }

        Divider(Modifier.padding(vertical = 8.dp))

        // --- SEZIONE 4: CEDOLE E DATE ---
        OutlinedTextField(value = premio, onValueChange = { premio = it }, label = { Text("Importo Cedola (€)") }, modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = bonusMonths, onValueChange = { bonusMonths = it }, label = { Text("Freq. Cedola (Mesi)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = rawNextBonus, onValueChange = { rawNextBonus = it.filter { it.isDigit() } }, label = { Text("Data Cedola (DDMMYY)") }, modifier = Modifier.weight(1.2f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = autocallMonths, onValueChange = { autocallMonths = it }, label = { Text("Freq. Autocall (Mesi)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = rawValAutocall, onValueChange = { rawValAutocall = it.filter { it.isDigit() } }, label = { Text("Data Autocall (DDMMYY)") }, modifier = Modifier.weight(1.2f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- BOTTONI FINALI ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        val newCertificate = Certificate(
                            isin = isin,
                            underlyingName = und1, // Compatibilità v12
                            strike = s1.toDoubleOrNull() ?: 0.0, // Compatibilità v12
                            barrier = 0.0, // Ora usiamo barrierPerc
                            bonusLevel = 0.0, // Ora usiamo bonusPerc
                            bonusMonths = bonusMonths.toIntOrNull() ?: 0,
                            autocallLevel = 0.0, // Ora usiamo autocallPerc
                            autocallMonths = autocallMonths.toIntOrNull() ?: 0,
                            premio = premio.toDoubleOrNull() ?: 0.0,
                            nextbonus = rawToDisplayDate(rawNextBonus),
                            valautocall = rawToDisplayDate(rawValAutocall),
                            lastPrice = certificate?.lastPrice ?: 0.0,
                            lastUpdate = certificate?.lastUpdate,
                            quantity = quantity.toIntOrNull() ?: 0,
                            purchasePrice = purchasePrice.toDoubleOrNull(),
                            // Campi v13
                            und1 = und1, und1Strike = s1.toDoubleOrNull() ?: 0.0,
                            und2 = und2, und2Strike = s2.toDoubleOrNull() ?: 0.0,
                            und3 = und3, und3Strike = s3.toDoubleOrNull() ?: 0.0,
                            und4 = und4, und4Strike = s4.toDoubleOrNull() ?: 0.0,
                            und5 = und5, und5Strike = s5.toDoubleOrNull() ?: 0.0,
                            und6 = und6, und6Strike = s6.toDoubleOrNull() ?: 0.0,
                            barrierPerc = bPerc.toDoubleOrNull() ?: 0.0,
                            bonusPerc = boPerc.toDoubleOrNull() ?: 0.0,
                            autocallPerc = auPerc.toDoubleOrNull() ?: 0.0
                        )

                        if (certificate == null) viewModel.addCertificate(newCertificate)
                        else {
                            viewModel.deleteCertificate(certificate.isin)
                            viewModel.addCertificate(newCertificate)
                        }
                        onDone()
                    }
                },
                modifier = Modifier.weight(1f).height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) { Text(if (certificate == null) "Aggiungi" else "Aggiorna", fontWeight = FontWeight.Bold) }

            Button(onClick = { onDone() }, modifier = Modifier.weight(1f).height(55.dp)) { Text("Annulla") }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}