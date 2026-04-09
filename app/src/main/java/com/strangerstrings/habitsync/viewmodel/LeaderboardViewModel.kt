package com.strangerstrings.habitsync.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strangerstrings.habitsync.data.LeaderboardFilter
import com.strangerstrings.habitsync.data.LeaderboardUser
import com.strangerstrings.habitsync.data.repository.SocialRepository
import kotlinx.coroutines.Job
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
    val selectedFilter: LeaderboardFilter = LeaderboardFilter.OVERALL,
    val errorMessage: String? = null,
)

class LeaderboardViewModel(
    private val socialRepository: SocialRepository = SocialRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(LeaderboardUiState(isLoading = true))
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()
    private var leaderboardJob: Job? = null

    init {
        observeLeaderboard(LeaderboardFilter.OVERALL)
    }

    fun selectFilter(filter: LeaderboardFilter) {
        if (_uiState.value.selectedFilter == filter) return
        _uiState.update { it.copy(selectedFilter = filter, isLoading = true) }
        observeLeaderboard(filter)
    }

    private fun observeLeaderboard(filter: LeaderboardFilter) {
        leaderboardJob?.cancel()
        leaderboardJob = viewModelScope.launch {
            socialRepository.getLeaderboard(filter)
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
                            selectedFilter = filter,
                            errorMessage = null,
                        )
                    }
                }
        }
    }
}
