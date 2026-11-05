package com.pricewatch.electronics.domain.models

/** The application supports exactly one top-level category: ELECTRONICS. */
enum class Category(val displayName: String) {
    ELECTRONICS("Electronics")
}

enum class Subcategory(val displayName: String) {
    SMARTPHONES("Smartphones"),
    LAPTOPS("Laptops"),
    DESKTOP_COMPUTERS("Desktop Computers"),
    GRAPHICS_CARDS("Graphics Cards"),
    PROCESSORS("Processors"),
    MOTHERBOARDS("Motherboards"),
    MONITORS("Monitors"),
    TELEVISIONS("Televisions"),
    TABLETS("Tablets"),
    GAMING_CONSOLES("Gaming Consoles"),
    HEADPHONES("Headphones"),
    CAMERAS("Cameras"),
    SSDS("SSDs"),
    HDDS("HDDs"),
    RAM("RAM"),
    NETWORKING_EQUIPMENT("Networking Equipment"),
    COMPUTER_ACCESSORIES("Computer Accessories"),
    SMART_HOME_DEVICES("Smart Home Devices")
}

enum class CheckStatus {
    NEVER_CHECKED,
    CHECK_OK,
    OK,
    CHECKING,
    CHECK_FAILED,
    PRICE_DECREASED,
    PRICE_INCREASED,
    PRICE_UNCHANGED
}

enum class IntegrationStatus {
    LIVE_INTEGRATION,
    PARTIAL_SUPPORT,
    MANUAL_URL,
    UNAVAILABLE
}

enum class IntegrationType {
    OFFICIAL_API,
    PRODUCT_FEED,
    THIRD_PARTY,
    COMPLIANT_EXTRACTION,
    MANUAL
}

enum class SiteGroup {
    MAJOR_RETAILERS,
    SPECIALISTS,
    MARKETPLACES,
    PRICE_COMPARISON,
    OTHER_RETAILERS
}

enum class CheckResultType {
    SUCCESS,
    FAILED,
    PRICE_DECREASED,
    PRICE_INCREASED,
    PRICE_UNCHANGED
}
