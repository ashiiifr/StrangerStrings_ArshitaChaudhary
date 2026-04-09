package com.strangerstrings.habitsync.data

enum class HabitCategory {
    FITNESS,
    READING,
    HYDRATION,
    SLEEP,
    CUSTOM,
}

enum class HabitType(val displayName: String) {
    READING("Reading"),
    WRITING("Writing"),
    STUDYING("Studying"),
    DRINK_WATER("Drink Water"),
    RUNNING("Running"),
    WALKING("Walking"),
    SLEEP_EARLY("Sleep Early"),
    MEDITATION("Meditation"),
    WORKOUT("Workout"),
    OTHER("Other"),
}

enum class HabitVisibility {
    PUBLIC,
    PRIVATE,
}

data class Habit(
    val id: String,
    val title: String,
    val streak: Int,
    val isCompletedToday: Boolean,
    val lastCompletedDate: Long? = null,
    val proofImageUrl: String? = null,
    val category: HabitCategory = HabitCategory.CUSTOM,
    val type: HabitType = HabitType.OTHER,
    val reminderTime: String = "20:00",
    val visibility: HabitVisibility = HabitVisibility.PRIVATE,
    val completionDates: List<Long> = emptyList(),
    val completionTimestamps: List<Long> = emptyList(),
    val completionHistory: List<HabitCompletionRecord> = emptyList(),
    val target: String = "",
    val note: String = "",
)

data class HabitCompletionRecord(
    val epochDay: Long,
    val completedAt: Long,
    val proofImageUrl: String? = null,
)
