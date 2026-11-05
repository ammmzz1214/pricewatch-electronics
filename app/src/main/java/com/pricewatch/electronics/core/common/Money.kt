package com.pricewatch.electronics.core.common

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Money is stored as integer minor units (pence for GBP) — never as Double. */
object Money {

    fun fromDecimalString(value: String, currencyCode: String = Constants.DEFAULT_CURRENCY): Long? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        return try {
            val bd = BigDecimal(trimmed).setScale(2, RoundingMode.HALF_UP)
            val minor = bd.movePointRight(2).longValueExact()
            if (minor <= 0 || minor > Constants.MAX_PRICE_MINOR_UNITS) null else minor
        } catch (e: Exception) {
            null
        }
    }

    fun toDecimalString(minorUnits: Long): String =
        BigDecimal(minorUnits).movePointLeft(2).setScale(2, RoundingMode.HALF_UP).toPlainString()

    fun format(minorUnits: Long, currencyCode: String = Constants.DEFAULT_CURRENCY): String {
        return try {
            val currency = Currency.getInstance(currencyCode)
            val locale = when (currencyCode.uppercase()) {
                "GBP" -> Locale.UK
                "USD" -> Locale.US
                "EUR" -> Locale.GERMANY
                else -> Locale.UK
            }
            val format = NumberFormat.getCurrencyInstance(locale).apply { this.currency = currency }
            format.format(BigDecimal(minorUnits).movePointLeft(2))
        } catch (e: Exception) {
            "£${toDecimalString(minorUnits)}"
        }
    }

    /** Percentage change from [oldPrice] to [newPrice]; null when old price is zero. */
    fun percentageChange(oldPriceMinorUnits: Long, newPriceMinorUnits: Long): Double? {
        if (oldPriceMinorUnits <= 0) return null
        val diff = newPriceMinorUnits - oldPriceMinorUnits
        return diff.toDouble() / oldPriceMinorUnits.toDouble() * 100.0
    }

    fun differenceMinorUnits(oldPriceMinorUnits: Long, newPriceMinorUnits: Long): Long =
        newPriceMinorUnits - oldPriceMinorUnits

    fun isValidPrice(minorUnits: Long): Boolean = minorUnits > 0 && minorUnits <= Constants.MAX_PRICE_MINOR_UNITS

    fun isValidCurrency(code: String): Boolean = try {
        Currency.getInstance(code.uppercase())
        code.length == 3 && code.all { it.isLetter() }
    } catch (e: Exception) {
        false
    }
}
