package com.example.cyberpunktcg.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.cyberpunktcg.R
import com.example.cyberpunktcg.data.local.collection.COLLECTION_ASSET_DIRECTORY
import java.io.File

@Composable
internal fun CardArtwork(path: String?, modifier: Modifier) {
    val placeholder = painterResource(R.drawable.card_art_placeholder)
    val assetUri = path?.let { Uri.fromFile(File("/android_asset/$COLLECTION_ASSET_DIRECTORY/$it")) }
    val artworkPainter = rememberAsyncImagePainter(
        model = assetUri,
        contentScale = ContentScale.Fit,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,
    )
    Image(
        painter = artworkPainter,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        colorFilter = if (artworkPainter.state is AsyncImagePainter.State.Success) {
            null
        } else {
            ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
        },
    )
}

internal fun Int?.cardStat(): String = this?.toString() ?: "—"
