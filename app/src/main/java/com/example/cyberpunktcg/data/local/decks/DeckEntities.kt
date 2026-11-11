package com.example.cyberpunktcg.data.local.decks

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.cyberpunktcg.data.local.collection.CardEntity
import com.example.cyberpunktcg.data.model.Deck
import com.example.cyberpunktcg.data.model.DeckItem

@Entity(tableName = "decks")
internal data class DeckEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
)

@Entity(
    tableName = "deck_legends",
    primaryKeys = ["deckId", "cardId"],
    foreignKeys = [
        ForeignKey(entity = DeckEntity::class, parentColumns = ["id"], childColumns = ["deckId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CardEntity::class, parentColumns = ["id"], childColumns = ["cardId"]),
    ],
    indices = [Index("cardId")],
)
internal data class DeckLegendEntity(val deckId: String, val cardId: String, val position: Int)

@Entity(
    tableName = "deck_items",
    primaryKeys = ["deckId", "cardId"],
    foreignKeys = [
        ForeignKey(entity = DeckEntity::class, parentColumns = ["id"], childColumns = ["deckId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CardEntity::class, parentColumns = ["id"], childColumns = ["cardId"]),
    ],
    indices = [Index("cardId")],
)
internal data class DeckItemEntity(val deckId: String, val cardId: String, val quantity: Int, val position: Int)

internal data class StoredDeck(
    @Embedded val deck: DeckEntity,
    @Relation(parentColumn = "id", entityColumn = "deckId") val legends: List<DeckLegendEntity>,
    @Relation(parentColumn = "id", entityColumn = "deckId") val items: List<DeckItemEntity>,
) {
    fun toDeck(): Deck = Deck(
        id = deck.id,
        name = deck.name,
        legendIds = legends.sortedBy { it.position }.map { it.cardId },
        items = items.sortedBy { it.position }.map { DeckItem(it.cardId, it.quantity) },
    )
}
