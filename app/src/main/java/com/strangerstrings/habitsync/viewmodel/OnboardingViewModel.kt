package com.strangerstrings.habitsync.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.strangerstrings.habitsync.data.repository.OnboardingRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class OnboardingViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val onboardingRepository = OnboardingRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<Unit>()
    val navigationEvents: SharedFlow<Unit> = _navigationEvents.asSharedFlow()

    fun completeOnboarding() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                onboardingRepository.markOnboardingCompleted()
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                _navigationEvents.emit(Unit)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Unable to save onboarding progress.",
                    )
                }
            }
        }
    }
}
