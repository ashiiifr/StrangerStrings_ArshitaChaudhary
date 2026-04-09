package com.strangerstrings.habitsync.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strangerstrings.habitsync.data.Badge
import com.strangerstrings.habitsync.data.EditableProfile
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
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val editableProfile: EditableProfile = EditableProfile(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // Password change
    val isChangingPassword: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val isPasswordSaving: Boolean = false,
    val passwordError: String? = null,
    val passwordSuccess: String? = null,
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

    fun startEditing() {
        val profile = _uiState.value.profile ?: return
        _uiState.update {
            it.copy(
                isEditing = true,
                successMessage = null,
                editableProfile = EditableProfile(
                    firstName = profile.firstName,
                    lastName = profile.lastName,
                    username = profile.username,
                    bio = profile.bio,
                    gender = profile.gender,
                    heightCm = formatDecimal(profile.heightCm),
                    weightKg = formatDecimal(profile.weightKg),
                ),
            )
        }
    }

    fun cancelEditing() {
        _uiState.update {
            it.copy(
                isEditing = false,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun onFirstNameChange(value: String) = updateEditable { it.copy(firstName = value) }
    fun onLastNameChange(value: String) = updateEditable { it.copy(lastName = value) }
    fun onUsernameChange(value: String) = updateEditable {
        it.copy(username = value.lowercase().filter { ch -> ch.isLowerCase() || ch.isDigit() }.take(12))
    }
    fun onBioChange(value: String) = updateEditable { it.copy(bio = value.take(180)) }
    fun onGenderChange(value: String) = updateEditable { it.copy(gender = value) }
    fun onHeightChange(value: String) = updateEditable { it.copy(heightCm = sanitizeDecimal(value)) }
    fun onWeightChange(value: String) = updateEditable { it.copy(weightKg = sanitizeDecimal(value)) }

    fun saveProfile() {
        val editableProfile = _uiState.value.editableProfile
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            runCatching { repository.updateMyProfile(editableProfile) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isEditing = false,
                            errorMessage = null,
                            successMessage = "Profile updated.",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Unable to update profile.",
                            successMessage = null,
                        )
                    }
                }
        }
    }

    // ── Password change ─────────────────────────────────────
    fun openChangePassword() {
        _uiState.update {
            it.copy(
                isChangingPassword = true,
                currentPassword = "",
                newPassword = "",
                confirmNewPassword = "",
                passwordError = null,
                passwordSuccess = null,
            )
        }
    }

    fun closeChangePassword() {
        _uiState.update {
            it.copy(
                isChangingPassword = false,
                currentPassword = "",
                newPassword = "",
                confirmNewPassword = "",
                passwordError = null,
            )
        }
    }

    fun onCurrentPasswordChange(value: String) = _uiState.update { it.copy(currentPassword = value, passwordError = null) }
    fun onNewPasswordChange(value: String) = _uiState.update { it.copy(newPassword = value, passwordError = null) }
    fun onConfirmNewPasswordChange(value: String) = _uiState.update { it.copy(confirmNewPassword = value, passwordError = null) }

    fun submitPasswordChange() {
        val state = _uiState.value
        if (state.currentPassword.isBlank()) {
            _uiState.update { it.copy(passwordError = "Enter your current password.") }
            return
        }
        if (state.newPassword.length < 8) {
            _uiState.update { it.copy(passwordError = "New password must be at least 8 characters.") }
            return
        }
        if (state.newPassword != state.confirmNewPassword) {
            _uiState.update { it.copy(passwordError = "Passwords do not match.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isPasswordSaving = true, passwordError = null) }
            runCatching {
                repository.changePassword(state.currentPassword, state.newPassword)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isPasswordSaving = false,
                        isChangingPassword = false,
                        currentPassword = "",
                        newPassword = "",
                        confirmNewPassword = "",
                        passwordSuccess = "Password changed successfully.",
                        passwordError = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isPasswordSaving = false,
                        passwordError = error.message ?: "Failed to change password.",
                    )
                }
            }
        }
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
                            errorMessage = if (it.isEditing) it.errorMessage else null,
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

    private fun updateEditable(transform: (EditableProfile) -> EditableProfile) {
        _uiState.update {
            it.copy(
                editableProfile = transform(it.editableProfile),
                errorMessage = null,
                successMessage = null,
            )
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

    private fun formatDecimal(value: Float): String {
        return if (value == 0f) "" else if (value % 1f == 0f) value.toInt().toString() else value.toString()
    }
}
