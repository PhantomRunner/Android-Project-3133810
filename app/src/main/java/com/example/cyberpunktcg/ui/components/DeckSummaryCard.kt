package com.example.cyberpunktcg.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cyberpunktcg.R
import com.example.cyberpunktcg.data.model.Card
import com.example.cyberpunktcg.data.model.Deck

@Composable
internal fun DeckSummaryCard(
    deck: Deck,
    legends: List<Card>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    OutlinedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (legends.isEmpty()) {
                Text(stringResource(R.string.no_legends_selected), style = MaterialTheme.typography.bodySmall)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    legends.take(3).forEach { legend ->
                        key(legend.id) { LegendArtworkPreview(legend, Modifier.weight(1f)) }
                    }
                }
            }
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
            if (onDelete != null) {
                val deleteLabel = stringResource(R.string.delete_named_deck, deck.name)
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.align(Alignment.End).semantics { contentDescription = deleteLabel },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.delete_deck))
                }
            }
        }
    }
}
