package com.example.cyberpunktcg.ui.decks

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cyberpunktcg.R
import com.example.cyberpunktcg.data.model.Card
import com.example.cyberpunktcg.data.model.Deck
import com.example.cyberpunktcg.data.model.DeckItem
import com.example.cyberpunktcg.sensors.ShakeEffect
import com.example.cyberpunktcg.ui.components.CardBrowserPreview
import kotlinx.coroutines.flow.Flow

@Composable
internal fun DeckPreviewScreen(
    deck: Deck?,
    legends: List<Card>,
    mainCards: List<Card>,
    previewItems: List<DeckItem>,
    shakeEvents: Flow<Unit>,
    accelerometerAvailable: Boolean,
    onShuffle: () -> Unit,
    onOpenCard: (String) -> Unit,
    onBackToDecks: () -> Unit,
) {
    if (deck == null) {
        DeckNotFoundScreen(onBackToDecks)
        return
    }
    val selectedLegends = deck.legendIds.mapNotNull { id -> legends.firstOrNull { it.id == id } }
    val selectedMain = previewItems.mapNotNull { item ->
        mainCards.firstOrNull { it.id == item.cardId }?.let { card -> card to item.quantity }
    }
    ShakeEffect(
        events = shakeEvents,
        enabled = accelerometerAvailable && selectedMain.size > 1,
        onShake = onShuffle,
    )
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val spacing = 12.dp
        val columnCount = ((maxWidth - 32.dp + spacing) / (150.dp * fontScale + spacing)).toInt().coerceAtLeast(1)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
            modifier = Modifier.fillMaxSize(),
            state = rememberLazyGridState(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "preview_heading", span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (deck.name.isBlank()) stringResource(R.string.unnamed_draft) else deck.name,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(stringResource(R.string.deck_draft), style = MaterialTheme.typography.labelLarge)
                    Text(stringResource(R.string.deck_preview_notice))
                    Text(stringResource(when {
                        !accelerometerAvailable -> R.string.shake_unavailable
                        selectedMain.size < 2 -> R.string.shake_preview_needs_cards
                        else -> R.string.shake_shuffle_preview
                    }))
                }
            }
            item(key = "legends_heading", span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.deck_legend_count, deck.legendIds.size), style = MaterialTheme.typography.titleMedium)
            }
            if (selectedLegends.isEmpty()) {
                item(key = "no_legends", span = { GridItemSpan(maxLineSpan) }) {
                    Text(stringResource(R.string.no_legends_selected))
                }
            }
            items(selectedLegends, key = { "legend_${it.id}" }) { card ->
                DeckPreviewCard(card, onOpenCard)
            }
            item(key = "main_heading", span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.deck_main_count, deck.mainCardCount), style = MaterialTheme.typography.titleMedium)
            }
            if (selectedMain.isEmpty()) {
                item(key = "no_main", span = { GridItemSpan(maxLineSpan) }) {
                    Text(stringResource(R.string.no_main_cards))
                }
            }
            items(
                items = selectedMain.chunked(columnCount),
                key = { row -> "main_${row.first().first.id}" },
                span = { GridItemSpan(maxLineSpan) },
            ) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    row.forEach { (card, quantity) ->
                        key(card.id) {
                            Column(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                DeckPreviewCard(card, onOpenCard)
                                Spacer(Modifier.weight(1f))
                                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                                    Text(stringResource(R.string.deck_card_copies, quantity), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                    repeat(columnCount - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun DeckPreviewCard(card: Card, onOpenCard: (String) -> Unit) {
    CardBrowserPreview(
        card = card,
        subtitle = card.subtitle,
        imagePath = card.imagePath,
        powerLabel = card.powerLabel,
        onClick = { onOpenCard(card.id) },
    )
}
