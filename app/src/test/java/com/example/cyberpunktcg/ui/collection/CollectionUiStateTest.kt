package com.example.cyberpunktcg.ui.collection

import com.example.cyberpunktcg.data.local.collection.CardEntity
import com.example.cyberpunktcg.data.local.collection.CardPrintingEntity
import com.example.cyberpunktcg.data.local.collection.CardSetEntity
import com.example.cyberpunktcg.data.local.collection.CollectionPrinting
import com.example.cyberpunktcg.data.model.CardColor
import com.example.cyberpunktcg.data.model.CardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CollectionUiStateTest {
    @Test
    fun waitsForTheActiveCatalogAndAllItsPrintings() {
        val state = CollectionUiState(printings = listOf(printing("first"), printing("old")))
        assertNull(state.activeCatalogPrintings())

        val imported = state.copy(catalog = CatalogStatus.Ready(setOf("first", "second")))
        assertNull(imported.activeCatalogPrintings())
    }

    @Test
    fun selectsOnlyPrintingsFromTheActiveCatalog() {
        val first = printing("first")
        val second = printing("second")
        val state = CollectionUiState(
            printings = listOf(first, printing("old"), second),
            catalog = CatalogStatus.Ready(setOf("first", "second")),
        )

        assertEquals(listOf(first, second), state.activeCatalogPrintings())
    }

    private fun printing(id: String) = CollectionPrinting(
        printing = CardPrintingEntity(id, "card", "set", "004", null, null, null),
        card = CardEntity(
            id = "card", name = "Signal Runner", subtitle = "", type = CardType.UNIT,
            color = CardColor.BLUE, cost = null, power = null, ram = null, unitTypes = emptyList(),
            eddies = null, powerLabel = null,
        ),
        cardSet = CardSetEntity("set", "Relay District", "en"),
        owned = null,
    )
}
