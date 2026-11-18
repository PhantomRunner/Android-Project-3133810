package com.example.cyberpunktcg.ui.decks

import com.example.cyberpunktcg.data.model.CardType
import com.example.cyberpunktcg.data.model.Card
import com.example.cyberpunktcg.data.sample.DemoCards
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DeckSensorActionsTest {
    @get:Rule val rule = DecksTestRule()

    private val legend: Card = DemoCards.cards.first()
    private val first: Card = DemoCards.cards.last().copy(id = "first", name = "First", type = CardType.UNIT, color = legend.color)
    private val second: Card = first.copy(id = "second", name = "Second")
    private val third: Card = first.copy(id = "third", name = "Third")

    @Test
    fun previewShuffleChangesOrderWithoutEditingOrSavingTheDeck() = runTest {
        val repository = MemoryDeckRepository(listOf(legend, first, second))
        val model = rule.createModel(repository)
        val deck = model.createDeck("Preview")!!
        model.addLegend(deck.id, legend.id)
        model.addCard(deck.id, first.id)
        model.addCard(deck.id, first.id)
        model.addCard(deck.id, second.id)
        assertTrue(model.saveDraft(deck.id))
        val saved = model.findDeck(deck.id)!!

        repeat(5) {
            val before = model.previewItems(deck.id)
            model.shufflePreview(deck.id)
            assertNotEquals(before, model.previewItems(deck.id))
            assertEquals(saved.items.toSet(), model.previewItems(deck.id).toSet())
            assertEquals(saved, model.editingDeck(deck.id))
            assertEquals(saved, repository.decks.value.single())
            assertFalse(model.hasUnsavedChanges(deck.id))
        }
    }

    @Test
    fun editingMainCardsResetsThePreviewAndKeepsTheSavedSnapshot() = runTest {
        val model = rule.createModel(listOf(legend, first, second, third))
        val deck = model.createDeck("Working copy")!!
        model.addLegend(deck.id, legend.id)
        model.addCard(deck.id, first.id)
        model.addCard(deck.id, second.id)
        assertTrue(model.saveDraft(deck.id))
        val saved = model.findDeck(deck.id)
        model.shufflePreview(deck.id)
        model.addCard(deck.id, third.id)

        assertEquals(model.editingDeck(deck.id)!!.items, model.previewItems(deck.id))
        model.shufflePreview(deck.id)
        assertTrue(model.hasUnsavedChanges(deck.id))
        assertEquals(saved, model.findDeck(deck.id))
        model.discardChanges(deck.id)
        assertEquals(saved!!.items, model.previewItems(deck.id))
    }

    @Test
    fun emptySingleCardAndMissingDeckPreviewsAreSafe() = runTest {
        val model = rule.createModel(listOf(legend, first))
        model.shufflePreview("missing")
        assertTrue(model.previewItems("missing").isEmpty())
        val deck = model.createDeck("One card")!!
        model.shufflePreview(deck.id)
        assertTrue(model.previewItems(deck.id).isEmpty())
        model.addLegend(deck.id, legend.id)
        model.addCard(deck.id, first.id)
        val before = model.previewItems(deck.id)
        model.shufflePreview(deck.id)
        assertEquals(before, model.previewItems(deck.id))
    }

    @Test
    fun randomSelectionUsesOnlySavedDeckIdsAndHandlesAnEmptyList() = runTest {
        val model = rule.createModel()
        assertNull(model.randomSavedDeckId())
        val first = model.createDeck("First deck")!!
        assertEquals(first.id, model.randomSavedDeckId())
        val second = model.createDeck("Second deck")!!
        repeat(10) { assertTrue(model.randomSavedDeckId() in setOf(first.id, second.id)) }
        model.deleteDeck(first.id)
        assertEquals(second.id, model.randomSavedDeckId())
        model.deleteDeck(second.id)
        assertNull(model.randomSavedDeckId())
    }
}
