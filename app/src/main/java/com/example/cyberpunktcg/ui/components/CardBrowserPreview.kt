package com.example.cyberpunktcg.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cyberpunktcg.R
import com.example.cyberpunktcg.data.model.BrowsableCard

@Composable
internal fun CardBrowserPreview(
    card: BrowsableCard,
    subtitle: String,
    imagePath: String?,
    powerLabel: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CardArtworkFrame(imagePath, card.color, Modifier.fillMaxWidth())
        Text(card.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(stringResource(card.type.labelResId), style = MaterialTheme.typography.labelMedium)
        CardBrowserStats(card, powerLabel, MaterialTheme.typography.labelMedium)
        if (actionLabel != null) {
            Spacer(Modifier.weight(1f))
            Text(actionLabel, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
internal fun CardBrowserStats(card: BrowsableCard, powerLabel: String?, style: TextStyle) {
    val unknown = stringResource(R.string.collection_unknown_value)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.collection_card_cost, card.cost?.toString() ?: unknown), style = style)
        Text(stringResource(R.string.collection_card_power, powerLabel ?: card.power?.toString() ?: unknown), style = style)
        Text(stringResource(R.string.collection_card_ram, card.ram?.toString() ?: unknown), style = style)
    }
}
