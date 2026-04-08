package com.strangerstrings.habitsync.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strangerstrings.habitsync.data.repository.AuthRepository
import com.strangerstrings.habitsync.data.repository.SignUpRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode {
    SIGN_IN,
    SIGN_UP,
}

enum class PasswordStrength {
    EMPTY,
    WEAK,
    MEDIUM,
    STRONG,
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val mode: AuthMode = AuthMode.SIGN_IN,
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val age: String = "",
    val heightCm: String = "",
    val weightKg: String = "",
    val gender: String = "Prefer not to say",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val passwordStrength: PasswordStrength = PasswordStrength.EMPTY,
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

    fun switchMode(mode: AuthMode) {
        _uiState.update {
            it.copy(
                mode = mode,
                errorMessage = null,
            )
        }
    }

    fun onFirstNameChange(value: String) = updateField { it.copy(firstName = value, errorMessage = null) }
    fun onLastNameChange(value: String) = updateField { it.copy(lastName = value, errorMessage = null) }
    fun onUsernameChange(value: String) {
        val normalized = value.lowercase().filter { it.isLowerCase() || it.isDigit() }.take(USERNAME_MAX_LENGTH)
        updateField { it.copy(username = normalized, errorMessage = null) }
    }

    fun onAgeChange(value: String) = updateField { it.copy(age = value.filter { ch -> ch.isDigit() }.take(3), errorMessage = null) }
    fun onHeightChange(value: String) = updateField { it.copy(heightCm = sanitizeDecimal(value), errorMessage = null) }
    fun onWeightChange(value: String) = updateField { it.copy(weightKg = sanitizeDecimal(value), errorMessage = null) }
    fun onGenderChange(value: String) = updateField { it.copy(gender = value, errorMessage = null) }
    fun onEmailChange(value: String) = updateField { it.copy(email = value.trim(), errorMessage = null) }

    fun onPasswordChange(value: String) {
        updateField {
            it.copy(
                password = value,
                passwordStrength = evaluatePasswordStrength(value),
                errorMessage = null,
            )
        }
    }

    fun onConfirmPasswordChange(value: String) = updateField { it.copy(confirmPassword = value, errorMessage = null) }
    fun togglePasswordVisibility() = updateField { it.copy(passwordVisible = !it.passwordVisible) }
    fun toggleConfirmPasswordVisibility() = updateField { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }

    fun submit() {
        when (_uiState.value.mode) {
            AuthMode.SIGN_IN -> signIn()
            AuthMode.SIGN_UP -> signUp()
        }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.update {
            it.copy(
                isLoading = false,
                isLoggedIn = false,
                errorMessage = null,
                password = "",
                confirmPassword = "",
            )
        }
    }

    private fun signIn() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            setError("Email and password are required.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.signIn(email = state.email.trim(), password = state.password)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            errorMessage = null,
                            password = "",
                            confirmPassword = "",
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

    private fun signUp() {
        val state = _uiState.value
        val validationError = validateSignUp(state)
        if (validationError != null) {
            setError(validationError)
            return
        }

        val age = state.age.toIntOrNull() ?: 0
        val heightCm = state.heightCm.toFloatOrNull() ?: 0f
        val weightKg = state.weightKg.toFloatOrNull() ?: 0f

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.signUp(
                SignUpRequest(
                    firstName = state.firstName.trim(),
                    lastName = state.lastName.trim(),
                    username = state.username.lowercase(),
                    age = age,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    gender = state.gender,
                    email = state.email.trim(),
                    password = state.password,
                ),
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        errorMessage = null,
                        password = "",
                        confirmPassword = "",
                    )
                }
            }.onFailure { error ->
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

    private fun validateSignUp(state: AuthUiState): String? {
        if (state.firstName.isBlank()) return "First name is required."
        if (state.lastName.isBlank()) return "Last name is required."
        if (state.username.isBlank()) return "Username is required."
        if (!USERNAME_REGEX.matches(state.username)) {
            return "Username must be 3-12 chars using lowercase letters and numbers."
        }
        val age = state.age.toIntOrNull() ?: return "Enter a valid age."
        if (age !in 10..120) return "Age should be between 10 and 120."

        val heightCm = state.heightCm.toFloatOrNull() ?: return "Enter a valid height in cm."
        if (heightCm !in 50f..250f) return "Height should be between 50 and 250 cm."

        val weightKg = state.weightKg.toFloatOrNull() ?: return "Enter a valid weight in kg."
        if (weightKg !in 20f..400f) return "Weight should be between 20 and 400 kg."

        if (state.email.isBlank()) return "Email is required."
        if (!EMAIL_REGEX.matches(state.email)) return "Enter a valid email address."
        if (state.password.length < 8) return "Password must be at least 8 characters."
        if (state.confirmPassword != state.password) return "Passwords do not match."
        return null
    }

    private fun updateField(transform: (AuthUiState) -> AuthUiState) {
        _uiState.update(transform)
    }

    private fun setError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    private fun evaluatePasswordStrength(password: String): PasswordStrength {
        if (password.isBlank()) return PasswordStrength.EMPTY
        var score = 0
        if (password.length >= 8) score++
        if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        return when {
            score <= 1 -> PasswordStrength.WEAK
            score <= 3 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.STRONG
        }
    }

    private fun friendlyAuthMessage(error: Throwable): String {
        val message = error.message.orEmpty().lowercase()
        return when {
            "username already taken" in message -> "That username is already taken. Try another one."
            "password is invalid" in message -> "Invalid email or password."
            "no user record" in message -> "Invalid email or password."
            "email address is already in use" in message -> "Email already exists. Please sign in."
            "network error" in message -> "Network issue. Please try again."
            "badly formatted" in message -> "Enter a valid email address."
            "password should be at least" in message -> "Password is too weak."
            else -> "Authentication failed. Please try again."
        }
    }

    private fun sanitizeDecimal(input: String): String {
        val filtered = buildString {
            var dotSeen = false
            input.forEach { ch ->
                if (ch.isDigit()) append(ch)
                if (ch == '.' && !dotSeen) {
                    append(ch)
                    dotSeen = true
                }
            }
        }
        return filtered.take(6)
    }

    private companion object {
        const val USERNAME_MAX_LENGTH = 12
        val USERNAME_REGEX = Regex("^[a-z0-9]{3,12}$")
        val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    }
}
