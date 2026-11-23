package com.example.cyberpunktcg.ui.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cyberpunktcg.data.local.collection.CollectionRepository
import com.example.cyberpunktcg.ui.collection.CollectionViewModel
import com.example.cyberpunktcg.ui.theme.setCompanionContent
import java.io.File

class ScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val photoDirectory = File(cacheDir, "scanner_photos")
        val collection = ViewModelProvider(this)[CollectionViewModel::class.java]
        val factory = viewModelFactory {
            initializer {
                val repository = CollectionRepository(applicationContext)
                ScannerViewModel(
                    photoDirectory = photoDirectory,
                    recognizeText = ScannerTextRecognizer()::recognize,
                    loadCatalog = { retry ->
                        if (retry) collection.retry()
                        collection.awaitActiveCatalog()
                    },
                    addOwnedCopy = { printingId -> repository.changeQuantity(printingId, 1) },
                )
            }
        }
        val scanner = ViewModelProvider(this, factory)[ScannerViewModel::class.java]

        setCompanionContent { _, sensors ->
            ScannerFlow(
                scanner = scanner,
                collection = collection,
                light = sensors.light,
                onBack = onBackPressedDispatcher::onBackPressed,
                onDone = ::finish,
            )
        }
    }
}
