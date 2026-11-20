package com.example.cyberpunktcg.ui.home

import androidx.lifecycle.ViewModelStore
import com.example.cyberpunktcg.data.local.collection.CollectionStatistics
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val store = ViewModelStore()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun loadingDoesNotPretendTheCollectionIsEmpty() = runTest {
        val model = HomeViewModel(flow { awaitCancellation() }).also { store.put("home", it) }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect {} }
        runCurrent()
        assertEquals(HomeStatisticsState.Loading, model.state.value)
    }

    @Test
    fun successfulEmptyCollectionAndLaterChangesAreDisplayed() = runTest {
        val source = MutableStateFlow(CollectionStatistics(0L, 0, 0))
        val model = HomeViewModel(source).also { store.put("home", it) }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect {} }
        runCurrent()
        assertEquals(HomeStatisticsState.Ready(CollectionStatistics(0L, 0, 0)), model.state.value)
        source.value = CollectionStatistics(3L, 1, 2)
        runCurrent()
        assertEquals(HomeStatisticsState.Ready(CollectionStatistics(3L, 1, 2)), model.state.value)
    }

    @Test
    fun readFailureIsVisibleAndRetryRestartsTheSource() = runTest {
        var attempts = 0
        val expected = CollectionStatistics(3L, 1, 2)
        val source = flow {
            if (attempts++ == 0) throw IOException("Synthetic read failure")
            emit(expected)
        }
        val model = HomeViewModel(source).also { store.put("home", it) }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect {} }
        runCurrent()
        assertEquals(HomeStatisticsState.Error, model.state.value)
        model.retry()
        runCurrent()
        assertEquals(2, attempts)
        assertEquals(HomeStatisticsState.Ready(expected), model.state.value)
    }

    @Test
    fun cancellationIsNotShownAsAReadFailure() = runTest {
        val model = HomeViewModel(flow { throw CancellationException("Screen observation cancelled") })
            .also { store.put("home", it) }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect {} }
        runCurrent()
        assertEquals(HomeStatisticsState.Loading, model.state.value)
    }

    @Test
    fun observationStopsWithoutSubscribersAndReturningReadsLatestStatistics() = runTest {
        var activeObservers = 0
        val source = MutableStateFlow(CollectionStatistics(1L, 1, 1))
        val model = HomeViewModel(flow {
            activeObservers++
            try {
                source.collect { emit(it) }
            } finally {
                activeObservers--
            }
        }).also { store.put("home", it) }
        val observer = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect {} }
        runCurrent()
        assertEquals(1, activeObservers)
        observer.cancel()
        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(0, activeObservers)
        assertEquals(HomeStatisticsState.Loading, model.state.value)
        source.value = CollectionStatistics(4L, 2, 3)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.state.collect {} }
        runCurrent()
        assertEquals(1, activeObservers)
        assertEquals(HomeStatisticsState.Ready(source.value), model.state.value)
    }
}
