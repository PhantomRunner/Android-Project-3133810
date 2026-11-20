package com.example.cyberpunktcg.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cyberpunktcg.data.model.Card

@Composable
internal fun LegendArtworkPreview(card: Card, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CardArtworkFrame(card.imagePath, card.color, Modifier.fillMaxWidth())
        Text(text = card.name, style = MaterialTheme.typography.labelLarge)
    }
}
