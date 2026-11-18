package com.example.cyberpunktcg.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.cyberpunktcg.R
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cyberpunktcg.data.local.decks.RoomDeckRepository
import com.example.cyberpunktcg.ui.decks.DecksViewModel
import com.example.cyberpunktcg.data.model.ThemeMode
import com.example.cyberpunktcg.sensors.SensorsViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
internal fun CompanionApp(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    sensors: SensorsViewModel,
) {

    val context = LocalContext.current.applicationContext
    val factory = remember(context) { viewModelFactory { initializer { DecksViewModel(RoomDeckRepository(context)) } } }
    val decksViewModel: DecksViewModel = viewModel(factory = factory)
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentDestination = AppDestination.entries.firstOrNull {
        it.route == currentRoute
    } ?: AppDestination.Home

    val editorDeckId = if (currentDestination == AppDestination.DeckEditor) {
        backStackEntry?.arguments?.getString("deckId")
    } else null
    var discardDeckId by rememberSaveable { mutableStateOf<String?>(null) }

    val onBack: () -> Unit = {
        if (decksViewModel.hasUnsavedChanges(editorDeckId)) {
            discardDeckId = editorDeckId
        } else {
            navController.popBackStack()
        }
    }

    val onTabSelected: (AppDestination) -> Unit = { selectedTab ->
        if (selectedTab != currentDestination.mainTab) {
            navigateToMainTab(navController, selectedTab)
        }
    }

    // Navigation restores its current entry after this state, so keep the pending ID
    // without a rememberSaveable input key that would reset it during rotation.
    if (discardDeckId != null && discardDeckId == editorDeckId &&
        decksViewModel.hasUnsavedChanges(discardDeckId)
    ) {
        AlertDialog(
            onDismissRequest = { discardDeckId = null },
            title = { Text(stringResource(R.string.discard_changes_title)) },
            text = { Text(stringResource(R.string.discard_changes_message)) },
            confirmButton = {
                TextButton(onClick = {
                    discardDeckId?.let { decksViewModel.discardChanges(it) }
                    discardDeckId = null
                    navController.popBackStack()
                }) { Text(stringResource(R.string.discard_changes)) }
            },
            dismissButton = {
                TextButton(onClick = { discardDeckId = null }) {
                    Text(stringResource(R.string.keep_editing))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                titleResId = currentDestination.titleResId,
                showBackButton = currentDestination != currentDestination.mainTab,
                onBackClick = onBack,
            )
        },
        bottomBar = {
            AppBottomBar(
                selectedTab = currentDestination.mainTab,
                onTabSelected = onTabSelected,
            )
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            sensors = sensors,
            decksViewModel = decksViewModel,
            onEditorBack = onBack,
            onTabSelected = onTabSelected,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }
}

private fun navigateToMainTab(navController: NavHostController, destination: AppDestination) {
    navController.navigate(destination.route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
