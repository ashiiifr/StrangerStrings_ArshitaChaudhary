package com.strangerstrings.habitsync.data

data class FeedEvent(
    val id: String,
    val userId: String,
    val userName: String,
    val message: String,
    val timestampMillis: Long,
)
