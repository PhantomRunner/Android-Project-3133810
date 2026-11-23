package com.example.cyberpunktcg.ui.scanner

import androidx.lifecycle.ViewModelStore
import com.example.cyberpunktcg.data.local.collection.CardEntity
import com.example.cyberpunktcg.data.local.collection.CardPrintingEntity
import com.example.cyberpunktcg.data.local.collection.CardSetEntity
import com.example.cyberpunktcg.data.local.collection.CollectionPrinting
import com.example.cyberpunktcg.data.model.CardColor
import com.example.cyberpunktcg.data.model.CardType
import com.example.cyberpunktcg.ui.collection.ActiveCatalog
import java.io.File
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
class ScannerMatchingFlowTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    private val store = ViewModelStore()
    private val recognizedText = "ADAM SMASHER\nENDER OF LEGENDS"
    private val standard = printing("adam", "Adam Smasher", "Ender of Legends", "standard")
    private val foil = standard.copy(printing = standard.printing.copy(id = "foil", finish = "Foil"))

    @Before fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun retriesCatalogLookupOnceUsingTheExistingText() = runTest {
        var recognitionCalls = 0
        val catalogRequests = mutableListOf<Boolean>()
        val scanner = scanner(
            recognizeText = {
                recognitionCalls++
                recognizedText
            },
            loadCatalog = { retry ->
                catalogRequests.add(retry)
                if (retry) ActiveCatalog.Ready(listOf(standard)) else ActiveCatalog.Failed
            },
        )
        val photo = scanner.takePhoto()
        runCurrent()
        assertEquals(ScannerMatchState.Failed, scanner.uiState.matchState)

        scanner.retryMatching()
        scanner.retryMatching()
        runCurrent()

        assertEquals(listOf(false, true), catalogRequests)
        assertEquals(1, recognitionCalls)
        assertEquals(photo, scanner.uiState.photoFile)
        assertEquals(ScannerOcrState.Recognized(recognizedText), scanner.uiState.ocr)
        assertEquals(standard.card, results(scanner).single().card)
    }

    @Test
    fun ignoresACompletedLookupFromBeforeRetake() = runTest {
        val oldCatalog = CompletableDeferred<ActiveCatalog>()
        val next = printing("runner", "Signal Runner", "", "runner")
        var catalogCalls = 0
        var recognitionCalls = 0
        val scanner = scanner(
            recognizeText = { if (recognitionCalls++ == 0) recognizedText else "SIGNAL RUNNER" },
            loadCatalog = {
                if (catalogCalls++ == 0) {
                    withContext(NonCancellable) { oldCatalog.await() }
                } else {
                    ActiveCatalog.Ready(listOf(next))
                }
            },
        )
        scanner.takePhoto()
        runCurrent()
        assertEquals(ScannerMatchState.Searching, scanner.uiState.matchState)

        scanner.reset()
        assertEquals(ScannerMatchState.Idle, scanner.uiState.matchState)
        assertNull(scanner.uiState.photoFile)
        val newPhoto = scanner.takePhoto()
        runCurrent()
        assertEquals(next.card, results(scanner).single().card)

        oldCatalog.complete(ActiveCatalog.Ready(listOf(standard)))
        runCurrent()
        assertEquals(next.card, results(scanner).single().card)
        assertEquals(newPhoto, scanner.uiState.photoFile)
    }

    @Test
    fun opensConfirmationDirectlyForASingleEdition() = runTest {
        val scanner = scanner()
        scanner.takePhoto()
        runCurrent()

        scanner.selectSuggestedCard(standard.card.id)

        assertNull(scanner.uiState.selectedSuggestion)
        assertEquals(
            ManualAddState.Confirming(standard, source = ConfirmationSource.SUGGESTION),
            scanner.manualAddState,
        )
    }

    @Test
    fun returnsFromConfirmationAndAddsOnlyTheSelectedEditionOnce() = runTest {
        val writes = mutableListOf<String>()
        val pendingSave = CompletableDeferred<Boolean>()
        val scanner = scanner(
            loadCatalog = { ActiveCatalog.Ready(listOf(standard, foil)) },
            addOwnedCopy = { printingId ->
                writes.add(printingId)
                pendingSave.await()
            },
        )
        val photo = scanner.takePhoto()
        runCurrent()
        val suggestions = results(scanner)

        scanner.selectSuggestedCard(standard.card.id)
        assertEquals(suggestions.single(), scanner.uiState.selectedSuggestion)
        scanner.selectSuggestedEdition("unrelated")
        assertEquals(ManualAddState.Inactive, scanner.manualAddState)
        scanner.selectSuggestedEdition(foil.printing.id)
        assertEquals(
            ManualAddState.Confirming(foil, source = ConfirmationSource.SUGGESTION),
            scanner.manualAddState,
        )

        scanner.backFromManualAdd()
        assertEquals(ManualAddState.Inactive, scanner.manualAddState)
        assertNull(scanner.uiState.selectedSuggestion)
        assertEquals(photo, scanner.uiState.photoFile)
        assertEquals(suggestions, results(scanner))
        assertTrue(writes.isEmpty())

        scanner.selectSuggestedCard(standard.card.id)
        scanner.selectSuggestedEdition(foil.printing.id)
        scanner.confirmPrinting()
        scanner.confirmPrinting()
        runCurrent()
        assertEquals(listOf(foil.printing.id), writes)

        pendingSave.complete(true)
        runCurrent()
        assertEquals(ManualAddState.Added(foil), scanner.manualAddState)
        assertEquals(ScannerMatchState.Idle, scanner.uiState.matchState)
        assertNull(scanner.uiState.photoFile)
    }

    private fun results(scanner: ScannerViewModel): List<ScannerCardMatch> =
        (scanner.uiState.matchState as ScannerMatchState.Results).cards

    private suspend fun ScannerViewModel.takePhoto(): File {
        val request = checkNotNull(beginCapture())
        photoCaptured(request)
        return request.file
    }

    private suspend fun scanner(
        recognizeText: suspend (File) -> String = { recognizedText },
        loadCatalog: suspend (Boolean) -> ActiveCatalog = { ActiveCatalog.Ready(listOf(standard)) },
        addOwnedCopy: suspend (String) -> Boolean = { error("Unexpected collection write") },
    ): ScannerViewModel {
        val scanner = ScannerViewModel(
            photoDirectory = temporaryFolder.newFolder(),
            recognizeText = recognizeText,
            loadCatalog = loadCatalog,
            matchingDispatcher = Dispatchers.Main,
            addOwnedCopy = addOwnedCopy,
        )
        store.put("scanner", scanner)
        scanner.prepare()
        return scanner
    }

    private fun printing(cardId: String, name: String, subtitle: String, id: String) = CollectionPrinting(
        printing = CardPrintingEntity(id, cardId, "set", "004", null, null, null),
        card = CardEntity(
            id = cardId, name = name, subtitle = subtitle, type = CardType.UNIT,
            color = CardColor.BLUE, cost = null, power = null, ram = null, unitTypes = emptyList(),
            eddies = null, powerLabel = null,
        ),
        cardSet = CardSetEntity("set", "Relay District", "en"),
        owned = null,
    )
}
