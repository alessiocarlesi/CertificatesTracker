// filename: MonthlySummaryScreen.kt
package com.example.certificatestracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MonthlySummaryScreen(certificates: List<Certificate>) {
    val (monthNames, perIsinBonuses, totalBonuses) = remember {
        MonthlyBonusCalculator.calculateDetailed(certificates)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            "📊 Riepilogo Bonus Mensili",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // intestazione
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("ISIN", fontWeight = FontWeight.Bold)
            monthNames.forEach { Text(it.take(3), fontWeight = FontWeight.Bold) }
        }

        Divider(Modifier.padding(vertical = 4.dp))

        // righe ISIN
        LazyColumn {
            items(perIsinBonuses.entries.toList()) { (isin, values) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(isin.take(12))
                    values.forEach { value ->
                        val color = if (value < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                        Text("€${"%.2f".format(value)}", color = color)
                    }
                }
            }

            // totale finale
            item {
                Divider(Modifier.padding(vertical = 4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TOTALE", fontWeight = FontWeight.Bold)
                    totalBonuses.forEach {
                        Text("€${"%.2f".format(it)}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
