package com.example.cyberpunktcg.ui.decks

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.example.cyberpunktcg.R
import com.example.cyberpunktcg.data.model.Card
import com.example.cyberpunktcg.ui.components.LegendArtworkPreview
import com.example.cyberpunktcg.ui.components.cardStat
import com.example.cyberpunktcg.ui.theme.LegacyNavigationBarScrim

@Composable
internal fun DeckEditorLegendSlots(
    legendIds: List<String>,
    legends: List<Card>,
    onChooseLegend: () -> Unit,
    onRemoveLegend: (String) -> Unit,
    onOpenCard: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { index ->
            val legend = legends.firstOrNull { it.id == legendIds.getOrNull(index) }
            key(legend?.id ?: "empty_$index") {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (legend == null) {
                        OutlinedCard(onClick = onChooseLegend, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.legend_slot, index + 1), style = MaterialTheme.typography.labelMedium)
                                Text(stringResource(R.string.choose_legend), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    } else {
                        LegendArtworkPreview(
                            legend,
                            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = { onOpenCard(legend.id) }),
                        )
                        Text(stringResource(R.string.card_ram, legend.ram.cardStat()), style = MaterialTheme.typography.labelSmall)
                        val removeLabel = stringResource(R.string.remove_named_legend, legend.name)
                        TextButton(
                            onClick = { onRemoveLegend(legend.id) },
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = removeLabel },
                        ) { Text(stringResource(R.string.remove_legend), style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeckEditorLegendPicker(
    ownedLegends: List<Card>,
    ownedQuantity: (String) -> Int,
    canAddLegend: (String) -> Boolean,
    onSelectLegend: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val view = LocalView.current
            SideEffect {
                @Suppress("DEPRECATION")
                (view.parent as? DialogWindowProvider)?.window?.navigationBarColor = LegacyNavigationBarScrim.toArgb()
            }
        }
        val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(136.dp * fontScale),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "heading", span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.choose_legend), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.legend_selection_hint), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (ownedLegends.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    Text(stringResource(R.string.deck_no_owned_legends))
                }
            }
            items(ownedLegends, key = { it.id }) { legend ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LegendArtworkPreview(legend)
                    Text(stringResource(R.string.card_ram, legend.ram.cardStat()), style = MaterialTheme.typography.labelMedium)
                    Text(stringResource(R.string.collection_owned_count, ownedQuantity(legend.id)), style = MaterialTheme.typography.labelMedium)
                    Button(
                        onClick = { onSelectLegend(legend.id) },
                        enabled = canAddLegend(legend.id),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.select_legend)) }
                }
            }
            item(key = "cancel", span = { GridItemSpan(maxLineSpan) }) {
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}
