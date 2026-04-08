package com.strangerstrings.habitsync.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strangerstrings.habitsync.data.FeedEvent
import com.strangerstrings.habitsync.data.repository.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedUiState(
    val isLoading: Boolean = false,
    val events: List<FeedEvent> = emptyList(),
    val errorMessage: String? = null,
)

class FeedViewModel(
    private val socialRepository: SocialRepository = SocialRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(FeedUiState(isLoading = true))
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        observeFeed()
    }

    private fun observeFeed() {
        viewModelScope.launch {
            socialRepository.getFeed()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to load activity feed.",
                        )
                    }
                }
                .collect { events ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            events = events,
                            errorMessage = null,
                        )
                    }
                }
        }
    }
}
