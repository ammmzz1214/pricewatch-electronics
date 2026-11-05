package com.pricewatch.electronics

import com.pricewatch.electronics.core.common.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    @Test
    fun fromDecimalString_parsesPoundsToPence() {
        assertEquals(24828L, Money.fromDecimalString("248.28"))
        assertEquals(100L, Money.fromDecimalString("1.00"))
        assertEquals(1L, Money.fromDecimalString("0.01"))
    }

    @Test
    fun fromDecimalString_rejectsZeroNegativeAndBlank() {
        assertNull(Money.fromDecimalString("0"))
        assertNull(Money.fromDecimalString("-5.00"))
        assertNull(Money.fromDecimalString(""))
        assertNull(Money.fromDecimalString("abc"))
    }

    @Test
    fun toDecimalString_roundTrips() {
        assertEquals("248.28", Money.toDecimalString(24828L))
    }

    @Test
    fun percentageChange_dropIsNegative() {
        val pct = Money.percentageChange(30000L, 24828L)!!
        assertTrue(pct < 0.0)
        assertEquals(-17.24, pct, 0.01)
    }

    @Test
    fun percentageChange_zeroOldPriceIsNull() {
        assertNull(Money.percentageChange(0L, 100L))
    }

    @Test
    fun difference_isNewMinusOld() {
        assertEquals(-5172L, Money.differenceMinorUnits(30000L, 24828L))
        assertEquals(1000L, Money.differenceMinorUnits(10000L, 11000L))
    }

    @Test
    fun isValidPrice_andCurrency() {
        assertTrue(Money.isValidPrice(1L))
        assertFalse(Money.isValidPrice(0L))
        assertTrue(Money.isValidCurrency("GBP"))
        assertFalse(Money.isValidCurrency("GB"))
        assertFalse(Money.isValidCurrency("123"))
    }
}
