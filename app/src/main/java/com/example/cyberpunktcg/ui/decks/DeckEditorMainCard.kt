package com.example.cyberpunktcg.ui.decks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.cyberpunktcg.R
import com.example.cyberpunktcg.data.model.Card
import com.example.cyberpunktcg.domain.DeckValidation
import com.example.cyberpunktcg.ui.components.CardArtworkFrame

@Composable
internal fun DeckEditorMainCard(
    card: Card,
    quantity: Int,
    validation: DeckValidation?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CardArtworkFrame(card.imagePath, card.color, Modifier.fillMaxWidth())
        Text(card.name, style = MaterialTheme.typography.titleSmall)
        if (card.subtitle.isNotBlank()) {
            Text(card.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (validation != null && card.color !in validation.allowedColors) {
            CardColorWarningText(card)
        }
        validation?.ramWarnings?.firstOrNull { it.cardId == card.id }?.let { CardRamWarningText(it) }
        Spacer(Modifier.weight(1f))
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
            Text(
                stringResource(R.string.deck_card_copies, quantity),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
