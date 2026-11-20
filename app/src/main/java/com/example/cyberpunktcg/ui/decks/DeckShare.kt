package com.example.cyberpunktcg.ui.decks

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.cyberpunktcg.R
import com.example.cyberpunktcg.data.model.Card
import com.example.cyberpunktcg.data.model.Deck

internal fun shareDeck(context: Context, deck: Deck, findCard: (String) -> Card?) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, deckShareText(deck, findCard))
    }
    try {
        context.startActivity(Intent.createChooser(send, null))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.deck_share_unavailable, Toast.LENGTH_SHORT).show()
    }
}

internal fun deckShareText(deck: Deck, findCard: (String) -> Card?): String = buildString {
    appendLine(deck.name)
    appendLine()
    appendLine("Legends (${deck.legendIds.size})")
    if (deck.legendIds.isEmpty()) appendLine("None")
    deck.legendIds.forEach { id -> appendLine("1 × ${sharedCardName(id, findCard)}") }

    appendLine()
    appendLine("Main (${deck.mainCardCount})")
    if (deck.items.isEmpty()) appendLine("None")
    deck.items.forEach { item -> appendLine("${item.quantity} × ${sharedCardName(item.cardId, findCard)}") }
}.trimEnd()

private fun sharedCardName(id: String, findCard: (String) -> Card?): String {
    val card = findCard(id) ?: return "Unknown card ($id)"
    return if (card.subtitle.isBlank()) card.name else "${card.name} — ${card.subtitle}"
}
