package com.strangerstrings.habitsync.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strangerstrings.habitsync.data.repository.AuthRepository
import com.strangerstrings.habitsync.data.repository.SignUpRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

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
    val dobMillis: Long? = null,
    val ageYears: Int? = null,
    val heightCm: String = "",
    val weightKg: String = "",
    val gender: String = "Prefer not to say",
    val selectedAvatarAssetPath: String? = null,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val passwordStrength: PasswordStrength = PasswordStrength.EMPTY,
    val emailValidationError: String? = null,
    val confirmPasswordError: String? = null,
    val usernameValidationError: String? = null,
    val isCheckingUsername: Boolean = false,
    val isCheckingEmail: Boolean = false,
    val isRecoveryMode: Boolean = false,
    val isRecoveryLoading: Boolean = false,
    val recoveryUsername: String = "",
    val recoveryEmail: String = "",
    val recoveryDobMillis: Long? = null,
    val recoveryErrorMessage: String? = null,
    val recoverySuccessMessage: String? = null,
)

data class PasswordSaveRequest(
    val email: String,
    val password: String,
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
) : ViewModel() {
    private var usernameValidationJob: Job? = null
    private var emailValidationJob: Job? = null

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isLoggedIn = authRepository.getCurrentUserId().isNotBlank(),
        ),
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _passwordSaveEvents = MutableSharedFlow<PasswordSaveRequest>()
    val passwordSaveEvents: SharedFlow<PasswordSaveRequest> = _passwordSaveEvents.asSharedFlow()

    fun switchMode(mode: AuthMode) {
        _uiState.update {
            val base = it.copy(
                mode = mode,
                errorMessage = null,
                emailValidationError = null,
                confirmPasswordError = null,
                usernameValidationError = null,
                isCheckingUsername = false,
                isCheckingEmail = false,
                isRecoveryMode = false,
                recoveryErrorMessage = null,
                recoverySuccessMessage = null,
            )
            if (mode == AuthMode.SIGN_UP) base.clearSignUpFields() else base
        }
    }

    fun toggleRecoveryMode() {
        _uiState.update {
            it.copy(
                isRecoveryMode = !it.isRecoveryMode,
                recoveryErrorMessage = null,
                recoverySuccessMessage = null,
            )
        }
    }

    fun onFirstNameChange(value: String) = updateField { it.copy(firstName = value, errorMessage = null) }
    fun onLastNameChange(value: String) = updateField { it.copy(lastName = value, errorMessage = null) }

    fun onUsernameChange(value: String) {
        val normalized = value.lowercase().filter { it.isLowerCase() || it.isDigit() }.take(USERNAME_MAX_LENGTH)
        usernameValidationJob?.cancel()
        updateField {
            it.copy(
                username = normalized,
                errorMessage = null,
                isCheckingUsername = false,
                usernameValidationError = when {
                    normalized.isBlank() -> null
                    !USERNAME_REGEX.matches(normalized) -> "Use 3-12 lowercase letters or numbers."
                    else -> null
                },
            )
        }

        if (_uiState.value.mode != AuthMode.SIGN_UP || normalized.length < 3 || !USERNAME_REGEX.matches(normalized)) {
            return
        }

        _uiState.update { it.copy(isCheckingUsername = true, usernameValidationError = null) }
        usernameValidationJob = viewModelScope.launch {
            delay(350)
            authRepository.isUsernameAvailable(normalized)
                .onSuccess { available ->
                    _uiState.update {
                        if (it.username != normalized || it.mode != AuthMode.SIGN_UP) {
                            it
                        } else {
                            it.copy(
                                isCheckingUsername = false,
                                usernameValidationError = if (available) null else "Username not available.",
                            )
                        }
                    }
                }.onFailure {
                    _uiState.update { state ->
                        if (state.username != normalized || state.mode != AuthMode.SIGN_UP) {
                            state
                        } else {
                            state.copy(
                                isCheckingUsername = false,
                                usernameValidationError = "Could not verify username. Check internet.",
                            )
                        }
                    }
                }
        }
    }

    fun onDateOfBirthSelected(dobMillis: Long?) {
        val age = dobMillis?.let { calculateAgeYears(it) }
        updateField {
            it.copy(
                dobMillis = dobMillis,
                ageYears = age,
                errorMessage = null,
            )
        }
    }

    fun onHeightChange(value: String) = updateField { it.copy(heightCm = sanitizeDecimal(value), errorMessage = null) }
    fun onWeightChange(value: String) = updateField { it.copy(weightKg = sanitizeDecimal(value), errorMessage = null) }
    fun onGenderChange(value: String) = updateField { it.copy(gender = value, errorMessage = null) }
    fun onAvatarSelected(value: String) = updateField { it.copy(selectedAvatarAssetPath = value, errorMessage = null) }

    fun onEmailChange(value: String) {
        emailValidationJob?.cancel()
        val trimmed = value.trim()
        updateField {
            it.copy(
                email = trimmed,
                errorMessage = null,
                isCheckingEmail = false,
                emailValidationError = when {
                    trimmed.isBlank() -> null
                    EMAIL_REGEX.matches(trimmed) -> null
                    else -> "Enter a valid email address."
                },
            )
        }

        if (_uiState.value.mode != AuthMode.SIGN_UP || trimmed.isBlank() || !EMAIL_REGEX.matches(trimmed)) {
            return
        }

        _uiState.update { it.copy(isCheckingEmail = true, emailValidationError = null) }
        emailValidationJob = viewModelScope.launch {
            delay(350)
            authRepository.isEmailAvailable(trimmed)
                .onSuccess { available ->
                    _uiState.update {
                        if (it.email != trimmed || it.mode != AuthMode.SIGN_UP) {
                            it
                        } else {
                            it.copy(
                                isCheckingEmail = false,
                                emailValidationError = if (available) null else "Email already registered.",
                            )
                        }
                    }
                }
                .onFailure {
                    _uiState.update {
                        if (it.email != trimmed || it.mode != AuthMode.SIGN_UP) {
                            it
                        } else {
                            it.copy(
                                isCheckingEmail = false,
                                emailValidationError = "Could not verify email right now.",
                            )
                        }
                    }
                }
        }
    }

    fun onPasswordChange(value: String) {
        updateField {
            it.copy(
                password = value,
                passwordStrength = evaluatePasswordStrength(value),
                errorMessage = null,
                confirmPasswordError = if (it.confirmPassword.isNotBlank() && it.confirmPassword != value) {
                    "Passwords do not match."
                } else {
                    null
                },
            )
        }
    }

    fun onConfirmPasswordChange(value: String) {
        updateField {
            it.copy(
                confirmPassword = value,
                errorMessage = null,
                confirmPasswordError = if (value.isNotBlank() && value != it.password) {
                    "Passwords do not match."
                } else {
                    null
                },
            )
        }
    }

    fun onRecoveryUsernameChange(value: String) {
        val normalized = value.lowercase().filter { it.isLowerCase() || it.isDigit() }.take(USERNAME_MAX_LENGTH)
        updateField { it.copy(recoveryUsername = normalized, recoveryErrorMessage = null, recoverySuccessMessage = null) }
    }

    fun onRecoveryEmailChange(value: String) {
        updateField { it.copy(recoveryEmail = value.trim(), recoveryErrorMessage = null, recoverySuccessMessage = null) }
    }

    fun onRecoveryDobSelected(dobMillis: Long?) {
        updateField { it.copy(recoveryDobMillis = dobMillis, recoveryErrorMessage = null, recoverySuccessMessage = null) }
    }

    fun togglePasswordVisibility() = updateField { it.copy(passwordVisible = !it.passwordVisible) }
    fun toggleConfirmPasswordVisibility() = updateField { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }

    fun submit() {
        when (_uiState.value.mode) {
            AuthMode.SIGN_IN -> signIn()
            AuthMode.SIGN_UP -> signUp()
        }
    }

    fun submitRecovery() {
        val state = _uiState.value
        if (state.recoveryUsername.isBlank() || state.recoveryEmail.isBlank() || state.recoveryDobMillis == null) {
            _uiState.update { it.copy(recoveryErrorMessage = "Enter username, email, and date of birth.") }
            return
        }
        if (!EMAIL_REGEX.matches(state.recoveryEmail)) {
            _uiState.update { it.copy(recoveryErrorMessage = "Enter a valid recovery email.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isRecoveryLoading = true, recoveryErrorMessage = null, recoverySuccessMessage = null) }
            val verified = authRepository.verifyRecoveryInfo(
                email = state.recoveryEmail,
                username = state.recoveryUsername,
                dobMillis = state.recoveryDobMillis,
            ).getOrElse { false }

            if (!verified) {
                _uiState.update {
                    it.copy(
                        isRecoveryLoading = false,
                        recoveryErrorMessage = "Recovery details did not match.",
                    )
                }
                return@launch
            }

            authRepository.sendPasswordResetEmail(state.recoveryEmail)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isRecoveryLoading = false,
                            recoverySuccessMessage = "Password reset email sent. Check your inbox.",
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isRecoveryLoading = false,
                            recoveryErrorMessage = "Could not send reset email. Please try again.",
                        )
                    }
                }
        }
    }

    fun signOut() {
        usernameValidationJob?.cancel()
        emailValidationJob?.cancel()
        authRepository.signOut()
        _uiState.update {
            it.copy(
                isLoading = false,
                isLoggedIn = false,
                errorMessage = null,
                password = "",
                confirmPassword = "",
            ).clearSignUpFields()
        }
    }

    private fun signIn() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            setError("Email and password are required.")
            return
        }
        if (!EMAIL_REGEX.matches(state.email)) {
            _uiState.update { it.copy(emailValidationError = "Enter a valid email address.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.signIn(email = state.email.trim(), password = state.password)
                .onSuccess {
                    val currentPassword = state.password
                    val currentEmail = state.email.trim()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            errorMessage = null,
                            password = "",
                            confirmPassword = "",
                        )
                    }
                    if (currentEmail.isNotBlank() && currentPassword.isNotBlank()) {
                        _passwordSaveEvents.emit(
                            PasswordSaveRequest(
                                email = currentEmail,
                                password = currentPassword,
                            ),
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

        val age = state.ageYears ?: 0
        val heightCm = state.heightCm.toFloatOrNull() ?: 0f
        val weightKg = state.weightKg.toFloatOrNull() ?: 0f
        val dobMillis = state.dobMillis ?: 0L

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.signUp(
                SignUpRequest(
                    firstName = state.firstName.trim(),
                    lastName = state.lastName.trim(),
                    username = state.username.lowercase(),
                    age = age,
                    dobMillis = dobMillis,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    gender = state.gender,
                    profileImageUrl = state.selectedAvatarAssetPath,
                    email = state.email.trim(),
                    password = state.password,
                ),
            ).onSuccess {
                val currentPassword = state.password
                val currentEmail = state.email.trim()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        errorMessage = null,
                        password = "",
                        confirmPassword = "",
                    )
                }
                if (currentEmail.isNotBlank() && currentPassword.isNotBlank()) {
                    _passwordSaveEvents.emit(
                        PasswordSaveRequest(
                            email = currentEmail,
                            password = currentPassword,
                        ),
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
        if (!USERNAME_REGEX.matches(state.username)) return "Username must be 3-12 chars using lowercase letters and numbers."
        if (state.usernameValidationError != null) return state.usernameValidationError
        if (state.selectedAvatarAssetPath.isNullOrBlank()) return "Choose a profile icon."

        val dobMillis = state.dobMillis ?: return "Date of birth is required."
        val age = calculateAgeYears(dobMillis)
        if (age !in 10..120) return "Age should be between 10 and 120."

        val heightCm = state.heightCm.toFloatOrNull() ?: return "Enter a valid height in cm."
        if (heightCm !in 50f..250f) return "Height should be between 50 and 250 cm."

        val weightKg = state.weightKg.toFloatOrNull() ?: return "Enter a valid weight in kg."
        if (weightKg !in 20f..400f) return "Weight should be between 20 and 400 kg."

        if (state.email.isBlank()) return "Email is required."
        if (!EMAIL_REGEX.matches(state.email)) return "Enter a valid email address."
        if (state.isCheckingEmail) return "Checking email..."
        if (state.emailValidationError != null) return state.emailValidationError
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

    private fun calculateAgeYears(dobMillis: Long): Int {
        val dob = Calendar.getInstance().apply { timeInMillis = dobMillis }
        val now = Calendar.getInstance()

        var age = now.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        val monthNow = now.get(Calendar.MONTH)
        val monthDob = dob.get(Calendar.MONTH)
        val dayNow = now.get(Calendar.DAY_OF_MONTH)
        val dayDob = dob.get(Calendar.DAY_OF_MONTH)
        val hasNotHadBirthdayYet = monthNow < monthDob || (monthNow == monthDob && dayNow < dayDob)
        if (hasNotHadBirthdayYet) age--

        return age.coerceAtLeast(0)
    }

    private companion object {
        const val USERNAME_MAX_LENGTH = 12
        val USERNAME_REGEX = Regex("^[a-z0-9]{3,12}$")
        val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    }
}

private fun AuthUiState.clearSignUpFields(): AuthUiState {
    return copy(
        firstName = "",
        lastName = "",
        username = "",
        dobMillis = null,
        ageYears = null,
        heightCm = "",
        weightKg = "",
        gender = "Prefer not to say",
        selectedAvatarAssetPath = null,
        email = "",
        password = "",
        confirmPassword = "",
        passwordVisible = false,
        confirmPasswordVisible = false,
        passwordStrength = PasswordStrength.EMPTY,
        emailValidationError = null,
        confirmPasswordError = null,
        usernameValidationError = null,
        isCheckingUsername = false,
        isCheckingEmail = false,
    )
}
