package net.matsudamper.gptclient.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import net.matsudamper.gptclient.navigation.AppNavigator
import net.matsudamper.gptclient.navigation.Navigator
import net.matsudamper.gptclient.room.entity.ChatRoomId

class AppNavigationViewModel(
    initialChatRoomId: ChatRoomId?,
) : ViewModel() {
    val backStack = mutableStateListOf<Navigator>(Navigator.StartChat).apply {
        if (initialChatRoomId != null) {
            add(
                Navigator.Chat(
                    openContext = Navigator.Chat.ChatOpenContext.OpenChat(initialChatRoomId),
                ),
            )
        }
    }

    val appNavigator = AppNavigator(backStack)
}
