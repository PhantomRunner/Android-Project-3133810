package com.example.cyberpunktcg.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cyberpunktcg.data.model.CardColor
import com.example.cyberpunktcg.ui.theme.frameColor

@Composable
internal fun CardArtworkFrame(path: String?, color: CardColor, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier, border = BorderStroke(2.dp, color.frameColor)) {
        CardArtwork(path, Modifier.fillMaxWidth().padding(2.dp).aspectRatio(0.716f))
    }
}
