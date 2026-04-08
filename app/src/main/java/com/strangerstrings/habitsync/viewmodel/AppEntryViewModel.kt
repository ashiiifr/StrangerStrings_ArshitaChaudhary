package com.strangerstrings.habitsync.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.strangerstrings.habitsync.data.repository.AuthRepository
import com.strangerstrings.habitsync.data.repository.OnboardingRepository
import com.strangerstrings.habitsync.navigation.HabitSyncDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppEntryUiState(
    val isLoading: Boolean = true,
    val destinationRoute: String = "",
)

class AppEntryViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val authRepository = AuthRepository()
    private val onboardingRepository = OnboardingRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(AppEntryUiState())
    val uiState: StateFlow<AppEntryUiState> = _uiState.asStateFlow()

    init {
        resolveAppEntry()
    }

    fun resolveAppEntry() {
        viewModelScope.launch {
            val onboardingCompleted = onboardingRepository.onboardingCompletedFlow().first()
            val isLoggedIn = authRepository.getCurrentUserId().isNotBlank()
            val destination = when {
                !onboardingCompleted -> HabitSyncDestination.Onboarding.route
                isLoggedIn -> HabitSyncDestination.Home.route
                else -> HabitSyncDestination.Login.route
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    destinationRoute = destination,
                )
            }
        }
    }
}
