package com.smartbudget.ui.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

fun formatCurrency(amount: Double): String {
    val symbols = DecimalFormatSymbols(Locale.FRANCE)
    val formatter = DecimalFormat("#,##0.00", symbols)
    return "${formatter.format(amount)} DH"
}
