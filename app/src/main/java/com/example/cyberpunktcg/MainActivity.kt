package com.example.cyberpunktcg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cyberpunktcg.navigation.CompanionApp
import com.example.cyberpunktcg.data.model.ThemeMode
import com.example.cyberpunktcg.sensors.SensorsViewModel
import com.example.cyberpunktcg.ui.theme.CyberpunkTcgTheme
import com.example.cyberpunktcg.ui.theme.LegacyNavigationBarScrim
import com.example.cyberpunktcg.ui.theme.ThemeViewModel
import com.example.cyberpunktcg.ui.theme.TransparentSystemBar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeViewModel = ViewModelProvider(this)[ThemeViewModel::class.java]
        val sensors = ViewModelProvider(this)[SensorsViewModel::class.java]
        enableEdgeToEdge()
        setContent {
            val mode = themeViewModel.mode
            val light = if (mode == ThemeMode.AUTO) {
                sensors.light.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.RESUMED).value
            } else null
            val isDarkTheme = mode.resolveDarkTheme(light?.automaticDarkTheme, isSystemInDarkTheme())
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
                    themeMode = mode,
                    onThemeModeChange = themeViewModel::changeMode,
                    sensors = sensors,
                )
            }
        }
    }
}
