package ir.hrka.face.navigation

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

/**
 * Creates a navigation state that persists config changes and process death.
 *
 * @param startDestination Initial destination; also the app-exit root.
 * @param topLevelDestinations Destinations that own independent sub-stacks.
 */
@Composable
fun rememberNavigationState(
    startDestination: NavKey,
    topLevelDestinations: Set<NavKey>,
): NavigationState {
    val topLevelStack = rememberNavBackStack(startDestination)
    val subStacks = topLevelDestinations.associateWith { destination ->
        rememberNavBackStack(destination)
    }

    return remember(startDestination, topLevelDestinations) {
        NavigationState(
            startDestination = startDestination,
            topLevelStack = topLevelStack,
            subStacks = subStacks,
        )
    }
}

/**
 * State holder for Navigation 3 back stacks.
 *
 * @property startDestination Root destination the user exits through.
 * @property topLevelStack Stack of top-level destinations.
 * @property subStacks Per top-level destination nested stacks.
 */
class NavigationState(
    val startDestination: NavKey,
    val topLevelStack: NavBackStack<NavKey>,
    val subStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    /** Currently selected top-level destination. */
    val currentTopLevelDestination: NavKey by derivedStateOf { topLevelStack.last() }

    /** All registered top-level keys. */
    val topLevelKeys
        get() = subStacks.keys

    /** Active nested stack for the current top-level destination. */
    @get:VisibleForTesting
    val currentSubStack: NavBackStack<NavKey>
        get() = subStacks[currentTopLevelDestination]
            ?: error("Sub stack for $currentTopLevelDestination does not exist")

    /** Deepest destination in the active nested stack. */
    @get:VisibleForTesting
    val currentDestination: NavKey by derivedStateOf { currentSubStack.last() }
}

/**
 * Converts [NavigationState] into decorated [NavEntry] lists for [androidx.navigation3.ui.NavDisplay].
 *
 * @param entryProvider Mapping from [NavKey] to UI entry.
 */
@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): SnapshotStateList<NavEntry<NavKey>> {
    val decoratedEntries = subStacks.mapValues { (_, stack) ->
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            rememberViewModelStoreNavEntryDecorator<NavKey>(),
        )
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = decorators,
            entryProvider = entryProvider,
        )
    }

    return topLevelStack
        .flatMap { decoratedEntries[it] ?: emptyList() }
        .toMutableStateList()
}
