package com.example.cyberpunktcg.ui.collection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cyberpunktcg.R
import com.example.cyberpunktcg.data.local.collection.CardEntity
import com.example.cyberpunktcg.data.local.collection.CollectionPrinting
import com.example.cyberpunktcg.ui.components.labelResId
import com.example.cyberpunktcg.ui.components.CardArtwork
import com.example.cyberpunktcg.ui.components.CardArtworkFrame

@Composable
internal fun CollectionCardPreview(
    printing: CollectionPrinting,
    onClick: () -> Unit,
    actionLabel: String? = null,
) {
    val card = printing.card
    Column(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CardArtworkFrame(printing.printing.imagePath, card.color, Modifier.fillMaxWidth())
        Text(
            card.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (card.subtitle.isNotBlank()) {
            Text(
                card.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(stringResource(card.type.labelResId), style = MaterialTheme.typography.labelMedium)
        CollectionCardStats(card, MaterialTheme.typography.labelMedium)
        if (actionLabel != null) {
            Text(actionLabel, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
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
        Text(stringResource(card.color.labelResId))
        Text(stringResource(card.type.labelResId))
        PrintingLabel(printing, includeVariantAndFinish = true)
        Text(stringResource(R.string.collection_owned_count, printing.quantity))
        CollectionCardStats(card, MaterialTheme.typography.bodyLarge)
        if (card.unitTypes.isNotEmpty()) Text(card.unitTypes.joinToString(" • "))
        if (card.eddies == true) Text(stringResource(R.string.card_sell_indicator))
        CardArtwork(printing.printing.imagePath, Modifier.fillMaxWidth().aspectRatio(0.72f))
    }
}

@Composable
private fun CollectionCardStats(card: CardEntity, style: TextStyle, modifier: Modifier = Modifier) {
    val unknown = stringResource(R.string.collection_unknown_value)
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.collection_card_cost, card.cost?.toString() ?: unknown), style = style)
        Text(stringResource(R.string.collection_card_power, card.powerLabel ?: card.power?.toString() ?: unknown), style = style)
        Text(stringResource(R.string.collection_card_ram, card.ram?.toString() ?: unknown), style = style)
    }
}

@Composable
internal fun PrintingLabel(
    printing: CollectionPrinting,
    includeVariantAndFinish: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Text(
        stringResource(R.string.collection_printing_label, printing.cardSet.name, printing.printing.collectorNumber),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
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
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.collection_owned_count, printing.quantity), style = MaterialTheme.typography.labelLarge)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
