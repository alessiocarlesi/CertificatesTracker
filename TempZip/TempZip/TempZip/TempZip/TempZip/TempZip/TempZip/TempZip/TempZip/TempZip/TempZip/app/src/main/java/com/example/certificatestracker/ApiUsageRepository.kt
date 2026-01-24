package com.example.certificatestracker

import com.example.certificatestracker.api.ApiProvider
import java.util.Calendar

class ApiUsageRepository(private val dao: ApiUsageDao) {

    suspend fun recordCall(provider: ApiProvider) {
        val now = Calendar.getInstance()
        val usage = dao.getUsage(provider.name)

        if (usage == null) {
            // Primo utilizzo → nuovo record
            dao.insertOrUpdate(
                ApiUsage(
                    provider = provider.name,
                    dailyUsage = 1,
                    monthlyUsage = 1,
                    lastResetDayMillis = now.timeInMillis,
                    lastResetMonthMillis = now.timeInMillis
                )
            )
        } else {
            var updated = usage

            // Reset giornaliero
            val lastDaily = Calendar.getInstance().apply { timeInMillis = usage.lastResetDayMillis }
            if (!isSameDay(now, lastDaily)) {
                updated = updated.copy(dailyUsage = 0, lastResetDayMillis = now.timeInMillis)
            }

            // Reset mensile
            val lastMonthly = Calendar.getInstance().apply { timeInMillis = usage.lastResetMonthMillis }
            if (!isSameMonth(now, lastMonthly)) {
                updated = updated.copy(monthlyUsage = 0, lastResetMonthMillis = now.timeInMillis)
            }

            // Aggiorna conteggio
            dao.insertOrUpdate(
                updated.copy(
                    dailyUsage = updated.dailyUsage + 1,
                    monthlyUsage = updated.monthlyUsage + 1
                )
            )
        }
    }

    suspend fun getUsage(provider: ApiProvider): ApiUsage? {
        return dao.getUsage(provider.name)
    }

    private fun isSameDay(c1: Calendar, c2: Calendar) =
        c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)

    private fun isSameMonth(c1: Calendar, c2: Calendar) =
        c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
}
