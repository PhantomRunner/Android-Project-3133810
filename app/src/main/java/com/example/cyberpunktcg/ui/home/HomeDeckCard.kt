package com.example.cyberpunktcg.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cyberpunktcg.R
import com.example.cyberpunktcg.data.model.Card
import com.example.cyberpunktcg.data.model.Deck
import com.example.cyberpunktcg.ui.components.LegendArtworkPreview

@Composable
internal fun HomeDeckCard(deck: Deck, legends: List<Card>, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = deck.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(stringResource(R.string.deck_draft), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.deck_main_count, deck.mainCardCount), modifier = Modifier.weight(1f))
                Text(stringResource(R.string.deck_legend_count, deck.legendIds.size), modifier = Modifier.weight(1f))
            }
            if (legends.isEmpty()) {
                Text(stringResource(R.string.no_legends_selected), style = MaterialTheme.typography.bodySmall)
            } else {
                val shownLegends = legends.take(3)
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    shownLegends.forEach { card ->
                        LegendArtworkPreview(card, Modifier.weight(1f).fillMaxHeight())
                    }

                    repeat(3 - shownLegends.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
