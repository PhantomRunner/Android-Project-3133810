package com.example.cyberpunktcg.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.cyberpunktcg.ui.collection.CollectionCardDetailsScreen
import com.example.cyberpunktcg.ui.collection.CollectionLoading
import com.example.cyberpunktcg.ui.collection.CollectionReadError
import com.example.cyberpunktcg.ui.collection.CollectionScreen
import com.example.cyberpunktcg.ui.collection.CollectionViewModel

internal fun NavGraphBuilder.collectionDestinations(navController: NavController) {
    composable(AppDestination.Collection.route) { entry ->
        val collection: CollectionViewModel = viewModel()
        val browser: CardBrowserViewModel = viewModel()
        // Turn the ViewModel's flow into Compose state while this screen is active.
        val state by collection.state.collectAsStateWithLifecycle()
        // The chosen tab is saved with this navigation entry.
        val ownedOnly by entry.savedStateHandle.getStateFlow(OWNED_ONLY, false).collectAsStateWithLifecycle()
        CollectionScreen(
            state = state,
            browser = browser,
            ownedOnly = ownedOnly,
            onOwnedOnlyChange = { entry.savedStateHandle[OWNED_ONLY] = it },
            onOpenPrinting = { id -> navController.navigate(cardDetailsRoute(id)) { launchSingleTop = true } },
            onChangeQuantity = collection::changeQuantity,
            onRetry = collection::retry,
        )
    }
    composable(AppDestination.AddCollectionCards.route) { entry ->
        // Tasks saved before All cards moved into Collection can still restore this route.
        LaunchedEffect(entry) {
            val collection = navController.getBackStackEntry(AppDestination.Collection.route)
            collection.savedStateHandle[OWNED_ONLY] = false
            navController.popBackStack(AppDestination.Collection.route, inclusive = false)
        }
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

private const val OWNED_ONLY = "collection_owned_only"

// Details use the collection screen's ViewModel so both see the same Room data.
@Composable
private fun sharedCollectionViewModel(navController: NavController, entry: NavBackStackEntry): CollectionViewModel {
    val collectionEntry = remember(entry) { navController.getBackStackEntry(AppDestination.Collection.route) }
    return viewModel(viewModelStoreOwner = collectionEntry)
}
