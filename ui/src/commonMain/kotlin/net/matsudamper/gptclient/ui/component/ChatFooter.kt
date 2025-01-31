package net.matsudamper.gptclient.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowUp
import compose.icons.feathericons.Image
import compose.icons.feathericons.Mic
import compose.icons.feathericons.Upload

@Composable
internal fun ChatFooter(
    textFieldState: TextFieldState,
    selectedMedia: List<String>,
    visibleMediaLoading: Boolean,
    onClickImage: () -> Unit,
    onClickVoice: () -> Unit,
    onClickSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        val imageModifier = Modifier.size(120.dp)
            .padding(12.dp)
        LazyRow {
            items(selectedMedia) { media ->
                AsyncImage(
                    modifier = imageModifier,
                    model = media,
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                )
            }
            if (visibleMediaLoading) {
                item {
                    Box(
                        modifier = imageModifier
                            .background(MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
        }
        FooterTextSection(
            modifier = Modifier.fillMaxWidth(),
            onClickSelectImage = onClickImage,
            textFieldState = textFieldState,
            onClickVoice = onClickVoice,
            onClickSend = onClickSend,
        )
    }
}

@Composable
private fun FooterTextSection(
    textFieldState: TextFieldState,
    onClickSelectImage: () -> Unit,
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
        IconButton(onClick = { onClickSelectImage() }) {
            Icon(
                imageVector = FeatherIcons.Image,
                contentDescription = "add image",
            )
        }
        Row(
            modifier = Modifier.weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.onSecondary)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                modifier = modifier
                    .fillMaxHeight()
                    .weight(1f),
                state = textFieldState,
                decorator = {
                    Box(contentAlignment = Alignment.CenterStart) {
                        it()
                    }
                },
            )
            IconButton(
                modifier = Modifier.size(32.dp),
                onClick = { onClickVoice() },
            ) {
                Icon(
                    modifier = Modifier.padding(4.dp),
                    imageVector = FeatherIcons.Mic,
                    contentDescription = "input voice",
                )
            }
        }

        IconButton(onClick = { onClickSend() }) {
            Icon(
                imageVector = FeatherIcons.ArrowUp,
                contentDescription = "send",
            )
        }
    }
}