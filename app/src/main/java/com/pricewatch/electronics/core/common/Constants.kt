package com.pricewatch.electronics.core.common

object Constants {
    const val DATABASE_NAME = "pricewatch.db"
    const val DATABASE_VERSION = 1
    const val DEFAULT_CURRENCY = "GBP"
    const val DEFAULT_MONITORING_INTERVAL_MINUTES = 60
    const val MIN_MONITORING_INTERVAL_MINUTES = 15
    const val MAX_PRICE_MINOR_UNITS = 100_000_000_00L // £100,000,000.00 ceiling guard
    const val NOTIFICATION_CHANNEL_PRICE_ALERTS = "price_alerts"
    const val NOTIFICATION_CHANNEL_MONITORING = "monitoring"
    const val PERIODIC_WORK_NAME = "pricewatch_periodic_check"
    const val PRICE_CHECK_WORK_TAG = "price_check"
    const val HISTORY_RETENTION_DAYS_DEFAULT = 365
    const val MAX_PRODUCT_NAME_LENGTH = 200
    const val MAX_URL_LENGTH = 2048
}
