package com.example.certificatestracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CertificatesScreen(viewModel: CertificatesViewModel, navController: NavController) {
    val certificatesFlow by viewModel.certificates.collectAsState(initial = emptyList())
    val insertionDates by viewModel.insertionDates.collectAsState()

    // 🔹 Sensori dal ViewModel per lo stato della sincronizzazione
    val lastOpFailed by viewModel.lastOperationFailed.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()

    var currentIndex by remember { mutableStateOf(0) }
    var showEditScreen by remember { mutableStateOf(false) }
    var selectedCert by remember { mutableStateOf<Certificate?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val recentlyUpdated = remember { mutableStateMapOf<String, Boolean>() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // 🟢 Logica Colore LED per il Centro Sincronizzazione
    val syncStatusColor = remember(lastSyncTime, lastOpFailed) {
        if (lastOpFailed) return@remember Color.Magenta
        if (lastSyncTime == null) return@remember Color.Red
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val lastDate = sdf.parse(lastSyncTime!!)
            val diffMinutes = (Date().time - lastDate.time) / (1000 * 60)
            when {
                diffMinutes < 15 -> Color(0xFF32CD32) // Verde
                diffMinutes < 240 -> Color(0xFFFFC107) // Giallo
                else -> Color.Red // Rosso
            }
        } catch (e: Exception) { Color.Gray }
    }

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
                // Reset dell'indice se superiore alla dimensione attuale (dopo eliminazione)
                if (currentIndex >= certificates.size) currentIndex = (certificates.size - 1).coerceAtLeast(0)

                val cert = certificates.getOrNull(currentIndex)
                cert?.let {
                    val textColor = if (recentlyUpdated[it.isin] == true) Color(0xFF008000) else Color.Black

                    // 🛠️ LOGICA WORST-OF DINAMICA v14
                    val sottostanti = listOf(
                        it.und1 to it.und1Strike,
                        it.und2 to it.und2Strike,
                        it.und3 to it.und3Strike,
                        it.und4 to it.und4Strike,
                        it.und5 to it.und5Strike,
                        it.und6 to it.und6Strike
                    ).filter { pair -> !pair.first.isNullOrBlank() && pair.second > 0.0 }

                    val worstOf = sottostanti.map { (ticker, strike) ->
                        val currentPrice = viewModel.getLastKnownPrice(ticker!!)
                        val perf = if (strike > 0) ((currentPrice - strike) / strike * 100) else 0.0
                        Triple(ticker, currentPrice, perf)
                    }.minByOrNull { it.third } ?: Triple(it.underlyingName, it.underlyingPrice, 0.0)

                    val worstTicker = worstOf.first
                    val worstPrice = worstOf.second
                    val worstPerf = worstOf.third

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

                    Spacer(modifier = Modifier.height(20.dp))

                    // 🔹 NAVIGAZIONE TRA SCHEDE
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

                    // 🔹 BOTTONI AZIONE: MODIFICA ED ELIMINA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { selectedCert = it; showEditScreen = true },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE6E6FA), contentColor = Color.Black)
                        ) { Text("📝 Modifica", fontSize = 16.sp) }

                        IconButton(
                            onClick = { selectedCert = it; showDeleteDialog = true },
                            modifier = Modifier.height(50.dp).width(50.dp),
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFFFEBEE))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = Color.Red)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 🔹 CALCOLO E VISUALIZZAZIONE BONUS
            val monthlyBonuses = remember(certificatesFlow, insertionDates) {
                MonthlyBonusCalculator.calculate(
                    certificates = certificatesFlow,
                    insertionDates = insertionDates,
                    viewModel = viewModel
                )
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

            Spacer(modifier = Modifier.height(20.dp))

            // 🔹 PULSANTI AGGIUNGI E RIEPILOGO
            Button(
                onClick = { selectedCert = null; showEditScreen = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFADD8E6), contentColor = Color.Black)
            ) { Text("➕ Aggiungi nuovo certificato", fontSize = 18.sp) }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { navController.navigate("summary") },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFADD8E6), contentColor = Color.Black)
            ) { Text("📊 Vedi Riepilogo Bonus", fontSize = 18.sp) }

            Spacer(modifier = Modifier.height(24.dp))

            // 🔹 CENTRO SINCRONIZZAZIONE v14
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (lastOpFailed) Color(0xFFFFF0F0) else Color(0xFFF5F5F5)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = syncStatusColor) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CENTRO SINCRONIZZAZIONE", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }

                    Text(
                        text = if (lastOpFailed) "⚠️ Errore rilevato" else "Ultimo check: ${lastSyncTime?.split(" ")?.getOrNull(1) ?: "Mai"}",
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.updateAllCertificates() },
                            modifier = Modifier.weight(1f).height(60.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF32CD32))
                        ) {
                            Text("🔄 PORTAFOGLIO\n(Borsa IT)", fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.updateAllUnderlyings() },
                            modifier = Modifier.weight(1f).height(60.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) {
                            Text("📊 MERCATI\n(Worst-Of)", fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { navController.navigate("apilogs") },
                        modifier = Modifier.fillMaxWidth().height(45.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0), contentColor = Color.Black)
                    ) { Text("📡 ISPEZIONA LOG CONNESSIONI", fontSize = 13.sp) }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 🔹 BACKUP
            val context = androidx.compose.ui.platform.LocalContext.current
            Button(
                onClick = { viewModel.avviaEsportazione(context) },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64), contentColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) { Text("💾 ESPORTA DATABASE (BACKUP)", fontSize = 16.sp, fontWeight = FontWeight.Bold) }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // 🔹 DIALOGO DI CONFERMA ELIMINAZIONE
    if (showDeleteDialog && selectedCert != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Conferma eliminazione") },
            text = { Text("Vuoi davvero cancellare l'ISIN ${selectedCert!!.isin}?") },
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

// Aggiungi queste funzioni in fondo al file, fuori dal blocco CertificatesScreen
fun Double.format(digits: Int) = "%.${digits}f".format(Locale.US, this)
fun Double.format2(digits: Int) = "%.${digits}f".format(Locale.US, this)