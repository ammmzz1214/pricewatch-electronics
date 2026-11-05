package com.pricewatch.electronics.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.pricewatch.electronics.core.common.Constants
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PriceWatchApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channels = listOf(
            NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_PRICE_ALERTS,
                "Price Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when a tracked product price changes or reaches its target price."
            },
            NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_MONITORING,
                "Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Status of background price monitoring, including repeated check failures."
            }
        )
        channels.forEach { manager.createNotificationChannel(it) }
    }
}
