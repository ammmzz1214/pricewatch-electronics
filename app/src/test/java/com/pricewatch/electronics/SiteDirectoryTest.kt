package com.pricewatch.electronics

import com.pricewatch.electronics.data.local.SiteSeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteDirectoryTest {

    @Test
    fun directory_hasExactly51Sites() {
        assertEquals(51, SiteSeedData.all().size)
    }

    @Test
    fun siteIds_areOneTo51Unique() {
        val ids = SiteSeedData.all().map { it.id }
        assertEquals((1L..51L).toList(), ids.sorted())
        assertEquals(51, ids.toSet().size)
    }

    @Test
    fun noSite_isFalselyLive() {
        assertTrue(SiteSeedData.all().none { it.integrationStatus == "LIVE_INTEGRATION" })
    }

    @Test
    fun ebay_isPartialSupport_othersManual() {
        val sites = SiteSeedData.all()
        val ebay = sites.single { it.name == "eBay UK" }
        assertEquals("PARTIAL_SUPPORT", ebay.integrationStatus)
        assertTrue(sites.filter { it.name != "eBay UK" }.all { it.integrationStatus == "MANUAL_URL" })
    }

    @Test
    fun allSites_haveNameAndDomain() {
        SiteSeedData.all().forEach {
            assertTrue(it.name.isNotBlank())
            assertTrue(it.domain.isNotBlank())
            assertTrue(it.domain.contains("."))
        }
    }

    @Test
    fun firstAndLastSites_matchSpec() {
        val sites = SiteSeedData.all()
        assertEquals("Amazon UK", sites.first { it.id == 1L }.name)
        assertEquals("QVC UK", sites.first { it.id == 51L }.name)
        assertEquals("Costco UK", sites.first { it.id == 50L }.name)
    }
}
