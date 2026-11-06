package com.pricewatch.electronics.core.network

import com.pricewatch.electronics.core.common.Result
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

@Serializable
data class GenericPriceResponse(
    val priceMinorUnits: Long? = null,
    val currencyCode: String? = null,
    val inStock: Boolean? = null
)

/**
 * Generic API surface for authorised provider endpoints only.
 * No site is called unless its provider declares an official API/feed.
 * Manual-only sites never reach this service.
 */
interface ApiService {
    @GET
    suspend fun getPrice(@Url url: String): GenericPriceResponse
}

object NetworkFactory {
    fun okHttpClient(isDebug: Boolean): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
        if (isDebug) {
            builder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        }
        return builder.build()
    }

    fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(
                Json.asConverterFactory(
                    "application/json".toMediaType()
                )
            )
            .build()
}

fun <T> runCatchingResult(block: suspend () -> T): suspend () -> Result<T> = {
    try { Result.Success(block()) } catch (e: Exception) { Result.Error(e.message ?: "Network request failed.", e) }
}
