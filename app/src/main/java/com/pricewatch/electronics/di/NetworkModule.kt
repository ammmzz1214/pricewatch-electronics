package com.pricewatch.electronics.di

import com.pricewatch.electronics.BuildConfig
import com.pricewatch.electronics.core.network.ApiService
import com.pricewatch.electronics.core.network.NetworkFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        NetworkFactory.okHttpClient(BuildConfig.DEBUG)

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        // Base URL is a placeholder host for authorised provider endpoints
        // only; no request is made to it unless a provider declares an
        // official API. Manual-only sites never use this client.
        NetworkFactory.retrofit("https://api.pricewatch.example/", client)

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService =
        retrofit.create(ApiService::class.java)
}
