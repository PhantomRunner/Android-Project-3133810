package com.example.cyberpunktcg.ui.scanner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cyberpunktcg.data.local.collection.CollectionPrinting
import com.example.cyberpunktcg.ui.collection.ActiveCatalog
import java.io.File
import java.io.IOException
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class ScannerUiState(
    val photoFile: File? = null,
    val isPreparing: Boolean = true,
    val isCapturing: Boolean = false,
    val hasCaptureError: Boolean = false,
    val ocr: ScannerOcrState = ScannerOcrState.Idle,
    val matchState: ScannerMatchState = ScannerMatchState.Idle,
)

internal data class PhotoCaptureRequest(val id: Long, val file: File)

internal sealed interface ScannerOcrState {
    data object Idle : ScannerOcrState
    data object Reading : ScannerOcrState
    data class Recognized(val text: String) : ScannerOcrState
    data object Empty : ScannerOcrState
    data object Failed : ScannerOcrState
}

internal sealed interface ScannerMatchState {
    data object Idle : ScannerMatchState
    data object Searching : ScannerMatchState
    data object CatalogMissing : ScannerMatchState
    data object Failed : ScannerMatchState
    data class Results(val cards: List<ScannerCardMatch>) : ScannerMatchState
}

internal sealed interface ManualAddState {
    data object Inactive : ManualAddState
    data object Browsing : ManualAddState
    data class Confirming(
        val printing: CollectionPrinting,
        val isSaving: Boolean = false,
        val error: ManualAddError? = null,
    ) : ManualAddState
    data class Added(val printing: CollectionPrinting) : ManualAddState
}

internal enum class ManualAddError { SAVE_FAILED, QUANTITY_LIMIT }

internal class ScannerViewModel(
    private val photoDirectory: File,
    private val recognizeText: suspend (File) -> String,
    private val loadCatalog: suspend (retry: Boolean) -> ActiveCatalog,
    private val matchingDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val addOwnedCopy: suspend (String) -> Boolean,
) : ViewModel() {
    var uiState by mutableStateOf(ScannerUiState())
        private set
    var manualAddState by mutableStateOf<ManualAddState>(ManualAddState.Inactive)
        private set

    private var requestId = 0L
    private var pendingCapture: PhotoCaptureRequest? = null
    private var photoJob: Job? = null
    private var closed = false


    private val cachePreparation = viewModelScope.async(Dispatchers.IO) {
        try {
            ensurePhotoDirectory()
            val files = photoDirectory.listFiles() ?: throw IOException("Unable to read scanner photo directory")
            files.filter { it.isFile }.forEach { file ->
                if (!file.delete() && file.exists()) throw IOException("Unable to remove old photograph")
            }
            true
        } catch (_: IOException) {
            false
        }
    }

    suspend fun prepare() {
        if (!uiState.isPreparing) return
        val prepared = cachePreparation.await()
        if (!closed) uiState = uiState.copy(isPreparing = false, hasCaptureError = !prepared)
    }

    suspend fun beginCapture(): PhotoCaptureRequest? {
        if (closed || uiState.isPreparing || uiState.isCapturing || uiState.photoFile != null) return null
        val id = ++requestId
        uiState = uiState.copy(isCapturing = true, hasCaptureError = false)
        var file: File? = null
        try {
            withContext(Dispatchers.IO) {
                ensurePhotoDirectory()
                file = File.createTempFile("photo_", ".jpg", photoDirectory)
            }
        } catch (error: CancellationException) {
            deletePhoto(file)
            if (id == requestId) cancelPendingCapture()
            throw error
        } catch (_: IOException) {
            if (id == requestId) uiState = uiState.copy(isCapturing = false, hasCaptureError = true)
            return null
        }
        if (closed || id != requestId) {
            deletePhoto(file)
            return null
        }
        return PhotoCaptureRequest(id, checkNotNull(file)).also { pendingCapture = it }
    }

    fun photoCaptured(request: PhotoCaptureRequest) {
        if (closed || request != pendingCapture) {
            if (request.file != uiState.photoFile) deletePhoto(request.file)
            return
        }
        pendingCapture = null
        uiState = uiState.copy(photoFile = request.file, isCapturing = false, hasCaptureError = false)
        recognizePhoto(request.file)
    }

    fun captureFailed(request: PhotoCaptureRequest) {
        if (request.file != uiState.photoFile) deletePhoto(request.file)
        if (!closed && request == pendingCapture) {
            pendingCapture = null
            uiState = uiState.copy(isCapturing = false, hasCaptureError = true)
        }
    }

    fun cancelPendingCapture() {
        if (!uiState.isCapturing) return
        ++requestId
        deletePhoto(pendingCapture?.file)
        pendingCapture = null
        uiState = uiState.copy(isCapturing = false, hasCaptureError = false)
    }

    fun reset() {
        ++requestId
        photoJob?.cancel()
        photoJob = null
        deletePhoto(pendingCapture?.file)
        deletePhoto(uiState.photoFile)
        pendingCapture = null
        uiState = uiState.copy(
            photoFile = null,
            isCapturing = false,
            hasCaptureError = false,
            ocr = ScannerOcrState.Idle,
            matchState = ScannerMatchState.Idle,
        )
    }

    fun retryRecognition() {
        val state = uiState
        val photo = state.photoFile ?: return
        if (state.ocr == ScannerOcrState.Failed) recognizePhoto(photo)
    }

    fun retryMatching() {
        val state = uiState
        if (state.matchState != ScannerMatchState.Failed) return
        val text = (state.ocr as? ScannerOcrState.Recognized)?.text ?: return
        val photoRequestId = requestId
        uiState = state.copy(matchState = ScannerMatchState.Searching)
        photoJob = viewModelScope.launch { findMatches(text, photoRequestId, retry = true) }
    }

    private fun recognizePhoto(photo: File) {
        val photoRequestId = requestId
        uiState = uiState.copy(ocr = ScannerOcrState.Reading, matchState = ScannerMatchState.Idle)
        photoJob = viewModelScope.launch {
            val result = try {
                val text = recognizeText(photo)
                if (text.isBlank()) ScannerOcrState.Empty else ScannerOcrState.Recognized(text)
            } catch (_: Exception) {
                ensureActive()
                ScannerOcrState.Failed
            }
            ensureActive()
            if (photoRequestId != requestId) return@launch
            if (result is ScannerOcrState.Recognized) {
                uiState = uiState.copy(ocr = result, matchState = ScannerMatchState.Searching)
                findMatches(result.text, photoRequestId, retry = false)
            } else {
                uiState = uiState.copy(ocr = result)
            }
        }
    }

    private suspend fun findMatches(text: String, photoRequestId: Long, retry: Boolean) {
        val result = try {
            when (val catalog = loadCatalog(retry)) {
                ActiveCatalog.Missing -> ScannerMatchState.CatalogMissing
                ActiveCatalog.Failed -> ScannerMatchState.Failed
                is ActiveCatalog.Ready -> withContext(matchingDispatcher) {
                    ScannerMatchState.Results(matchScannerCards(text, catalog.printings))
                }
            }
        } catch (_: Exception) {
            coroutineContext.ensureActive()
            ScannerMatchState.Failed
        }
        coroutineContext.ensureActive()
        if (photoRequestId == requestId) uiState = uiState.copy(matchState = result)
    }

    fun findCardManually() {
        if (manualAddState != ManualAddState.Inactive) return
        cancelPendingCapture()
        manualAddState = ManualAddState.Browsing
    }

    fun selectPrinting(printing: CollectionPrinting) {
        if (manualAddState == ManualAddState.Browsing) {
            manualAddState = ManualAddState.Confirming(printing)
        }
    }

    fun backFromManualAdd() {
        when (val state = manualAddState) {
            ManualAddState.Browsing -> manualAddState = ManualAddState.Inactive
            is ManualAddState.Confirming -> if (!state.isSaving) manualAddState = ManualAddState.Browsing
            else -> Unit
        }
    }

    fun confirmPrinting() {
        val confirmation = manualAddState as? ManualAddState.Confirming ?: return
        if (confirmation.isSaving) return
        manualAddState = confirmation.copy(isSaving = true, error = null)
        viewModelScope.launch {
            val error = try {
                if (addOwnedCopy(confirmation.printing.printing.id)) null else ManualAddError.QUANTITY_LIMIT
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ManualAddError.SAVE_FAILED
            }
            if (error == null) {
                reset()
                manualAddState = ManualAddState.Added(confirmation.printing)
            } else {
                manualAddState = confirmation.copy(error = error)
            }
        }
    }

    fun scanAnother() {
        if (manualAddState is ManualAddState.Added) manualAddState = ManualAddState.Inactive
    }

    override fun onCleared() {
        closed = true
        reset()
    }

    private fun ensurePhotoDirectory() {
        if (!photoDirectory.isDirectory && !photoDirectory.mkdirs()) {
            throw IOException("Unable to create scanner photo directory")
        }
    }

    private fun deletePhoto(file: File?) {

        if (file != null) Dispatchers.IO.asExecutor().execute { file.delete() }
    }
}
