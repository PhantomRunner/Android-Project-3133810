package com.example.cyberpunktcg.ui.decks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cyberpunktcg.data.local.decks.DeckRepository
import com.example.cyberpunktcg.data.model.Card
import com.example.cyberpunktcg.data.model.CardType
import com.example.cyberpunktcg.data.model.Deck
import com.example.cyberpunktcg.data.model.DeckItem
import java.util.UUID
import com.example.cyberpunktcg.domain.DeckValidation
import com.example.cyberpunktcg.domain.countCardCopies
import com.example.cyberpunktcg.domain.validateDeck
import com.example.cyberpunktcg.domain.selectedLegendColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Gives Home and the deck screens a common view of saved decks, edits and card availability.
internal class DecksViewModel(private val repository: DeckRepository) : ViewModel() {
    var decks: List<Deck> by mutableStateOf(emptyList())
        private set

    // Keep a separate working copy per deck so switching screens or rotating keeps edits.
    // These copies stay in memory until saved to Room and are lost if the process ends.
    private var workingCopies: Map<String, Deck> by mutableStateOf(emptyMap())
    private var previewOrders: Map<String, List<String>> by mutableStateOf(emptyMap())

    private var cards: List<Card> by mutableStateOf(emptyList())
    private var ownedQuantities: Map<String, Int> by mutableStateOf(emptyMap())
    var isLoading by mutableStateOf(true)
        private set
    var loadFailed by mutableStateOf(false)
        private set
    var writeFailed by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set
    private var loadJob: Job? = null
    private val writes = Mutex()

    val legends: List<Card> get() = cards.filter { it.type == CardType.LEGEND }
    val mainCards: List<Card> get() = cards.filter { it.type != CardType.LEGEND }
    val ownedLegends: List<Card> get() = legends.filter { ownedQuantity(it.id) > 0 }

    init {
        retry()
    }

    fun retry() {
        if (loadJob?.isActive == true) return
        isLoading = true
        loadFailed = false
        // viewModelScope keeps this collection running across rotation and cancels it
        // when the ViewModel is cleared.
        loadJob = viewModelScope.launch {
            try {
                repository.loadCatalog()
                // Either flow can refresh the screen, including when ownership changes elsewhere.
                combine(repository.decks, repository.catalog) { saved, catalog -> saved to catalog }
                    .collect { (saved, catalog) ->
                        decks = saved
                        cards = catalog.cards
                        ownedQuantities = catalog.ownedQuantities
                        workingCopies = workingCopies.filterKeys { id -> saved.any { it.id == id } }
                        previewOrders = previewOrders.filterKeys { id -> saved.any { it.id == id } }
                        isLoading = false
                    }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                loadFailed = true
                isLoading = false
            }
        }
    }

    fun ownedQuantity(cardId: String): Int = ownedQuantities[cardId] ?: 0

    fun findCard(cardId: String?): Card? = cards.firstOrNull { it.id == cardId }

    // Keep selected cards visible even if ownership drops to zero, so they can still be removed.
    fun selectableMainCards(deckId: String?): List<Card> {
        val selected = editingDeck(deckId)?.items.orEmpty().map { it.cardId }.toSet()
        return mainCards.filter { ownedQuantity(it.id) > 0 || it.id in selected }
    }

    // Create an empty saved deck before the editor opens.
    suspend fun createDeck(name: String): Deck? {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty() || isLoading || loadFailed) {
            return null
        }

        val deck = Deck(id = UUID.randomUUID().toString(), name = trimmedName)
        val saved = write {
            repository.createDeck(deck)
            decks = decks.filterNot { it.id == deck.id } + deck
            true
        }
        return deck.takeIf { saved }
    }

    fun findDeck(deckId: String?): Deck? {
        return decks.firstOrNull { it.id == deckId }
    }

    fun randomSavedDeckId(): String? = decks.randomOrNull()?.id

    // A shuffle only changes the order shown in Preview. Reuse it while the same cards are present.
    fun previewItems(deckId: String?): List<DeckItem> {
        val deck = editingDeck(deckId) ?: return emptyList()
        val order = previewOrders[deck.id] ?: return deck.items
        if (order.toSet() != deck.items.map { it.cardId }.toSet()) return deck.items
        val positions = order.withIndex().associate { it.value to it.index }
        return deck.items.sortedBy { positions.getValue(it.cardId) }
    }

    fun shufflePreview(deckId: String?) {
        val deck = editingDeck(deckId) ?: return
        val current = previewItems(deckId).map { it.cardId }
        if (current.size < 2) return
        val shuffled = current.shuffled()
        // Guarantee visible feedback when random happens to produce the same order.
        val order = if (shuffled == current) current.drop(1) + current.first() else shuffled
        previewOrders = previewOrders + (deck.id to order)
    }

    // Show the working copy if one exists, or start from the saved deck.
    fun editingDeck(deckId: String?): Deck? {
        val saved = findDeck(deckId) ?: return null
        return workingCopies[deckId] ?: saved
    }

    fun hasUnsavedChanges(deckId: String?): Boolean {
        return editingDeck(deckId) != findDeck(deckId)
    }

    fun renameDeck(deckId: String, name: String) {
        val deck = editingDeck(deckId) ?: return
        workingCopies = workingCopies + (deckId to deck.copy(name = name))
    }

    // Each slot needs an owned Legend with a name that is still unused in the deck.
    fun canAddLegend(deckId: String, cardId: String): Boolean {
        val deck = editingDeck(deckId) ?: return false
        val legend = legends.firstOrNull { it.id == cardId } ?: return false
        if (ownedQuantity(cardId) == 0) return false
        val selectedNames = legends.filter { it.id in deck.legendIds }.map { it.name }
        return deck.legendIds.size < 3 && legend.name !in selectedNames
    }

    fun addLegend(deckId: String, cardId: String) {
        if (!canAddLegend(deckId, cardId)) return
        val deck = editingDeck(deckId) ?: return
        workingCopies = workingCopies + (deckId to deck.copy(legendIds = deck.legendIds + cardId))
    }

    fun removeLegend(deckId: String, cardId: String) {
        val deck = editingDeck(deckId) ?: return
        workingCopies = workingCopies + (deckId to deck.copy(legendIds = deck.legendIds - cardId))
    }

    fun cardQuantity(deckId: String, cardId: String): Int {
        return editingDeck(deckId)?.items?.firstOrNull { it.cardId == cardId }?.quantity ?: 0
    }

    // Adding a Main card needs a matching Legend color, room under the copy limit and an owned copy.
    fun canAddCard(deckId: String, cardId: String): Boolean {
        val deck = editingDeck(deckId) ?: return false
        val card = mainCards.firstOrNull { it.id == cardId } ?: return false
        if (card.color !in selectedLegendColors(deck, cards)) return false
        return countCardCopies(deck.items, cards, card) < 3 && cardQuantity(deckId, cardId) < ownedQuantity(cardId)
    }

    // Keep one Main entry per card ID. Adding another copy updates that entry's quantity.
    fun addCard(deckId: String, cardId: String) {
        if (!canAddCard(deckId, cardId)) return
        val deck = editingDeck(deckId) ?: return
        val quantity = cardQuantity(deckId, cardId)
        val items = if (quantity == 0) {
            deck.items + DeckItem(cardId, 1)
        } else {
            deck.items.map { if (it.cardId == cardId) it.copy(quantity = it.quantity + 1) else it }
        }
        workingCopies = workingCopies + (deckId to deck.copy(items = items))
        previewOrders = previewOrders - deckId
    }

    // Removing the last copy drops the entry and clears any preview order.
    fun removeCard(deckId: String, cardId: String) {
        val deck = editingDeck(deckId) ?: return
        val items = deck.items.mapNotNull { item ->
            when {
                item.cardId != cardId -> item
                item.quantity > 1 -> item.copy(quantity = item.quantity - 1)
                else -> null
            }
        }
        workingCopies = workingCopies + (deckId to deck.copy(items = items))
        previewOrders = previewOrders - deckId
    }

    fun validation(deckId: String?): DeckValidation? {
        val deck = editingDeck(deckId) ?: return null
        return validateDeck(deck, cards, ownedQuantities)
    }

    // Save a snapshot of the draft, so the editor can keep any newer edits made while saving.
    suspend fun saveDraft(deckId: String): Boolean {
        val deck = editingDeck(deckId) ?: return false
        val name = deck.name.trim()
        if (name.isEmpty()) return false
        val saved = deck.copy(name = name)
        return write {
            if (!repository.saveDeck(saved)) return@write false
            decks = decks.map { if (it.id == deckId) saved else it }
            if (workingCopies[deckId] == deck) workingCopies = workingCopies - deckId
            true
        }
    }

    suspend fun deleteDeck(deckId: String) {
        write {
            repository.deleteDeck(deckId)
            decks = decks.filterNot { it.id == deckId }
            workingCopies = workingCopies - deckId
            previewOrders = previewOrders - deckId
            true
        }
    }

    fun discardChanges(deckId: String) {
        workingCopies = workingCopies - deckId
        previewOrders = previewOrders - deckId
    }

    // Let only one database write run at a time so create, save and delete do not overlap.
    private suspend fun write(operation: suspend () -> Boolean): Boolean = writes.withLock {
        isSaving = true
        writeFailed = false
        try {
            operation().also { writeFailed = !it }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            writeFailed = true
            false
        } finally {
            isSaving = false
        }
    }
}
