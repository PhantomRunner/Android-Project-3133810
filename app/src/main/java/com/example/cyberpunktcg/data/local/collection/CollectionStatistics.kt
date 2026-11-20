package com.example.cyberpunktcg.data.local.collection

internal data class CollectionStatistics(
    val totalCards: Long,
    val uniqueCards: Int,
    val ownedEditions: Int,
)

internal fun calculateCollectionStatistics(printings: List<CollectionPrinting>): CollectionStatistics {
    var totalCards = 0L
    val cardIds = mutableSetOf<String>()
    val printingIds = mutableSetOf<String>()
    printings.forEach { printing ->
        if (printing.quantity > 0) {
            totalCards += printing.quantity.toLong()
            cardIds += printing.card.id
            printingIds += printing.printing.id
        }
    }
    return CollectionStatistics(totalCards, cardIds.size, printingIds.size)
}
