package com.example.cyberpunktcg.ui.decks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.cyberpunktcg.R
import com.example.cyberpunktcg.data.model.Card
import com.example.cyberpunktcg.data.model.Deck
import com.example.cyberpunktcg.domain.DeckValidation

// Displays the current deck and sends edits, navigation and save actions back through callbacks.
@Composable
internal fun DeckEditorScreen(
    deck: Deck?,
    validation: DeckValidation?,
    onBack: () -> Unit,
    legends: List<Card>,
    ownedLegends: List<Card>,
    ownedQuantity: (String) -> Int,
    mainCards: List<Card>,
    isSaving: Boolean,
    onAddCards: () -> Unit,
    onPreview: () -> Unit,
    onOpenCard: (String) -> Unit,
    hasUnsavedChanges: Boolean,
    onNameChange: (String) -> Unit,
    canAddLegend: (String) -> Boolean,
    onAddLegend: (String) -> Unit,
    onRemoveLegend: (String) -> Unit,
    onSaveDraft: () -> Unit,
    onBackToDecks: () -> Unit,
) {
    // Use the same unsaved-changes check for Android Back and the top bar's back button.
    BackHandler(onBack = onBack)
    if (deck == null) {
        DeckNotFoundScreen(onBackToDecks)
        return
    }
    val focusManager = LocalFocusManager.current
    // Keep these display choices across rotation and reset them if the editor switches decks.
    var showLegendPicker by rememberSaveable(deck.id) { mutableStateOf(false) }
    var showChecks by rememberSaveable(deck.id) { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    // A deck stores IDs and counts. Look up the card details needed to draw the tiles.
    val selectedMain = deck.items.mapNotNull { item ->
        mainCards.firstOrNull { it.id == item.cardId }?.let { card -> card to item.quantity }
    }
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val spacing = 12.dp
        val columns = ((maxWidth - 32.dp + spacing) / (136.dp * fontScale + spacing)).toInt().coerceIn(1, 4)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            state = gridState,
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            item(key = "editor_header", span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = deck.name,
                        onValueChange = onNameChange,
                        label = { Text(stringResource(R.string.deck_name)) },
                        singleLine = true,
                        isError = deck.name.isBlank(),
                        supportingText = if (deck.name.isBlank()) {
                            { Text(stringResource(R.string.deck_name_required)) }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onSaveDraft()
                            },
                            enabled = deck.name.isNotBlank() && hasUnsavedChanges && !isSaving,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        ) { Text(stringResource(R.string.save_draft)) }
                        OutlinedButton(
                            onClick = {
                                focusManager.clearFocus()
                                onPreview()
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        ) { Text(stringResource(R.string.deck_preview)) }
                    }
                    Text(
                        stringResource(if (hasUnsavedChanges) R.string.unsaved_changes else R.string.draft_saved),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (validation != null) {
                item(key = "checks_toggle", span = { GridItemSpan(maxLineSpan) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(if (validation.hasWarnings) R.string.deck_checks_attention else R.string.deck_no_basic_warnings),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (validation.hasWarnings) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = { showChecks = !showChecks }) {
                            Text(stringResource(if (showChecks) R.string.hide_deck_checks else R.string.show_deck_checks))
                        }
                    }
                }
                if (showChecks) {
                    item(key = "checks_details", span = { GridItemSpan(maxLineSpan) }) {
                        DeckValidationSummary(validation)
                    }
                }
            }
            item(key = "legends_heading", span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.deck_legend_count, deck.legendIds.size), style = MaterialTheme.typography.titleMedium)
            }
            item(key = "legend_slots", span = { GridItemSpan(maxLineSpan) }) {
                DeckEditorLegendSlots(
                    legendIds = deck.legendIds,
                    legends = legends,
                    onChooseLegend = { showLegendPicker = true },
                    onRemoveLegend = onRemoveLegend,
                    onOpenCard = onOpenCard,
                )
            }
            item(key = "main_heading", span = { GridItemSpan(maxLineSpan) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.deck_main_count, deck.mainCardCount),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Button(onClick = {
                        focusManager.clearFocus()
                        onAddCards()
                    }) { Text(stringResource(R.string.add_cards)) }
                }
            }
            if (selectedMain.isEmpty()) {
                item(key = "no_main", span = { GridItemSpan(maxLineSpan) }) {
                    Text(stringResource(R.string.no_main_cards))
                }
            }
            items(
                items = selectedMain.chunked(columns),
                key = { row -> "main_${row.first().first.id}" },
                span = { GridItemSpan(maxLineSpan) },
            ) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    row.forEach { (card, quantity) ->
                        key(card.id) {
                            DeckEditorMainCard(
                                card = card,
                                quantity = quantity,
                                validation = validation,
                                onClick = { onOpenCard(card.id) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
    if (showLegendPicker) {
        DeckEditorLegendPicker(
            ownedLegends = ownedLegends,
            ownedQuantity = ownedQuantity,
            canAddLegend = canAddLegend,
            onSelectLegend = { id ->
                onAddLegend(id)
                showLegendPicker = false
            },
            onDismiss = { showLegendPicker = false },
        )
    }
}
