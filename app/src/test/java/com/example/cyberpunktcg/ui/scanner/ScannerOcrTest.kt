package com.example.cyberpunktcg.ui.scanner

import androidx.lifecycle.ViewModelStore
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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

    private suspend fun scanner(recognizeText: suspend (File) -> String): ScannerViewModel {
        val scanner = ScannerViewModel(temporaryFolder.newFolder(), recognizeText) {
            error("Text recognition must not change the collection")
        }
        store.put("scanner", scanner)
        scanner.prepare()
        return scanner
    }
}
