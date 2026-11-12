package com.example.cyberpunktcg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModelProvider
import com.example.cyberpunktcg.navigation.CompanionApp
import com.example.cyberpunktcg.ui.theme.CyberpunkTcgTheme
import com.example.cyberpunktcg.ui.theme.LegacyNavigationBarScrim
import com.example.cyberpunktcg.ui.theme.ThemeViewModel
import com.example.cyberpunktcg.ui.theme.TransparentSystemBar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeViewModel = ViewModelProvider(this)[ThemeViewModel::class.java]
        enableEdgeToEdge()
        setContent {
            val isDarkTheme = themeViewModel.isDarkTheme
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(TransparentSystemBar.toArgb())
                    } else {
                        SystemBarStyle.light(TransparentSystemBar.toArgb(), TransparentSystemBar.toArgb())
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(TransparentSystemBar.toArgb())
                    } else {
                        // Older Android versions need a dark scrim for white navigation icons.
                        SystemBarStyle.light(TransparentSystemBar.toArgb(), LegacyNavigationBarScrim.toArgb())
                    },
                )
            }
            CyberpunkTcgTheme(darkTheme = isDarkTheme) {
                CompanionApp(
                    isDarkTheme = isDarkTheme,
                    onDarkThemeChange = themeViewModel::changeTheme,
                )
            }
        }
    }
}
