package com.example.certificatestracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun ApiLogsScreen(viewModel: CertificatesViewModel) {
    val logs by remember { mutableStateOf(viewModel.apiLogs) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "📡 Log API Providers",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (logs.isEmpty()) {
            Text("Nessun log ancora registrato.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(logs.reversed()) { log ->
                    Text(log, style = MaterialTheme.typography.bodyMedium)
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}
