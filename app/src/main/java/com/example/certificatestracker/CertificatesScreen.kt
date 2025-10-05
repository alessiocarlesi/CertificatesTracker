// filename: CertificatesScreen.kt
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import java.util.*

@Composable
fun CertificatesScreen(viewModel: CertificatesViewModel) {
    val certificatesFlow by viewModel.certificates.collectAsState(initial = emptyList())
    val apiUsages by viewModel.apiUsages.collectAsState(initial = emptyList())

    var currentIndex by remember { mutableStateOf(0) }

    // 🔹 Per EditCertificateScreen
    var showEditScreen by remember { mutableStateOf(false) }
    var selectedCert by remember { mutableStateOf<Certificate?>(null) }

    val recentlyUpdated = remember { mutableStateMapOf<String, Boolean>() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    if (showEditScreen) {
        EditCertificateScreen(
            certificate = selectedCert,
            viewModel = viewModel
        ) {
            // callback al termine della modifica
            showEditScreen = false
            selectedCert = null
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            val certificates = certificatesFlow.map { viewModel.updateDatesIfNeeded(it) }

            // 🔹 Calcolo dei bonus mensili
            LaunchedEffect(certificates) {
                if (certificates.isNotEmpty()) {
                    MonthlyBonusCalculator.calculate(certificates)
                }
            }


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
                                "Quantità: ${it.quantity}\n" +
                                "Strike: ${it.strike} (${strikePerc.format(1)}%)\n" +
                                "Barrier: ${it.barrier} (${barrierPerc.format(1)}%)\n" +
                                "Bonus: ${it.bonusLevel} (${bonusPerc.format(1)}%) - ${(it.premio * it.quantity).format(2)} € - il: ${it.nextbonus}\n" +
                                "Autocall: ${it.autocallLevel} (${autocallPerc.format(1)}%) - Valutazione: ${it.valautocall}",
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )


                    Spacer(modifier = Modifier.height(8.dp))

                    // 🔹 API usage
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

                    Spacer(modifier = Modifier.height(30.dp))
// 🔹 Navigazione tra certificati
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { if (currentIndex > 0) currentIndex-- },
                            modifier = Modifier.weight(1f).height(60.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentIndex == 0) Color.LightGray else Color(0xFFADD8E6), // grigio chiaro se primo record
                                contentColor = Color.DarkGray // testo nero
                            )

                        ) {

                            Text("<", fontSize = 30.sp)

                        }

                        // Numero centrale (indice + totale)
                        Text(
                            text = "${currentIndex + 1} / ${certificates.size}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = { if (currentIndex < certificates.size - 1) currentIndex++ },
                            modifier = Modifier.weight(1f).height(60.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentIndex == certificates.size - 1) Color.LightGray else Color(0xFFADD8E6), // grigio se ultimo
                                contentColor = Color.DarkGray // testo nero
                            )

                        ) {
                            Text(">", fontSize = 40.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // 🔹 Bottoni Azioni
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.deleteCertificate(it.isin) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFADD8E6), // celeste chiaro
                                contentColor = Color.DarkGray           // testo nero
                            )
                        ) { Text("Cancella questo ISIN", fontSize = 20.sp) }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.fetchAndUpdatePrice(it.isin)
                                    recentlyUpdated[it.isin] = true
                                    delay(2000)
                                    recentlyUpdated[it.isin] = false
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFADD8E6), // celeste chiaro
                                contentColor = Color.Black           // testo nero
                            )
                        ) {
                            Text(
                                "Aggiorna prezzo",
                                fontSize = 20.sp
                            )
                        }
                    }


                    Spacer(modifier = Modifier.height(30.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                selectedCert = it
                                showEditScreen = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFADD8E6), // celeste chiaro
                                contentColor = Color.Black           // testo nero
                            )
                        ) {
                            Text(
                                "Modifica questo ISIN",
                                fontSize = 20.sp
                            )
                        }
                    }

                }
            } else {
                Text("Nessun certificato inserito")
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 🔹 Bottone Aggiungi Nuovo

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        selectedCert = null
                        showEditScreen = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFADD8E6), // celeste chiaro
                        contentColor = Color.Black           // testo nero
                    )
                ) {
                    Text(
                        "Aggiungi nuovo certificato",
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
// 🔹 Mostra bonus mensili
            val monthlyBonuses = remember(certificatesFlow) {
                MonthlyBonusCalculator.calculate(certificatesFlow)
            }

            Text(
                text =   "BONUS \n"+
                        "${monthlyBonuses.monthNames[0]}: ${monthlyBonuses.bonuses[0].format2(2)} €\n" +
                        "${monthlyBonuses.monthNames[1]}: ${monthlyBonuses.bonuses[1].format2(2)} €\n" +
                        "${monthlyBonuses.monthNames[2]}: ${monthlyBonuses.bonuses[2].format2(2)} €",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF444444),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )


        }
    }
}

// 🔹 Funzione di supporto per percentuali
fun Double.format(digits: Int) = "%.${digits}f".format(this)
fun Double.format2(digits: Int) = "%.${digits}f".format(this)
