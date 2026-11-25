package com.example.cyberpunktcg

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.cyberpunktcg.navigation.CompanionApp
import com.example.cyberpunktcg.ui.scanner.ScannerActivity
import com.example.cyberpunktcg.ui.theme.setCompanionContent

// Android opens this Activity from the launcher. It sets up the themed Compose app.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCompanionContent { theme, sensors ->
            CompanionApp(
                themeMode = theme.mode,
                onThemeModeChange = theme::changeMode,
                sensors = sensors,
                // The scanner runs in its own Activity and returns here when it closes.
                onOpenScanner = {
                    startActivity(Intent(this, ScannerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
                },
            )
        }
    }
}
