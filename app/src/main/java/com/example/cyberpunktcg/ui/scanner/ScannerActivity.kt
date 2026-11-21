package com.example.cyberpunktcg.ui.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cyberpunktcg.data.local.collection.CollectionRepository
import com.example.cyberpunktcg.ui.theme.setCompanionContent
import java.io.File

class ScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val photoDirectory = File(cacheDir, "scanner_photos")
        val factory = viewModelFactory {
            initializer {
                val collection = CollectionRepository(applicationContext)
                ScannerViewModel(
                    photoDirectory = photoDirectory,
                    recognizeText = ScannerTextRecognizer()::recognize,
                    addOwnedCopy = { printingId -> collection.changeQuantity(printingId, 1) },
                )
            }
        }
        val scanner = ViewModelProvider(this, factory)[ScannerViewModel::class.java]

        setCompanionContent { _, sensors ->
            ScannerFlow(scanner, sensors.light, onBackPressedDispatcher::onBackPressed, onDone = ::finish)
        }
    }
}
