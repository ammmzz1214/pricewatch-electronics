package com.pricewatch.electronics.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pricewatch.electronics.core.designsystem.Theme.PriceWatchTheme
import com.pricewatch.electronics.presentation.settings.AppThemePreference
import com.pricewatch.electronics.presentation.settings.observeThemePreference
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Theme preference from DataStore: DARK forces the dark luxury
            // theme; SYSTEM follows the device. The palette itself stays
            // the dark brand theme in both cases (see PriceWatchTheme).
            val themePreference by applicationContext
                .observeThemePreference()
                .collectAsStateWithLifecycle(initialValue = AppThemePreference.DARK)
            val darkTheme = when (themePreference) {
                AppThemePreference.DARK -> true
                AppThemePreference.SYSTEM -> isSystemInDarkTheme()
            }
            PriceWatchTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PriceWatchNavHost()
                }
            }
        }
    }
}
