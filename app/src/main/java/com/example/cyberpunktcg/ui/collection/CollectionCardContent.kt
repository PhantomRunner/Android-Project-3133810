package com.example.cyberpunktcg.ui.collection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cyberpunktcg.R
import com.example.cyberpunktcg.data.local.collection.CollectionPrinting
import com.example.cyberpunktcg.ui.components.labelResId
import com.example.cyberpunktcg.ui.theme.frameColor

@Composable
internal fun CollectionCardPreview(
    printing: CollectionPrinting,
    onClick: () -> Unit,
    actionLabel: String? = null,
) {
    val card = printing.card
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(2.dp, card.color.frameColor),
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(card.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (card.subtitle.isNotBlank()) {
                Text(card.subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Text(stringResource(card.type.labelResId), style = MaterialTheme.typography.labelMedium)
            CollectionCardArtwork(Modifier.fillMaxWidth().height(132.dp))
            if (actionLabel != null) {
                Text(actionLabel, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
internal fun CollectionCardDetailsScreen(printing: CollectionPrinting?) {
    if (printing == null) {
        CollectionMessage(stringResource(R.string.card_not_found))
        return
    }
    val card = printing.card
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(card.name, style = MaterialTheme.typography.headlineSmall)
        if (card.subtitle.isNotBlank()) Text(card.subtitle, style = MaterialTheme.typography.titleMedium)
        CollectionCardArtwork(Modifier.fillMaxWidth().aspectRatio(0.72f))
        Text(stringResource(card.color.labelResId))
        Text(stringResource(card.type.labelResId))
        PrintingLabel(printing, includeVariantAndFinish = true)
        Text(stringResource(R.string.collection_owned_count, printing.quantity))
    }
}

@Composable
private fun CollectionCardArtwork(modifier: Modifier) {
    Image(
        painter = painterResource(R.drawable.card_art_placeholder),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.outline),
        alpha = 0.35f,
    )
}

@Composable
internal fun PrintingLabel(printing: CollectionPrinting, includeVariantAndFinish: Boolean = false) {
    Text(
        stringResource(R.string.collection_printing_label, printing.cardSet.name, printing.printing.collectorNumber),
        style = MaterialTheme.typography.labelMedium,
    )
    if (includeVariantAndFinish) {
        val unknown = stringResource(R.string.collection_unknown_value)
        Text(stringResource(R.string.collection_variant, printing.printing.variant ?: unknown))
        Text(stringResource(R.string.collection_finish, printing.printing.finish ?: unknown))
    }
}

@Composable
internal fun OwnedQuantityControls(printing: CollectionPrinting, onChangeQuantity: (String, Int) -> Unit) {
    val removeLabel = stringResource(R.string.remove_card_copy, printing.card.name)
    val addLabel = stringResource(R.string.add_card_copy, printing.card.name)
    Text(stringResource(R.string.collection_owned_count, printing.quantity))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { onChangeQuantity(printing.printing.id, -1) },
            enabled = printing.quantity > 0,
            modifier = Modifier.weight(1f).semantics { contentDescription = removeLabel },
            contentPadding = PaddingValues(8.dp),
        ) { Text("−") }
        OutlinedButton(
            onClick = { onChangeQuantity(printing.printing.id, 1) },
            enabled = printing.quantity < Int.MAX_VALUE,
            modifier = Modifier.weight(1f).semantics { contentDescription = addLabel },
            contentPadding = PaddingValues(8.dp),
        ) { Text("+") }
    }
}

@Composable
internal fun QuantitySaveError(modifier: Modifier = Modifier) {
    Text(stringResource(R.string.collection_quantity_failed), color = MaterialTheme.colorScheme.error, modifier = modifier)
}

@Composable
internal fun CollectionMessage(message: String, action: @Composable () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, textAlign = TextAlign.Center)
        action()
    }
}
