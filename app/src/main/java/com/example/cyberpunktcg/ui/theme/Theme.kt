package com.example.cyberpunktcg.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cyberpunktcg.data.model.ThemeMode
import com.example.cyberpunktcg.sensors.SensorsViewModel

@Composable
fun CyberpunkTcgTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}

internal fun ComponentActivity.setCompanionContent(
    content: @Composable (ThemeViewModel, SensorsViewModel) -> Unit,
) {
    val theme = ViewModelProvider(this)[ThemeViewModel::class.java]
    val sensors = ViewModelProvider(this)[SensorsViewModel::class.java]
    enableEdgeToEdge()
    setContent {
        val mode = theme.mode
        val light = if (mode == ThemeMode.AUTO) {
            sensors.light.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.RESUMED).value
        } else null
        val darkTheme = mode.resolveDarkTheme(light?.automaticDarkTheme, isSystemInDarkTheme())
        SideEffect {
            enableEdgeToEdge(
                statusBarStyle = if (darkTheme) {
                    SystemBarStyle.dark(TransparentSystemBar.toArgb())
                } else {
                    SystemBarStyle.light(TransparentSystemBar.toArgb(), TransparentSystemBar.toArgb())
                },
                navigationBarStyle = if (darkTheme) {
                    SystemBarStyle.dark(TransparentSystemBar.toArgb())
                } else {
                    // Older Android versions need a dark scrim for white navigation icons.
                    SystemBarStyle.light(TransparentSystemBar.toArgb(), LegacyNavigationBarScrim.toArgb())
                },
            )
        }
        CyberpunkTcgTheme(darkTheme = darkTheme) { content(theme, sensors) }
    }
}
