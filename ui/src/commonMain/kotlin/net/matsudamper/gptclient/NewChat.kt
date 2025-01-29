package net.matsudamper.gptclient

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Menu
import net.matsudamper.gptclient.component.ChatFooter
import kotlin.math.ceil

public data class NewChatUiState(
    val projects: List<Project> = listOf(),
    val listener: Listener,
) {
    data class Project(
        val name: String,
        val listener: Listener,
    ) {
        @Immutable
        interface Listener {
            fun onClick()
        }
    }

    @Immutable
    interface Listener {
        fun send(text: String)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun NewChat(
    uiState: NewChatUiState,
    onClickMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .navigationBarsPadding()
            .imePadding(),
    ) {
        val maxWidth = maxWidth
        Column {
            val projectModifier = Modifier.fillMaxWidth()
                .height(300.dp)

            TopAppBar(
                modifier = Modifier,
                title = {
                    Text("Chat")
                },
                navigationIcon = {
                    IconButton(onClick = { onClickMenu() }) {
                        Icon(
                            imageVector = FeatherIcons.Menu,
                            contentDescription = null
                        )
                    }
                }
            )
            LazyVerticalGrid(
                modifier = Modifier.fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    horizontal = 24.dp,
                ),
                columns = GridCells.Fixed(
                    ceil(
                        with(LocalDensity.current) {
                            maxWidth.roundToPx() / 160.dp.roundToPx()
                        }.toFloat()
                    ).toInt()
                ),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center,
            ) {
                item(
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Column {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            text = "Projects",
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                items(uiState.projects) { project ->
                    Project(
                        modifier = projectModifier,
                        content = { Text(project.name) },
                        onClick = { project.listener.onClick() },
                    )
                }
                item {
                    Project(
                        modifier = projectModifier,
                        content = {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                )
                                Text("追加")
                            }
                        },
                        onClick = {

                        }
                    )
                }
            }
            val state = rememberTextFieldState()
            ChatFooter(
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 8.dp),
                state = state,
                onClickImage = { },
                onClickVoice = { },
                onClickSend = {},
            )
        }
    }
}

@Composable
private fun Project(
    content: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = { onClick() },
    ) {
        content()
    }
}
