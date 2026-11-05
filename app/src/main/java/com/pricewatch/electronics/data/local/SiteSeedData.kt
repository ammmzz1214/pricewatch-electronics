package com.pricewatch.electronics.data.local

import com.pricewatch.electronics.domain.models.IntegrationStatus
import com.pricewatch.electronics.domain.models.IntegrationType
import com.pricewatch.electronics.domain.models.SiteGroup

/**
 * Exactly 51 sites. Integration statuses are honest:
 * no site is marked LIVE_INTEGRATION — no live integration has been
 * implemented and verified. eBay is PARTIAL_SUPPORT (manual URL +
 * limited third-party lookup where permitted); every other site is
 * MANUAL_URL: the user saves the product URL and updates the price
 * manually. Comparison platforms are directories, not retailers.
 */
object SiteSeedData {

    fun all(): List<SiteEntity> = listOf(
        // Group A: Major UK Retailers (1-15)
        site(1, "Amazon UK", "amazon.co.uk", SiteGroup.MAJOR_RETAILERS),
        site(2, "Argos", "argos.co.uk", SiteGroup.MAJOR_RETAILERS),
        site(3, "Currys", "currys.co.uk", SiteGroup.MAJOR_RETAILERS),
        site(4, "Very", "very.co.uk", SiteGroup.MAJOR_RETAILERS),
        site(5, "John Lewis", "johnlewis.com", SiteGroup.MAJOR_RETAILERS),
        site(6, "AO", "ao.com", SiteGroup.MAJOR_RETAILERS),
        site(7, "Appliances Direct", "appliancesdirect.co.uk", SiteGroup.MAJOR_RETAILERS),
        site(8, "Ebuyer", "ebuyer.com", SiteGroup.MAJOR_RETAILERS),
        site(9, "Scan.co.uk", "scan.co.uk", SiteGroup.MAJOR_RETAILERS),
        site(10, "Overclockers UK", "overclockers.co.uk", SiteGroup.MAJOR_RETAILERS),
        site(11, "Box.co.uk", "box.co.uk", SiteGroup.MAJOR_RETAILERS),
        site(12, "CCL Computers", "cclonline.com", SiteGroup.MAJOR_RETAILERS),
        site(13, "Laptops Direct", "laptopsdirect.co.uk", SiteGroup.MAJOR_RETAILERS),
        site(14, "Dell UK", "dell.com/en-uk", SiteGroup.MAJOR_RETAILERS),
        site(15, "HP UK", "hp.com/gb-en", SiteGroup.MAJOR_RETAILERS),
        // Group B: Computer and Electronics Specialists (16-27)
        site(16, "Lenovo UK", "lenovo.com/gb/en", SiteGroup.SPECIALISTS),
        site(17, "ASUS UK", "asus.com/uk", SiteGroup.SPECIALISTS),
        site(18, "Acer UK", "acer.com/gb-en", SiteGroup.SPECIALISTS),
        site(19, "Samsung UK", "samsung.com/uk", SiteGroup.SPECIALISTS),
        site(20, "Sony UK", "sony.co.uk", SiteGroup.SPECIALISTS),
        site(21, "Apple UK", "apple.com/uk", SiteGroup.SPECIALISTS),
        site(22, "Razer UK", "razer.com/gb-en", SiteGroup.SPECIALISTS),
        site(23, "PCSpecialist", "pcspecialist.co.uk", SiteGroup.SPECIALISTS),
        site(24, "Novatech", "novatech.co.uk", SiteGroup.SPECIALISTS),
        site(25, "AWD-IT", "awd-it.co.uk", SiteGroup.SPECIALISTS),
        site(26, "Chillblast", "chillblast.com", SiteGroup.SPECIALISTS),
        site(27, "MoreCoCo", "morecoco.com", SiteGroup.SPECIALISTS),
        // Group C: Marketplaces (28-32)
        site(28, "eBay UK", "ebay.co.uk", SiteGroup.MARKETPLACES,
            IntegrationStatus.PARTIAL_SUPPORT, IntegrationType.THIRD_PARTY),
        site(29, "OnBuy", "onbuy.com", SiteGroup.MARKETPLACES),
        site(30, "Back Market UK", "backmarket.co.uk", SiteGroup.MARKETPLACES),
        site(31, "Refurbed UK", "refurbed.co.uk", SiteGroup.MARKETPLACES),
        site(32, "MusicMagpie", "musicmagpie.co.uk", SiteGroup.MARKETPLACES),
        // Group D: Price Comparison Platforms (33-38)
        site(33, "PriceSpy UK", "pricespy.co.uk", SiteGroup.PRICE_COMPARISON),
        site(34, "PriceRunner UK", "pricerunner.com", SiteGroup.PRICE_COMPARISON),
        site(35, "Idealo UK", "idealo.co.uk", SiteGroup.PRICE_COMPARISON),
        site(36, "Google Shopping UK", "shopping.google.com", SiteGroup.PRICE_COMPARISON),
        site(37, "Kelkoo UK", "kelkoo.co.uk", SiteGroup.PRICE_COMPARISON),
        site(38, "Trolley.co.uk", "trolley.co.uk", SiteGroup.PRICE_COMPARISON),
        // Group E: Other Electronics Retailers (39-51)
        site(39, "BT Shop", "shop.bt.com", SiteGroup.OTHER_RETAILERS),
        site(40, "EE Store", "ee.co.uk", SiteGroup.OTHER_RETAILERS),
        site(41, "Vodafone UK", "vodafone.co.uk", SiteGroup.OTHER_RETAILERS),
        site(42, "Three UK", "three.co.uk", SiteGroup.OTHER_RETAILERS),
        site(43, "Sky Shop", "sky.com/shop", SiteGroup.OTHER_RETAILERS),
        site(44, "Mobile Phones Direct", "mobilephonesdirect.co.uk", SiteGroup.OTHER_RETAILERS),
        site(45, "Clove Technology", "clove.co.uk", SiteGroup.OTHER_RETAILERS),
        site(46, "Wex Photo Video", "wexphotovideo.com", SiteGroup.OTHER_RETAILERS),
        site(47, "Park Cameras", "parkcameras.com", SiteGroup.OTHER_RETAILERS),
        site(48, "Richer Sounds", "richersounds.com", SiteGroup.OTHER_RETAILERS),
        site(49, "Hughes", "hughes.co.uk", SiteGroup.OTHER_RETAILERS),
        site(50, "Costco UK", "costco.co.uk", SiteGroup.OTHER_RETAILERS),
        site(51, "QVC UK", "qvcuk.com", SiteGroup.OTHER_RETAILERS)
    )

    private fun site(
        id: Long,
        name: String,
        domain: String,
        group: SiteGroup,
        status: IntegrationStatus = IntegrationStatus.MANUAL_URL,
        type: IntegrationType = IntegrationType.MANUAL
    ) = SiteEntity(
        id = id,
        name = name,
        domain = domain,
        siteGroup = group.name,
        integrationType = type.name,
        integrationStatus = status.name,
        isEnabled = true,
        lastSuccessfulConnectionAt = null
    )
}
