package com.strangerstrings.habitsync.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strangerstrings.habitsync.data.Badge
import com.strangerstrings.habitsync.data.Habit
import com.strangerstrings.habitsync.data.UserProfile
import com.strangerstrings.habitsync.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val badges: List<Badge> = emptyList(),
    val publicHabits: List<Habit> = emptyList(),
    val errorMessage: String? = null,
)

class ProfileViewModel(
    private val repository: ProfileRepository = ProfileRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.syncFreezeTokensForCurrentMonth() }
        }
        observeProfile()
        observeBadges()
        observePublicHabits()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            repository.observeMyProfile()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to load profile.",
                        )
                    }
                }
                .collect { profile ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = profile,
                            errorMessage = null,
                        )
                    }
                }
        }
    }

    private fun observeBadges() {
        viewModelScope.launch {
            repository.observeMyBadges()
                .catch { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Unable to load badges.")
                    }
                }
                .collect { badges ->
                    _uiState.update { it.copy(badges = badges) }
                }
        }
    }

    private fun observePublicHabits() {
        viewModelScope.launch {
            repository.observeMyPublicHabits()
                .catch { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Unable to load public habits.")
                    }
                }
                .collect { habits ->
                    _uiState.update { it.copy(publicHabits = habits) }
                }
        }
    }
}
