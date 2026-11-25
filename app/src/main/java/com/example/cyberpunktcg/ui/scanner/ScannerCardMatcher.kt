package com.example.cyberpunktcg.ui.scanner

import com.example.cyberpunktcg.data.local.collection.CardEntity
import com.example.cyberpunktcg.data.local.collection.CollectionPrinting
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

internal data class ScannerCardMatch(
    val card: CardEntity,
    val editions: List<CollectionPrinting>,
    val isApproximate: Boolean,
)

// Suggest up to five catalog cards and keep their printings together for edition selection.
internal fun matchScannerCards(
    recognizedText: String,
    printings: List<CollectionPrinting>,
): List<ScannerCardMatch> {
    val lines = recognizedText.lineSequence().map(::normalize).filter(String::isNotEmpty).toList()
    if (lines.isEmpty()) return emptyList()
    // Try single lines and neighboring pairs because a title can span two lines.
    val phrases = (lines + lines.zipWithNext { first, second -> "$first $second" }).toSet()
    val matches = printings.groupBy { it.card.id }.values.mapNotNull { editions ->
        rankMatch(editions, phrases)
    }
    // A matching subtitle narrows suggestions that share the same card name.
    val namesWithMatchingSubtitles = matches.filter { it.subtitleMatches }.map { it.name }.toSet()
    return matches
        .filter { it.subtitleMatches || it.name !in namesWithMatchingSubtitles }
        .sortedWith(
            compareBy<RankedMatch> { it.distance != 0 }
                .thenByDescending { it.subtitleMatches }
                .thenBy { it.distance }
                .thenBy { it.name }
                .thenBy { it.subtitle }
                .thenBy { it.card.id },
        )
        .take(5)
        .map { ScannerCardMatch(it.card, it.editions.sortedBy { edition -> edition.printing.id }, it.distance != 0) }
}

private data class RankedMatch(
    val card: CardEntity,
    val editions: List<CollectionPrinting>,
    val name: String,
    val subtitle: String,
    val distance: Int,
    val subtitleMatches: Boolean,
)

private fun rankMatch(editions: List<CollectionPrinting>, phrases: Set<String>): RankedMatch? {
    val card = editions.first().card
    val name = normalize(card.name)
    if (name.isEmpty()) return null
    val subtitle = normalize(card.subtitle)
    // Allow small OCR errors in longer names, where one wrong letter matters less.
    val maxEdits = when {
        name.length >= 12 -> 2
        name.length >= 8 -> 1
        else -> 0
    }
    val plainDistance = closestDistance(name, phrases, maxEdits)
    val combinedDistance = if (subtitle.isEmpty()) {
        null
    } else {
        val suffix = " $subtitle"
        val titles = phrases.filter { it.endsWith(suffix) }.map { it.removeSuffix(suffix) }
        closestDistance(name, titles, maxEdits)
    }
    val distance = listOfNotNull(plainDistance, combinedDistance).minOrNull() ?: return null
    val subtitleMatches = subtitle in phrases || combinedDistance != null
    if (name.length == 1 && !subtitleMatches) return null
    return RankedMatch(card, editions, name, subtitle, distance, subtitleMatches)
}

private fun closestDistance(name: String, phrases: Collection<String>, maxEdits: Int): Int? {
    if (name in phrases) return 0
    if (maxEdits == 0) return null
    return phrases.asSequence()
        .filter { abs(name.length - it.length) <= maxEdits }
        .map { editDistance(name, it) }
        .filter { it <= maxEdits }
        .minOrNull()
}

// Count the fewest insertions, deletions and replacements needed to match two strings.
private fun editDistance(expected: String, actual: String): Int {
    var previous = IntArray(actual.length + 1) { it }
    var current = IntArray(actual.length + 1)
    for (expectedIndex in expected.indices) {
        current[0] = expectedIndex + 1
        for (actualIndex in actual.indices) {
            val substitutionCost = if (expected[expectedIndex] == actual[actualIndex]) 0 else 1
            current[actualIndex + 1] = minOf(
                previous[actualIndex + 1] + 1,
                current[actualIndex] + 1,
                previous[actualIndex] + substitutionCost,
            )
        }
        val completedRow = current
        current = previous
        previous = completedRow
    }
    return previous[actual.length]
}

private val apostrophe = Regex("['’‘ʼ]")
private val separator = Regex("""[^\p{L}\p{M}\p{N}]+""")

private fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKC)
    .lowercase(Locale.ROOT)
    .replace(apostrophe, "")
    .replace(separator, " ")
    .trim()
