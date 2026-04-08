package com.strangerstrings.habitsync.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strangerstrings.habitsync.data.Habit
import com.strangerstrings.habitsync.data.repository.AuthRepository
import com.strangerstrings.habitsync.data.repository.FirebaseHabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class HomeUiState(
    val isLoading: Boolean = false,
    val habits: List<Habit> = emptyList(),
    val errorMessage: String? = null,
    val userId: String = "",
    val uploadingHabitId: String? = null,
)

class HomeViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val habitRepository: FirebaseHabitRepository = FirebaseHabitRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeHabits()
    }

    fun addHabit() {
        val state = _uiState.value
        if (state.userId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please sign in to add habits.") }
            return
        }

        viewModelScope.launch {
            runCatching {
                val habitNumber = state.habits.size + 1
                val newHabit = Habit(
                    id = generateHabitId(),
                    title = "New Habit $habitNumber",
                    streak = 0,
                    isCompletedToday = false,
                    lastCompletedDate = null,
                )
                habitRepository.addHabit(newHabit)
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(errorMessage = error.message ?: "Failed to add habit.")
                }
            }
        }
    }

    fun markHabitDone(habitId: String, proofImageBytes: ByteArray? = null) {
        val state = _uiState.value
        if (state.userId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please sign in to update habits.") }
            return
        }

        val currentHabit = state.habits.firstOrNull { it.id == habitId } ?: return
        if (currentHabit.isCompletedToday) return

        viewModelScope.launch {
            _uiState.update { it.copy(uploadingHabitId = habitId, errorMessage = null) }
            runCatching {
                val updatedHabit = currentHabit.copy(
                    streak = currentHabit.streak + 1,
                    isCompletedToday = true,
                    lastCompletedDate = currentEpochDay(),
                    proofImageUrl = null,
                )
                habitRepository.updateHabit(
                    habit = updatedHabit,
                    proofImageBytes = proofImageBytes,
                )
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(errorMessage = error.message ?: "Failed to mark habit as done.")
                }
            }.also {
                _uiState.update { current ->
                    current.copy(uploadingHabitId = null)
                }
            }
        }
    }

    private fun observeHabits() {
        val userId = authRepository.getCurrentUserId()
        if (userId.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    habits = emptyList(),
                    userId = "",
                    errorMessage = "No authenticated user. Sign in to sync habits.",
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, userId = userId) }

        viewModelScope.launch {
            habitRepository.getHabits(userId)
                .catch { error ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to load habits.",
                        )
                    }
                }
                .collect { habits ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            habits = habits,
                            errorMessage = null,
                        )
                    }
                }
        }
    }

    private fun generateHabitId(): String {
        return UUID.randomUUID().toString()
    }

    private fun currentEpochDay(): Long {
        val millisecondsPerDay = 24 * 60 * 60 * 1000L
        return System.currentTimeMillis() / millisecondsPerDay
    }
}
