package com.example.certificatestracker

import com.example.certificatestracker.api.ApiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class PriceUsageRepository(private val dao: ApiUsageDao) {

    /**
     * Recupera l'uso corrente del provider, resettando giornaliero/mensile se necessario.
     */
    suspend fun getUsage(provider: ApiProvider): ApiUsage = withContext(Dispatchers.IO) {
        val usage = dao.getUsage(provider.name) ?: ApiUsage(provider = provider.name)
        resetIfNeeded(usage)
    }

    /**
     * Incrementa i contatori giornaliero e mensile.
     */
    suspend fun incrementUsage(provider: ApiProvider) = withContext(Dispatchers.IO) {
        val usage = dao.getUsage(provider.name) ?: ApiUsage(provider = provider.name)
        val updated = resetIfNeeded(usage).copy(
            dailyUsage = usage.dailyUsage + 1,
            monthlyUsage = usage.monthlyUsage + 1
        )
        dao.insertOrUpdate(updated)
    }

    /**
     * Resetta i contatori se il giorno o il mese è cambiato.
     */
    private fun resetIfNeeded(usage: ApiUsage): ApiUsage {
        val now = Calendar.getInstance()

        // reset giornaliero
        val lastDaily = Calendar.getInstance().apply { timeInMillis = usage.lastResetDayMillis }
        val dailyUsage = if (!isSameDay(now, lastDaily)) 0 else usage.dailyUsage
        val dailyReset = if (!isSameDay(now, lastDaily)) now.timeInMillis else usage.lastResetDayMillis

        // reset mensile
        val lastMonthly = Calendar.getInstance().apply { timeInMillis = usage.lastResetMonthMillis }
        val monthlyUsage = if (!isSameMonth(now, lastMonthly)) 0 else usage.monthlyUsage
        val monthlyReset = if (!isSameMonth(now, lastMonthly)) now.timeInMillis else usage.lastResetMonthMillis

        return usage.copy(
            dailyUsage = dailyUsage,
            monthlyUsage = monthlyUsage,
            lastResetDayMillis = dailyReset,
            lastResetMonthMillis = monthlyReset
        )
    }

    private fun isSameDay(c1: Calendar, c2: Calendar) =
        c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)

    private fun isSameMonth(c1: Calendar, c2: Calendar) =
        c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
}
