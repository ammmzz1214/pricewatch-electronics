package com.pricewatch.electronics.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.pricewatch.electronics.core.designsystem.PriceWatchColors

/**
 * Deletion confirmation. The caller performs the delete only after the user
 * taps Delete; Cancel dismisses with no side effect.
 */
@Composable
fun ConfirmDeleteDialog(
    productName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PriceWatchColors.CardBackground,
        title = {
            Text(
                text = "Delete product?",
                style = MaterialTheme.typography.titleLarge,
                color = PriceWatchColors.White
            )
        },
        text = {
            Text(
                text = "“$productName” and its price history will be permanently deleted. " +
                    "Its scheduled monitoring will be cancelled. This cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = PriceWatchColors.SecondaryText
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = PriceWatchColors.WarningRed)
            ) {
                Text(text = "Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = PriceWatchColors.Gold)
            ) {
                Text(text = "Cancel")
            }
        }
    )
}
