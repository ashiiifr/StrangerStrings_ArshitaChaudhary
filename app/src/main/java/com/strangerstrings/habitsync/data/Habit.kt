package com.strangerstrings.habitsync.data

enum class HabitCategory {
    FITNESS,
    READING,
    HYDRATION,
    SLEEP,
    CUSTOM,
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
    val reminderTime: String = "20:00",
    val visibility: HabitVisibility = HabitVisibility.PRIVATE,
    val completionDates: List<Long> = emptyList(),
    val completionTimestamps: List<Long> = emptyList(),
)
