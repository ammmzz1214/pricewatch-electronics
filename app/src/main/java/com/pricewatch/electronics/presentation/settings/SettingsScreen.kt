package com.pricewatch.electronics.presentation.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pricewatch.electronics.BuildConfig
import com.pricewatch.electronics.core.designsystem.PriceWatchColors
import com.pricewatch.electronics.core.designsystem.PriceWatchShapes
import com.pricewatch.electronics.core.designsystem.PriceWatchSpacing
import com.pricewatch.electronics.core.designsystem.components.PriceWatchGoldButton
import com.pricewatch.electronics.core.designsystem.components.PriceWatchOutlineButton
import kotlinx.coroutines.launch

private val INTERVALS = listOf(15, 30, 60, 120)
private val CURRENCIES = listOf("GBP", "EUR", "USD")
private const val SUPPORT_EMAIL = "support@pricewatch-electronics.co.uk"

private const val PRIVACY_TEXT = "PriceWatch Electronics stores your tracked products, " +
    "price history and preferences only on your device, in its local database. " +
    "No account is required and no personal information is collected, sold or shared. " +
    "Product pages are opened in your browser; the app does not bypass website " +
    "authentication, CAPTCHA or access controls. You can delete all products and " +
    "clear all price history at any time from Settings > Data management."

private const val TERMS_TEXT = "PriceWatch Electronics tracks prices for personal use. Prices " +
    "shown are the last verified prices from the listed shops; shops may change " +
    "prices at any time and availability is not guaranteed. Automated checking is " +
    "only performed where a verified integration exists — most shops are " +
    "manual-URL and rely on prices you enter. Background checks run at best " +
    "effort on the schedule you choose (Android may delay them to save battery). " +
    "The app is provided as-is without warranty."

/**
 * Settings. Signature matches the NavHost in app/Navigation.kt (no args).
 * Every setting persists — monitoring/notification/alert settings in Room
 * via SettingsRepository, the theme preference in DataStore.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showClearHistory by remember { mutableStateOf(false) }
    var showDeleteAll by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setNotificationsEnabled(true)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Notifications stay off — permission was denied. Enable it in system settings to receive price alerts."
                )
            }
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val settings = state.settings
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(PriceWatchSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PriceWatchSpacing.md)
        ) {
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PriceWatchColors.White
                )
                Text(
                    text = "PriceWatch Electronics v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodySmall,
                    color = PriceWatchColors.SecondaryText
                )
            }

            // Monitoring.
            item {
                SettingsSection(title = "Monitoring") {
                    SwitchRow(
                        label = "Automatic monitoring",
                        description = "Check tracked products in the background.",
                        checked = settings.monitoringEnabled,
                        onCheckedChange = { viewModel.setMonitoringEnabled(it) }
                    )
                    Text(
                        text = "Check every:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PriceWatchColors.White
                    )
                    for (minutes in INTERVALS) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = settings.intervalMinutes == minutes,
                                onClick = { viewModel.setIntervalMinutes(minutes) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = PriceWatchColors.Gold,
                                    unselectedColor = PriceWatchColors.SecondaryText
                                )
                            )
                            Text(
                                text = "$minutes minutes" + if (minutes == 15) " (minimum)" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PriceWatchColors.White
                            )
                        }
                    }
                    Text(
                        text = "Background checks are inexact by design — Android may delay them to save battery. 15 minutes is the minimum interval, not an exact schedule.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PriceWatchColors.SecondaryText
                    )
                }
            }

            // Notifications.
            item {
                SettingsSection(title = "Notifications") {
                    SwitchRow(
                        label = "Notifications",
                        description = "Allow price alerts on this device.",
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                viewModel.setNotificationsEnabled(false)
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.setNotificationsEnabled(true)
                            }
                        }
                    )
                    SwitchRow(
                        label = "Price decrease alerts",
                        description = "Notify when a tracked price falls.",
                        checked = settings.priceDecreaseAlerts,
                        onCheckedChange = { viewModel.setPriceDecreaseAlerts(it) },
                        enabled = settings.notificationsEnabled
                    )
                    SwitchRow(
                        label = "Price increase alerts",
                        description = "Notify when a tracked price rises.",
                        checked = settings.priceIncreaseAlerts,
                        onCheckedChange = { viewModel.setPriceIncreaseAlerts(it) },
                        enabled = settings.notificationsEnabled
                    )
                    SwitchRow(
                        label = "Target price alerts",
                        description = "Notify when a price reaches your target.",
                        checked = settings.targetPriceAlerts,
                        onCheckedChange = { viewModel.setTargetPriceAlerts(it) },
                        enabled = settings.notificationsEnabled
                    )
                }
            }

            // Preferences.
            item {
                SettingsSection(title = "Preferences") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Currency",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PriceWatchColors.White
                        )
                        TextButton(onClick = { currencyExpanded = true }) {
                            Text(text = settings.currencyPreference, color = PriceWatchColors.Gold)
                        }
                        DropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false }
                        ) {
                            for (currency in CURRENCIES) {
                                DropdownMenuItem(
                                    text = { Text(text = currency) },
                                    onClick = {
                                        viewModel.setCurrency(currency)
                                        currencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PriceWatchColors.White
                        )
                        TextButton(onClick = { themeExpanded = true }) {
                            Text(
                                text = if (state.themePreference == AppThemePreference.SYSTEM) "System" else "Dark",
                                color = PriceWatchColors.Gold
                            )
                        }
                        DropdownMenu(
                            expanded = themeExpanded,
                            onDismissRequest = { themeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(text = "Dark") },
                                onClick = {
                                    viewModel.setThemePreference(AppThemePreference.DARK)
                                    themeExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(text = "System") },
                                onClick = {
                                    viewModel.setThemePreference(AppThemePreference.SYSTEM)
                                    themeExpanded = false
                                }
                            )
                        }
                    }
                    Text(
                        text = "Dark luxury is the PriceWatch brand theme and the default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PriceWatchColors.SecondaryText
                    )
                }
            }

            // Data management.
            item {
                SettingsSection(title = "Data management") {
                    PriceWatchOutlineButton(
                        text = "Clear price history",
                        onClick = { showClearHistory = true }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PriceWatchOutlineButton(
                        text = "Delete all products",
                        onClick = { showDeleteAll = true }
                    )
                    Text(
                        text = "Clearing history keeps your products. Deleting all products also deletes their history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PriceWatchColors.SecondaryText
                    )
                }
            }

            // About / legal / support.
            item {
                SettingsSection(title = "About") {
                    SettingsLink(label = "About PriceWatch") { showAbout = true }
                    SettingsLink(label = "Privacy Policy") { showPrivacy = true }
                    SettingsLink(label = "Terms and Conditions") { showTerms = true }
                    SettingsLink(label = "Contact Support") {
                        try {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_SENDTO,
                                    Uri.parse("mailto:$SUPPORT_EMAIL")
                                )
                            )
                        } catch (_: ActivityNotFoundException) {
                            scope.launch {
                                snackbarHostState.showSnackbar("No email app found. Email $SUPPORT_EMAIL")
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showClearHistory) {
        AlertDialog(
            onDismissRequest = { showClearHistory = false },
            containerColor = PriceWatchColors.CardBackground,
            title = { Text(text = "Clear price history?", color = PriceWatchColors.White) },
            text = {
                Text(
                    text = "All recorded price history will be deleted. Your products and their current prices are kept.",
                    color = PriceWatchColors.SecondaryText
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showClearHistory = false; viewModel.clearHistory() },
                    colors = ButtonDefaults.textButtonColors(contentColor = PriceWatchColors.WarningRed)
                ) { Text(text = "Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistory = false }) {
                    Text(text = "Cancel", color = PriceWatchColors.Gold)
                }
            }
        )
    }
    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = { showDeleteAll = false },
            containerColor = PriceWatchColors.CardBackground,
            title = { Text(text = "Delete all products?", color = PriceWatchColors.White) },
            text = {
                Text(
                    text = "Every tracked product and its price history will be permanently deleted. This cannot be undone.",
                    color = PriceWatchColors.SecondaryText
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteAll = false; viewModel.deleteAllProducts() },
                    colors = ButtonDefaults.textButtonColors(contentColor = PriceWatchColors.WarningRed)
                ) { Text(text = "Delete all") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAll = false }) {
                    Text(text = "Cancel", color = PriceWatchColors.Gold)
                }
            }
        )
    }
    if (showAbout) {
        InfoDialog(
            title = "About PriceWatch",
            body = "PriceWatch Electronics v${BuildConfig.VERSION_NAME} — Track smarter. Buy at the right price.\n\n" +
                "Track electronics prices across 51 UK websites, see verified price history, " +
                "and get alerted when a price falls or reaches your target. Category: Electronics only.",
            onDismiss = { showAbout = false }
        )
    }
    if (showPrivacy) {
        InfoDialog(title = "Privacy Policy", body = PRIVACY_TEXT, onDismiss = { showPrivacy = false })
    }
    if (showTerms) {
        InfoDialog(title = "Terms and Conditions", body = TERMS_TEXT, onDismiss = { showTerms = false })
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = PriceWatchShapes.large,
        colors = CardDefaults.cardColors(containerColor = PriceWatchColors.CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(PriceWatchSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = PriceWatchColors.Gold
            )
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = PriceWatchColors.White)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = PriceWatchColors.SecondaryText
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun SettingsLink(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = PriceWatchColors.White,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun InfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PriceWatchColors.CardBackground,
        title = { Text(text = title, color = PriceWatchColors.White) },
        text = { Text(text = body, color = PriceWatchColors.SecondaryText) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close", color = PriceWatchColors.Gold)
            }
        }
    )
}
