package com.example.cyberpunktcg.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cyberpunktcg.R
import com.example.cyberpunktcg.data.model.Card
import com.example.cyberpunktcg.ui.theme.frameColor

@Composable
internal fun CardPreviewTile(card: Card, onClick: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val border = BorderStroke(2.dp, card.color.frameColor)
    val colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    if (onClick == null) {
        OutlinedCard(modifier = modifier, border = border, colors = colors) { PreviewContent(card) }
    } else {
        OutlinedCard(onClick = onClick, modifier = modifier, border = border, colors = colors) { PreviewContent(card) }
    }
}

@Composable
private fun PreviewContent(card: Card) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val alignRows = maxWidth < 240.dp
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardArtwork(card.imagePath, Modifier.fillMaxWidth().height(132.dp))
            Text(
                card.name,
                style = MaterialTheme.typography.titleSmall,
                minLines = if (alignRows) 2 else 1,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (card.subtitle.isNotBlank() || alignRows) {
                Text(
                    card.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(stringResource(card.type.labelResId), style = MaterialTheme.typography.labelMedium)
            FlowRow(
                modifier = if (alignRows) Modifier.heightIn(min = 36.dp * LocalDensity.current.fontScale) else Modifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(stringResource(R.string.card_cost, card.cost.cardStat()), style = MaterialTheme.typography.labelMedium)
                Text(stringResource(R.string.card_power, card.powerLabel ?: card.power.cardStat()), style = MaterialTheme.typography.labelMedium)
                Text(stringResource(R.string.card_ram, card.ram.cardStat()), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
