package com.example.cyberpunktcg.ui.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cyberpunktcg.R
import com.example.cyberpunktcg.data.local.collection.CardEntity
import com.example.cyberpunktcg.data.local.collection.CollectionPrinting
import com.example.cyberpunktcg.ui.browser.CardBrowser
import com.example.cyberpunktcg.ui.browser.CardBrowserViewModel

@Composable
internal fun AddCollectionCardsScreen(
    state: CollectionUiState,
    browser: CardBrowserViewModel,
    onChangeQuantity: (String, Int) -> Unit,
    onRetry: () -> Unit,
) {
    if (state.readFailed) {
        CollectionReadError(onRetry)
        return
    }
    val catalog = when (val status = state.catalog) {
        CatalogStatus.Loading -> {
            CollectionLoading()
            return
        }
        CatalogStatus.Missing -> {
            CollectionMessage(stringResource(R.string.collection_catalog_missing))
            return
        }
        CatalogStatus.Failed -> {
            CollectionMessage(stringResource(R.string.collection_catalog_failed)) {
                Button(onClick = onRetry) { Text(stringResource(R.string.collection_retry)) }
            }
            return
        }
        is CatalogStatus.Ready -> status
    }
    val printings = remember(state.printings, catalog) {
        state.printings.orEmpty().filter { it.printing.id in catalog.printingIds }
    }
    // The Room query may emit the imported rows after the import itself returns.
    if (printings.size < catalog.printingIds.size) {
        CollectionLoading()
        return
    }

    val byCard = remember(printings) { printings.groupBy { it.card.id } }
    var selectedCardId by rememberSaveable { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    CardBrowser(
        cards = browser.visibleCards(byCard.values.map { it.first().card }),
        itemKey = CardEntity::id,
        state = browser.uiState,
        onQueryChange = browser::setQuery,
        onTypesChange = browser::setSelectedTypes,
        onColorsChange = browser::setSelectedColors,
        onSortFieldChange = browser::setSortField,
        onSortDirectionChange = browser::setSortDirection,
    ) { card ->
        CollectionCardPreview(
            printing = byCard.getValue(card.id).first(),
            actionLabel = stringResource(R.string.collection_choose_edition),
            onClick = {
                keyboard?.hide()
                focusManager.clearFocus()
                selectedCardId = card.id
            },
        )
    }

    byCard[selectedCardId]?.let { editions ->
        EditionPicker(
            editions = editions,
            saveFailed = state.saveFailed,
            onChangeQuantity = onChangeQuantity,
            onDismiss = { selectedCardId = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditionPicker(
    editions: List<CollectionPrinting>,
    saveFailed: Boolean,
    onChangeQuantity: (String, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val card = editions.first().card
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(stringResource(R.string.collection_choose_edition), style = MaterialTheme.typography.titleLarge)
                Text(card.name, style = MaterialTheme.typography.titleMedium)
                if (card.subtitle.isNotBlank()) Text(card.subtitle)
                if (saveFailed) QuantitySaveError()
            }
            items(editions, key = { it.printing.id }) { printing ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PrintingLabel(printing, includeVariantAndFinish = true)
                        OwnedQuantityControls(printing, onChangeQuantity)
                    }
                }
            }
        }
    }
}
