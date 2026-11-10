package com.example.cyberpunktcg.data.local.collection

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [CardEntity::class, CardSetEntity::class, CardPrintingEntity::class, OwnedPrintingEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(CollectionConverters::class)
internal abstract class CollectionDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao

    companion object {
        @Volatile private var instance: CollectionDatabase? = null

        fun get(context: Context): CollectionDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                CollectionDatabase::class.java,
                "collection.db",
            ).build().also { instance = it }
        }
    }
}

@Dao
internal abstract class CollectionDao {
    @Transaction
    @Query("SELECT * FROM card_printings ORDER BY id")
    abstract fun observePrintings(): Flow<List<CollectionPrinting>>

    @Transaction
    open suspend fun importCatalog(catalog: CollectionCatalog) {
        // Upsert keeps existing rows and their ownership references intact.
        upsertSets(catalog.sets)
        upsertCards(catalog.cards)
        upsertPrintings(catalog.printings)
    }

    @Transaction
    open suspend fun changeQuantity(printingId: String, delta: Int) {
        require(delta == -1 || delta == 1)
        val current = findQuantity(printingId) ?: 0
        val quantity = (current.toLong() + delta).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        if (quantity == 0) {
            deleteOwned(printingId)
        } else {
            upsertOwned(OwnedPrintingEntity(printingId, quantity))
        }
    }

    @Upsert
    protected abstract suspend fun upsertSets(sets: List<CardSetEntity>)

    @Upsert
    protected abstract suspend fun upsertCards(cards: List<CardEntity>)

    @Upsert
    protected abstract suspend fun upsertPrintings(printings: List<CardPrintingEntity>)

    @Upsert
    protected abstract suspend fun upsertOwned(printing: OwnedPrintingEntity)

    @Query("SELECT quantity FROM owned_printings WHERE printingId = :printingId")
    protected abstract suspend fun findQuantity(printingId: String): Int?

    @Query("DELETE FROM owned_printings WHERE printingId = :printingId")
    protected abstract suspend fun deleteOwned(printingId: String)
}
