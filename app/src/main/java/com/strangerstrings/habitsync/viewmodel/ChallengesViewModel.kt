package com.strangerstrings.habitsync.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strangerstrings.habitsync.data.Challenge
import com.strangerstrings.habitsync.data.ChallengeInvite
import com.strangerstrings.habitsync.data.ChallengeMessage
import com.strangerstrings.habitsync.data.ChallengeParticipant
import com.strangerstrings.habitsync.data.HabitCategory
import com.strangerstrings.habitsync.data.repository.AuthRepository
import com.strangerstrings.habitsync.data.repository.ChallengesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChallengesUiState(
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val challenges: List<Challenge> = emptyList(),
    val selectedChallenge: Challenge? = null,
    val participantLeaderboard: List<ChallengeParticipant> = emptyList(),
    val chatMessages: List<ChallengeMessage> = emptyList(),
    val chatInput: String = "",
    val isRoomLoading: Boolean = false,
    val isMarkingDone: Boolean = false,
    val currentUserId: String = "",
    val pendingInviteCount: Int = 0,
    val errorMessage: String? = null,
)

class ChallengesViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val repository: ChallengesRepository = ChallengesRepository(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ChallengesUiState(
            isLoading = true,
            currentUserId = authRepository.getCurrentUserId(),
        ),
    )
    val uiState: StateFlow<ChallengesUiState> = _uiState.asStateFlow()
    private var leaderboardJob: Job? = null
    private var chatJob: Job? = null
    private val settlingChallengeIds = mutableSetOf<String>()

    init {
        observeChallenges()
        observeChallengeInvites()
    }

    fun createChallenge(
        name: String,
        rule: String,
        durationDays: Int,
        category: HabitCategory,
        inviteUsernames: List<String>,
    ) {
        if (name.isBlank() || rule.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Challenge name and rule are required.") }
            return
        }
        if (durationDays !in listOf(7, 14, 30)) {
            _uiState.update { it.copy(errorMessage = "Duration must be 7, 14, or 30 days.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, errorMessage = null) }
            runCatching {
                repository.createChallenge(
                    name = name.trim(),
                    category = category,
                    rule = rule.trim(),
                    durationDays = durationDays,
                    inviteUsernames = inviteUsernames,
                )
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Failed to create challenge.")
                }
            }
            _uiState.update { it.copy(isCreating = false) }
        }
    }

    fun openChallengeRoom(challenge: Challenge) {
        _uiState.update {
            it.copy(
                selectedChallenge = challenge,
                participantLeaderboard = emptyList(),
                chatMessages = emptyList(),
                isRoomLoading = true,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            runCatching { repository.syncChallengeState(challenge.id) }
            observeRoom(challenge.id)
        }
    }

    fun openChallengeRoom(challengeId: String) {
        val challenge = _uiState.value.challenges.firstOrNull { it.id == challengeId } ?: return
        openChallengeRoom(challenge)
    }

    fun closeChallengeRoom() {
        leaderboardJob?.cancel()
        chatJob?.cancel()
        _uiState.update {
            it.copy(
                selectedChallenge = null,
                participantLeaderboard = emptyList(),
                chatMessages = emptyList(),
                chatInput = "",
                isRoomLoading = false,
            )
        }
    }

    fun onChatInputChange(value: String) {
        _uiState.update { it.copy(chatInput = value) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val challengeId = state.selectedChallenge?.id.orEmpty()
        val message = state.chatInput.trim()
        if (challengeId.isBlank() || message.isBlank()) return

        viewModelScope.launch {
            runCatching {
                repository.sendChallengeMessage(
                    challengeId = challengeId,
                    message = message,
                )
            }.onSuccess {
                _uiState.update { it.copy(chatInput = "") }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Failed to send message.")
                }
            }
        }
    }

    fun markChallengeDone() {
        val challengeId = _uiState.value.selectedChallenge?.id.orEmpty()
        if (challengeId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isMarkingDone = true, errorMessage = null) }
            runCatching {
                repository.markChallengeDone(challengeId)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Failed to mark challenge done.")
                }
            }
            _uiState.update { it.copy(isMarkingDone = false) }
        }
    }

    private fun observeChallenges() {
        viewModelScope.launch {
            repository.observeMyChallenges()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to load challenges.",
                        )
                    }
                }
                .collect { challenges ->
                    challenges.forEach { challenge ->
                        runCatching { repository.syncChallengeState(challenge.id) }
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            challenges = challenges,
                            errorMessage = null,
                        )
                    }
                    settleEndedChallenges(challenges)
                }
        }
    }

    private fun observeChallengeInvites() {
        viewModelScope.launch {
            repository.observeChallengeInvites()
                .catch { }
                .collect { invites: List<ChallengeInvite> ->
                    _uiState.update { it.copy(pendingInviteCount = invites.size) }
                }
        }
    }

    private fun observeRoom(challengeId: String) {
        leaderboardJob?.cancel()
        chatJob?.cancel()

        leaderboardJob = viewModelScope.launch {
            repository.observeChallengeLeaderboard(challengeId)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isRoomLoading = false,
                            errorMessage = error.message ?: "Unable to load challenge leaderboard.",
                        )
                    }
                }
                .collect { participants ->
                    _uiState.update {
                        it.copy(
                            isRoomLoading = false,
                            participantLeaderboard = participants,
                            errorMessage = null,
                        )
                    }
                }
        }

        chatJob = viewModelScope.launch {
            repository.observeChallengeChat(challengeId)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isRoomLoading = false,
                            errorMessage = error.message ?: "Unable to load challenge chat.",
                        )
                    }
                }
                .collect { messages ->
                    _uiState.update {
                        it.copy(
                            isRoomLoading = false,
                            chatMessages = messages,
                            errorMessage = null,
                        )
                    }
                }
        }
    }

    private fun settleEndedChallenges(challenges: List<Challenge>) {
        val now = System.currentTimeMillis()
        challenges
            .filter { challenge ->
                challenge.endsAtMillis <= now &&
                    challenge.settledAtMillis == null &&
                    !settlingChallengeIds.contains(challenge.id)
            }
            .forEach { challenge ->
                settlingChallengeIds += challenge.id
                viewModelScope.launch {
                    runCatching { repository.settleChallengeIfEnded(challenge.id) }
                    delay(80)
                    settlingChallengeIds.remove(challenge.id)
                }
            }
    }
}
