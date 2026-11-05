package com.pricewatch.electronics.presentation.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Theme preference persisted in DataStore. The PriceWatch dark-luxury theme
 * is the default; SYSTEM follows the device. MonitoringSettings in Room has
 * no theme field, so this lives here.
 *
 * MainActivity reads [observeThemePreference] and passes the resulting
 * darkTheme flag into PriceWatchTheme. The palette itself stays the dark
 * brand theme in both modes — that is the brand default.
 */
enum class AppThemePreference { SYSTEM, DARK }

private val Context.priceWatchDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pricewatch_preferences"
)

private val THEME_KEY = stringPreferencesKey("theme_preference")

fun Context.observeThemePreference(): Flow<AppThemePreference> =
    priceWatchDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            runCatching { AppThemePreference.valueOf(prefs[THEME_KEY] ?: "DARK") }
                .getOrDefault(AppThemePreference.DARK)
        }

suspend fun Context.setThemePreference(preference: AppThemePreference) {
    priceWatchDataStore.edit { prefs -> prefs[THEME_KEY] = preference.name }
}
