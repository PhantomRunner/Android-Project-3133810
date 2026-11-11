package com.example.cyberpunktcg.data.local.decks

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cyberpunktcg.data.local.collection.CollectionDatabase
import com.example.cyberpunktcg.data.local.collection.parseCollectionCatalog
import com.example.cyberpunktcg.data.model.Deck
import com.example.cyberpunktcg.data.model.DeckItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DeckDatabaseTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val name = "deck-test-${UUID.randomUUID()}.db"
    private var database: CollectionDatabase? = null

    @After
    fun cleanup() {
        database?.close()
        context.deleteDatabase(name)
    }

    private fun open(): CollectionDatabase = Room.databaseBuilder(context, CollectionDatabase::class.java, name)
        .build().also { database = it }

    @Test
    fun savedDeckSurvivesReimportOwnershipRemovalAndReopen() = runBlocking {
        val db = open()
        val collection = db.collectionDao()
        val snapshot = parseCollectionCatalog(SAMPLE_CATALOG)
        collection.importCatalog(snapshot)
        collection.changeQuantity("main-paper", 1)
        collection.changeQuantity("main-foil", 1)
        collection.changeQuantity("main-foil", 1)
        collection.changeQuantity("legend-paper", 1)
        val repository = RoomDeckRepository(context, db)
        assertEquals(3, repository.catalog.first().ownedQuantities.getValue("main"))
        val deck = Deck("saved", "Local deck", listOf("legend"), listOf(DeckItem("main", 2)))
        repository.createDeck(deck)
        assertTrue(repository.saveDeck(deck))

        try {
            repository.saveDeck(deck.copy(name = "Invalid write", items = listOf(DeckItem("missing-card", 1))))
            fail("Unknown card should roll back the entire save")
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            assertEquals(deck, repository.decks.first().single())
        }
        collection.changeQuantity("main-paper", -1)
        repeat(2) { collection.changeQuantity("main-foil", -1) }
        collection.importCatalog(snapshot)
        db.close()

        val reopened = RoomDeckRepository(context, open())
        assertEquals(deck, reopened.decks.first().single())
        assertEquals(0, reopened.catalog.first().ownedQuantities.getValue("main"))
        reopened.deleteDeck(deck.id)
        assertTrue(reopened.decks.first().isEmpty())
    }

    @Test
    fun upgradingVersionOneKeepsTheExistingCollection() = runBlocking {
        // Recreate the shipped schema, then let Room perform the normal upgrade on open.
        val schema = instrumentation.context.assets.open("${CollectionDatabase::class.java.name}/1.json")
            .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        context.openOrCreateDatabase(name, android.content.Context.MODE_PRIVATE, null).apply {
            val entities = schema.getJSONArray("entities")
            repeat(entities.length()) { index ->
                val entity = entities.getJSONObject(index)
                val tableName = entity.getString("tableName")
                execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", tableName))
                entity.optJSONArray("indices")?.let { indices ->
                    repeat(indices.length()) { indexPosition ->
                        execSQL(indices.getJSONObject(indexPosition).getString("createSql").replace("\${TABLE_NAME}", tableName))
                    }
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            repeat(setup.length()) { execSQL(setup.getString(it)) }
            execSQL("INSERT INTO cards VALUES ('main', 'Test Signal', '', 'GEAR', 'BLUE', NULL, 0, 1, '[]', NULL, NULL)")
            execSQL("INSERT INTO card_sets VALUES ('test', 'Test Set', 'en')")
            execSQL("INSERT INTO card_printings VALUES ('paper', 'main', 'test', '001', NULL, NULL, NULL)")
            execSQL("INSERT INTO owned_printings VALUES ('paper', 2)")
            version = 1
            close()
        }
        val db = open()
        val printing = db.collectionDao().observePrintings().first().single()
        assertEquals(2, printing.quantity)
        assertEquals(0, printing.card.power)
        val repository = RoomDeckRepository(context, db)
        assertTrue(repository.decks.first().isEmpty())
        repository.createDeck(Deck("after-upgrade", "Upgraded"))
        assertEquals("Upgraded", repository.decks.first().single().name)
    }

    private companion object {
        const val SAMPLE_CATALOG = """
          {"version":1,
           "sets":[{"id":"test","name":"Test Set","language":"en"}],
           "cards":[
             {"id":"legend","name":"Relay Leader","subtitle":"","type":"LEGEND","color":"BLUE",
              "cost":null,"power":null,"ram":2,"unitTypes":[]},
             {"id":"main","name":"Test Signal","subtitle":"","type":"GEAR","color":"BLUE",
              "cost":1,"power":0,"ram":1,"unitTypes":[]}
           ],
           "printings":[
             {"id":"legend-paper","cardId":"legend","setId":"test","collectorNumber":"001"},
             {"id":"main-paper","cardId":"main","setId":"test","collectorNumber":"002"},
             {"id":"main-foil","cardId":"main","setId":"test","collectorNumber":"002","finish":"Foil"}
           ]}
        """
    }
}
