package com.example.certificatestracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MonthlySummaryScreen(viewModel: CertificatesViewModel) {
    val certificates by viewModel.certificates.collectAsState()
    val insertionDates by viewModel.insertionDates.collectAsState()

    val (monthNames, perIsinBonuses, totalBonuses, virtualBonuses) = remember(certificates, insertionDates) {
        MonthlyBonusCalculator.calculateDetailed(certificates, insertionDates)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("📊 Riepilogo Bonus Mensili", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("💠 = simulazione | 🟩 = Totale virtuale | ⚫ = Totale reale", color = Color.Gray, fontSize = 12.sp)

        // Tabella Intestazione
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("ISIN", fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
            monthNames.forEach {
                Text(it.take(3).uppercase(), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }

        Divider()

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(perIsinBonuses.entries.toList()) { (isin, values) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(isin.take(12), modifier = Modifier.weight(2f), fontSize = 14.sp)
                    values.forEach { value ->
                        val cert = certificates.find { it.isin == isin }
                        val isVirtual = cert?.purchasePrice?.let { ((it * 1000).toInt() % 10 == 1) } ?: false
                        Text(
                            "€${"%.2f".format(value)}",
                            color = if (isVirtual) Color(0xFF2196F3) else Color.Black,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                }
            }

            // Totali finali
            item {
                Divider(Modifier.padding(vertical = 8.dp))
                // Totale Reale
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("TOTALE REALE", fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                    totalBonuses.forEach {
                        Text("€${"%.2f".format(it)}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                }
                // Totale Virtuale (Somma reale + virtuale)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("TOTALE VIRTUALE", color = Color(0xFF008000), fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                    for (i in totalBonuses.indices) {
                        Text("€${"%.2f".format(totalBonuses[i] + virtualBonuses[i])}", color = Color(0xFF008000), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                }
            }
        }
    }
}