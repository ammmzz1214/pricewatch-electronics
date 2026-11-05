package com.pricewatch.electronics.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProductEntity::class,
        PriceHistoryEntity::class,
        SiteEntity::class,
        MonitoringSettingsEntity::class,
        CheckLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PriceWatchDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun siteDao(): SiteDao
    abstract fun settingsDao(): SettingsDao
    abstract fun checkLogDao(): CheckLogDao

    companion object {
        const val DATABASE_NAME = "pricewatch.db"

        /**
         * Migration 1 -> 2 (reserved for a future schema change).
         * Currently the database ships at version 1, so this migration is
         * defined and tested-ready but not yet applied. It adds a
         * lastSuccessfulConnectionAt column to sites if a future release
         * bumps the version — it is kept here so the upgrade path is
         * explicit and cannot be forgotten.
         * NOTE: Do not apply until version is bumped to 2 in @Database.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // lastSuccessfulConnectionAt already exists in v1 schema;
                // this migration is a no-op placeholder kept for the
                // documented upgrade path. A real v2 change must replace
                // this body before the version is bumped.
            }
        }
    }
}
