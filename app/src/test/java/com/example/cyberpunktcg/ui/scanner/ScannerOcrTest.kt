package com.example.cyberpunktcg.ui.scanner

import androidx.lifecycle.ViewModelStore
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerOcrTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    private val store = ViewModelStore()

    @Before fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun recognizesAnAcceptedPhotoOnceAndPreservesTheText() = runTest {
        val pending = CompletableDeferred<String>()
        val photos = mutableListOf<File>()
        val scanner = scanner { photo ->
            photos.add(photo)
            pending.await()
        }
        val capture = checkNotNull(scanner.beginCapture())
        assertEquals(ScannerOcrState.Idle, scanner.uiState.ocr)

        scanner.photoCaptured(capture)
        scanner.photoCaptured(capture)
        assertEquals(ScannerOcrState.Reading, scanner.uiState.ocr)
        runCurrent()
        assertEquals(listOf(capture.file), photos)
        assertTrue(capture.file.isFile)

        val text = "Adam Smasher\nEnder of Legends"
        pending.complete(text)
        runCurrent()
        assertEquals(ScannerOcrState.Recognized(text), scanner.uiState.ocr)
    }

    @Test
    fun displaysTheEmptyStateForWhitespace() = runTest {
        val scanner = scanner { " \n" }
        scanner.photoCaptured(checkNotNull(scanner.beginCapture()))
        runCurrent()

        assertEquals(ScannerOcrState.Empty, scanner.uiState.ocr)
    }

    @Test
    fun keepsThePhotoWhenRecognitionFails() = runTest {
        val scanner = scanner { throw IOException("Could not read the image") }
        val capture = checkNotNull(scanner.beginCapture())
        scanner.photoCaptured(capture)
        runCurrent()

        assertEquals(ScannerOcrState.Failed, scanner.uiState.ocr)
        assertEquals(capture.file, scanner.uiState.photoFile)
    }

    @Test
    fun retriesTheSamePhotoOnceWhenPressedRepeatedly() = runTest {
        val retryResult = CompletableDeferred<String>()
        val photos = mutableListOf<File>()
        val scanner = scanner { photo ->
            photos.add(photo)
            if (photos.size == 1) throw IOException("Temporary recognition failure")
            retryResult.await()
        }
        val capture = checkNotNull(scanner.beginCapture())
        scanner.photoCaptured(capture)
        runCurrent()
        assertEquals(ScannerOcrState.Failed, scanner.uiState.ocr)

        scanner.retryRecognition()
        scanner.retryRecognition()
        runCurrent()
        assertEquals(ScannerOcrState.Reading, scanner.uiState.ocr)
        assertEquals(listOf(capture.file, capture.file), photos)

        retryResult.complete("Recognized on retry")
        runCurrent()
        scanner.retryRecognition()
        runCurrent()
        assertEquals(ScannerOcrState.Recognized("Recognized on retry"), scanner.uiState.ocr)
        assertEquals(2, photos.size)
    }

    @Test
    fun discardsRecognitionFromBeforeRetake() = runTest {
        val firstResult = CompletableDeferred<String>()
        var attempts = 0
        val scanner = scanner {
            if (++attempts == 1) withContext(NonCancellable) { firstResult.await() } else "New photo"
        }
        scanner.photoCaptured(checkNotNull(scanner.beginCapture()))
        runCurrent()
        assertEquals(ScannerOcrState.Reading, scanner.uiState.ocr)

        scanner.reset()
        assertNull(scanner.uiState.photoFile)
        assertEquals(ScannerOcrState.Idle, scanner.uiState.ocr)
        val secondCapture = checkNotNull(scanner.beginCapture())
        scanner.photoCaptured(secondCapture)
        runCurrent()

        firstResult.complete("Old photo")
        runCurrent()
        assertEquals(ScannerOcrState.Recognized("New photo"), scanner.uiState.ocr)
        assertEquals(secondCapture.file, scanner.uiState.photoFile)
    }

    @Test
    fun retainsRecognitionWhenReturningFromManualSearch() = runTest {
        val pending = CompletableDeferred<String>()
        var attempts = 0
        val scanner = scanner {
            attempts++
            pending.await()
        }
        val capture = checkNotNull(scanner.beginCapture())
        scanner.photoCaptured(capture)
        runCurrent()

        scanner.findCardManually()
        assertEquals(ManualAddState.Browsing, scanner.manualAddState)
        pending.complete("Recognized while browsing")
        runCurrent()
        scanner.backFromManualAdd()

        assertEquals(ManualAddState.Inactive, scanner.manualAddState)
        assertEquals(capture.file, scanner.uiState.photoFile)
        assertEquals(ScannerOcrState.Recognized("Recognized while browsing"), scanner.uiState.ocr)
        assertEquals(1, attempts)
    }

    @Test
    fun discardsRecognitionAfterLeavingScanner() = runTest {
        val pending = CompletableDeferred<String>()
        val scanner = scanner { withContext(NonCancellable) { pending.await() } }
        scanner.photoCaptured(checkNotNull(scanner.beginCapture()))
        runCurrent()

        store.clear()
        pending.complete("Finished after exit")
        runCurrent()

        assertNull(scanner.uiState.photoFile)
        assertEquals(ScannerOcrState.Idle, scanner.uiState.ocr)
    }

    private suspend fun scanner(recognizeText: suspend (File) -> String): ScannerViewModel {
        val scanner = ScannerViewModel(temporaryFolder.newFolder(), recognizeText) {
            error("Text recognition must not change the collection")
        }
        store.put("scanner", scanner)
        scanner.prepare()
        return scanner
    }
}
