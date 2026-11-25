package com.example.cyberpunktcg.ui.collection

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cyberpunktcg.data.local.collection.CollectionPrinting
import com.example.cyberpunktcg.data.local.collection.CollectionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal sealed interface CatalogStatus {
    data object Loading : CatalogStatus
    data object Missing : CatalogStatus
    data object Failed : CatalogStatus
    data class Ready(val printingIds: Set<String>) : CatalogStatus
}

internal sealed interface ActiveCatalog {
    data object Missing : ActiveCatalog
    data object Failed : ActiveCatalog
    data class Ready(val printings: List<CollectionPrinting>) : ActiveCatalog
}

internal data class CollectionUiState(
    val printings: List<CollectionPrinting>? = null,
    val catalog: CatalogStatus = CatalogStatus.Loading,
    val readFailed: Boolean = false,
    val saveFailed: Boolean = false,
) {
    // Keep the catalog view to the IDs from the current asset. Owned can still use older rows.
    fun activeCatalogPrintings(): List<CollectionPrinting>? {
        val readyCatalog = catalog as? CatalogStatus.Ready ?: return null
        val activePrintings = printings?.filter { it.printing.id in readyCatalog.printingIds } ?: return null
        // Room can emit the imported rows after loadCatalog() returns.
        return activePrintings.takeIf { it.size == readyCatalog.printingIds.size }
    }
}

// Loads the catalog, observes stored printings and handles changes to owned quantities.
internal class CollectionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CollectionRepository(application)
    // The ViewModel updates the latest snapshot in MutableStateFlow. Screens get a read-only StateFlow.
    private val mutableState = MutableStateFlow(CollectionUiState())
    val state = mutableState.asStateFlow()

    private var observeJob: Job? = null
    private var importJob: Job? = null
    private val quantityChanges = Mutex()

    init {
        retry()
    }

    // Scanner matching waits until the catalog is ready, missing or failed to load.
    suspend fun awaitActiveCatalog(): ActiveCatalog = state.mapNotNull { snapshot ->
        when {
            snapshot.readFailed || snapshot.catalog == CatalogStatus.Failed -> ActiveCatalog.Failed
            snapshot.catalog == CatalogStatus.Missing -> ActiveCatalog.Missing
            else -> snapshot.activeCatalogPrintings()?.let { ActiveCatalog.Ready(it) }
        }
    }.first()

    fun retry() {
        // Watch Room separately from the import so stored cards can be read even without the asset.
        if (observeJob?.isActive != true) {
            mutableState.update { it.copy(readFailed = false) }
            observeJob = viewModelScope.launch {
                repository.printings.catch { error ->
                    Log.w(TAG, "Could not read collection", error)
                    mutableState.update { it.copy(readFailed = true) }
                }.collect { printings ->
                    mutableState.update { it.copy(printings = printings) }
                }
            }
        }
        if (importJob?.isActive != true && mutableState.value.catalog !is CatalogStatus.Ready) {
            mutableState.update { it.copy(catalog = CatalogStatus.Loading) }
            importJob = viewModelScope.launch {
                val status = try {
                    val ids = repository.loadCatalog()
                    if (ids == null) CatalogStatus.Missing else CatalogStatus.Ready(ids)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.w(TAG, "Could not load local catalog", error)
                    CatalogStatus.Failed
                }
                mutableState.update { it.copy(catalog = status) }
            }
        }
    }

    // Queue taps in this ViewModel. Room sends the new quantities back through its flow after each write.
    fun changeQuantity(printingId: String, delta: Int) {
        viewModelScope.launch {
            quantityChanges.withLock {
                val failed = try {
                    repository.changeQuantity(printingId, delta)
                    false
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.w(TAG, "Could not update collection quantity", error)
                    true
                }
                mutableState.update { it.copy(saveFailed = failed) }
            }
        }
    }

    private companion object {
        const val TAG = "CollectionViewModel"
    }
}
