package app.fasting.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fasting.domain.Durations
import app.fasting.ui.components.DsText
import app.fasting.ui.theme.DsColors
import app.fasting.ui.theme.DsFont

/** Asks for a number of hours. Commas or dots are accepted; OK stays disabled until the value is valid. */
@Composable
fun HoursDialog(title: String, onConfirm: (Double) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val hours = Durations.parseHours(text)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DsColors.White,
        shape = RoundedCornerShape(0.dp),
        title = { DsText(title, 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    isError = text.isNotBlank() && hours == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = TextStyle(fontFamily = DsFont, fontSize = 18.sp),
                    placeholder = { DsText("e.g. 14.5", 16.sp, color = DsColors.Subtle) },
                )
                DsText("Hours, decimals allowed. From 1 minute to 240 hours.", 12.sp, color = DsColors.Subtle)
            }
        },
        confirmButton = {
            TextButton(onClick = { hours?.let(onConfirm) }, enabled = hours != null) {
                DsText("OK", 16.sp, color = DsColors.TopDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                DsText("CANCEL", 16.sp, color = DsColors.Subtle)
            }
        },
    )
}

@Composable
fun ConfirmDialog(text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DsColors.White,
        shape = RoundedCornerShape(0.dp),
        text = { DsText(text, 16.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                DsText(confirmLabel, 16.sp, color = DsColors.TopDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                DsText("KEEP GOING", 16.sp, color = DsColors.Subtle)
            }
        },
    )
}
