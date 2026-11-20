package com.example.cyberpunktcg.data.local.collection

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CollectionDatabaseTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "collection-test-${UUID.randomUUID()}.db"
    private lateinit var database: CollectionDatabase

    @Before
    fun openDatabase() {
        database = Room.databaseBuilder(context, CollectionDatabase::class.java, databaseName).build()
    }

    @After
    fun deleteDatabase() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun editionsKeepIndependentQuantitiesAfterImportAndReopen() = runBlocking {
        val catalog = parseCollectionCatalog(SAMPLE_CATALOG)
        val dao = database.collectionDao()
        dao.importCatalog(catalog)

        dao.changeQuantity("paper", 1)
        dao.changeQuantity("paper", 1)
        dao.changeQuantity("foil", 1)
        val updatedCard = catalog.cards.single().copy(name = "Signal Relay")
        dao.importCatalog(catalog.copy(cards = listOf(updatedCard)))
        database.close()
        openDatabase()

        val printings = database.collectionDao().observePrintings().first().associateBy { it.printing.id }
        assertEquals(2, printings.getValue("paper").quantity)
        assertEquals(1, printings.getValue("foil").quantity)
        val card = printings.getValue("foil").card
        assertEquals("Signal Relay", card.name)
        assertEquals(0, card.power)
        assertNull(card.cost)
        assertEquals(listOf("Tech"), card.unitTypes)
    }

    @Test
    fun rapidChangesAreAtomicAndZeroRemovesOwnership() = runBlocking {
        val dao = database.collectionDao()
        dao.importCatalog(parseCollectionCatalog(SAMPLE_CATALOG))
        coroutineScope {
            repeat(12) { launch(Dispatchers.Default) { dao.changeQuantity("paper", 1) } }
        }
        assertEquals(12, dao.observePrintings().first().single { it.printing.id == "paper" }.quantity)

        coroutineScope {
            repeat(13) { launch(Dispatchers.Default) { dao.changeQuantity("paper", -1) } }
        }
        assertNull(dao.observePrintings().first().single { it.printing.id == "paper" }.owned)
    }

    @Test
    fun statisticsObserveQuantityChangesAndSurviveReimportAndReopen() = runBlocking {
        val catalog = parseCollectionCatalog(SAMPLE_CATALOG)
        val dao = database.collectionDao()
        dao.importCatalog(catalog)
        val repository = CollectionRepository(context, dao)
        val updates = Channel<CollectionStatistics>(Channel.UNLIMITED)
        val observer = launch { repository.statistics.collect { updates.send(it) } }
        try {
            assertEquals(CollectionStatistics(0L, 0, 0), withTimeout(5_000) { updates.receive() })
            repository.changeQuantity("paper", 1)
            assertEquals(CollectionStatistics(1L, 1, 1), withTimeout(5_000) { updates.receive() })
            repository.changeQuantity("paper", 1)
            assertEquals(CollectionStatistics(2L, 1, 1), withTimeout(5_000) { updates.receive() })
            repository.changeQuantity("foil", 1)
            assertEquals(CollectionStatistics(3L, 1, 2), withTimeout(5_000) { updates.receive() })
            repository.changeQuantity("foil", -1)
            assertEquals(CollectionStatistics(2L, 1, 1), withTimeout(5_000) { updates.receive() })
            dao.importCatalog(catalog)
        } finally {
            observer.cancelAndJoin()
            updates.close()
        }
        database.close()
        openDatabase()
        val reopened = CollectionRepository(context, database.collectionDao())
        assertEquals(CollectionStatistics(2L, 1, 1), reopened.statistics.first())
    }

    private companion object {
        const val SAMPLE_CATALOG = """
            {
              "version": 1,
              "sets": [{"id": "relay", "name": "Relay Test Set", "language": "en"}],
              "cards": [{
                "id": "signal", "name": "Signal Coil", "subtitle": "", "type": "GEAR",
                "color": "BLUE", "cost": null, "power": 0, "ram": 1, "unitTypes": ["Tech"]
              }],
              "printings": [
                {"id": "paper", "cardId": "signal", "setId": "relay", "collectorNumber": "001"},
                {"id": "foil", "cardId": "signal", "setId": "relay", "collectorNumber": "001", "finish": "Foil"}
              ]
            }
        """
    }
}
