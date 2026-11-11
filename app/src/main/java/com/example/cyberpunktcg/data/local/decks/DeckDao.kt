package com.example.cyberpunktcg.data.local.decks

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.cyberpunktcg.data.model.Deck
import kotlinx.coroutines.flow.Flow

@Dao
internal abstract class DeckDao {
    @Transaction
    @Query("SELECT * FROM decks ORDER BY createdAt, id")
    abstract fun observeDecks(): Flow<List<StoredDeck>>

    @Insert
    abstract suspend fun createDeck(deck: DeckEntity)

    @Transaction
    open suspend fun saveDeck(deck: Deck): Boolean {
        require(deck.name.isNotBlank() && deck.items.all { it.quantity > 0 })
        if (updateName(deck.id, deck.name) == 0) return false
        deleteLegends(deck.id)
        deleteItems(deck.id)
        insertLegends(deck.legendIds.mapIndexed { index, id -> DeckLegendEntity(deck.id, id, index) })
        insertItems(deck.items.mapIndexed { index, item -> DeckItemEntity(deck.id, item.cardId, item.quantity, index) })
        return true
    }

    @Query("DELETE FROM decks WHERE id = :deckId")
    abstract suspend fun deleteDeck(deckId: String)

    @Query("UPDATE decks SET name = :name WHERE id = :deckId")
    protected abstract suspend fun updateName(deckId: String, name: String): Int

    @Query("DELETE FROM deck_legends WHERE deckId = :deckId")
    protected abstract suspend fun deleteLegends(deckId: String)

    @Query("DELETE FROM deck_items WHERE deckId = :deckId")
    protected abstract suspend fun deleteItems(deckId: String)

    @Insert
    protected abstract suspend fun insertLegends(legends: List<DeckLegendEntity>)

    @Insert
    protected abstract suspend fun insertItems(items: List<DeckItemEntity>)
}
