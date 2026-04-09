package com.strangerstrings.habitsync.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strangerstrings.habitsync.data.Habit
import com.strangerstrings.habitsync.data.HabitCategory
import com.strangerstrings.habitsync.data.HabitCompletionRecord
import com.strangerstrings.habitsync.data.HabitType
import com.strangerstrings.habitsync.data.HabitVisibility
import com.strangerstrings.habitsync.data.repository.AuthRepository
import com.strangerstrings.habitsync.data.repository.FirebaseHabitRepository
import com.strangerstrings.habitsync.data.repository.ProfileRepository
import com.strangerstrings.habitsync.data.repository.ReminderRepository
import com.strangerstrings.habitsync.data.repository.SocialRepository
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
    val infoMessage: String? = null,
    val userId: String = "",
    val uploadingHabitId: String? = null,
    val freezeTokensThisMonth: Int = 2,
)

class HomeViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val habitRepository: FirebaseHabitRepository = FirebaseHabitRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository(),
    private val reminderRepository: ReminderRepository = ReminderRepository(),
    private val socialRepository: SocialRepository = SocialRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { profileRepository.syncFreezeTokensForCurrentMonth() }
        }
        observeHabits()
        observeProfile()
    }

    fun createHabit(
        title: String,
        category: HabitCategory,
        type: HabitType,
        target: String,
        note: String,
    ) {
        val state = _uiState.value
        if (state.userId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please sign in to add habits.") }
            return
        }

        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Choose or enter a habit name.") }
            return
        }

        viewModelScope.launch {
            runCatching {
                val newHabit = Habit(
                    id = generateHabitId(),
                    title = trimmedTitle,
                    streak = 0,
                    isCompletedToday = false,
                    lastCompletedDate = null,
                    category = category,
                    type = type,
                    visibility = HabitVisibility.PRIVATE,
                    completionDates = emptyList(),
                    target = target.trim(),
                    note = note.trim(),
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
            _uiState.update { it.copy(uploadingHabitId = habitId, errorMessage = null, infoMessage = null) }
            runCatching {
                val today = currentEpochDay()
                val latestDay = currentHabit.completionDates.maxOrNull()
                val freezeProtected = latestDay == today - 2 && profileRepository.consumeFreezeTokenIfAvailable()
                val nextStreak = when {
                    currentHabit.completionDates.contains(today) -> currentHabit.streak
                    latestDay == null -> 1
                    latestDay == today - 1 -> currentHabit.streak + 1
                    freezeProtected -> currentHabit.streak + 1
                    else -> 1
                }
                val updatedCompletions = (currentHabit.completionDates + today)
                    .distinct()
                    .sorted()
                val updatedCompletionTimestamps = (currentHabit.completionTimestamps + System.currentTimeMillis())
                    .sorted()
                val completionMoment = System.currentTimeMillis()
                val updatedCompletionHistory = currentHabit.completionHistory
                    .filterNot { it.epochDay == today }
                    .plus(
                        HabitCompletionRecord(
                            epochDay = today,
                            completedAt = completionMoment,
                            proofImageUrl = currentHabit.proofImageUrl.takeIf { currentHabit.lastCompletedDate == today },
                        ),
                    )
                    .sortedBy { it.completedAt }
                val updatedHabit = currentHabit.copy(
                    streak = nextStreak,
                    isCompletedToday = true,
                    lastCompletedDate = today,
                    proofImageUrl = null,
                    completionDates = updatedCompletions,
                    completionTimestamps = updatedCompletionTimestamps,
                    completionHistory = updatedCompletionHistory,
                )
                habitRepository.updateHabit(
                    habit = updatedHabit,
                    proofImageBytes = proofImageBytes,
                )

                if (freezeProtected) {
                    _uiState.update {
                        it.copy(
                            infoMessage = "Streak protected using 1 freeze token.",
                            freezeTokensThisMonth = (it.freezeTokensThisMonth - 1).coerceAtLeast(0),
                        )
                    }
                }
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

    fun renameCustomHabit(habitId: String, newTitle: String) {
        val state = _uiState.value
        val habit = state.habits.firstOrNull { it.id == habitId } ?: return
        if (habit.type != HabitType.OTHER) {
            _uiState.update { it.copy(errorMessage = "Only custom habits can be renamed.") }
            return
        }

        val trimmedTitle = newTitle.trim()
        if (trimmedTitle.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Habit name cannot be empty.") }
            return
        }

        viewModelScope.launch {
            runCatching {
                habitRepository.updateHabit(
                    habit = habit.copy(title = trimmedTitle),
                )
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Failed to update habit.") }
            }
        }
    }

    fun updateCustomHabit(habitId: String, newTitle: String, newTarget: String, newNote: String) {
        val state = _uiState.value
        val habit = state.habits.firstOrNull { it.id == habitId } ?: return
        val trimmedTitle = newTitle.trim()
        if (habit.type == HabitType.OTHER && trimmedTitle.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Habit name cannot be empty.") }
            return
        }

        viewModelScope.launch {
            runCatching {
                habitRepository.updateHabit(
                    habit = habit.copy(
                        title = if (habit.type == HabitType.OTHER) trimmedTitle else habit.title,
                        target = newTarget.trim(),
                        note = newNote.trim(),
                    ),
                )
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Failed to update habit.") }
            }
        }
    }

    fun deleteCustomHabit(habitId: String) {
        val state = _uiState.value
        state.habits.firstOrNull { it.id == habitId } ?: return

        viewModelScope.launch {
            runCatching { habitRepository.deleteHabit(habitId) }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "Failed to delete habit.") }
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
                    runCatching {
                        reminderRepository.maybeSendEveningReminder(habits)
                    }
                    runCatching {
                        socialRepository.syncLeaderboardForHabits(habits)
                    }
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

    private fun observeProfile() {
        viewModelScope.launch {
            profileRepository.observeMyProfile()
                .catch { }
                .collect { profile ->
                    _uiState.update { current ->
                        current.copy(
                            freezeTokensThisMonth = profile?.freezeTokensThisMonth ?: current.freezeTokensThisMonth,
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
