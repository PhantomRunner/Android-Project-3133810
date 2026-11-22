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

internal data class CollectionUiState(
    val printings: List<CollectionPrinting>? = null,
    val catalog: CatalogStatus = CatalogStatus.Loading,
    val readFailed: Boolean = false,
    val saveFailed: Boolean = false,
) {
    fun activeCatalogPrintings(): List<CollectionPrinting>? {
        val readyCatalog = catalog as? CatalogStatus.Ready ?: return null
        val activePrintings = printings?.filter { it.printing.id in readyCatalog.printingIds } ?: return null
        // Room can emit the imported rows after loadCatalog() returns.
        return activePrintings.takeIf { it.size == readyCatalog.printingIds.size }
    }
}

internal class CollectionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CollectionRepository(application)
    private val mutableState = MutableStateFlow(CollectionUiState())
    val state = mutableState.asStateFlow()

    private var observeJob: Job? = null
    private var importJob: Job? = null
    private val quantityChanges = Mutex()

    init {
        retry()
    }

    fun retry() {
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
