package com.example.certificatestracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CertificatesScreen(viewModel: CertificatesViewModel, navController: NavController) {
    val certificatesFlow by viewModel.certificates.collectAsState(initial = emptyList())
    val apiUsages by viewModel.apiUsages.collectAsState(initial = emptyList())
    val insertionDates by viewModel.insertionDates.collectAsState()

    var currentIndex by remember { mutableStateOf(0) }
    var showEditScreen by remember { mutableStateOf(false) }
    var selectedCert by remember { mutableStateOf<Certificate?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val recentlyUpdated = remember { mutableStateMapOf<String, Boolean>() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    if (showEditScreen) {
        EditCertificateScreen(
            certificate = selectedCert,
            viewModel = viewModel
        ) {
            showEditScreen = false
            selectedCert = null
            viewModel.refreshInsertionDates()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            val certificates = certificatesFlow.map { viewModel.updateDatesIfNeeded(it) }

            if (certificates.isNotEmpty()) {
                val cert = certificates.getOrNull(currentIndex)
                cert?.let {
                    val textColor = if (recentlyUpdated[it.isin] == true) Color(0xFF008000) else Color.Black

                    // 🛠️ LOGICA WORST-OF DINAMICA (v13)
                    val sottostanti = listOf(
                        it.und1 to it.und1Strike,
                        it.und2 to it.und2Strike,
                        it.und3 to it.und3Strike,
                        it.und4 to it.und4Strike,
                        it.und5 to it.und5Strike,
                        it.und6 to it.und6Strike
                    ).filter { pair -> !pair.first.isNullOrBlank() && pair.second > 0.0 }

                    // Calcoliamo la performance per ogni titolo nel paniere
                    val worstOf = sottostanti.map { (ticker, strike) ->
                        // Recuperiamo l'ultimo prezzo salvato per quel ticker specifico
                        val currentPrice = viewModel.getLastKnownPrice(ticker!!)
                        val perf = if (strike > 0) ((currentPrice - strike) / strike * 100) else 0.0
                        Triple(ticker, currentPrice, perf)
                    }.minByOrNull { it.third } ?: Triple(it.underlyingName, it.underlyingPrice, 0.0)

                    val worstTicker = worstOf.first
                    val worstPrice = worstOf.second
                    val worstPerf = worstOf.third

                    // 🛠️ CALCOLO DISTANZE DALLE SOGLIE PERCENTUALI
                    // (Performance Worst-Of) - (Soglia desiderata - 100)
                    val distBarrier = worstPerf - (it.barrierPerc - 100)
                    val distBonus = worstPerf - (it.bonusPerc - 100)
                    val distAutocall = worstPerf - (it.autocallPerc - 100)

                    Text(
                        text = buildString {
                            append("ISIN: ${it.isin} (${it.lastUpdate ?: "-"})\n")
                            append("Valore Mercato: ${it.lastPrice} EUR\n\n")

                            append("🏆 WORST-OF: $worstTicker\n")
                            append("Prezzo: $worstPrice EUR (${worstPerf.format(1)}% dallo Strike)\n")
                            append("Quantità: ${it.quantity}")
                            if (it.purchasePrice != null) append("  Costo: €${it.purchasePrice}")

                            append("\n\nDISTANZA DALLE SOGLIE (%):\n")
                            append("Barriera (${it.barrierPerc}%): ${distBarrier.format(1)}%\n")
                            append("Bonus (${it.bonusPerc}%): ${distBonus.format(1)}%\n")
                            append("Autocall (${it.autocallPerc}%): ${distAutocall.format(1)}%\n\n")

                            append("Cedola: ${(it.premio * it.quantity).format2(2)} € - il: ${it.nextbonus}\n")
                            append("Valutazione Autocall: ${it.valautocall}")
                        },
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { if (currentIndex > 0) currentIndex-- },
                            modifier = Modifier.weight(1f).height(60.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFADD8E6), contentColor = Color.DarkGray)
                        ) { Text("<", fontSize = 30.sp) }

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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFADD8E6), contentColor = Color.DarkGray)
                        ) { Text(">", fontSize = 30.sp) }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.fetchAndUpdatePrice(it.isin, useBorsaItaliana = true)
                                    recentlyUpdated[it.isin] = true
                                    delay(2000)
                                    recentlyUpdated[it.isin] = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF90EE90), contentColor = Color.Black)
                        ) { Text("Aggiorna Quotazione Borsa IT", fontSize = 18.sp) }

                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.fetchAndUpdatePrice(it.isin, useBorsaItaliana = false)
                                    recentlyUpdated[it.isin] = true
                                    delay(2000)
                                    recentlyUpdated[it.isin] = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFADD8E6), contentColor = Color.Black)
                        ) { Text("Aggiorna Sottostante (API)", fontSize = 18.sp) }

                        Button(
                            onClick = { selectedCert = it; showEditScreen = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE6E6FA), contentColor = Color.Black)
                        ) { Text("Modifica questo ISIN", fontSize = 18.sp) }
                    }
                }
            } else {
                Text("Nessun certificato inserito", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = { viewModel.updateAllCertificates() },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF32CD32), contentColor = Color.White)
            ) {
                Text("🔄 AGGIORNA TUTTO (Borsa IT)", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { selectedCert = null; showEditScreen = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFADD8E6), contentColor = Color.Black)
            ) { Text("➕ Aggiungi nuovo certificato", fontSize = 20.sp) }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { navController.navigate("summary") },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFADD8E6), contentColor = Color.Black)
            ) { Text("📊 Vedi Riepilogo Bonus", fontSize = 20.sp) }

            Spacer(modifier = Modifier.height(20.dp))

            val monthlyBonuses = remember(certificatesFlow, insertionDates) {
                MonthlyBonusCalculator.calculate(certificatesFlow, insertionDates)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8FF))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BONUS PROSSIMI MESI", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    for (i in monthlyBonuses.monthNames.indices) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(monthlyBonuses.monthNames[i], fontWeight = FontWeight.Bold)
                            Text("${monthlyBonuses.bonuses[i].format2(2)} €", fontWeight = FontWeight.Bold, color = Color(0xFF005A9C))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { navController.navigate("apilogs") },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFADD8E6), contentColor = Color.Black)
            ) { Text("📡 Log API", fontSize = 20.sp) }

            Spacer(modifier = Modifier.height(30.dp))

            val context = androidx.compose.ui.platform.LocalContext.current

            Button(
                onClick = { viewModel.avviaEsportazione(context) },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64), contentColor = Color.White),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
            ) {
                Text("💾 ESPORTA DATABASE (BACKUP)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "Il file backup_certificates_v12.db sarà salvato nei Download",
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { viewModel.updateAllUnderlyings() },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2), contentColor = Color.White),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
            ) {
                Text("🔄 ", fontSize = 18.sp)
                Text("AGGIORNA SOTTOSTANTI (Yahoo)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showDeleteDialog && selectedCert != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Conferma eliminazione") },
            text = { Text("Vuoi davvero cancellare l’ISIN ${selectedCert!!.isin}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCertificate(selectedCert!!.isin)
                    showDeleteDialog = false
                    selectedCert = null
                }) { Text("Sì, elimina", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false; selectedCert = null }) { Text("Annulla") }
            }
        )
    }
}

fun Double.format(digits: Int) = "%.${digits}f".format(this)
fun Double.format2(digits: Int) = "%.${digits}f".format(this)