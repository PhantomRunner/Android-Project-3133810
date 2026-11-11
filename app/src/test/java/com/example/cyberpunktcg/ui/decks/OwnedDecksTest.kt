package com.example.cyberpunktcg.ui.decks

import com.example.cyberpunktcg.data.model.CardType
import com.example.cyberpunktcg.data.sample.DemoCards
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class OwnedDecksTest {
    @get:Rule val modelRule = DecksTestRule()

    @Test
    fun collectionChangesLimitAdditionsAndKeepSavedDeckReferences() = runTest {
        val legend = DemoCards.cards.first().copy(id = "owned-legend", ram = null)
        val unit = DemoCards.cards.last().copy(id = "owned-main", type = CardType.UNIT, color = legend.color, ram = null)
        val unowned = unit.copy(id = "unowned", name = "Unowned")
        val repository = MemoryDeckRepository(listOf(legend, unit, unowned))
        repository.catalog.value = repository.catalog.value.copy(ownedQuantities = mapOf(legend.id to 1, unit.id to 1))
        val model = modelRule.createModel(repository)
        val deck = model.createDeck("Owned deck")!!
        assertEquals(listOf(legend), model.ownedLegends)
        assertEquals(listOf(unit), model.selectableMainCards(deck.id))
        model.addLegend(deck.id, legend.id)
        model.addCard(deck.id, unit.id)
        assertFalse(model.canAddCard(deck.id, unit.id))
        assertTrue(model.validation(deck.id)!!.hasUnknownRam)
        assertTrue(model.saveDraft(deck.id))

        repository.catalog.value = repository.catalog.value.copy(ownedQuantities = emptyMap())
        assertTrue(model.ownedLegends.isEmpty())
        assertFalse(model.canAddLegend(deck.id, legend.id))
        assertEquals(listOf(unit), model.selectableMainCards(deck.id))
        assertEquals(2, model.validation(deck.id)!!.ownershipWarnings.size)
        assertEquals(listOf(legend.id), model.findDeck(deck.id)!!.legendIds)
        val reopened = modelRule.createModel(repository)
        assertEquals(model.findDeck(deck.id), reopened.findDeck(deck.id))
    }

    @Test
    fun failedSaveKeepsLastSavedDeckAndUnsavedChanges() = runTest {
        val repository = MemoryDeckRepository(DemoCards.cards)
        val model = modelRule.createModel(repository)
        val deck = model.createDeck("Original")!!
        model.renameDeck(deck.id, "Edited")
        repository.beforeSave = { throw IOException("Test write failure") }
        assertFalse(model.saveDraft(deck.id))
        assertTrue(model.writeFailed)
        assertTrue(model.hasUnsavedChanges(deck.id))
        assertEquals("Original", model.findDeck(deck.id)!!.name)
    }

    @Test
    fun editsMadeDuringSaveRemainUnsavedAfterTheSnapshotIsWritten() = runTest {
        val repository = MemoryDeckRepository(DemoCards.cards)
        val model = modelRule.createModel(repository)
        val deck = model.createDeck("Original")!!
        model.renameDeck(deck.id, "First edit")
        val saving = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        repository.beforeSave = {
            saving.complete(Unit)
            finish.await()
        }
        val save = async { model.saveDraft(deck.id) }
        saving.await()
        model.renameDeck(deck.id, "Later edit")
        finish.complete(Unit)
        assertTrue(save.await())
        assertEquals("First edit", model.findDeck(deck.id)!!.name)
        assertEquals("Later edit", model.editingDeck(deck.id)!!.name)
        assertTrue(model.hasUnsavedChanges(deck.id))
    }
}
