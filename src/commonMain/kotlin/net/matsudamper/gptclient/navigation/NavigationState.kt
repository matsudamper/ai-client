package net.matsudamper.gptclient.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey

/**
 * Create a navigation state that persists config changes and process death.
 */
@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>
): NavigationState {
    val topLevelRouteState = remember { mutableStateOf(startRoute) }

    // Use SnapshotStateList for observability
    val backStacks = remember(topLevelRoutes) {
        topLevelRoutes.associateWith { key ->
            SnapshotStateList<NavKey>().apply { add(key) }
        }
    }

    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRouteState = topLevelRouteState,
            backStacks = backStacks
        )
    }
}

class NavigationState(
    val startRoute: NavKey,
    private val topLevelRouteState: MutableState<NavKey>,
    val backStacks: Map<NavKey, SnapshotStateList<NavKey>>
) {
    var topLevelRoute: NavKey by topLevelRouteState

    val currentStack: SnapshotStateList<NavKey>
        get() = backStacks[topLevelRoute] ?: error("Stack for $topLevelRoute not found")
}
