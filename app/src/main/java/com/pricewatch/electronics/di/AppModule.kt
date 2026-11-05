package com.pricewatch.electronics.di

import com.pricewatch.electronics.data.local.PriceWatchDatabase
import com.pricewatch.electronics.data.repositories.CheckLogRepositoryImpl
import com.pricewatch.electronics.data.repositories.PriceHistoryRepositoryImpl
import com.pricewatch.electronics.data.repositories.ProductRepositoryImpl
import com.pricewatch.electronics.data.repositories.SettingsRepositoryImpl
import com.pricewatch.electronics.data.repositories.SiteRepositoryImpl
import com.pricewatch.electronics.domain.repositories.CheckLogRepository
import com.pricewatch.electronics.domain.repositories.PriceHistoryRepository
import com.pricewatch.electronics.domain.repositories.ProductRepository
import com.pricewatch.electronics.domain.repositories.SettingsRepository
import com.pricewatch.electronics.domain.repositories.SiteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    @Singleton
    abstract fun bindPriceHistoryRepository(impl: PriceHistoryRepositoryImpl): PriceHistoryRepository

    @Binds
    @Singleton
    abstract fun bindSiteRepository(impl: SiteRepositoryImpl): SiteRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindCheckLogRepository(impl: CheckLogRepositoryImpl): CheckLogRepository
}
