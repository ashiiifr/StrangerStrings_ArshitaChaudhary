package com.strangerstrings.habitsync.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strangerstrings.habitsync.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isLoggedIn = authRepository.getCurrentUserId().isNotBlank(),
        ),
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Email and password are required.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.signIn(email.trim(), password)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = false,
                            errorMessage = friendlyAuthMessage(error),
                        )
                    }
                }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.update {
            it.copy(
                isLoading = false,
                isLoggedIn = false,
                errorMessage = null,
            )
        }
    }

    private fun friendlyAuthMessage(error: Throwable): String {
        val message = error.message.orEmpty().lowercase()
        return when {
            "password is invalid" in message -> "Invalid email or password."
            "no user record" in message -> "Invalid email or password."
            "network error" in message -> "Network issue. Please try again."
            else -> "Login failed. Please try again."
        }
    }
}
