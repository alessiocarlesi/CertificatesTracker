// filename: app/src/main/java/com/example/certificatestracker/CertificatesScreen.kt
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
    // Osserviamo i flussi dal ViewModel
    val certificatesFlow by viewModel.certificates.collectAsState(initial = emptyList())
    val apiUsages by viewModel.apiUsages.collectAsState(initial = emptyList())
    val insertionDates by viewModel.insertionDates.collectAsState() // 🔹 Fondamentale per il calcolo bonus

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
            viewModel.refreshInsertionDates() // 🔹 Aggiorna le date quando torni indietro
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

                    val strikePerc = if (it.strike != 0.0) ((it.lastPrice - it.strike) / it.strike * 100) else 0.0
                    val barrierPerc = if (it.barrier != 0.0) ((it.lastPrice - it.barrier) / it.barrier * 100) else 0.0
                    val bonusPerc = if (it.bonusLevel != 0.0) ((it.lastPrice - it.bonusLevel) / it.bonusLevel * 100) else 0.0
                    val autocallPerc = if (it.autocallLevel != 0.0) ((it.lastPrice - it.autocallLevel) / it.autocallLevel * 100) else 0.0

                    Text(
                        text = buildString {
                            append("ISIN: ${it.isin} (${it.lastUpdate ?: "-"})\n")
                            append("Sottostante: ${it.underlyingName} - Prezzo: ${it.lastPrice} EUR\n")
                            append("Quantità: ${it.quantity}")
                            if (it.purchasePrice != null) {
                                append("  Costo: €${it.purchasePrice}")
                            }
                            append("\nStrike: ${it.strike} (${strikePerc.format(1)}%)\n")
                            append("Barrier: ${it.barrier} (${barrierPerc.format(1)}%)\n")
                            append("Bonus: ${it.bonusLevel} (${bonusPerc.format(1)}%) - ${(it.premio * it.quantity).format2(2)} € - il: ${it.nextbonus}\n")
                            append("Autocall: ${it.autocallLevel} (${autocallPerc.format(1)}%) - Valutazione: ${it.valautocall}")
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

                    // Navigazione tra certificati
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

                    // Azioni sul certificato corrente
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.fetchAndUpdatePrice(it.isin)
                                    recentlyUpdated[it.isin] = true
                                    delay(2000)
                                    recentlyUpdated[it.isin] = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFADD8E6), contentColor = Color.Black)
                        ) { Text("Aggiorna prezzo", fontSize = 20.sp) }

                        Button(
                            onClick = { selectedCert = it; showEditScreen = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFADD8E6), contentColor = Color.Black)
                        ) { Text("Modifica questo ISIN", fontSize = 20.sp) }

                        Button(
                            onClick = { selectedCert = it; showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFADD8E6), contentColor = Color.DarkGray)
                        ) { Text("Cancella questo ISIN", fontSize = 20.sp) }
                    }
                }
            } else {
                Text("Nessun certificato inserito", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Pulsanti Generali
            Button(
                onClick = { selectedCert = null; showEditScreen = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFADD8E6), contentColor = Color.Black)
            ) { Text("Aggiungi nuovo certificato", fontSize = 20.sp) }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { navController.navigate("summary") },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFADD8E6), contentColor = Color.Black)
            ) { Text("📊 Vedi Riepilogo Bonus", fontSize = 20.sp) }

            Spacer(modifier = Modifier.height(20.dp))

            // 🔹 CALCOLO BONUS FILTRATO PER DATA ACQUISTO
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
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
}// Aggiungi queste funzioni in fondo a CertificatesScreen.kt
// (fuori dalla classe/funzione principale) o in Helpers.kt

fun Double.format(digits: Int) = "%.${digits}f".format(this)
fun Double.format2(digits: Int) = "%.${digits}f".format(this)