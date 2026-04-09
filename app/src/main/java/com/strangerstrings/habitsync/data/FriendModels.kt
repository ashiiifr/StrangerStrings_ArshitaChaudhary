package com.strangerstrings.habitsync.data

data class FriendUser(
    val userId: String,
    val username: String,
    val displayName: String,
    val profileImageUrl: String? = null,
)

enum class FriendRelationshipState {
    NONE,
    REQUEST_SENT,
    REQUEST_RECEIVED,
    FRIEND,
}

data class SearchFriendResult(
    val userId: String,
    val username: String,
    val displayName: String,
    val profileImageUrl: String? = null,
    val relationshipState: FriendRelationshipState = FriendRelationshipState.NONE,
)

data class FriendRequest(
    val id: String,
    val fromUserId: String,
    val fromUsername: String,
    val fromDisplayName: String,
    val status: String,
    val createdAtMillis: Long,
)

data class FriendLeaderboardEntry(
    val userId: String,
    val username: String,
    val displayName: String = username,
    val profileImageUrl: String? = null,
    val score: Int,
    val activeHabitsCount: Int = 0,
    val rank: Int,
    val rankDelta: Int,
)

data class FriendPublicHabit(
    val id: String,
    val title: String,
    val streak: Int,
)

data class FriendProfileDetails(
    val userId: String,
    val username: String,
    val displayName: String,
    val bio: String,
    val profileImageUrl: String? = null,
    val badges: List<String> = emptyList(),
    val publicHabits: List<FriendPublicHabit> = emptyList(),
)
