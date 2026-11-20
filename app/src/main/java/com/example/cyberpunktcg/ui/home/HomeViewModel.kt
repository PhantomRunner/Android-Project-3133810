package com.example.cyberpunktcg.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cyberpunktcg.data.local.collection.CollectionStatistics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal sealed interface HomeStatisticsState {
    data object Loading : HomeStatisticsState
    data class Ready(val statistics: CollectionStatistics) : HomeStatisticsState
    data object Error : HomeStatisticsState
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class HomeViewModel(statisticsSource: Flow<CollectionStatistics>) : ViewModel() {
    private val retryRequests = MutableStateFlow(0L)
    val state = retryRequests.flatMapLatest {
        statisticsSource.map<CollectionStatistics, HomeStatisticsState> { HomeStatisticsState.Ready(it) }
            .onStart { emit(HomeStatisticsState.Loading) }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(HomeStatisticsState.Error)
            }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000, replayExpirationMillis = 0),
        HomeStatisticsState.Loading,
    )

    fun retry() {
        retryRequests.update { it + 1 }
    }
}
