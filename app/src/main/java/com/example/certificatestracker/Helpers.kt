package com.example.certificatestracker

// Formatta percentuali
/*
fun Double.format(digits: Int) = "%.${digits}f".format(this)
*/

// Formatta date DDMMYY -> DD/MM/YYYY
fun formatDate(input: String): String {
    if (input.length != 6) return input
    val day = input.substring(0, 2)
    val month = input.substring(2, 4)
    val year = input.substring(4, 6).toIntOrNull()?.let { 2000 + it } ?: return input
    return "$day/$month/$year"
}

/**
 * Normalizza una data per l'input nei campi di modifica:
 * - se è in formato DD/MM/YYYY → la converte in DDMMYY
 * - se è in formato DDMMYYYY → la converte in DDMMYY
 * - se è già in DDMMYY → la lascia invariata
 */
fun normalizeToShortRawDateForEdit(input: String): String {
    val digits = input.filter { it.isDigit() }
    return when (digits.length) {
        6 -> digits
        8 -> {
            val dayMonth = digits.substring(0, 4)
            val yearShort = digits.substring(6, 8)
            dayMonth + yearShort
        }
        else -> digits
    }
}

/**
 * Converte l'input grezzo (DDMMYY o DDMMYYYY o con slash) in DD/MM/YYYY
 * da utilizzare al momento del salvataggio.
 */
fun rawToDisplayDate(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return when (digits.length) {
        6 -> formatDate(digits)
        8 -> {
            val day = digits.substring(0, 2)
            val month = digits.substring(2, 4)
            val year = digits.substring(4, 8)
            "$day/$month/$year"
        }
        else -> {
            if (raw.contains("/")) raw else raw
        }
    }
}
