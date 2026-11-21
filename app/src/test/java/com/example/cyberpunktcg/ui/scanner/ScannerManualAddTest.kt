package com.example.cyberpunktcg.ui.scanner

import androidx.lifecycle.ViewModelStore
import com.example.cyberpunktcg.data.local.collection.CardEntity
import com.example.cyberpunktcg.data.local.collection.CardPrintingEntity
import com.example.cyberpunktcg.data.local.collection.CardSetEntity
import com.example.cyberpunktcg.data.local.collection.CollectionPrinting
import com.example.cyberpunktcg.data.model.CardColor
import com.example.cyberpunktcg.data.model.CardType
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
class ScannerManualAddTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    private val store = ViewModelStore()
    private val printing = CollectionPrinting(
        printing = CardPrintingEntity("foil", "card", "set", "004", "Retail", "Foil", null),
        card = CardEntity(
            id = "card", name = "Signal Runner", subtitle = "", type = CardType.UNIT,
            color = CardColor.BLUE, cost = null, power = null, ram = null, unitTypes = emptyList(),
            eddies = null, powerLabel = null,
        ),
        cardSet = CardSetEntity("set", "Relay District", "en"),
        owned = null,
    )

    @Before fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun confirmsTheSelectedPrintingOnceDuringAndAfterSaving() = runTest {
        val pending = CompletableDeferred<Boolean>()
        val writes = mutableListOf<String>()
        val scanner = scanner { id ->
            writes.add(id)
            pending.await()
        }
        scanner.findCardManually()
        scanner.selectPrinting(printing)
        assertTrue(writes.isEmpty())

        scanner.confirmPrinting()
        scanner.confirmPrinting()
        scanner.backFromManualAdd()
        runCurrent()
        assertEquals(ManualAddState.Confirming(printing, isSaving = true), scanner.manualAddState)
        assertEquals(listOf("foil"), writes)

        pending.complete(true)
        runCurrent()
        scanner.confirmPrinting()
        runCurrent()
        assertEquals(ManualAddState.Added(printing), scanner.manualAddState)
        assertEquals(listOf("foil"), writes)
    }

    @Test
    fun keepsTheEditionSelectedAfterASaveFailureAndAllowsRetry() = runTest {
        var attempts = 0
        val scanner = scanner {
            if (++attempts == 1) throw IOException("write failed")
            true
        }
        scanner.findCardManually()
        scanner.selectPrinting(printing)
        scanner.confirmPrinting()
        runCurrent()
        assertEquals(ManualAddState.Confirming(printing, error = ManualAddError.SAVE_FAILED), scanner.manualAddState)

        scanner.confirmPrinting()
        runCurrent()
        assertEquals(ManualAddState.Added(printing), scanner.manualAddState)
        assertEquals(2, attempts)
    }

    @Test
    fun showsTheQuantityLimitWhenTheTransactionCannotAddACopy() = runTest {
        val scanner = scanner { false }
        scanner.findCardManually()
        scanner.selectPrinting(printing)
        scanner.confirmPrinting()
        runCurrent()

        assertEquals(ManualAddState.Confirming(printing, error = ManualAddError.QUANTITY_LIMIT), scanner.manualAddState)
        scanner.backFromManualAdd()
        scanner.backFromManualAdd()
        assertEquals(ManualAddState.Inactive, scanner.manualAddState)
    }

    private fun scanner(addOwnedCopy: suspend (String) -> Boolean) =
        ScannerViewModel(temporaryFolder.newFolder(), recognizeText = { "" }, addOwnedCopy = addOwnedCopy)
            .also { store.put("scanner", it) }
}
