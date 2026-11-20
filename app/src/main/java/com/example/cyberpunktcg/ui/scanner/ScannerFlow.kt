package com.example.cyberpunktcg.ui.scanner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cyberpunktcg.R
import com.example.cyberpunktcg.navigation.AppTopBar
import com.example.cyberpunktcg.sensors.LightSensorState
import com.example.cyberpunktcg.ui.browser.CardBrowserViewModel
import com.example.cyberpunktcg.ui.collection.CatalogBrowser
import com.example.cyberpunktcg.ui.collection.CollectionMessage
import com.example.cyberpunktcg.ui.collection.CollectionViewModel
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun ScannerFlow(
    scanner: ScannerViewModel,
    light: StateFlow<LightSensorState>,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val state = scanner.manualAddState
    val savedViews = rememberSaveableStateHolder()
    BackHandler(
        enabled = state == ManualAddState.Browsing || state is ManualAddState.Confirming,
        onBack = scanner::backFromManualAdd,
    )
    Scaffold(topBar = {
        AppTopBar(
            titleResId = R.string.scanner,
            showBackButton = (state as? ManualAddState.Confirming)?.isSaving != true,
            onBackClick = onBack,
        )
    }) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding).imePadding()) {
            when (state) {
                ManualAddState.Inactive -> savedViews.SaveableStateProvider("camera") {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            ScannerScreen(viewModel = scanner, light = light)
                        }
                        TextButton(
                            onClick = scanner::findCardManually,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        ) { Text(stringResource(R.string.scanner_find_manually)) }
                    }
                }
                ManualAddState.Browsing -> savedViews.SaveableStateProvider("catalog") {
                    val collection: CollectionViewModel = viewModel()
                    val browser: CardBrowserViewModel = viewModel()
                    val catalog by collection.state.collectAsStateWithLifecycle()
                    CatalogBrowser(catalog, browser, collection::retry) { printing, dismiss ->
                        Button(onClick = {
                            dismiss()
                            scanner.selectPrinting(printing)
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.scanner_select_printing))
                        }
                    }
                }
                is ManualAddState.Confirming -> ConfirmManualCard(
                    state = state,
                    onConfirm = scanner::confirmPrinting,
                    onCancel = scanner::backFromManualAdd,
                )
                is ManualAddState.Added -> CollectionMessage(
                    stringResource(R.string.scanner_added_card, state.printing.card.name),
                ) {
                    Button(onClick = onDone) { Text(stringResource(R.string.scanner_done)) }
                    TextButton(onClick = scanner::scanAnother) { Text(stringResource(R.string.scanner_scan_another)) }
                }
            }
        }
    }
}
