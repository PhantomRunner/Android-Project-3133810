package com.example.cyberpunktcg.data.local.collection

import com.example.cyberpunktcg.data.model.CardColor
import com.example.cyberpunktcg.data.model.CardType
import org.json.JSONArray
import org.json.JSONObject

internal data class CollectionCatalog(
    val sets: List<CardSetEntity>,
    val cards: List<CardEntity>,
    val printings: List<CardPrintingEntity>,
)

internal fun parseCollectionCatalog(json: String): CollectionCatalog {
    val catalog = JSONObject(json)
    require(catalog.getInt("version") == 1) { "Unsupported collection catalog version" }

    val sets = catalog.getJSONArray("sets").mapObjects { set ->
        CardSetEntity(set.requiredString("id"), set.requiredString("name"), set.requiredString("language"))
    }
    val cards = catalog.getJSONArray("cards").mapObjects { card ->
        val types = card.getJSONArray("unitTypes")
        CardEntity(
            id = card.requiredString("id"),
            name = card.requiredString("name"),
            subtitle = card.getString("subtitle"),
            type = CardType.valueOf(card.getString("type")),
            color = CardColor.valueOf(card.getString("color")),
            cost = card.stat("cost"),
            power = card.stat("power"),
            ram = card.stat("ram"),
            unitTypes = List(types.length()) { types.getString(it) },
            eddies = if (card.isNull("eddies")) null else card.getBoolean("eddies"),
            powerLabel = card.optionalString("powerLabel"),
        )
    }
    val printings = catalog.getJSONArray("printings").mapObjects { printing ->
        CardPrintingEntity(
            id = printing.requiredString("id"),
            cardId = printing.requiredString("cardId"),
            setId = printing.requiredString("setId"),
            collectorNumber = printing.requiredString("collectorNumber"),
            variant = printing.optionalString("variant"),
            finish = printing.optionalString("finish"),
            imagePath = printing.optionalString("imagePath")?.also { path ->
                require(path.startsWith("card-images/") && '\\' !in path &&
                    path.split('/').none { it.isBlank() || it == "." || it == ".." }) {
                    "Invalid collection image path"
                }
            },
        )
    }

    val setIds = sets.map { it.id }.toSet()
    val cardIds = cards.map { it.id }.toSet()
    require(setIds.size == sets.size && cardIds.size == cards.size &&
        printings.map { it.id }.toSet().size == printings.size) { "Duplicate collection IDs" }
    require(printings.isNotEmpty() && printings.all { it.cardId in cardIds && it.setId in setIds }) {
        "Collection printings must reference known cards and sets"
    }
    return CollectionCatalog(sets, cards, printings)
}

private fun <T> JSONArray.mapObjects(read: (JSONObject) -> T): List<T> =
    List(length()) { read(getJSONObject(it)) }

private fun JSONObject.requiredString(key: String): String = getString(key).also {
    require(!isNull(key) && it.isNotBlank()) { "Missing catalog field $key" }
}

private fun JSONObject.optionalString(key: String): String? =
    if (isNull(key)) null else getString(key).takeIf { it.isNotBlank() }

private fun JSONObject.stat(key: String): Int? = if (isNull(key)) null else getInt(key).also {
    require(it >= 0) { "Negative catalog stat $key" }
}
