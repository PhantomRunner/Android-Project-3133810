package com.example.cyberpunktcg.ui.scanner

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// Reads text with ML Kit and lets only one recognition job use this reader at a time.
internal class ScannerTextRecognizer {
    private val recognitionMutex = Mutex()

    suspend fun recognize(photo: File): String = recognitionMutex.withLock {
        withContext(Dispatchers.IO) {
            val bitmap = decodeScannerPhoto(photo, maxSide = 2048)
            try {
                ensureActive()
                val image = InputImage.fromBitmap(bitmap, 0)
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).use { recognizer ->
                    // Keep the bitmap and recognizer alive until ML Kit finishes reading the image.
                    withContext(NonCancellable) { recognizer.process(image).await().text }
                }
            } finally {
                bitmap.recycle()
            }
        }
    }
}
