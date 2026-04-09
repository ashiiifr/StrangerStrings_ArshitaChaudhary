package com.strangerstrings.habitsync.data

enum class LeaderboardFilter(
    val label: String,
    val habitType: HabitType?,
) {
    OVERALL("Overall", null),
    READING("Reading", HabitType.READING),
    WRITING("Writing", HabitType.WRITING),
    STUDYING("Studying", HabitType.STUDYING),
    DRINK_WATER("Water", HabitType.DRINK_WATER),
    RUNNING("Running", HabitType.RUNNING),
    WALKING("Walking", HabitType.WALKING),
    SLEEP_EARLY("Sleep", HabitType.SLEEP_EARLY),
    MEDITATION("Meditation", HabitType.MEDITATION),
    WORKOUT("Workout", HabitType.WORKOUT),
}

data class LeaderboardUser(
    val userId: String,
    val name: String,
    val username: String = "",
    val score: Int,
    val activeHabitsCount: Int = 0,
    val bestHabitTitle: String = "",
    val lastUpdatedMillis: Long,
)
