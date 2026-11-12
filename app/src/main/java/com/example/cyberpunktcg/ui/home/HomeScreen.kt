package com.example.cyberpunktcg.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cyberpunktcg.R
import com.example.cyberpunktcg.data.model.Card
import com.example.cyberpunktcg.data.model.Deck

@Composable
internal fun HomeScreen(
    deckCount: Int,
    recentDecks: List<Deck>,
    legendsByDeck: Map<String, List<Card>>,
    onBrowseCollection: () -> Unit,
    onOpenDecks: () -> Unit,
    onViewAllDecks: () -> Unit,
    onOpenDeck: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        HomeStatistics()
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onBrowseCollection, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_browse_collection))
            }
            OutlinedButton(onClick = onOpenDecks, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.my_decks))
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.home_your_decks, deckCount),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.deck_storage_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (recentDecks.isEmpty()) {
                Text(stringResource(R.string.no_decks_yet))
                TextButton(onClick = onViewAllDecks, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_go_to_my_decks))
                }
            } else {
                recentDecks.forEach { deck ->
                    key(deck.id) {
                        HomeDeckCard(
                            deck = deck,
                            legends = legendsByDeck[deck.id].orEmpty(),
                            onClick = { onOpenDeck(deck.id) },
                        )
                    }
                }
                TextButton(onClick = onViewAllDecks, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_view_all_decks))
                }
            }
        }
    }
}

@Composable
private fun HomeStatistics() {
    val minimumStatisticWidth = 100.dp * LocalDensity.current.fontScale
    val labels = listOf(R.string.home_total_cards, R.string.home_unique_cards, R.string.home_complete_sets)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= minimumStatisticWidth * labels.size + 24.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    labels.forEach { label ->
                        HomeStatistic(label, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    labels.forEach { label ->
                        HomeStatistic(label, Modifier.fillMaxWidth())
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.home_statistics_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HomeStatistic(@StringRes labelResId: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Owned quantities and set completion are not available in the demo catalog.
            Text(stringResource(R.string.home_statistic_placeholder), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(labelResId),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}
