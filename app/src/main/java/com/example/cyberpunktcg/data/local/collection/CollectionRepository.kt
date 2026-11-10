package com.example.cyberpunktcg.data.local.collection

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

internal const val COLLECTION_ASSET_DIRECTORY = "collection"

internal class CollectionRepository(context: Context) {
    private val assets = context.applicationContext.assets
    private val dao = CollectionDatabase.get(context).collectionDao()

    val printings = dao.observePrintings()

    suspend fun loadCatalog(): Set<String>? {
        val catalog = withContext(Dispatchers.IO) {
            val input = try {
                assets.open("$COLLECTION_ASSET_DIRECTORY/catalog.json")
            } catch (_: FileNotFoundException) {
                return@withContext null
            }
            input.bufferedReader().use { parseCollectionCatalog(it.readText()) }
        } ?: return null

        dao.importCatalog(catalog)
        return catalog.printings.map { it.id }.toSet()
    }

    suspend fun changeQuantity(printingId: String, delta: Int) {
        dao.changeQuantity(printingId, delta)
    }
}
