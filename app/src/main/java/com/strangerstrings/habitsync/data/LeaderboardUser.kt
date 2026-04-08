package com.strangerstrings.habitsync.data

data class LeaderboardUser(
    val userId: String,
    val name: String,
    val score: Int,
    val lastUpdatedMillis: Long,
)
