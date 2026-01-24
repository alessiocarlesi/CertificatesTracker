package com.example.certificatestracker

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.certificatestracker.api.ApiProvider
import com.example.certificatestracker.ApiUsage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// funzione helper per determinare il provider corretto
fun getProviderForUnderlying(underlying: String): ApiProvider {
    return when {
        underlying.endsWith(".MI", ignoreCase = true) -> ApiProvider.MARKETSTACK
        '.' !in underlying -> ApiProvider.TWELVEDATA   // nessun suffisso -> mercato USA
        else -> ApiProvider.ALPHAVANTAGE
    }
}

@Composable
fun CertificatesScreen(
    viewModel: CertificatesViewModel,
    usageRepository: ApiUsageRepository
) {
    val certificates by viewModel.certificates.collectAsState(initial = emptyList())

    var currentIndex by remember { mutableIntStateOf(0) }
    var newIsin by remember { mutableStateOf("") }
    var newUnderlying by remember { mutableStateOf("") }
    var newStrike by remember { mutableStateOf("") }
    var newBarrier by remember { mutableStateOf("") }
    var newBonus by remember { mutableStateOf("") }
    var newAutocall by remember { mutableStateOf("") }

    val recentlyUpdated: SnapshotStateMap<String, Boolean> = remember { mutableStateMapOf() }
    val scope = rememberCoroutineScope()
    val fieldHeight = 56.dp

    // usage con tipo esplicito
    var usage: ApiUsage? by remember { mutableStateOf(null) }

    Column(modifier = Modifier.padding(16.dp)) {
        if (certificates.isNotEmpty()) {
            val cert = certificates.getOrNull(currentIndex)
            cert?.let {
                val provider = getProviderForUnderlying(it.underlyingName)

                val textColor = if (recentlyUpdated[it.isin] == true) Color(0xFF008000) else Color.Black

                val strikePerc = if (it.strike != 0.0) ((it.lastPrice - it.strike) / it.strike * 100) else 0.0
                val barrierPerc = if (it.barrier != 0.0) ((it.lastPrice - it.barrier) / it.barrier * 100) else 0.0
                val bonusPerc = if (it.bonusLevel != 0.0) ((it.lastPrice - it.bonusLevel) / it.bonusLevel * 100) else 0.0
                val autocallPerc = if (it.autocallLevel != 0.0) ((it.lastPrice - it.autocallLevel) / it.autocallLevel * 100) else 0.0

                Text(
                    text = "ISIN: ${it.isin} (${it.lastUpdate})\n" +
                            "Sottostante: ${it.underlyingName} - Prezzo: ${it.lastPrice} EUR\n" +
                            "Strike: ${it.strike} (${strikePerc.format(1)}%)\n" +
                            "Barrier: ${it.barrier} (${barrierPerc.format(1)}%)\n" +
                            "Bonus: ${it.bonusLevel} (${bonusPerc.format(1)}%)\n" +
                            "Autocall: ${it.autocallLevel} (${autocallPerc.format(1)}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )

                usage?.let { u ->
                    Text(
                        text = "Fornitore: ${provider.displayName} | " +
                                "Day: ${u.dailyUsage}/${provider.dailyLimit} " +
                                "(${(u.dailyUsage * 100 / provider.dailyLimit)}%) | " +
                                "Month: ${u.monthlyUsage}/${provider.monthlyLimit} " +
                                "(${(u.monthlyUsage * 100 / provider.monthlyLimit)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    Button(onClick = { if (currentIndex > 0) currentIndex-- }, enabled = currentIndex > 0) { Text("<") }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { if (currentIndex < certificates.size - 1) currentIndex++ }, enabled = currentIndex < certificates.size - 1) { Text(">") }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = { viewModel.deleteCertificate(it.isin) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancella questo certificato")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        scope.launch {
                            viewModel.fetchAndUpdatePriceWithLimit(it.isin, provider)
                            usageRepository.recordCall(provider)
                            usage = usageRepository.getUsage(provider)
                            recentlyUpdated[it.isin] = true
                            delay(2000)
                            recentlyUpdated[it.isin] = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Aggiorna prezzo visibile") }
            }
        } else {
            Text("Nessun certificato inserito")
        }

        Spacer(modifier = Modifier.height(16.dp))

        @Composable
        fun field(value: String, onChange: (String) -> Unit, label: String) {
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(fieldHeight)
            )
        }

        field(newIsin, { newIsin = it }, "ISIN")
        field(newUnderlying, { newUnderlying = it }, "Sottostante")
        field(newStrike, { newStrike = it }, "Strike")
        field(newBarrier, { newBarrier = it }, "Barrier")
        field(newBonus, { newBonus = it }, "Bonus")
        field(newAutocall, { newAutocall = it }, "Autocall Level")

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (newIsin.isNotEmpty()) {
                    val strikeVal = newStrike.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val barrierVal = newBarrier.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val bonusVal = newBonus.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val autocallVal = newAutocall.replace(',', '.').toDoubleOrNull() ?: 0.0

                    viewModel.addCertificate(
                        isin = newIsin,
                        underlyingName = newUnderlying,
                        strike = strikeVal,
                        barrier = barrierVal,
                        bonusLevel = bonusVal,
                        autocallLevel = autocallVal
                    )

                    newIsin = ""
                    newUnderlying = ""
                    newStrike = ""
                    newBarrier = ""
                    newBonus = ""
                    newAutocall = ""

                    currentIndex = certificates.size
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Aggiungi certificato") }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    certificates.forEach { cert ->
                        val provider = getProviderForUnderlying(cert.underlyingName)
                        viewModel.fetchAndUpdatePriceWithLimit(cert.isin, provider)
                        usageRepository.recordCall(provider)
                        usage = usageRepository.getUsage(provider)
                        recentlyUpdated[cert.isin] = true
                        delay(2000)
                        recentlyUpdated[cert.isin] = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Aggiorna tutti i prezzi") }
    }
}

fun Double.format(digits: Int) = "%.${digits}f".format(this)
