package com.pricewatch.electronics.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pricewatch.electronics.app.MainActivity
import com.pricewatch.electronics.core.common.Constants
import com.pricewatch.electronics.core.common.Money
import com.pricewatch.electronics.domain.models.Product
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Price-change notifications. One notification per real price event only:
 * the same (product, old price, new price) event is never notified twice.
 * Failed checks never produce a price notification.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val EXTRA_PRODUCT_ID = "extra_product_id"
    }

    private val notifiedEvents = Collections.synchronizedSet(mutableSetOf<String>())

    private fun canPost(): Boolean {
        if (android.os.Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun productIntent(productId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_PRODUCT_ID, productId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, productId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun postOnce(eventKey: String, notificationId: Int, build: () -> android.app.Notification) {
        if (!canPost()) return
        if (!notifiedEvents.add(eventKey)) return
        try {
            NotificationManagerCompat.from(context).notify(notificationId, build())
        } catch (_: SecurityException) {
        }
    }

    fun notifyPriceDecrease(product: Product, oldPrice: Long, newPrice: Long) {
        val pct = Money.percentageChange(oldPrice, newPrice) ?: 0.0
        val text = "${product.name}: ${Money.format(oldPrice)} -> ${Money.format(newPrice)} " +
            "(${String.format("%.1f", pct)}%) at ${product.siteName.ifBlank { "saved shop" }}"
        postOnce("dec:${product.id}:$oldPrice:$newPrice", 1000 + product.id.toInt()) {
            NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_PRICE_ALERTS)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Price dropped")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(productIntent(product.id))
                .setAutoCancel(true)
                .build()
        }
    }

    fun notifyPriceIncrease(product: Product, oldPrice: Long, newPrice: Long) {
        val pct = Money.percentageChange(oldPrice, newPrice) ?: 0.0
        val text = "${product.name}: ${Money.format(oldPrice)} -> ${Money.format(newPrice)} " +
            "(${String.format("%.1f", pct)}%) at ${product.siteName.ifBlank { "saved shop" }}"
        postOnce("inc:${product.id}:$oldPrice:$newPrice", 2000 + product.id.toInt()) {
            NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_PRICE_ALERTS)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("Price increased")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(productIntent(product.id))
                .setAutoCancel(true)
                .build()
        }
    }

    fun notifyTargetReached(product: Product) {
        val text = "${product.name} is now ${Money.format(product.currentPriceMinorUnits)}" +
            (product.targetPriceMinorUnits?.let { " — target was ${Money.format(it)}" } ?: "")
        postOnce("target:${product.id}:${product.currentPriceMinorUnits}", 3000 + product.id.toInt()) {
            NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_PRICE_ALERTS)
                .setSmallIcon(android.R.drawable.star_on)
                .setContentTitle("Target price reached")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(productIntent(product.id))
                .setAutoCancel(true)
                .build()
        }
    }

    fun notifyRepeatedFailures(failedCount: Int, totalCount: Int) {
        if (failedCount <= 0) return
        val text = "$failedCount of $totalCount products failed to check. Last known prices are kept unchanged."
        postOnce("failures:$failedCount:$totalCount:${System.currentTimeMillis() / 3_600_000}", 9001) {
            NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_MONITORING)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("Price checks failing")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .build()
        }
    }
}
