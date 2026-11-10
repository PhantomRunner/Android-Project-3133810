package com.example.cyberpunktcg.data.local.collection

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.TypeConverter
import com.example.cyberpunktcg.data.model.BrowsableCard
import com.example.cyberpunktcg.data.model.CardColor
import com.example.cyberpunktcg.data.model.CardType
import org.json.JSONArray

@Entity(tableName = "cards")
internal data class CardEntity(
    @PrimaryKey override val id: String,
    override val name: String,
    val subtitle: String,
    override val type: CardType,
    override val color: CardColor,
    override val cost: Int?,
    override val power: Int?,
    override val ram: Int?,
    val unitTypes: List<String>,
    val eddies: Boolean?,
    val powerLabel: String?,
) : BrowsableCard

@Entity(tableName = "card_sets")
internal data class CardSetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val language: String,
)

@Entity(
    tableName = "card_printings",
    foreignKeys = [
        ForeignKey(entity = CardEntity::class, parentColumns = ["id"], childColumns = ["cardId"]),
        ForeignKey(entity = CardSetEntity::class, parentColumns = ["id"], childColumns = ["setId"]),
    ],
    indices = [Index("cardId"), Index("setId")],
)
internal data class CardPrintingEntity(
    @PrimaryKey val id: String,
    val cardId: String,
    val setId: String,
    val collectorNumber: String,
    val variant: String?,
    val finish: String?,
    val imagePath: String?,
)

@Entity(
    tableName = "owned_printings",
    foreignKeys = [
        ForeignKey(entity = CardPrintingEntity::class, parentColumns = ["id"], childColumns = ["printingId"]),
    ],
)
internal data class OwnedPrintingEntity(
    @PrimaryKey val printingId: String,
    val quantity: Int,
)

internal data class CollectionPrinting(
    @Embedded val printing: CardPrintingEntity,
    @Relation(parentColumn = "cardId", entityColumn = "id") val card: CardEntity,
    @Relation(parentColumn = "setId", entityColumn = "id") val cardSet: CardSetEntity,
    @Relation(parentColumn = "id", entityColumn = "printingId") val owned: OwnedPrintingEntity?,
) {
    val quantity: Int get() = owned?.quantity ?: 0
}

internal class CollectionConverters {
    @TypeConverter
    fun encodeUnitTypes(types: List<String>): String = JSONArray(types).toString()

    @TypeConverter
    fun decodeUnitTypes(json: String): List<String> = JSONArray(json).let { types ->
        List(types.length()) { types.getString(it) }
    }
}
