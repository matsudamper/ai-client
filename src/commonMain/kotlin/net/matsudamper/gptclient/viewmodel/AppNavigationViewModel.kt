package net.matsudamper.gptclient.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import net.matsudamper.gptclient.navigation.AppNavigator
import net.matsudamper.gptclient.navigation.Navigator

class AppNavigationViewModel : ViewModel() {
    val backStack = mutableStateListOf<Navigator>(Navigator.StartChat)

    val appNavigator = AppNavigator(backStack)
}
