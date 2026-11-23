package com.example.cyberpunktcg.ui.scanner

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cyberpunktcg.R
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun PhotoReview(
    photo: File,
    ocr: ScannerOcrState,
    matchState: ScannerMatchState,
    onRetake: () -> Unit,
    onRetry: () -> Unit,
    onRetryMatching: () -> Unit,
    onSelectCard: (String) -> Unit,
) {
    var bitmap by remember(photo) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(photo) { mutableStateOf(true) }
    LaunchedEffect(photo) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                decodeScannerPhoto(photo, maxSide = 1600)
            } catch (_: IOException) {
                null
            }
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val photoHeight = (maxHeight * 0.65f).coerceAtMost(420.dp)
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(photoHeight)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    val image = bitmap
                    when {
                        isLoading -> CircularProgressIndicator()
                        image != null -> Image(
                            bitmap = image.asImageBitmap(),
                            contentDescription = stringResource(R.string.camera_captured_photo),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                        else -> Text(
                            stringResource(R.string.camera_photo_unavailable),
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when (ocr) {
                        ScannerOcrState.Idle -> Unit
                        ScannerOcrState.Reading -> Text(stringResource(R.string.scanner_ocr_reading))
                        ScannerOcrState.Empty -> Text(stringResource(R.string.scanner_ocr_empty))
                        ScannerOcrState.Failed -> {
                            Text(
                                stringResource(R.string.scanner_ocr_failed),
                                color = MaterialTheme.colorScheme.error,
                            )
                            TextButton(onClick = onRetry) { Text(stringResource(R.string.camera_retry)) }
                        }
                        is ScannerOcrState.Recognized -> {
                            ScannerSuggestions(matchState, onRetryMatching, onSelectCard)
                            Text(
                                stringResource(R.string.scanner_ocr_result),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(ocr.text)
                        }
                    }
                }
            }
        }
        Button(
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(stringResource(R.string.camera_retake))
        }
    }
}

@Composable
private fun ScannerSuggestions(
    state: ScannerMatchState,
    onRetry: () -> Unit,
    onSelectCard: (String) -> Unit,
) {
    when (state) {
        ScannerMatchState.Idle -> Unit
        ScannerMatchState.Searching -> Text(stringResource(R.string.scanner_matches_searching))
        ScannerMatchState.CatalogMissing -> Text(stringResource(R.string.collection_catalog_missing))
        ScannerMatchState.Failed -> {
            Text(
                stringResource(R.string.collection_catalog_failed),
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = onRetry) { Text(stringResource(R.string.collection_retry)) }
        }
        is ScannerMatchState.Results -> if (state.cards.isEmpty()) {
            Text(stringResource(R.string.scanner_matches_empty))
        } else {
            Text(
                stringResource(R.string.scanner_matches_title),
                style = MaterialTheme.typography.titleMedium,
            )
            state.cards.forEach { match ->
                OutlinedCard(
                    onClick = { onSelectCard(match.card.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(match.card.name, style = MaterialTheme.typography.titleSmall)
                        if (match.card.subtitle.isNotBlank()) Text(match.card.subtitle)
                        if (match.isApproximate) {
                            Text(
                                stringResource(R.string.scanner_match_possible),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
