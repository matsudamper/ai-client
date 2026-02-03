package net.matsudamper.gptclient.navigation

import androidx.compose.runtime.snapshots.SnapshotStateList

class AppNavigator(private val backStack: SnapshotStateList<Navigator>) {
    fun navigate(route: Navigator) {
        backStack.add(route)
    }

    fun navigateToStart() {
        backStack.clear()
        backStack.add(Navigator.StartChat)
    }

    fun navigateClearToStart(route: Navigator) {
        while (backStack.size > 1) {
            backStack.removeLast()
        }
        backStack.add(route)
    }

    fun popBackStack() {
        if (backStack.size > 1) {
            backStack.removeLast()
        }
    }
}
