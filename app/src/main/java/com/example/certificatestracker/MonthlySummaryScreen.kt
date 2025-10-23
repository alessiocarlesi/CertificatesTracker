// filename: MonthlySummaryScreen.kt
package com.example.certificatestracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MonthlySummaryScreen(certificates: List<Certificate>) {
    val (monthNames, perIsinBonuses, totalBonuses) = remember {
        MonthlyBonusCalculator.calculateDetailed(certificates)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "📊 Riepilogo Bonus Mensili",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 🔹 Intestazione
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                "ISIN",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.weight(2f)
            )
            monthNames.forEach {
                Text(
                    it.take(3).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }

        Divider(Modifier.padding(vertical = 4.dp))

        // 🔹 Righe per ogni ISIN
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(perIsinBonuses.entries.toList()) { (isin, values) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = isin.take(12),
                        modifier = Modifier.weight(2f),
                        fontSize = 14.sp
                    )
                    values.forEach { value ->
                        val color = if (value < 0)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onBackground

                        Text(
                            text = "€${"%.2f".format(value)}",
                            color = color,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                }
            }

            // 🔹 Riga Totali
            item {
                Divider(Modifier.padding(vertical = 6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        "TOTALE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(2f)
                    )
                    totalBonuses.forEach {
                        Text(
                            text = "€${"%.2f".format(it)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                }
            }
        }
    }
}
