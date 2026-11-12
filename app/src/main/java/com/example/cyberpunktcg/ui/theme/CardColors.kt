package com.example.cyberpunktcg.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.cyberpunktcg.data.model.CardColor


internal val CardColor.frameColor: Color
    get() = when (this) {
        CardColor.RED -> CardFrameRed
        CardColor.YELLOW -> CardFrameYellow
        CardColor.BLUE -> CardFrameBlue
        CardColor.GREEN -> CardFrameGreen
    }
