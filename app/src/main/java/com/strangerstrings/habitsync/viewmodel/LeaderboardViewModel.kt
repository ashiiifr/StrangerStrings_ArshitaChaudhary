package com.strangerstrings.habitsync.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strangerstrings.habitsync.data.LeaderboardUser
import com.strangerstrings.habitsync.data.repository.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LeaderboardUiState(
    val isLoading: Boolean = false,
    val users: List<LeaderboardUser> = emptyList(),
    val errorMessage: String? = null,
)

class LeaderboardViewModel(
    private val socialRepository: SocialRepository = SocialRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(LeaderboardUiState(isLoading = true))
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        observeLeaderboard()
    }

    private fun observeLeaderboard() {
        viewModelScope.launch {
            socialRepository.getLeaderboard()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to load leaderboard.",
                        )
                    }
                }
                .collect { users ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            users = users,
                            errorMessage = null,
                        )
                    }
                }
        }
    }
}
