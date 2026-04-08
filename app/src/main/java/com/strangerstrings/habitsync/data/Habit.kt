package com.strangerstrings.habitsync.data

data class Habit(
    val id: String,
    val title: String,
    val streak: Int,
    val isCompletedToday: Boolean,
    val lastCompletedDate: Long? = null,
    val proofImageUrl: String? = null,
)
