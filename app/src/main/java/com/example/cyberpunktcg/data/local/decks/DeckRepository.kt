package com.example.cyberpunktcg.data.local.decks

import android.content.Context
import com.example.cyberpunktcg.data.local.collection.CollectionDatabase
import com.example.cyberpunktcg.data.local.collection.CollectionRepository
import com.example.cyberpunktcg.data.model.Card
import com.example.cyberpunktcg.data.model.Deck
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal data class DeckCatalog(val cards: List<Card>, val ownedQuantities: Map<String, Int>)

internal interface DeckRepository {
    val decks: Flow<List<Deck>>
    val catalog: Flow<DeckCatalog>
    suspend fun loadCatalog()
    suspend fun createDeck(deck: Deck)
    suspend fun saveDeck(deck: Deck): Boolean
    suspend fun deleteDeck(deckId: String)
}

internal class RoomDeckRepository(
    context: Context,
    database: CollectionDatabase = CollectionDatabase.get(context),
) : DeckRepository {
    private val collection = CollectionRepository(context, database.collectionDao())
    private val dao = database.deckDao()

    override val decks = dao.observeDecks().map { decks -> decks.map { it.toDeck() } }
    override val catalog = collection.printings.map { printings ->
        val byCard = printings.groupBy { it.card.id }
        val cards = byCard.values.map { editions ->
            val representative = editions.firstOrNull { it.printing.imagePath != null } ?: editions.first()
            val card = representative.card
            Card(
                id = card.id, name = card.name, subtitle = card.subtitle, type = card.type, color = card.color,
                cost = card.cost, power = card.power, ram = card.ram, unitTypes = card.unitTypes,
                description = "", powerLabel = card.powerLabel, eddies = card.eddies,
                imagePath = representative.printing.imagePath,
            )
        }
        val quantities = byCard.mapValues { (_, editions) ->
            editions.sumOf { it.quantity.toLong() }.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        }
        DeckCatalog(cards, quantities)
    }

    override suspend fun loadCatalog() {
        collection.loadCatalog()
    }

    override suspend fun createDeck(deck: Deck) {
        dao.createDeck(DeckEntity(deck.id, deck.name, System.currentTimeMillis()))
    }

    override suspend fun saveDeck(deck: Deck): Boolean = dao.saveDeck(deck)

    override suspend fun deleteDeck(deckId: String) {
        dao.deleteDeck(deckId)
    }
}
