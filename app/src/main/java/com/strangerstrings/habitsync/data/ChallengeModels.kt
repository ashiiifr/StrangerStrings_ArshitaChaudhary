package com.strangerstrings.habitsync.data

data class Challenge(
    val id: String,
    val name: String,
    val category: HabitCategory,
    val rule: String,
    val durationDays: Int,
    val creatorUserId: String,
    val participantIds: List<String>,
    val startedAtMillis: Long,
    val endsAtMillis: Long,
    val winnerUserId: String? = null,
    val settledAtMillis: Long? = null,
)

data class ChallengeParticipant(
    val userId: String,
    val username: String,
    val streak: Int,
    val isCompletedToday: Boolean = false,
    val lastCompletedDate: Long? = null,
    val completionDates: List<Long> = emptyList(),
    val proofImageUrl: String? = null,
)

data class ChallengeMessage(
    val id: String,
    val userId: String,
    val username: String,
    val message: String,
    val timestampMillis: Long,
)

data class ChallengeInvite(
    val challengeId: String,
    val name: String,
    val category: HabitCategory,
    val durationDays: Int,
    val rule: String,
    val status: String,
    val createdAtMillis: Long,
)
