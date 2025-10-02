package com.example.certificatestracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CertificatesScreen(viewModel: CertificatesViewModel) {
    val certificates by viewModel.certificates.collectAsState(initial = emptyList())
    val apiUsages by viewModel.apiUsages.collectAsState(initial = emptyList())

    var currentIndex by remember { mutableStateOf(0) }
    var newIsin by remember { mutableStateOf("") }
    var newUnderlying by remember { mutableStateOf("") }
    var newStrike by remember { mutableStateOf("") }
    var newBarrier by remember { mutableStateOf("") }
    var newBonus by remember { mutableStateOf("") }
    var newAutocall by remember { mutableStateOf("") }
    var newPremio by remember { mutableStateOf("") }
    var newNextbonus by remember { mutableStateOf("") }
    var newValautocall by remember { mutableStateOf("") }

    val recentlyUpdated = remember { mutableStateMapOf<String, Boolean>() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Helper conversione sicura String -> Double
    fun parseDoubleSafe(value: String): Double = value.replace(',', '.').toDoubleOrNull() ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {

        if (certificates.isNotEmpty()) {
            val cert = certificates.getOrNull(currentIndex)
            cert?.let {
                val textColor = if (recentlyUpdated[it.isin] == true) Color(0xFF008000) else Color.Black

                val strikePerc = if (it.strike != 0.0) ((it.lastPrice - it.strike) / it.strike * 100) else 0.0
                val barrierPerc = if (it.barrier != 0.0) ((it.lastPrice - it.barrier) / it.barrier * 100) else 0.0
                val bonusPerc = if (it.bonusLevel != 0.0) ((it.lastPrice - it.bonusLevel) / it.bonusLevel * 100) else 0.0
                val autocallPerc = if (it.autocallLevel != 0.0) ((it.lastPrice - it.autocallLevel) / it.autocallLevel * 100) else 0.0

                Text(
                    text = "ISIN: ${it.isin} (${it.lastUpdate ?: "-"})\n" +
                            "Sottostante: ${it.underlyingName} - Prezzo: ${it.lastPrice} EUR\n" +
                            "Strike: ${it.strike} (${strikePerc.format(1)}%)\n" +
                            "Barrier: ${it.barrier} (${barrierPerc.format(1)}%)\n" +
                            "Bonus: ${it.bonusLevel} (${bonusPerc.format(1)}%) - " +
                            "E: ${it.premio} - " +
                            "il: ${formatDate( it.nextbonus )}\n" +
                            "Autocall: ${it.autocallLevel} (${autocallPerc.format(1)}%) - Valutazione: ${formatDate( it.valautocall )}",
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 🔹 Percentuali utilizzo API sopra le frecce
                apiUsages.forEach { usage ->
                    val provider = ApiProvider.values().firstOrNull { it.displayName == usage.providerName } ?: return@forEach
                    val dailyPercent = usage.dailyCount * 100.0 / provider.dailyLimit
                    val monthlyPercent = usage.monthlyCount * 100.0 / provider.monthlyLimit

                    Text(
                        text = "${provider.displayName}: Giornaliero ${dailyPercent.format(1)}%, Mensile ${monthlyPercent.format(1)}%",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // FRECCE < >
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        modifier = Modifier.weight(1f).height(30.dp),
                        contentPadding = PaddingValues(vertical = 0.dp)
                    ) { Text("<", fontSize = 12.sp) }

                    Button(
                        onClick = { if (currentIndex < certificates.size - 1) currentIndex++ },
                        modifier = Modifier.weight(1f).height(30.dp),
                        contentPadding = PaddingValues(vertical = 0.dp)
                    ) { Text(">", fontSize = 12.sp) }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // BOTTONI Cancella / Aggiorna
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.deleteCertificate(it.isin) },
                        modifier = Modifier.weight(1f).height(30.dp),
                        contentPadding = PaddingValues(vertical = 0.dp)
                    ) { Text("Cancella", fontSize = 12.sp) }

                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.fetchAndUpdatePrice(it.isin)
                                recentlyUpdated[it.isin] = true
                                delay(2000)
                                recentlyUpdated[it.isin] = false
                            }
                        },
                        modifier = Modifier.weight(1f).height(30.dp),
                        contentPadding = PaddingValues(vertical = 0.dp)
                    ) { Text("Aggiorna", fontSize = 12.sp) }
                }
            }
        } else {
            Text("Nessun certificato inserito")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Helper per i campi di inserimento
        @Composable
        fun field(value: String, onChange: (String) -> Unit, label: String) {
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 16.sp)
            )
        }

        field(newIsin, { newIsin = it }, "ISIN")
        field(newUnderlying, { newUnderlying = it }, "Sottostante")
        field(newStrike, { newStrike = it }, "Strike")
        field(newBarrier, { newBarrier = it }, "Barrier")
        field(newBonus, { newBonus = it }, "Bonus")
        field(newAutocall, { newAutocall = it }, "Autocall Level")
        field(newPremio, { newPremio = it }, "Premio")
        field(newNextbonus, { newNextbonus = it }, "Next Bonus")
        field(newValautocall, { newValautocall = it }, "Valutazione Autocall")

        Spacer(modifier = Modifier.height(8.dp))

        // BOTTONI Aggiungi / Aggiorna tutti
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (newIsin.isNotEmpty()) {
                        viewModel.addCertificate(
                            isin = newIsin,
                            underlyingName = newUnderlying,
                            strike = parseDoubleSafe(newStrike),
                            barrier = parseDoubleSafe(newBarrier),
                            bonusLevel = parseDoubleSafe(newBonus),
                            autocallLevel = parseDoubleSafe(newAutocall),
                            premio = parseDoubleSafe(newPremio),
                            nextbonus = newNextbonus,
                            valautocall = newValautocall
                        )

                        newIsin = ""
                        newUnderlying = ""
                        newStrike = ""
                        newBarrier = ""
                        newBonus = ""
                        newAutocall = ""
                        newPremio = ""
                        newNextbonus = ""
                        newValautocall = ""

                        currentIndex = certificates.size
                    }
                },
                modifier = Modifier.weight(1f).height(30.dp),
                contentPadding = PaddingValues(vertical = 0.dp)
            ) { Text("Aggiungi", fontSize = 12.sp) }

            Button(
                onClick = {
                    certificates.forEach { cert ->
                        scope.launch {
                            viewModel.fetchAndUpdatePrice(cert.isin)
                            recentlyUpdated[cert.isin] = true
                            delay(2000)
                            recentlyUpdated[cert.isin] = false
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(30.dp),
                contentPadding = PaddingValues(vertical = 0.dp)
            ) { Text("Aggiorna tutti", fontSize = 12.sp) }

        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// Funzione di estensione per formattare percentuali
fun Double.format(digits: Int) = "%.${digits}f".format(this)
