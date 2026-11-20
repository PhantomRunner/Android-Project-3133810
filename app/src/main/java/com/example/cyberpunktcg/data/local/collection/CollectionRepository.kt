package com.example.cyberpunktcg.data.local.collection

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

internal const val COLLECTION_ASSET_DIRECTORY = "collection"

internal class CollectionRepository(
    context: Context,
    private val dao: CollectionDao = CollectionDatabase.get(context).collectionDao(),
) {
    private val assets = context.applicationContext.assets

    val printings = dao.observePrintings()
    val statistics = printings.map(::calculateCollectionStatistics)
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

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
