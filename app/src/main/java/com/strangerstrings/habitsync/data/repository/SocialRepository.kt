package com.strangerstrings.habitsync.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.strangerstrings.habitsync.data.FeedEvent
import com.strangerstrings.habitsync.data.Habit
import com.strangerstrings.habitsync.data.LeaderboardUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SocialRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val authRepository: AuthRepository = AuthRepository(),
) {
    fun getLeaderboard(): Flow<List<LeaderboardUser>> = callbackFlow {
        val listener = firestore.collection(LEADERBOARD_COLLECTION)
            .orderBy(FIELD_SCORE, com.google.firebase.firestore.Query.Direction.DESCENDING)
            .orderBy(FIELD_LAST_UPDATED, com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val users = snapshot?.documents.orEmpty()
                    .mapNotNull { it.toLeaderboardUserOrNull() }
                trySend(users)
            }

        awaitClose { listener.remove() }
    }

    fun getFeed(): Flow<List<FeedEvent>> = callbackFlow {
        val listener = firestore.collection(FEED_COLLECTION)
            .orderBy(FIELD_TIMESTAMP, com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(120)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val events = snapshot?.documents.orEmpty()
                    .mapNotNull { it.toFeedEventOrNull() }
                trySend(events)
            }

        awaitClose { listener.remove() }
    }

    suspend fun recordHabitCompletion(habit: Habit, hasProof: Boolean) {
        val userId = authRepository.getCurrentUserId()
        if (userId.isBlank()) return

        val userName = authRepository.getCurrentUserName()
        val leaderboardRef = firestore.collection(LEADERBOARD_COLLECTION).document(userId)
        val feedRef = firestore.collection(FEED_COLLECTION).document()

        val completionMessage = if (hasProof) {
            "$userName completed ${habit.streak}-day streak with proof 📸 in ${habit.title}"
        } else {
            "$userName completed ${habit.streak}-day streak in ${habit.title}"
        }

        val updates = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any?>>>()
        updates += leaderboardRef to mapOf(
            FIELD_NAME to userName,
            FIELD_SCORE to FieldValue.increment(1),
            FIELD_LAST_UPDATED to FieldValue.serverTimestamp(),
        )
        updates += feedRef to mapOf(
            FIELD_USER_ID to userId,
            FIELD_NAME to userName,
            FIELD_MESSAGE to completionMessage,
            FIELD_TIMESTAMP to FieldValue.serverTimestamp(),
        )

        if (habit.streak > 0 && habit.streak % MILESTONE_INTERVAL == 0) {
            val milestoneRef = firestore.collection(FEED_COLLECTION).document()
            updates += milestoneRef to mapOf(
                FIELD_USER_ID to userId,
                FIELD_NAME to userName,
                FIELD_MESSAGE to "$userName hit a ${habit.streak}-day milestone",
                FIELD_TIMESTAMP to FieldValue.serverTimestamp(),
            )
        }

        val batch = firestore.batch()
        updates.forEach { (ref, values) ->
            batch.set(ref, values, com.google.firebase.firestore.SetOptions.merge())
        }
        batch.commit().await()
    }

    private fun DocumentSnapshot.toLeaderboardUserOrNull(): LeaderboardUser? {
        val userId = id
        val name = getString(FIELD_NAME).orEmpty().ifBlank { "Habit Hero" }
        val score = getLong(FIELD_SCORE)?.toInt() ?: 0
        val lastUpdatedMillis = getTimestamp(FIELD_LAST_UPDATED)?.toDate()?.time ?: 0L
        return LeaderboardUser(
            userId = userId,
            name = name,
            score = score.coerceAtLeast(0),
            lastUpdatedMillis = lastUpdatedMillis,
        )
    }

    private fun DocumentSnapshot.toFeedEventOrNull(): FeedEvent? {
        val eventId = id
        val userId = getString(FIELD_USER_ID).orEmpty()
        val name = getString(FIELD_NAME).orEmpty().ifBlank { "Habit Hero" }
        val message = getString(FIELD_MESSAGE).orEmpty().ifBlank { return null }
        val timestampMillis = getTimestamp(FIELD_TIMESTAMP)?.toDate()?.time ?: 0L
        return FeedEvent(
            id = eventId,
            userId = userId,
            userName = name,
            message = message,
            timestampMillis = timestampMillis,
        )
    }

    private companion object {
        const val LEADERBOARD_COLLECTION = "leaderboard"
        const val FEED_COLLECTION = "feed"
        const val FIELD_NAME = "name"
        const val FIELD_SCORE = "score"
        const val FIELD_LAST_UPDATED = "lastUpdated"
        const val FIELD_USER_ID = "userId"
        const val FIELD_MESSAGE = "message"
        const val FIELD_TIMESTAMP = "timestamp"
        const val MILESTONE_INTERVAL = 5
    }
}
