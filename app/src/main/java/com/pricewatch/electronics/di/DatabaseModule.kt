package com.pricewatch.electronics.di

import android.content.Context
import androidx.room.Room
import com.pricewatch.electronics.data.local.MonitoringSettingsEntity
import com.pricewatch.electronics.data.local.PriceWatchDatabase
import com.pricewatch.electronics.data.local.SiteSeedData
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PriceWatchDatabase {
        val db = Room.databaseBuilder(
            context,
            PriceWatchDatabase::class.java,
            PriceWatchDatabase.DATABASE_NAME
        ).build()
        // Seed sites asynchronously after the database is ready.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (db.siteDao().count() == 0) {
                    db.siteDao().insertAll(SiteSeedData.all())
                }
                if (db.settingsDao().get() == null) {
                    db.settingsDao().upsert(MonitoringSettingsEntity())
                }
            } catch (_: Exception) {
                // Seeding failure must not crash app start; sites can be
                // re-seeded on next launch (count stays 0).
            }
        }
        return db
    }

    @Provides
    fun provideProductDao(db: PriceWatchDatabase) = db.productDao()

    @Provides
    fun providePriceHistoryDao(db: PriceWatchDatabase) = db.priceHistoryDao()

    @Provides
    fun provideSiteDao(db: PriceWatchDatabase) = db.siteDao()

    @Provides
    fun provideSettingsDao(db: PriceWatchDatabase) = db.settingsDao()

    @Provides
    fun provideCheckLogDao(db: PriceWatchDatabase) = db.checkLogDao()
}
