package com.example.cyberpunktcg.ui.browser

import com.example.cyberpunktcg.data.model.BrowsableCard

internal enum class CardSortField {
    NAME,
    COST,
    POWER,
    RAM,
    COLOR,
    TYPE,
}

internal enum class SortDirection {
    ASCENDING,
    DESCENDING,
}

internal fun <T : BrowsableCard> sortCards(
    cards: List<T>,
    field: CardSortField,
    direction: SortDirection,
): List<T> {
    val descending = direction == SortDirection.DESCENDING
    val nameOrder = if (descending) String.CASE_INSENSITIVE_ORDER.reversed() else String.CASE_INSENSITIVE_ORDER
    val numberOrder = if (descending) reverseOrder<Int>() else naturalOrder<Int>()
    // Unknown stats go last whether the known numbers go up or down.
    val statOrder = nullsLast(numberOrder)
    val comparator: Comparator<T> = when (field) {
        CardSortField.NAME -> compareBy(nameOrder) { it.name }
        CardSortField.COST -> compareBy(statOrder) { it.cost }
        CardSortField.POWER -> compareBy(statOrder) { it.power }
        CardSortField.RAM -> compareBy(statOrder) { it.ram }
        CardSortField.COLOR -> compareBy(numberOrder) { it.color.ordinal }
        CardSortField.TYPE -> compareBy(numberOrder) { it.type.ordinal }
    }
    // Keep equal values predictable in either direction, including after filtering.
    return cards.sortedWith(
        comparator.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }.thenBy { it.id },
    )
}
