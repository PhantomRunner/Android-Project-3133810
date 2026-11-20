package com.example.cyberpunktcg.ui.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cyberpunktcg.R
import com.example.cyberpunktcg.navigation.AppTopBar
import com.example.cyberpunktcg.ui.theme.setCompanionContent
import java.io.File

class ScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val photoDirectory = File(cacheDir, "scanner_photos")
        val factory = viewModelFactory { initializer { ScannerViewModel(photoDirectory) } }
        val scanner = ViewModelProvider(this, factory)[ScannerViewModel::class.java]

        setCompanionContent { _, sensors ->
            Scaffold(topBar = {
                AppTopBar(
                    titleResId = R.string.scanner,
                    showBackButton = true,
                    onBackClick = onBackPressedDispatcher::onBackPressed,
                )
            }) { innerPadding ->
                Box(Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding)) {
                    ScannerScreen(viewModel = scanner, light = sensors.light)
                }
            }
        }
    }
}
