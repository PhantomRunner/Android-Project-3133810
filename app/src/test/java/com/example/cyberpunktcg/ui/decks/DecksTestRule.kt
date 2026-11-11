package com.example.cyberpunktcg.ui.decks

import androidx.lifecycle.ViewModelStore
import com.example.cyberpunktcg.data.local.decks.DeckCatalog
import com.example.cyberpunktcg.data.local.decks.DeckRepository
import com.example.cyberpunktcg.data.model.Card
import com.example.cyberpunktcg.data.model.Deck
import com.example.cyberpunktcg.data.sample.DemoCards
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class DecksTestRule : TestWatcher() {
    private val store = ViewModelStore()
    private var nextId = 0

    override fun starting(description: Description) {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    override fun finished(description: Description) {
        store.clear()
        Dispatchers.resetMain()
    }

    internal fun createModel(cards: List<Card> = DemoCards.cards): DecksViewModel = createModel(MemoryDeckRepository(cards))

    internal fun createModel(repository: DeckRepository): DecksViewModel = DecksViewModel(repository).also {
        store.put("decks-${nextId++}", it)
    }
}

internal class MemoryDeckRepository(cards: List<Card>) : DeckRepository {
    override val decks = MutableStateFlow<List<Deck>>(emptyList())
    override val catalog = MutableStateFlow(DeckCatalog(cards, cards.associate { it.id to 3 }))
    var beforeSave: suspend () -> Unit = {}

    override suspend fun loadCatalog() = Unit

    override suspend fun createDeck(deck: Deck) {
        decks.value += deck
    }

    override suspend fun saveDeck(deck: Deck): Boolean {
        beforeSave()
        if (decks.value.none { it.id == deck.id }) return false
        decks.value = decks.value.map { if (it.id == deck.id) deck else it }
        return true
    }

    override suspend fun deleteDeck(deckId: String) {
        decks.value = decks.value.filterNot { it.id == deckId }
    }
}
