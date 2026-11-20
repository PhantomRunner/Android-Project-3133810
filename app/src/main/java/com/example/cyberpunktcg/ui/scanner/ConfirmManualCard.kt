package com.example.cyberpunktcg.ui.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cyberpunktcg.R
import com.example.cyberpunktcg.ui.collection.PrintingLabel
import com.example.cyberpunktcg.ui.collection.QuantitySaveError

@Composable
internal fun ConfirmManualCard(
    state: ManualAddState.Confirming,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val card = state.printing.card
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.scanner_confirm_printing), style = MaterialTheme.typography.titleLarge)
        Text(card.name, style = MaterialTheme.typography.titleMedium)
        if (card.subtitle.isNotBlank()) Text(card.subtitle)
        PrintingLabel(state.printing, includeVariantAndFinish = true)
        when (state.error) {
            ManualAddError.SAVE_FAILED -> QuantitySaveError()
            ManualAddError.QUANTITY_LIMIT -> Text(
                stringResource(R.string.scanner_quantity_limit),
                color = MaterialTheme.colorScheme.error,
            )
            null -> Unit
        }
        Button(onClick = onConfirm, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (state.isSaving) R.string.scanner_adding_copy else R.string.scanner_add_copy))
        }
        TextButton(onClick = onCancel, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.cancel))
        }
    }
}
