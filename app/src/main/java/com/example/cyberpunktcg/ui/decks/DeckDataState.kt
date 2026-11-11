package com.example.cyberpunktcg.ui.decks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cyberpunktcg.R

@Composable
internal fun DeckDataState(model: DecksViewModel, content: @Composable () -> Unit) {
    if (model.isLoading || model.loadFailed) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(if (model.loadFailed) R.string.decks_load_failed else R.string.decks_loading))
            if (model.loadFailed) {
                Button(onClick = model::retry) { Text(stringResource(R.string.collection_retry)) }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            if (model.writeFailed) {
                Text(
                    stringResource(R.string.decks_write_failed),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Box(modifier = Modifier.weight(1f)) { content() }
        }
    }
}
