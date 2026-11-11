package com.example.cyberpunktcg.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cyberpunktcg.R
import com.example.cyberpunktcg.data.model.Card
import com.example.cyberpunktcg.ui.theme.frameColor

@Composable
internal fun CardTile(card: Card, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier, border = BorderStroke(2.dp, card.color.frameColor)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(card.name, style = MaterialTheme.typography.titleLarge)
            if (card.subtitle.isNotBlank()) Text(card.subtitle, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(card.type.labelResId))
            Text(stringResource(card.color.labelResId))
            Text(stringResource(R.string.card_cost, card.cost.cardStat()))
            Text(stringResource(R.string.card_power, card.powerLabel ?: card.power.cardStat()))
            Text(stringResource(R.string.card_ram, card.ram.cardStat()))
            if (card.unitTypes.isNotEmpty()) Text(card.unitTypes.joinToString(" • "))
            if (card.eddies == true) Text(stringResource(R.string.card_sell_indicator))
            if (card.description.isNotBlank()) Text(card.description)
            CardArtwork(card.imagePath, Modifier.fillMaxWidth().aspectRatio(0.72f))
        }
    }
}
