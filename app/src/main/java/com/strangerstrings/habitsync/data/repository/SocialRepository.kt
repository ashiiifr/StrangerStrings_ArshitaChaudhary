package com.strangerstrings.habitsync.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.strangerstrings.habitsync.data.FeedEvent
import com.strangerstrings.habitsync.data.Habit
import com.strangerstrings.habitsync.data.HabitCategory
import com.strangerstrings.habitsync.data.LeaderboardUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

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
        val notificationRef = firestore.collection(USERS_COLLECTION).document(userId)
            .collection(NOTIFICATIONS_COLLECTION)
            .document()

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
        updates += notificationRef to mapOf(
            FIELD_TITLE to "Habit completed",
            FIELD_MESSAGE to "You completed ${habit.title} today.",
            FIELD_TYPE to TYPE_CHALLENGE_ACTIVITY,
            FIELD_IS_READ to false,
            FIELD_TIMESTAMP to FieldValue.serverTimestamp(),
        )
        if (habit.streak in STREAK_MILESTONES) {
            val milestoneRef = firestore.collection(FEED_COLLECTION).document()
            val milestoneNotificationRef = firestore.collection(USERS_COLLECTION).document(userId)
                .collection(NOTIFICATIONS_COLLECTION)
                .document()
            updates += milestoneRef to mapOf(
                FIELD_USER_ID to userId,
                FIELD_NAME to userName,
                FIELD_MESSAGE to "$userName hit a ${habit.streak}-day milestone",
                FIELD_TIMESTAMP to FieldValue.serverTimestamp(),
            )
            updates += milestoneNotificationRef to mapOf(
                FIELD_TITLE to "Streak milestone",
                FIELD_MESSAGE to "You reached a ${habit.streak}-day streak on ${habit.title}.",
                FIELD_TYPE to TYPE_STREAK_MILESTONE,
                FIELD_IS_READ to false,
                FIELD_TIMESTAMP to FieldValue.serverTimestamp(),
            )
        }

        badgeIdForStreak(habit.streak)?.let { badgeId ->
            updates += badgeReference(userId, badgeId) to badgePayloadForStreak(habit.streak)
        }

        categoryBadgeId(habit.category, habit.streak)?.let { badgeId ->
            updates += badgeReference(userId, badgeId) to badgePayloadForCategory(habit.category)
        }

        if (qualifiesForEarlyBird(habit)) {
            updates += badgeReference(userId, BADGE_EARLY_BIRD) to mapOf(
                FIELD_TITLE to "Early Bird",
                FIELD_DESCRIPTION to "Completed a habit before 8 AM for 7 days in a row",
                FIELD_EARNED_AT to System.currentTimeMillis(),
            )
        }

        val batch = firestore.batch()
        updates.forEach { (ref, values) ->
            batch.set(ref, values, com.google.firebase.firestore.SetOptions.merge())
        }
        batch.commit().await()

        updateLongestStreak(userId = userId, streak = habit.streak)
    }

    suspend fun awardSocialButterflyIfEligible(userId: String) {
        if (userId.isBlank()) return
        val userRef = firestore.collection(USERS_COLLECTION).document(userId)
        val snapshot = userRef.get().await()
        val joinedChallengesCount = (snapshot.getLong(FIELD_JOINED_CHALLENGES_COUNT) ?: 0L).toInt()
        if (joinedChallengesCount >= 3) {
            badgeReference(userId, BADGE_SOCIAL_BUTTERFLY)
                .set(
                    mapOf(
                        FIELD_TITLE to "Social Butterfly",
                        FIELD_DESCRIPTION to "Joined 3 challenges",
                        FIELD_EARNED_AT to System.currentTimeMillis(),
                    ),
                    SetOptions.merge(),
                )
                .await()
        }
    }

    suspend fun incrementJoinedChallengesAndAwardIfEligible(userId: String) {
        if (userId.isBlank()) return
        firestore.collection(USERS_COLLECTION).document(userId)
            .set(
                mapOf(FIELD_JOINED_CHALLENGES_COUNT to FieldValue.increment(1L)),
                SetOptions.merge(),
            )
            .await()
        awardSocialButterflyIfEligible(userId)
    }

    private suspend fun updateLongestStreak(userId: String, streak: Int) {
        val userRef = firestore.collection(USERS_COLLECTION).document(userId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentLongest = (snapshot.getLong(FIELD_LONGEST_STREAK_EVER) ?: 0L).toInt()
            if (streak > currentLongest) {
                transaction.set(
                    userRef,
                    mapOf(FIELD_LONGEST_STREAK_EVER to streak),
                    SetOptions.merge(),
                )
            }
        }.await()
    }

    private fun badgeReference(userId: String, badgeId: String) =
        firestore.collection(USERS_COLLECTION).document(userId)
            .collection(COLLECTION_BADGES)
            .document(badgeId)

    private fun badgeIdForStreak(streak: Int): String? {
        return when (streak) {
            7 -> BADGE_ON_A_ROLL
            30 -> BADGE_IRON_WILL
            else -> null
        }
    }

    private fun badgePayloadForStreak(streak: Int): Map<String, Any> {
        return when (streak) {
            7 -> mapOf(
                FIELD_TITLE to "On a Roll",
                FIELD_DESCRIPTION to "Reached a 7-day streak on any habit",
                FIELD_EARNED_AT to System.currentTimeMillis(),
            )
            30 -> mapOf(
                FIELD_TITLE to "Iron Will",
                FIELD_DESCRIPTION to "Reached a 30-day streak on any habit",
                FIELD_EARNED_AT to System.currentTimeMillis(),
            )
            else -> emptyMap()
        }
    }

    private fun categoryBadgeId(category: HabitCategory, streak: Int): String? {
        if (streak < 14) return null
        return when (category) {
            HabitCategory.FITNESS -> BADGE_FITNESS_BEAST
            HabitCategory.READING -> BADGE_BOOKWORM
            HabitCategory.HYDRATION -> BADGE_HYDRATION_HERO
            else -> null
        }
    }

    private fun badgePayloadForCategory(category: HabitCategory): Map<String, Any> {
        val (title, description) = when (category) {
            HabitCategory.FITNESS -> "Fitness Beast" to "Reached a 14-day fitness streak"
            HabitCategory.READING -> "Bookworm" to "Reached a 14-day reading streak"
            HabitCategory.HYDRATION -> "Hydration Hero" to "Reached a 14-day hydration streak"
            else -> "Category Champ" to "Reached a category streak"
        }
        return mapOf(
            FIELD_TITLE to title,
            FIELD_DESCRIPTION to description,
            FIELD_EARNED_AT to System.currentTimeMillis(),
        )
    }

    private fun qualifiesForEarlyBird(habit: Habit): Boolean {
        if (habit.completionTimestamps.isEmpty()) return false
        val earlyEpochDays = habit.completionTimestamps
            .filter { isBeforeEightAm(it) }
            .map { epochDay(it) }
            .distinct()
            .sorted()
        if (earlyEpochDays.size < 7) return false
        val trailingSeven = earlyEpochDays.takeLast(7)
        return trailingSeven.zipWithNext().all { (previous, next) -> next == previous + 1L }
    }

    private fun isBeforeEightAm(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return calendar.get(Calendar.HOUR_OF_DAY) < 8
    }

    private fun epochDay(timestamp: Long): Long {
        return timestamp / MILLIS_PER_DAY
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
        const val USERS_COLLECTION = "users"
        const val COLLECTION_BADGES = "badges"
        const val NOTIFICATIONS_COLLECTION = "notifications"
        const val FIELD_NAME = "name"
        const val FIELD_SCORE = "score"
        const val FIELD_LAST_UPDATED = "lastUpdated"
        const val FIELD_USER_ID = "userId"
        const val FIELD_TITLE = "title"
        const val FIELD_MESSAGE = "message"
        const val FIELD_TYPE = "type"
        const val FIELD_IS_READ = "isRead"
        const val FIELD_TIMESTAMP = "timestamp"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_EARNED_AT = "earnedAtMillis"
        const val FIELD_LONGEST_STREAK_EVER = "longestStreakEver"
        const val FIELD_JOINED_CHALLENGES_COUNT = "joinedChallengesCount"
        const val TYPE_CHALLENGE_ACTIVITY = "challenge_activity"
        const val TYPE_STREAK_MILESTONE = "streak_milestone"
        const val BADGE_ON_A_ROLL = "on_a_roll"
        const val BADGE_IRON_WILL = "iron_will"
        const val BADGE_SOCIAL_BUTTERFLY = "social_butterfly"
        const val BADGE_EARLY_BIRD = "early_bird"
        const val BADGE_FITNESS_BEAST = "fitness_beast"
        const val BADGE_BOOKWORM = "bookworm"
        const val BADGE_HYDRATION_HERO = "hydration_hero"
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
        val STREAK_MILESTONES = setOf(7, 14, 30, 60, 100)
    }
}
