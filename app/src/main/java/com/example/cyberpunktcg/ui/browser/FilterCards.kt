package com.example.cyberpunktcg.ui.browser

import com.example.cyberpunktcg.data.model.BrowsableCard

// A card must match the search and both filter groups. An empty group allows all its values.
internal fun <T : BrowsableCard> filterCards(cards: List<T>, state: CardBrowserState): List<T> {
    val query = state.query.trim()
    return cards.filter { card ->
        val matchesName = card.name.contains(query, ignoreCase = true)
        val matchesType = state.selectedTypes.isEmpty() || card.type in state.selectedTypes
        val matchesColor = state.selectedColors.isEmpty() || card.color in state.selectedColors
        matchesName && matchesType && matchesColor
    }
}
