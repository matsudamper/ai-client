package net.matsudamper.gptclient.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Image
import compose.icons.feathericons.Mic
import compose.icons.feathericons.Upload

@Composable
internal fun ChatFooter(
    state: TextFieldState,
    onClickImage: () -> Unit,
    onClickVoice: () -> Unit,
    onClickSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onClickImage() }) {
            Icon(
                imageVector = FeatherIcons.Image,
                contentDescription = "add image",
            )
        }
        BasicTextField(
            modifier = modifier
                .fillMaxHeight()
                .weight(1f),
            state = state,
            decorator = { content ->
                Box(
                    modifier = Modifier.clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.onSecondary)
                        .padding(6.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    content()
                }
            }
        )
        val isBlank by remember { derivedStateOf { state.text.isBlank() } }
        if (isBlank) {
            IconButton(onClick = { onClickVoice() }) {
                Icon(
                    imageVector = FeatherIcons.Mic,
                    contentDescription = "input voice",
                )
            }
        } else {
            IconButton(onClick = { onClickSend() }) {
                Icon(
                    imageVector = FeatherIcons.Upload,
                    contentDescription = "send",
                )
            }
        }
    }
}
