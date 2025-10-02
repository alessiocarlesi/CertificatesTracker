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
