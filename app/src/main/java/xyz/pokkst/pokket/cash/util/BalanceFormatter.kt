package xyz.pokkst.pokket.cash.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class BalanceFormatter {
    companion object {
        fun formatBalance(amount: Double, pattern: String): String {
            val formatter = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
            return formatter.format(amount)
        }
    }
}