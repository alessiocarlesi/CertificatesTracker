package com.example.certificatestracker
fun formatDate(input: String): String {
    if (input.length != 6) return input // ritorna così com'è se lunghezza non corretta

    val day = input.substring(0, 2)
    val month = input.substring(2, 4)
    val year = input.substring(4, 6).toIntOrNull()?.let { 2000 + it } ?: return input

    return "$day/$month/$year"
}

/* Esempio di utilizzo:
fun main() {
    val rawDate = "311225"
    val formatted = formatDate(rawDate)
    println(formatted) // Stampa: 31/12/2025


}
*/