package ru.emink.chatviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

data class ChatUiState(
    val channels: List<String> = emptyList(),
    val selectedChat: String? = null,
    val messages: Map<String, List<ChatMessage>> = emptyMap(),
    val loadingChannels: Boolean = false,
    val channelsError: Boolean = false,
    val loadingChats: Set<String> = emptySet(),
    val failedChats: Set<String> = emptySet(),
    val finishedChats: Set<String> = emptySet(),
    val openedImage: String? = null,
)

class ChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadChannels()
    }

    fun loadChannels() {
        if (_uiState.value.loadingChannels) return

        _uiState.update { it.copy(loadingChannels = true, channelsError = false) }
        viewModelScope.launch {
            try {
                val channels = ChatApi.service.channels().sorted()
                _uiState.update {
                    it.copy(channels = channels, loadingChannels = false)
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(loadingChannels = false, channelsError = true)
                }
            }
        }
    }

    fun selectChat(channel: String) {
        _uiState.update { it.copy(selectedChat = channel, openedImage = null) }
        if (!_uiState.value.messages.containsKey(channel)) {
            loadMore(channel)
        }
    }

    fun closeChat() {
        _uiState.update { it.copy(selectedChat = null, openedImage = null) }
    }

    fun openImage(path: String) {
        _uiState.update { it.copy(openedImage = path) }
    }

    fun closeImage() {
        _uiState.update { it.copy(openedImage = null) }
    }

    fun loadMore(channel: String) {
        val state = _uiState.value
        if (channel in state.loadingChats || channel in state.finishedChats) return

        val lastKnownId = state.messages[channel]
            ?.minOfOrNull { it.id }
            ?: Long.MAX_VALUE

        _uiState.update {
            it.copy(
                loadingChats = it.loadingChats + channel,
                failedChats = it.failedChats - channel,
            )
        }

        viewModelScope.launch {
            try {
                val page = ChatApi.service.messages(
                    channel = channel,
                    lastKnownId = lastKnownId,
                    limit = PAGE_SIZE,
                    reverse = true,
                )
                _uiState.update { current ->
                    val allMessages = (current.messages[channel].orEmpty() + page)
                        .distinctBy { it.id }
                        .sortedByDescending { it.id }
                    current.copy(
                        messages = current.messages + (channel to allMessages),
                        loadingChats = current.loadingChats - channel,
                        finishedChats = if (page.size < PAGE_SIZE) {
                            current.finishedChats + channel
                        } else {
                            current.finishedChats
                        },
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        loadingChats = it.loadingChats - channel,
                        failedChats = it.failedChats + channel,
                    )
                }
            }
        }
    }
}
