package com.strangerstrings.habitsync.data

data class UserProfile(
    val userId: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val displayName: String,
    val bio: String = "",
    val profileImageUrl: String? = null,
    val weightKg: Float = 0f,
    val heightCm: Float = 0f,
    val totalHabitsCreated: Int = 0,
    val longestStreakEver: Int = 0,
    val challengesCompleted: Int = 0,
    val freezeTokensThisMonth: Int = 2,
)

data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val earnedAtMillis: Long,
)

