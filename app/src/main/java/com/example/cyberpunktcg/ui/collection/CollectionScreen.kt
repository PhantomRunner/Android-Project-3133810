package com.example.cyberpunktcg.ui.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cyberpunktcg.R
import com.example.cyberpunktcg.ui.browser.CardBrowser
import com.example.cyberpunktcg.ui.browser.CardBrowserViewModel

@Composable
internal fun CollectionScreen(
    state: CollectionUiState,
    browser: CardBrowserViewModel,
    onAddCards: () -> Unit,
    onOpenPrinting: (String) -> Unit,
    onChangeQuantity: (String, Int) -> Unit,
    onRetry: () -> Unit,
) {
    val printings = state.printings
    if (state.readFailed) {
        CollectionReadError(onRetry)
        return
    }
    if (printings == null) {
        CollectionLoading()
        return
    }
    val owned = remember(printings) { printings.filter { it.quantity > 0 }.groupBy { it.card.id } }
    if (owned.isEmpty()) {
        CollectionMessage(stringResource(R.string.stash_empty)) {
            Button(onClick = onAddCards) { Text(stringResource(R.string.add_cards)) }
        }
        return
    }

    val visible = browser.visibleCards(owned.values.map { it.first().card })
        .flatMap { owned.getValue(it.id) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = {
                keyboard?.hide()
                focusManager.clearFocus()
                onAddCards()
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Text(stringResource(R.string.add_cards))
        }
        if (state.saveFailed) {
            QuantitySaveError(Modifier.padding(horizontal = 16.dp))
        }
        CardBrowser(
            cards = visible,
            itemKey = { it.printing.id },
            state = browser.uiState,
            onQueryChange = browser::setQuery,
            onTypesChange = browser::setSelectedTypes,
            onColorsChange = browser::setSelectedColors,
            onSortFieldChange = browser::setSortField,
            onSortDirectionChange = browser::setSortDirection,
        ) { printing ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CollectionCardPreview(printing, onClick = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                    onOpenPrinting(printing.printing.id)
                })
                PrintingLabel(printing)
                OwnedQuantityControls(printing, onChangeQuantity)
            }
        }
    }
}

@Composable
internal fun CollectionLoading() {
    CollectionMessage(stringResource(R.string.collection_loading)) { CircularProgressIndicator() }
}

@Composable
internal fun CollectionReadError(onRetry: () -> Unit) {
    CollectionMessage(stringResource(R.string.collection_read_failed)) {
        Button(onClick = onRetry) { Text(stringResource(R.string.collection_retry)) }
    }
}
