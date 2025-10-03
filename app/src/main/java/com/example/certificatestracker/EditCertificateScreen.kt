// filename: EditCertificateScreen.kt
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
    // Campi di testo
    var isin by remember { mutableStateOf(certificate?.isin ?: "") }
    var underlyingName by remember { mutableStateOf(certificate?.underlyingName ?: "") }
    var strike by remember { mutableStateOf(certificate?.strike?.toString() ?: "") }
    var barrier by remember { mutableStateOf(certificate?.barrier?.toString() ?: "") }
    var bonusLevel by remember { mutableStateOf(certificate?.bonusLevel?.toString() ?: "") }
    var bonusMonths by remember { mutableStateOf(certificate?.bonusMonths?.toString() ?: "") }
    var autocallLevel by remember { mutableStateOf(certificate?.autocallLevel?.toString() ?: "") }
    var autocallMonths by remember { mutableStateOf(certificate?.autocallMonths?.toString() ?: "") }
    var premio by remember { mutableStateOf(certificate?.premio?.toString() ?: "") }
    var nextbonus by remember { mutableStateOf(certificate?.nextbonus ?: "") }
    var valautocall by remember { mutableStateOf(certificate?.valautocall ?: "") }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

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

        field(isin, { isin = it.uppercase() }, "ISIN")
        field(underlyingName, { underlyingName = it.uppercase() }, "Sottostante")
        field(strike, { strike = it }, "Strike")
        field(barrier, { barrier = it }, "Barrier")
        field(bonusLevel, { bonusLevel = it }, "Bonus Level")
        field(bonusMonths, { bonusMonths = it }, "Bonus Months")
        field(autocallLevel, { autocallLevel = it }, "Autocall Level")
        field(autocallMonths, { autocallMonths = it }, "Autocall Months")
        field(premio, { premio = it }, "Premio")
        field(nextbonus, { nextbonus = it }, "Next Bonus (DD/MM/YYYY)")
        field(valautocall, { valautocall = it }, "Valutazione Autocall (DD/MM/YYYY)")

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    scope.launch {
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
                            nextbonus = nextbonus,
                            valautocall = valautocall,
                            lastPrice = certificate?.lastPrice ?: 0.0,
                            lastUpdate = certificate?.lastUpdate
                        )

                        if (certificate == null) {
                            viewModel.addCertificate(
                                isin = newCertificate.isin,
                                underlyingName = newCertificate.underlyingName,
                                strike = newCertificate.strike,
                                bonusLevel = newCertificate.bonusLevel,
                                bonusMonths = newCertificate.bonusMonths,
                                autocallLevel = newCertificate.autocallLevel,
                                autocallMonths = newCertificate.autocallMonths,
                                barrier = newCertificate.barrier,
                                premio = newCertificate.premio,
                                nextbonus = newCertificate.nextbonus,
                                valautocall = newCertificate.valautocall
                            )
                        } else {
                            viewModel.deleteCertificate(certificate.isin)
                            viewModel.addCertificate(
                                isin = newCertificate.isin,
                                underlyingName = newCertificate.underlyingName,
                                strike = newCertificate.strike,
                                bonusLevel = newCertificate.bonusLevel,
                                bonusMonths = newCertificate.bonusMonths,
                                autocallLevel = newCertificate.autocallLevel,
                                autocallMonths = newCertificate.autocallMonths,
                                barrier = newCertificate.barrier,
                                premio = newCertificate.premio,
                                nextbonus = newCertificate.nextbonus,
                                valautocall = newCertificate.valautocall
                            )
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
