package com.strangerstrings.habitsync.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.strangerstrings.habitsync.data.FriendLeaderboardEntry
import com.strangerstrings.habitsync.data.FriendProfileDetails
import com.strangerstrings.habitsync.data.FriendRelationshipState
import com.strangerstrings.habitsync.data.FriendRequest
import com.strangerstrings.habitsync.data.FriendUser
import com.strangerstrings.habitsync.data.LeaderboardFilter
import com.strangerstrings.habitsync.data.SearchFriendResult
import com.strangerstrings.habitsync.data.repository.FriendsRepository
import com.strangerstrings.habitsync.data.repository.LeaderboardRankStore
import com.strangerstrings.habitsync.data.repository.RankSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class FriendsUiState(
    val isLoading: Boolean = false,
    val query: String = "",
    val friends: List<FriendUser> = emptyList(),
    val suggestedUsers: List<SearchFriendResult> = emptyList(),
    val searchResults: List<SearchFriendResult> = emptyList(),
    val leaderboard: List<FriendLeaderboardEntry> = emptyList(),
    val selectedLeaderboardFilter: LeaderboardFilter = LeaderboardFilter.OVERALL,
    val pendingRequestCount: Int = 0,
    val selectedFriendProfile: FriendProfileDetails? = null,
    val isFriendProfileLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchMessage: String? = null,
)

class FriendsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = FriendsRepository()
    private val _uiState = MutableStateFlow(FriendsUiState(isLoading = true))
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()
    private val rankStore = LeaderboardRankStore(application.applicationContext)

    private var searchJob: Job? = null
    private var leaderboardJob: Job? = null

    init {
        observeFriends()
        observeLeaderboard(LeaderboardFilter.OVERALL)
        observeIncomingRequests()
        loadSuggestedUsers()
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()

        val normalized = query.trim()
        if (normalized.length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), errorMessage = null, searchMessage = null) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(260)
            runCatching { repository.searchUsersByUsername(normalized) }
                .onSuccess { users ->
                    _uiState.update { state ->
                        val friendIds = state.friends.map(FriendUser::userId).toSet()
                        val filteredUsers = users.filterNot { candidate -> friendIds.contains(candidate.userId) }
                        state.copy(
                            searchResults = filteredUsers,
                            errorMessage = null,
                            searchMessage = when {
                                filteredUsers.isEmpty() -> "No users matched that username yet."
                                filteredUsers.any { it.relationshipState == FriendRelationshipState.NONE } ->
                                    "Tap the plus button to send a friend request."
                                else -> "User found."
                            },
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message ?: "Unable to search users right now.",
                            searchMessage = null,
                        )
                    }
                }
        }
    }

    fun submitSearch(query: String) {
        val normalized = query.trim()
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()

        if (normalized.length < 2) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    errorMessage = null,
                    searchMessage = "Enter at least 2 characters to search.",
                )
            }
            return
        }

        searchJob = viewModelScope.launch {
            runCatching { repository.searchUsersByUsername(normalized) }
                .onSuccess { users ->
                    _uiState.update { state ->
                        val friendIds = state.friends.map(FriendUser::userId).toSet()
                        val filteredUsers = users.filterNot { candidate -> friendIds.contains(candidate.userId) }
                        state.copy(
                            searchResults = filteredUsers,
                            errorMessage = null,
                            searchMessage = when {
                                filteredUsers.isEmpty() -> "No users matched that username yet."
                                filteredUsers.any { it.relationshipState == FriendRelationshipState.NONE } ->
                                    "Tap the plus button to send a friend request."
                                else -> "User found."
                            },
                        )
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) return@onFailure
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message ?: "Unable to search users right now.",
                            searchMessage = null,
                        )
                    }
                }
        }
    }

    fun sendFriendRequest(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.sendFriendRequest(userId) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            searchResults = state.searchResults.map { result ->
                                if (result.userId == userId) {
                                    result.copy(relationshipState = FriendRelationshipState.REQUEST_SENT)
                                } else {
                                    result
                                }
                            },
                            errorMessage = null,
                            searchMessage = "Friend request sent.",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Failed to send friend request.")
                    }
                }
        }
    }

    fun openFriendProfile(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isFriendProfileLoading = true, errorMessage = null) }
            runCatching { repository.fetchFriendProfile(userId) }
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            selectedFriendProfile = profile,
                            isFriendProfileLoading = false,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isFriendProfileLoading = false,
                            errorMessage = error.message ?: "Could not load friend profile.",
                        )
                    }
                }
        }
    }

    fun closeFriendProfile() {
        _uiState.update { it.copy(selectedFriendProfile = null, isFriendProfileLoading = false) }
    }

    fun removeFriend() {
        val friendUserId = _uiState.value.selectedFriendProfile?.userId.orEmpty()
        if (friendUserId.isBlank()) return

        viewModelScope.launch {
            runCatching { repository.removeFriend(friendUserId) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            selectedFriendProfile = null,
                            isFriendProfileLoading = false,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Unable to remove friend.")
                    }
                }
        }
    }

    fun selectLeaderboardFilter(filter: LeaderboardFilter) {
        if (_uiState.value.selectedLeaderboardFilter == filter) return
        _uiState.update { it.copy(selectedLeaderboardFilter = filter) }
        observeLeaderboard(filter)
    }

    private fun observeFriends() {
        viewModelScope.launch {
            repository.observeFriends()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to load friends.",
                        )
                    }
                }
                .collect { friends ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            friends = friends,
                            errorMessage = null,
                            searchResults = it.searchResults.filterNot { result ->
                                friends.any { friend -> friend.userId == result.userId }
                            },
                        )
                    }
                }
        }
    }

    private fun observeLeaderboard(filter: LeaderboardFilter) {
        leaderboardJob?.cancel()
        leaderboardJob = viewModelScope.launch {
            repository.observeFriendsLeaderboard(filter)
                .catch { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Unable to load friends leaderboard.")
                    }
                }
                .collect { leaderboard ->
                    _uiState.update {
                        it.copy(
                            leaderboard = if (filter == LeaderboardFilter.OVERALL) {
                                applyRankDelta(leaderboard)
                            } else {
                                leaderboard.map { entry -> entry.copy(rankDelta = 0) }
                            },
                        )
                    }
                }
        }
    }

    private fun observeIncomingRequests() {
        viewModelScope.launch {
            repository.observeIncomingFriendRequests()
                .catch { }
                .collect { requests: List<FriendRequest> ->
                    _uiState.update { it.copy(pendingRequestCount = requests.size) }
                }
        }
    }

    private fun loadSuggestedUsers() {
        viewModelScope.launch {
            runCatching { repository.fetchSuggestedUsers() }
                .onSuccess { suggestions ->
                    _uiState.update { it.copy(suggestedUsers = suggestions) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Unable to load friend suggestions.")
                    }
                }
        }
    }

    private fun applyRankDelta(entries: List<FriendLeaderboardEntry>): List<FriendLeaderboardEntry> {
        if (entries.isEmpty()) return entries

        val todayKey = dayKey(0)
        val yesterdayKey = dayKey(-1)
        val currentRanks = entries.associate { it.userId to it.rank }

        val storedToday = rankStore.loadTodaySnapshot()
        val storedYesterday = rankStore.loadYesterdaySnapshot()

        if (storedToday == null || storedToday.dayKey != todayKey) {
            if (storedToday != null) {
                rankStore.saveYesterdaySnapshot(storedToday)
            }
            rankStore.saveTodaySnapshot(
                RankSnapshot(
                    dayKey = todayKey,
                    ranks = currentRanks,
                ),
            )
        } else if (storedToday.ranks != currentRanks) {
            rankStore.saveTodaySnapshot(
                RankSnapshot(
                    dayKey = todayKey,
                    ranks = currentRanks,
                ),
            )
        }

        val yesterdayRanks = when {
            storedYesterday?.dayKey == yesterdayKey -> storedYesterday.ranks
            storedToday?.dayKey == yesterdayKey -> storedToday.ranks
            else -> emptyMap()
        }

        return entries.map { entry ->
            val previousRank = yesterdayRanks[entry.userId]
            val delta = if (previousRank != null) {
                previousRank - entry.rank
            } else {
                0
            }
            entry.copy(rankDelta = delta)
        }
    }

    private fun dayKey(dayOffset: Int): String {
        val calendar = Calendar.getInstance()
        if (dayOffset != 0) {
            calendar.add(Calendar.DAY_OF_YEAR, dayOffset)
        }
        val year = calendar.get(Calendar.YEAR)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        return "%04d-%03d".format(year, dayOfYear)
    }
}
