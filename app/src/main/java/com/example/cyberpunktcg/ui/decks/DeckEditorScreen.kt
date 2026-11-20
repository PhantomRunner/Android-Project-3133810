package com.example.cyberpunktcg.ui.decks

import androidx.compose.foundation.BorderStroke
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cyberpunktcg.R
import com.example.cyberpunktcg.data.model.Card
import com.example.cyberpunktcg.data.model.Deck
import com.example.cyberpunktcg.domain.DeckValidation
import com.example.cyberpunktcg.ui.components.labelResId
import com.example.cyberpunktcg.ui.theme.frameColor

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

    val focusManager = LocalFocusManager.current
    BackHandler(onBack = onBack)
    var showLegendPicker by rememberSaveable(deck?.id) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (deck == null) {
            Text(stringResource(R.string.deck_not_found))
            Button(onClick = onBackToDecks) {
                Text(stringResource(R.string.back_to_my_decks))
            }
        } else {
            Text(stringResource(R.string.deck_draft), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.deck_storage_notice))
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
            )
            Button(
                onClick = onSaveDraft,
                enabled = deck.name.isNotBlank() && hasUnsavedChanges && !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save_draft)) }
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    onPreview()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.deck_preview)) }
            Text(stringResource(if (hasUnsavedChanges) R.string.unsaved_changes else R.string.draft_saved))
            if (validation != null) {
                DeckValidationSummary(validation)
            }
            Text(stringResource(R.string.deck_legend_count, deck.legendIds.size))
            DeckEditorLegendSlots(
                legendIds = deck.legendIds,
                legends = legends,
                onChooseLegend = { showLegendPicker = true },
                onRemoveLegend = onRemoveLegend,
                onOpenCard = onOpenCard,
            )
            Text(stringResource(R.string.deck_main_count, deck.mainCardCount))
            Button(onClick = onAddCards, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.add_cards))
            }
            if (deck.items.isEmpty()) {
                Text(stringResource(R.string.no_main_cards))
            }
            deck.items.forEach { item ->
                val card = mainCards.firstOrNull { it.id == item.cardId }
                if (card != null) {
                    OutlinedCard(modifier = Modifier.fillMaxWidth(), border = BorderStroke(2.dp, card.color.frameColor)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(card.name, style = MaterialTheme.typography.titleMedium)
                            Text(card.subtitle)
                            Text(stringResource(card.type.labelResId))
                            Text(stringResource(R.string.deck_card_copies, item.quantity))
                            if (validation != null && card.color !in validation.allowedColors) {
                                CardColorWarningText(card)
                            }
                            validation?.ramWarnings?.firstOrNull { it.cardId == card.id }?.let { warning ->
                                CardRamWarningText(warning)
                            }
                        }
                    }
                }
            }
        }
    }
    if (showLegendPicker && deck != null) {
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
