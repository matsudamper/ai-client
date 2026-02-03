package net.matsudamper.gptclient.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey

class AppNavigator(val state: NavigationState) {
    fun navigate(route: NavKey, popToRoot: Boolean = false) {
        if (route in state.backStacks.keys) {
            // This is a top level route, just switch to it.
            state.topLevelRoute = route
            if (popToRoot) {
                // If switching to a top level route with popToRoot, reset its stack to just the route itself.
                val stack = state.backStacks[route]
                stack?.clear()
                stack?.add(route)
            }
        } else {
            // Child route
            val currentStack = state.currentStack
            if (popToRoot) {
                // Reset stack to its root (first element), then add new route
                val root = currentStack.firstOrNull() ?: state.topLevelRoute
                currentStack.clear()
                currentStack.add(root)
                currentStack.add(route)
            } else {
                currentStack.add(route)
            }
        }
    }

    fun navigateToStart() {
        state.topLevelRoute = state.startRoute
        // Also clear the start route's stack?
        // Usually clicking Home resets Home stack too.
        val stack = state.backStacks[state.startRoute]
        stack?.clear()
        stack?.add(state.startRoute)
    }

    fun goBack() {
        val currentStack = state.currentStack

        // If we're at the base of the current route (root of the stack)
        // Check if stack has only 1 item
        if (currentStack.size <= 1) {
             // If we are at the root of a stack
             if (state.topLevelRoute != state.startRoute) {
                 // Go back to start route
                 state.topLevelRoute = state.startRoute
             }
             // If already at start route, do nothing (or close app, but that's handled by BackHandler in UI)
        } else {
            currentStack.removeLastOrNull()
        }
    }
}

@Composable
fun rememberAppNavigator(state: NavigationState): AppNavigator {
    return remember(state) { AppNavigator(state) }
}
