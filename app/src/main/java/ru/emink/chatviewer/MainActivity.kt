package ru.emink.chatviewer

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ChatApp()
            }
        }
    }
}

@Composable
private fun ChatApp(viewModel: ChatViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val openedImage = state.openedImage

    if (openedImage != null) {
        BackHandler(onBack = viewModel::closeImage)
        FullImageScreen(
            path = openedImage,
            onClose = viewModel::closeImage,
        )
        return
    }

    BackHandler(enabled = state.selectedChat != null, onBack = viewModel::closeChat)

    Scaffold { innerPadding ->
        if (landscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                ChatList(
                    state = state,
                    onSelect = viewModel::selectChat,
                    onRetry = viewModel::loadChannels,
                    modifier = Modifier.weight(0.38f),
                )
                VerticalDivider()
                val channel = state.selectedChat
                if (channel == null) {
                    Box(
                        modifier = Modifier
                            .weight(0.62f)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.select_chat))
                    }
                } else {
                    MessagesScreen(
                        channel = channel,
                        state = state,
                        showBackButton = false,
                        onBack = viewModel::closeChat,
                        onLoadMore = { viewModel.loadMore(channel) },
                        onOpenImage = viewModel::openImage,
                        modifier = Modifier.weight(0.62f),
                    )
                }
            }
        } else {
            val channel = state.selectedChat
            if (channel == null) {
                ChatList(
                    state = state,
                    onSelect = viewModel::selectChat,
                    onRetry = viewModel::loadChannels,
                    modifier = Modifier.padding(innerPadding),
                )
            } else {
                MessagesScreen(
                    channel = channel,
                    state = state,
                    showBackButton = true,
                    onBack = viewModel::closeChat,
                    onLoadMore = { viewModel.loadMore(channel) },
                    onOpenImage = viewModel::openImage,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun ChatList(
    state: ChatUiState,
    onSelect: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.selectedChat, state.channels) {
        val index = state.channels.indexOf(state.selectedChat)
        if (index >= 0) {
            listState.scrollToItem(index)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.chats_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp),
        )
        HorizontalDivider()
        when {
            state.loadingChannels && state.channels.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.channelsError && state.channels.isEmpty() -> ErrorContent(onRetry)

            state.channels.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.no_chats))
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.channels, key = { it }) { channel ->
                    Surface(
                        color = if (channel == state.selectedChat) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(channel) },
                    ) {
                        Text(
                            text = channel,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessagesScreen(
    channel: String,
    state: ChatUiState,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenImage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val messages = state.messages[channel].orEmpty()
    val loading = channel in state.loadingChats
    val failed = channel in state.failedChats
    val listState = rememberLazyListState()

    LaunchedEffect(channel, messages.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { index ->
                if (messages.isNotEmpty() && index >= messages.lastIndex - 2) {
                    onLoadMore()
                }
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBackButton) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.back))
                }
            }
            Text(
                text = channel,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(8.dp),
            )
        }
        HorizontalDivider()
        if (failed) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.load_error))
                TextButton(onClick = onLoadMore) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            when {
                messages.isEmpty() && loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )

                messages.isEmpty() && !failed -> Text(
                    text = stringResource(R.string.no_messages),
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageItem(message, onOpenImage)
                    }
                }
            }
        }
        if (loading && messages.isNotEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun MessageItem(
    message: ChatMessage,
    onOpenImage: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
    ) {
        Text(
            text = message.from.orEmpty(),
            fontWeight = FontWeight.Bold,
        )
        message.data.text?.let { textMessage ->
            Text(textMessage.text)
        }
        message.data.image?.let { imageMessage ->
            AsyncImage(
                model = thumbnailUrl(imageMessage.link),
                contentDescription = stringResource(R.string.image_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenImage(imageMessage.link) },
            )
        }
    }
}

@Composable
private fun FullImageScreen(
    path: String,
    onClose: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Button(
                onClick = onClose,
                modifier = Modifier.padding(8.dp),
            ) {
                Text(stringResource(R.string.close))
            }
            AsyncImage(
                model = imageUrl(path),
                contentDescription = stringResource(R.string.image_description),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}

@Composable
private fun ErrorContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.load_error))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}
