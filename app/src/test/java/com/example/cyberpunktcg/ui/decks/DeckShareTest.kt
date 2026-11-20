package com.example.cyberpunktcg.ui.decks

import com.example.cyberpunktcg.data.model.Deck
import com.example.cyberpunktcg.data.model.DeckItem
import com.example.cyberpunktcg.data.sample.DemoCards
import org.junit.Assert.assertEquals
import org.junit.Test

class DeckShareTest {
    @Test
    fun sharesNamesQuantitiesAndSavedOrder() {
        val card = DemoCards.cards.first()
        val cards = listOf(
            card.copy(id = "legend-a", name = "Relay Guide", subtitle = "Off Shift"),
            card.copy(id = "legend-b", name = "Switch Runner", subtitle = ""),
            card.copy(id = "main-a", name = "Canal Watch", subtitle = ""),
            card.copy(id = "main-b", name = "Patch Panel", subtitle = ""),
        ).associateBy { it.id }
        val deck = Deck(
            id = "deck",
            name = "Training deck",
            legendIds = listOf("legend-b", "legend-a"),
            items = listOf(DeckItem("main-b", 3), DeckItem("main-a", 2)),
        )

        assertEquals(
            """
            Training deck

            Legends (2)
            1 × Switch Runner
            1 × Relay Guide — Off Shift

            Main (5)
            3 × Patch Panel
            2 × Canal Watch
            """.trimIndent(),
            deckShareText(deck, cards::get),
        )
    }

    @Test
    fun sharesAnEmptySectionAndKeepsMissingCardReferencesReadable() {
        val deck = Deck("deck", "Draft", items = listOf(DeckItem("missing", 1)))

        assertEquals(
            "Draft\n\nLegends (0)\nNone\n\nMain (1)\n1 × Unknown card (missing)",
            deckShareText(deck) { null },
        )
    }
}
