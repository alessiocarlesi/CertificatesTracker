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
import kotlinx.coroutines.launch

@Composable
fun EditCertificateScreen(
    certificate: Certificate?,
    viewModel: CertificatesViewModel,
    onDone: () -> Unit
) {
    // 🔹 Campi principali
    var isin by remember { mutableStateOf(certificate?.isin ?: "") }
    var underlyingName by remember { mutableStateOf(certificate?.underlyingName ?: "") }
    var strike by remember { mutableStateOf(certificate?.strike?.toString() ?: "") }
    var barrier by remember { mutableStateOf(certificate?.barrier?.toString() ?: "") }
    var bonusLevel by remember { mutableStateOf(certificate?.bonusLevel?.toString() ?: "") }
    var bonusMonths by remember { mutableStateOf(certificate?.bonusMonths?.toString() ?: "") }
    var autocallLevel by remember { mutableStateOf(certificate?.autocallLevel?.toString() ?: "") }
    var autocallMonths by remember { mutableStateOf(certificate?.autocallMonths?.toString() ?: "") }
    var premio by remember { mutableStateOf(certificate?.premio?.toString() ?: "") }

    // 🔹 Campi grezzi per le date
    var rawNextBonus by remember { mutableStateOf(certificate?.nextbonus?.replace("/", "") ?: "") }
    var rawValAutocall by remember { mutableStateOf(certificate?.valautocall?.replace("/", "") ?: "") }

    // 🔹 Campo quantità
    var quantity by remember { mutableStateOf(certificate?.quantity?.toString() ?: "") }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // 🔹 Funzione di supporto per i campi
        @Composable
        fun field(value: String, onChange: (String) -> Unit, label: String) {
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)
            )
        }

        // 🔹 Campi principali
        field(isin, { isin = it.uppercase() }, "ISIN")
        field(underlyingName, { underlyingName = it.uppercase() }, "Sottostante")
        field(strike, { strike = it }, "Strike")
        field(barrier, { barrier = it }, "Barrier")
        field(bonusLevel, { bonusLevel = it }, "Soglia Bonus")
        field(bonusMonths, { bonusMonths = it }, "Frequenza cedole in mesi")
        field(autocallLevel, { autocallLevel = it }, "Soglia Autocall")
        field(autocallMonths, { autocallMonths = it }, "Frequenza valutazione Autocall in mesi")
        field(premio, { premio = it }, "Bonus")

        // 🔹 Campi grezzi per le date
        field(rawNextBonus, { input -> rawNextBonus = input.filter { it.isDigit() } }, "Next Bonus (DDMMYY)")
        field(rawValAutocall, { input -> rawValAutocall = input.filter { it.isDigit() } }, "Valutazione Autocall (DDMMYY)")

        // 🔹 Campo quantità
        field(quantity, { quantity = it.filter { ch -> ch.isDigit() } }, "Quantità")

        Spacer(modifier = Modifier.height(16.dp))

        // 🔹 Bottoni Aggiungi/Aggiorna e Annulla
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        // 🔹 Conversione finale delle date
                        val nextBonusFinal = if (rawNextBonus.length == 6) formatDate(rawNextBonus) else rawNextBonus
                        val valAutocallFinal = if (rawValAutocall.length == 6) formatDate(rawValAutocall) else rawValAutocall
                        val quantityInt = quantity.toIntOrNull() ?: 0

                        val newCertificate = Certificate(
                            isin = isin,
                            underlyingName = underlyingName,
                            strike = strike.toDoubleOrNull() ?: 0.0,
                            barrier = barrier.toDoubleOrNull() ?: 0.0,
                            bonusLevel = bonusLevel.toDoubleOrNull() ?: 0.0,
                            bonusMonths = bonusMonths.toIntOrNull() ?: 0,
                            autocallLevel = autocallLevel.toDoubleOrNull() ?: 0.0,
                            autocallMonths = autocallMonths.toIntOrNull() ?: 0,
                            premio = premio.toDoubleOrNull() ?: 0.0,
                            nextbonus = nextBonusFinal,
                            valautocall = valAutocallFinal,
                            lastPrice = certificate?.lastPrice ?: 0.0,
                            lastUpdate = certificate?.lastUpdate,
                            quantity = quantityInt
                        )

                        if (certificate == null) {
                            viewModel.addCertificate(newCertificate)
                        } else {
                            viewModel.deleteCertificate(certificate.isin)
                            viewModel.addCertificate(newCertificate)
                        }

                        onDone()
                    }
                },
                modifier = Modifier.weight(1f).height(50.dp)
            ) { Text(if (certificate == null) "Aggiungi" else "Aggiorna") }

            Button(
                onClick = { onDone() },
                modifier = Modifier.weight(1f).height(50.dp)
            ) { Text("Annulla") }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
