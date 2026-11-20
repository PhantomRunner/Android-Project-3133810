package com.example.cyberpunktcg.data.local.collection

import com.example.cyberpunktcg.data.model.CardColor
import com.example.cyberpunktcg.data.model.CardType
import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionStatisticsTest {
    @Test
    fun emptyCollectionHasZeroStatistics() {
        assertEquals(CollectionStatistics(0L, 0, 0), calculateCollectionStatistics(emptyList()))
    }

    @Test
    fun catalogEntriesWithoutPositiveOwnershipDoNotCount() {
        val printings = listOf(printing("unowned", null), printing("zero", 0), printing("invalid", -1))
        assertEquals(CollectionStatistics(0L, 0, 0), calculateCollectionStatistics(printings))
    }

    @Test
    fun editionsOfOneCardCountSeparatelyFromCopiesAndCards() {
        val printings = listOf(printing("paper", 2), printing("foil", 1))
        assertEquals(CollectionStatistics(3L, 1, 2), calculateCollectionStatistics(printings))
    }

    @Test
    fun distinctCardIdsCountEvenWhenNamesMatch() {
        val printings = listOf(printing("paper-a", 2, "card-a"), printing("paper-b", 1, "card-b"))
        assertEquals(CollectionStatistics(3L, 2, 2), calculateCollectionStatistics(printings))
    }

    @Test
    fun removingLastCopyRemovesEditionAndOnlyRemovesCardWhenNoOtherEditionIsOwned() {
        val paper = printing("paper", 1)
        val foil = printing("foil", 1)
        assertEquals(CollectionStatistics(2L, 1, 2), calculateCollectionStatistics(listOf(paper, foil)))
        val removedPaper = paper.copy(owned = null)
        assertEquals(CollectionStatistics(1L, 1, 1), calculateCollectionStatistics(listOf(removedPaper, foil)))
        assertEquals(
            CollectionStatistics(0L, 0, 0),
            calculateCollectionStatistics(listOf(removedPaper, foil.copy(owned = null))),
        )
    }

    @Test
    fun totalCopiesDoNotOverflowInt() {
        val printings = listOf(printing("paper", Int.MAX_VALUE), printing("foil", Int.MAX_VALUE))
        assertEquals(CollectionStatistics(4_294_967_294L, 1, 2), calculateCollectionStatistics(printings))
    }

    private fun printing(id: String, quantity: Int?, cardId: String = "signal"): CollectionPrinting = CollectionPrinting(
        printing = CardPrintingEntity(id, cardId, "test", "001", null, null, null),
        card = CardEntity(
            id = cardId,
            name = "Signal Relay",
            subtitle = "",
            type = CardType.GEAR,
            color = CardColor.BLUE,
            cost = 1,
            power = null,
            ram = 1,
            unitTypes = emptyList(),
            eddies = null,
            powerLabel = null,
        ),
        cardSet = CardSetEntity("test", "Synthetic Set", "en"),
        owned = quantity?.let { OwnedPrintingEntity(id, it) },
    )
}
