package net.matsudamper.gptclient

import net.matsudamper.gptclient.navigation.Navigator

data class LaunchNavigationRequest(
    val id: Long,
    val navigator: Navigator?,
) {
    companion object {
        fun none(): LaunchNavigationRequest = LaunchNavigationRequest(
            id = 0L,
            navigator = null,
        )
    }
}

const val EXTRA_CHATROOM_ID = "chatRoomId"
