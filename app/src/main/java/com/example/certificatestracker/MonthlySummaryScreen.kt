package com.example.certificatestracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MonthlySummaryScreen(viewModel: CertificatesViewModel) {
    val certificates by viewModel.certificates.collectAsState()
    val insertionDates by viewModel.insertionDates.collectAsState()

    // Subroutine Calcolo Bonus (Dettagliato)
    val (monthNames, perIsinBonuses, totalBonuses, virtualBonuses) = remember(certificates, insertionDates) {
        MonthlyBonusCalculator.calculateDetailed(certificates, insertionDates)
    }

    // Subroutine Calcolo Capitale (ALU per il capitale investito)
    val stats = remember(certificates) {
        PortfolioCalculators.compute(certificates)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("📊 Riepilogo Portafoglio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("💠 = simulazione | 🟩 = Totale virtuale | ⚫ = Totale reale", color = Color.Gray, fontSize = 12.sp)

        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // --- SEZIONE TOTALI CAPITALE ---
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Investito: €${"%.2f".format(stats.capitaleInvestito)}", fontSize = 14.sp)
                            Text("Attuale: €${"%.2f".format(stats.valoreAttuale)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val colorGL = if (stats.gainLoss >= 0) Color(0xFF008000) else Color.Red
                        Text(
                            "Gain/Loss: €${"%.2f".format(stats.gainLoss)} (${"%.2f".format(stats.gainLossPerc)}%)",
                            color = colorGL,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
            }

            // --- INTESTAZIONE TABELLA ---
            item {
                Text("Dettaglio Strategico e Bonus", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("AUTOCALL / ISIN", fontWeight = FontWeight.Bold, modifier = Modifier.weight(3.2f), fontSize = 12.sp)
                    monthNames.forEach {
                        Text(it.take(3).uppercase(), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontSize = 12.sp)
                    }
                }
                Divider()
            }

            // --- RIGHE ISIN CON DISTANZA AUTOCALL (Logica 3 Colori) ---
            items(perIsinBonuses.entries.toList()) { (isin, values) ->
                val cert = certificates.find { it.isin == isin }
                val distAutocall = cert?.let { PortfolioCalculators.calcolaDistanzaAutocall(it) } ?: 0.0

                // Logica colore strategica
                val coloreAutocall = when {
                    distAutocall > 0 -> Color.Red             // Pericolo rimborso
                    distAutocall < -20.0 -> Color(0xFFFF9800) // Allarme barriera (Arancione)
                    else -> Color(0xFF4CAF50)                 // Zona ideale (Verde)
                }

                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${if (distAutocall > 0) "+" else ""}${"%.1f".format(distAutocall)}%",
                        modifier = Modifier.weight(1.2f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = coloreAutocall,
                        textAlign = TextAlign.Start
                    )

                    Text(isin.take(12), modifier = Modifier.weight(2f), fontSize = 13.sp)

                    values.forEach { value ->
                        val isVirtual = cert?.purchasePrice?.let { ((it * 1000).toInt() % 10 == 1) } ?: false
                        Text(
                            "€${"%.2f".format(value)}",
                            color = if (isVirtual) Color(0xFF2196F3) else Color.Black,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // --- TOTALI FINALI ---
            item {
                Divider(Modifier.padding(vertical = 8.dp))

                // 1. Totale Reale
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("TOTALE REALE", fontWeight = FontWeight.Bold, modifier = Modifier.weight(3.2f), fontSize = 12.sp)
                    totalBonuses.forEach {
                        Text("€${"%.2f".format(it)}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontSize = 12.sp)
                    }
                }

                // 2. RENDIMENTI (Solo Mese Corrente per precisione)
                if (totalBonuses.isNotEmpty()) {
                    val primoBonus = totalBonuses[0]
                    val yieldM = PortfolioCalculators.calcolaYieldMensile(primoBonus, stats.capitaleInvestito)
                    val yieldA = PortfolioCalculators.calcolaYieldAnnuo(yieldM)

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                        Text("REND. ATTUALE (Mese Corr.)", color = Color(0xFF1976D2), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(3.2f), fontSize = 11.sp)
                        Text("${"%.2f".format(yieldM)}% m.", color = Color(0xFF1976D2), modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontSize = 11.sp)
                        repeat(totalBonuses.size - 1) { Spacer(modifier = Modifier.weight(1f)) }
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("REND. ANNUO (Proiezione)", color = Color(0xFF1976D2), fontWeight = FontWeight.Bold, modifier = Modifier.weight(3.2f), fontSize = 11.sp)
                        Text("${"%.2f".format(yieldA)}% a.", color = Color(0xFF1976D2), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontSize = 11.sp)
                        repeat(totalBonuses.size - 1) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }

                // 3. Totale Virtuale
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text("TOTALE VIRTUALE", color = Color(0xFF008000), fontWeight = FontWeight.Bold, modifier = Modifier.weight(3.2f), fontSize = 12.sp)
                    for (i in totalBonuses.indices) {
                        Text("€${"%.2f".format(totalBonuses[i] + virtualBonuses[i])}", color = Color(0xFF008000), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}