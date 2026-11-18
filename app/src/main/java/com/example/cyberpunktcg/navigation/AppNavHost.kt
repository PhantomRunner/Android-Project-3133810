package com.example.cyberpunktcg.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.cyberpunktcg.ui.card.CardDetailsScreen
import com.example.cyberpunktcg.ui.browser.CardBrowserViewModel
import com.example.cyberpunktcg.ui.decks.DeckCardsScreen
import com.example.cyberpunktcg.ui.decks.DeckEditorScreen
import com.example.cyberpunktcg.ui.decks.DeckPreviewScreen
import com.example.cyberpunktcg.ui.decks.DeckNotFoundScreen
import com.example.cyberpunktcg.ui.decks.DecksViewModel
import com.example.cyberpunktcg.ui.decks.DeckDataState
import com.example.cyberpunktcg.ui.decks.MyDecksScreen
import com.example.cyberpunktcg.ui.home.HomeScreen
import com.example.cyberpunktcg.ui.scanner.ScannerScreen
import com.example.cyberpunktcg.ui.scanner.ScannerViewModel
import com.example.cyberpunktcg.ui.settings.SettingsScreen
import com.example.cyberpunktcg.data.model.ThemeMode
import com.example.cyberpunktcg.sensors.SensorsViewModel
import java.io.File
import kotlinx.coroutines.launch

@Composable
internal fun AppNavHost(
    navController: NavHostController,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    sensors: SensorsViewModel,
    decksViewModel: DecksViewModel,
    onEditorBack: () -> Unit,
    onTabSelected: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val onBackToDecks: () -> Unit = {
        if (!navController.popBackStack(AppDestination.Decks.route, false)) {
            navController.navigate(AppDestination.Decks.route) { launchSingleTop = true }
        }
    }
    NavHost(
        navController = navController,
        startDestination = AppDestination.Home.route,
        modifier = modifier,
    ) {
        deckDestination(AppDestination.Home, decksViewModel) {
            val decks = decksViewModel.decks
            val recentDecks = decks.takeLast(3).asReversed()
            val legendsByDeck = recentDecks.associate { deck ->
                deck.id to deck.legendIds.mapNotNull { id ->
                    decksViewModel.legends.firstOrNull { it.id == id }
                }
            }
            val onViewAllDecks: () -> Unit = {
                onTabSelected(AppDestination.Decks)
                navController.popBackStack(AppDestination.Decks.route, false)
            }
            HomeScreen(
                deckCount = decks.size,
                recentDecks = recentDecks,
                legendsByDeck = legendsByDeck,
                onBrowseCollection = { onTabSelected(AppDestination.Collection) },
                onOpenDecks = { onTabSelected(AppDestination.Decks) },
                onViewAllDecks = onViewAllDecks,
                onOpenDeck = { deckId ->
                    onViewAllDecks()
                    navController.navigate(deckEditorRoute(deckId)) { launchSingleTop = true }
                },
            )
        }
        collectionDestinations(navController)
        deckDestination(AppDestination.Decks, decksViewModel) {
            val openDeck: (String) -> Unit = { deckId ->
                navController.navigate(deckEditorRoute(deckId)) { launchSingleTop = true }
            }
            MyDecksScreen(
                decks = decksViewModel.decks,
                onDeleteDeck = { id -> scope.launch { decksViewModel.deleteDeck(id) } },
                onCreateDeck = { name ->
                    scope.launch {
                        val deck = decksViewModel.createDeck(name)
                        if (deck != null && navController.currentDestination?.route == AppDestination.Decks.route) {
                            navController.navigate(deckEditorRoute(deck.id)) {
                                launchSingleTop = true
                            }
                        }
                    }
                },
                onOpenDeck = openDeck,
                shakeEvents = sensors.shakes,
                accelerometerAvailable = sensors.accelerometerAvailable,
                onRandomDeck = { decksViewModel.randomSavedDeckId()?.let(openDeck) },
            )
        }
        deckDestination(
            destination = AppDestination.DeckEditor,
            decksViewModel = decksViewModel,
            arguments = listOf(navArgument("deckId") { type = NavType.StringType }),
        ) { entry ->
            val deckId = entry.arguments?.getString("deckId")
            val deck = decksViewModel.editingDeck(deckId)
            DeckEditorScreen(
                deck = deck,
                validation = decksViewModel.validation(deckId),
                onBack = onEditorBack,
                legends = decksViewModel.legends,
                ownedLegends = decksViewModel.ownedLegends,
                ownedQuantity = decksViewModel::ownedQuantity,
                mainCards = decksViewModel.mainCards,
                isSaving = decksViewModel.isSaving,
                onAddCards = { deckId?.let { navController.navigate(deckCardsRoute(it)) { launchSingleTop = true } } },
                onPreview = { deckId?.let { navController.navigate(deckPreviewRoute(it)) { launchSingleTop = true } } },
                hasUnsavedChanges = decksViewModel.hasUnsavedChanges(deckId),
                onNameChange = { name -> deckId?.let { decksViewModel.renameDeck(it, name) } },
                canAddLegend = { cardId -> deckId != null && decksViewModel.canAddLegend(deckId, cardId) },
                onAddLegend = { cardId -> deckId?.let { decksViewModel.addLegend(it, cardId) } },
                onRemoveLegend = { cardId -> deckId?.let { decksViewModel.removeLegend(it, cardId) } },
                onSaveDraft = { deckId?.let { scope.launch { decksViewModel.saveDraft(it) } } },
                onBackToDecks = onBackToDecks,
            )
        }
        deckDestination(
            destination = AppDestination.DeckCards,
            decksViewModel = decksViewModel,
            arguments = listOf(navArgument("deckId") { type = NavType.StringType }),
        ) { entry ->
            val deckId = entry.arguments?.getString("deckId")
            val deck = decksViewModel.editingDeck(deckId)


            val browserOwner = remember(entry, deckId) {
                if (deckId != null && decksViewModel.findDeck(deckId) != null) {
                    navController.getBackStackEntry(deckEditorRoute(deckId))
                } else entry
            }
            val browser: CardBrowserViewModel = viewModel(viewModelStoreOwner = browserOwner, key = "deck_cards")
            DeckCardsScreen(
                deck = deck,
                validation = decksViewModel.validation(deckId),
                cards = browser.visibleCards(decksViewModel.selectableMainCards(deckId)),
                state = browser.uiState,
                onQueryChange = browser::setQuery,
                onTypesChange = browser::setSelectedTypes,
                onColorsChange = browser::setSelectedColors,
                onSortFieldChange = browser::setSortField,
                onSortDirectionChange = browser::setSortDirection,
                canAddCard = { cardId -> deckId != null && decksViewModel.canAddCard(deckId, cardId) },
                ownedQuantity = decksViewModel::ownedQuantity,
                onAddCard = { cardId -> deckId?.let { decksViewModel.addCard(it, cardId) } },
                onRemoveCard = { cardId -> deckId?.let { decksViewModel.removeCard(it, cardId) } },
                onBackToDecks = onBackToDecks,
            )
        }
        deckDestination(
            destination = AppDestination.DeckPreview,
            decksViewModel = decksViewModel,
            arguments = listOf(navArgument("deckId") { type = NavType.StringType }),
        ) { entry ->
            val deckId = entry.arguments?.getString("deckId")
            DeckPreviewScreen(
                deck = decksViewModel.editingDeck(deckId),
                legends = decksViewModel.legends,
                mainCards = decksViewModel.mainCards,
                previewItems = decksViewModel.previewItems(deckId),
                shakeEvents = sensors.shakes,
                accelerometerAvailable = sensors.accelerometerAvailable,
                onShuffle = { decksViewModel.shufflePreview(deckId) },
                onOpenCard = { cardId ->
                    deckId?.let { navController.navigate(deckCardDetailsRoute(it, cardId)) { launchSingleTop = true } }
                },
                onBackToDecks = onBackToDecks,
            )
        }
        deckDestination(
            destination = AppDestination.DeckCardDetails,
            decksViewModel = decksViewModel,
            arguments = listOf(
                navArgument("deckId") { type = NavType.StringType },
                navArgument("cardId") { type = NavType.StringType },
            ),
        ) { entry ->
            val deck = decksViewModel.editingDeck(entry.arguments?.getString("deckId"))
            if (deck == null) {
                DeckNotFoundScreen(onBackToDecks)
            } else {
                val cardId = entry.arguments?.getString("cardId")
                val card = decksViewModel.findCard(cardId)?.takeIf { card ->
                    card.id in deck.legendIds || deck.items.any { it.cardId == card.id }
                }
                CardDetailsScreen(card)
            }
        }
        composable(AppDestination.Scanner.route) { entry ->
            val context = LocalContext.current
            val factory = remember(context.applicationContext) {
                val photoDirectory = File(context.cacheDir, "scanner_photos")
                viewModelFactory { initializer { ScannerViewModel(photoDirectory) } }
            }
            val scanner: ScannerViewModel = viewModel(factory = factory)
            val currentEntry by navController.currentBackStackEntryAsState()
            DisposableEffect(navController, scanner) {
                val listener = NavController.OnDestinationChangedListener { _, destination, _ ->

                    if (destination.route != AppDestination.Scanner.route) scanner.reset()
                }
                navController.addOnDestinationChangedListener(listener)
                onDispose { navController.removeOnDestinationChangedListener(listener) }
            }
            ScannerScreen(viewModel = scanner, isActive = currentEntry?.id == entry.id, light = sensors.light)
        }
        composable(AppDestination.Settings.route) {
            val light = if (themeMode == ThemeMode.AUTO) {
                sensors.light.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.RESUMED).value
            } else null
            SettingsScreen(
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                light = light,
            )
        }
    }
}

private fun NavGraphBuilder.deckDestination(
    destination: AppDestination,
    decksViewModel: DecksViewModel,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable(destination.route, arguments) { entry ->
        DeckDataState(decksViewModel) { content(entry) }
    }
}
