package com.example.cyberpunktcg.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.cyberpunktcg.ui.browser.CardBrowserViewModel
import com.example.cyberpunktcg.ui.collection.AddCollectionCardsScreen
import com.example.cyberpunktcg.ui.collection.CollectionCardDetailsScreen
import com.example.cyberpunktcg.ui.collection.CollectionLoading
import com.example.cyberpunktcg.ui.collection.CollectionReadError
import com.example.cyberpunktcg.ui.collection.CollectionScreen
import com.example.cyberpunktcg.ui.collection.CollectionViewModel

internal fun NavGraphBuilder.collectionDestinations(navController: NavController) {
    composable(AppDestination.Collection.route) {
        val collection: CollectionViewModel = viewModel()
        val browser: CardBrowserViewModel = viewModel()
        val state by collection.state.collectAsStateWithLifecycle()
        CollectionScreen(
            state = state,
            browser = browser,
            onAddCards = { navController.navigate(AppDestination.AddCollectionCards.route) { launchSingleTop = true } },
            onOpenPrinting = { id -> navController.navigate(cardDetailsRoute(id)) { launchSingleTop = true } },
            onChangeQuantity = collection::changeQuantity,
            onRetry = collection::retry,
        )
    }
    composable(AppDestination.AddCollectionCards.route) { entry ->
        val collection = sharedCollectionViewModel(navController, entry)
        val browser: CardBrowserViewModel = viewModel()
        val state by collection.state.collectAsStateWithLifecycle()
        AddCollectionCardsScreen(state, browser, collection::changeQuantity, collection::retry)
    }
    composable(
        route = AppDestination.CardDetails.route,
        arguments = listOf(navArgument("printingId") { type = NavType.StringType }),
    ) { entry ->
        val collection = sharedCollectionViewModel(navController, entry)
        val state by collection.state.collectAsStateWithLifecycle()
        val printingId = entry.arguments?.getString("printingId")
        val printings = state.printings
        when {
            state.readFailed -> CollectionReadError(collection::retry)
            printings == null -> CollectionLoading()
            else -> CollectionCardDetailsScreen(printings.firstOrNull { it.printing.id == printingId })
        }
    }
}

@Composable
private fun sharedCollectionViewModel(navController: NavController, entry: NavBackStackEntry): CollectionViewModel {
    val collectionEntry = remember(entry) { navController.getBackStackEntry(AppDestination.Collection.route) }
    return viewModel(viewModelStoreOwner = collectionEntry)
}
