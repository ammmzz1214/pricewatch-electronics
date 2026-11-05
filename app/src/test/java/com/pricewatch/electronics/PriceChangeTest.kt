package com.pricewatch.electronics

import com.pricewatch.electronics.domain.models.Category
import com.pricewatch.electronics.domain.models.Product
import com.pricewatch.electronics.domain.models.Subcategory
import com.pricewatch.electronics.domain.usecases.CalculatePriceChangeUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceChangeTest {

    private val useCase = CalculatePriceChangeUseCase()

    @Test
    fun decrease_hasNegativeDifferenceAndPercent() {
        val change = useCase(30000L, 24828L)
        assertEquals(-5172L, change.differenceMinorUnits)
        assertEquals(-17.24, change.percentageChange!!, 0.01)
    }

    @Test
    fun increase_hasPositiveDifferenceAndPercent() {
        val change = useCase(10000L, 11000L)
        assertEquals(1000L, change.differenceMinorUnits)
        assertEquals(10.0, change.percentageChange!!, 0.001)
    }

    @Test
    fun unchanged_isZero() {
        val change = useCase(5000L, 5000L)
        assertEquals(0L, change.differenceMinorUnits)
        assertEquals(0.0, change.percentageChange!!, 0.001)
    }

    @Test
    fun zeroOldPrice_givesNullPercent() {
        assertNull(useCase(0L, 100L).percentageChange)
    }

    @Test
    fun category_isElectronicsOnly() {
        assertEquals(1, Category.values().size)
        assertEquals(Category.ELECTRONICS, Category.valueOf("ELECTRONICS"))
    }

    @Test
    fun subcategories_areExactlyEighteen() {
        assertEquals(18, Subcategory.values().size)
    }

    @Test
    fun targetReached_onlyWhenAtOrBelowTarget() {
        val base = Product(
            name = "Test Phone", subcategory = Subcategory.SMARTPHONES, siteId = 1L,
            productUrl = "https://www.amazon.co.uk/dp/test",
            currentPriceMinorUnits = 20000L, targetPriceMinorUnits = 25000L
        )
        assertTrue(base.targetReached)
        assertFalse(base.copy(currentPriceMinorUnits = 26000L).targetReached)
        assertFalse(base.copy(targetPriceMinorUnits = null).targetReached)
    }
}
