package com.example.cyberpunktcg.ui.scanner

import com.example.cyberpunktcg.data.local.collection.CardEntity
import com.example.cyberpunktcg.data.local.collection.CardPrintingEntity
import com.example.cyberpunktcg.data.local.collection.CardSetEntity
import com.example.cyberpunktcg.data.local.collection.CollectionPrinting
import com.example.cyberpunktcg.data.model.CardColor
import com.example.cyberpunktcg.data.model.CardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannerCardMatcherTest {
    @Test
    fun matchesPunctuationWrappedTitlesAndTitlesOnTheSubtitleLine() {
        val printing = printing("bug", "T-Bug", "On the Edge")
        for (text in listOf(" t\nBUG!\nOn the edge.", "T—BUG:  ON THE EDGE!")) {
            val match = matchScannerCards(text, listOf(printing)).single()

            assertEquals(printing.card, match.card)
            assertFalse(match.isApproximate)
        }
    }

    @Test
    fun ranksExactNamesBeforeApproximateNames() {
        val approximate = printing("approximate", "Adam Smashed")
        val exact = printing("exact", "Adam Smasher")
        val unrelated = printing("unrelated", "Adam Opposer")

        val matches = matchScannerCards("ADAM SMASHER", listOf(approximate, unrelated, exact))

        assertEquals(listOf(exact.card, approximate.card), matches.map { it.card })
        assertEquals(listOf(false, true), matches.map { it.isApproximate })
    }

    @Test
    fun appliesEditLimitsToNormalizedTitles() {
        val short = printing("short", "Talon")
        val eight = printing("eight", "Redliner")
        val long = printing("long", "Adam Smasher", "Ender of Legends")

        assertTrue(matchScannerCards("TALOM", listOf(short)).isEmpty())
        assertTrue(matchScannerCards("REDLINEX", listOf(eight)).single().isApproximate)
        assertTrue(matchScannerCards("REDLINXX", listOf(eight)).isEmpty())
        assertTrue(matchScannerCards("ADAM SMASH ENDER OF LEGENDS", listOf(long)).single().isApproximate)
        assertTrue(matchScannerCards("ADAM SMAS", listOf(long)).isEmpty())
    }

    @Test
    fun usesSubtitlesToDistinguishCardsAndIncludesAllEditionsOfTheMatch() {
        val standard = printing("ender", "Adam Smasher", "Ender of Legends", "ender-standard")
        val foil = standard.copy(printing = standard.printing.copy(id = "ender-foil", finish = "Foil"))
        val other = printing("metal", "Adam Smasher", "Metal Over Meat")
        val catalog = listOf(other, standard, foil)

        val unspecified = matchScannerCards("ADAM SMASHER", catalog)
        assertEquals(setOf(standard.card, other.card), unspecified.map { it.card }.toSet())

        val match = matchScannerCards("ADAM SMASHER\nENDER OF LEGENDS", catalog).single()
        assertEquals(standard.card, match.card)
        assertEquals(listOf(foil, standard), match.editions)
    }

    @Test
    fun requiresTheSubtitleForAOneCharacterName() {
        val streetkid = printing("streetkid", "V", "Streetkid")
        val exile = printing("exile", "V", "Corporate Exile")
        val catalog = listOf(streetkid, exile)

        assertTrue(matchScannerCards("V", catalog).isEmpty())
        assertEquals(exile.card, matchScannerCards("V\nCorporate Exile", catalog).single().card)
    }

    @Test
    fun limitsResultsAndKeepsTheirOrderStable() {
        val catalog = (7 downTo 1).map { printing(it.toString(), "Signal Runner") }

        val matches = matchScannerCards("SIGNAL RUNNER", catalog)

        assertEquals(listOf("1", "2", "3", "4", "5"), matches.map { it.card.id })
        assertTrue(matchScannerCards("Give Signal Runner a bonus", catalog).isEmpty())
        assertTrue(matchScannerCards(" … ", catalog).isEmpty())
    }

    private fun printing(cardId: String, name: String, subtitle: String = "", id: String = cardId) =
        CollectionPrinting(
            printing = CardPrintingEntity(id, cardId, "set", "004", null, null, null),
            card = CardEntity(
                id = cardId, name = name, subtitle = subtitle, type = CardType.UNIT,
                color = CardColor.BLUE, cost = null, power = null, ram = null, unitTypes = emptyList(),
                eddies = null, powerLabel = null,
            ),
            cardSet = CardSetEntity("set", "Relay District", "en"),
            owned = null,
        )
}
