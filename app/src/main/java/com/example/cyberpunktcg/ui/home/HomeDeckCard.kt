package com.example.cyberpunktcg.ui.home

import androidx.compose.runtime.Composable
import com.example.cyberpunktcg.data.model.Card
import com.example.cyberpunktcg.data.model.Deck
import com.example.cyberpunktcg.ui.components.DeckSummaryCard

@Composable
internal fun HomeDeckCard(deck: Deck, legends: List<Card>, onClick: () -> Unit) {
    DeckSummaryCard(deck = deck, legends = legends, onClick = onClick)
}
