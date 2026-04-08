package com.strangerstrings.habitsync.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strangerstrings.habitsync.data.repository.InboxRepository
import com.strangerstrings.habitsync.ui.inbox.InboxItem
import com.strangerstrings.habitsync.ui.inbox.InboxType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InboxUiState(
    val isLoading: Boolean = false,
    val items: List<InboxItem> = emptyList(),
    val errorMessage: String? = null,
) {
    val unreadCount: Int get() = items.count { !it.isRead }
}

class InboxViewModel(
    private val repository: InboxRepository = InboxRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(InboxUiState(isLoading = true))
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()
    private val _challengeRoomOpenEvents = MutableSharedFlow<String>()
    val challengeRoomOpenEvents: SharedFlow<String> = _challengeRoomOpenEvents.asSharedFlow()

    init {
        observeInbox()
    }

    fun markAllRead() {
        val state = _uiState.value
        if (state.items.isEmpty()) return

        _uiState.update { ui ->
            ui.copy(items = ui.items.map { item -> item.copy(isRead = true) })
        }
        val notificationIds = state.items
            .filter { it.type !in listOf(InboxType.FRIEND_REQUEST, InboxType.CHALLENGE_INVITE) }
            .map { it.id }

        viewModelScope.launch {
            runCatching { repository.markAllNotificationItemsRead(notificationIds) }
        }
    }

    fun accept(item: InboxItem) {
        viewModelScope.launch {
            runCatching {
                when (item.type) {
                    InboxType.FRIEND_REQUEST -> repository.respondToFriendRequest(item.id, accept = true)
                    InboxType.CHALLENGE_INVITE -> repository.respondToChallengeInvite(item.id, accept = true)
                    else -> Unit
                }
                item.type
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Action failed.") }
            }.onSuccess { type ->
                if (type == InboxType.CHALLENGE_INVITE) {
                    _challengeRoomOpenEvents.emit(item.id)
                }
            }
        }
    }

    fun decline(item: InboxItem) {
        viewModelScope.launch {
            runCatching {
                when (item.type) {
                    InboxType.FRIEND_REQUEST -> repository.respondToFriendRequest(item.id, accept = false)
                    InboxType.CHALLENGE_INVITE -> repository.respondToChallengeInvite(item.id, accept = false)
                    else -> Unit
                }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Action failed.") }
            }
        }
    }

    private fun observeInbox() {
        viewModelScope.launch {
            repository.observeInboxItems()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to load inbox.",
                        )
                    }
                }
                .collect { items ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            items = items,
                            errorMessage = null,
                        )
                    }
                }
        }
    }
}
