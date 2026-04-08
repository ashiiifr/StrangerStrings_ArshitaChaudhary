package com.strangerstrings.habitsync.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.strangerstrings.habitsync.data.Habit
import com.strangerstrings.habitsync.data.HabitCategory
import com.strangerstrings.habitsync.data.HabitVisibility
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseHabitRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val proofStorageRepository: ProofStorageRepository = ProofStorageRepository(),
    private val socialRepository: SocialRepository = SocialRepository(),
) {
    fun getHabits(userId: String): Flow<List<Habit>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listenerRegistration = habitsCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val habits = snapshot?.documents.orEmpty()
                    .mapNotNull { document -> document.toHabitOrNull() }
                trySend(habits)
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun addHabit(habit: Habit) {
        val userId = authRepository.getCurrentUserId()
        if (userId.isBlank()) error("Cannot add a habit for an unauthenticated user.")
        habitsCollection(userId)
            .document(habit.id)
            .set(habit.toMap())
            .await()
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .set(
                mapOf(FIELD_TOTAL_HABITS_CREATED to com.google.firebase.firestore.FieldValue.increment(1L)),
                SetOptions.merge(),
            )
            .await()
    }

    suspend fun updateHabit(habit: Habit, proofImageBytes: ByteArray? = null) {
        val userId = authRepository.getCurrentUserId()
        if (userId.isBlank()) error("Cannot update a habit for an unauthenticated user.")
        val habitRef = habitsCollection(userId).document(habit.id)
        val existingSnapshot = habitRef.get().await()
        val wasCompletedToday = existingSnapshot.wasCompletedToday()
        val existingProofUrl = existingSnapshot.getString(FIELD_PROOF_IMAGE_URL).orEmpty()

        if (proofImageBytes != null && wasCompletedToday && existingProofUrl.isNotBlank()) {
            error("Proof already uploaded for today.")
        }

        var proofImageUrl = habit.proofImageUrl
        if (proofImageBytes != null) {
            proofImageUrl = proofStorageRepository.uploadProofImage(
                habitId = habit.id,
                proofImageBytes = proofImageBytes,
            )
        }

        val habitToSave = habit.copy(proofImageUrl = proofImageUrl)

        habitRef
            .set(habitToSave.toMap())
            .await()

        if (!wasCompletedToday && habitToSave.isCompletedToday) {
            socialRepository.recordHabitCompletion(
                habit = habitToSave,
                hasProof = !habitToSave.proofImageUrl.isNullOrBlank(),
            )
        }
    }

    private fun habitsCollection(userId: String) =
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(HABITS_COLLECTION)

    private companion object {
        const val USERS_COLLECTION = "users"
        const val HABITS_COLLECTION = "habits"
        const val FIELD_TITLE = "title"
        const val FIELD_STREAK = "streak"
        const val FIELD_IS_COMPLETED_TODAY = "isCompletedToday"
        const val FIELD_LAST_COMPLETED_DATE = "lastCompletedDate"
        const val FIELD_PROOF_IMAGE_URL = "proofImageUrl"
        const val FIELD_CATEGORY = "category"
        const val FIELD_REMINDER_TIME = "reminderTime"
        const val FIELD_VISIBILITY = "visibility"
        const val FIELD_COMPLETION_DATES = "completionDates"
        const val FIELD_COMPLETION_TIMESTAMPS = "completionTimestamps"
        const val FIELD_TOTAL_HABITS_CREATED = "totalHabitsCreated"
    }

    private fun Habit.toMap(): Map<String, Any?> {
        return mapOf(
            FIELD_TITLE to title,
            FIELD_STREAK to streak,
            FIELD_IS_COMPLETED_TODAY to isCompletedToday,
            FIELD_LAST_COMPLETED_DATE to lastCompletedDate,
            FIELD_PROOF_IMAGE_URL to proofImageUrl,
            FIELD_CATEGORY to category.name.lowercase(),
            FIELD_REMINDER_TIME to reminderTime,
            FIELD_VISIBILITY to visibility.name.lowercase(),
            FIELD_COMPLETION_DATES to completionDates,
            FIELD_COMPLETION_TIMESTAMPS to completionTimestamps,
        )
    }

    private fun DocumentSnapshot.toHabitOrNull(): Habit? {
        val habitId = id
        val title = getString(FIELD_TITLE).orEmpty().ifBlank { return null }
        val streak = getLong(FIELD_STREAK)?.toInt() ?: 0
        val rawCompletedToday = getBoolean(FIELD_IS_COMPLETED_TODAY) ?: false
        val lastCompletedDate = getLong(FIELD_LAST_COMPLETED_DATE)
        val isCompletedToday = rawCompletedToday && lastCompletedDate == currentEpochDay()
        val storedProofImageUrl = getString(FIELD_PROOF_IMAGE_URL)
        val proofImageUrl = if (isCompletedToday) storedProofImageUrl else null
        val category = getString(FIELD_CATEGORY)
            ?.uppercase()
            ?.let { raw ->
                runCatching { HabitCategory.valueOf(raw) }.getOrDefault(HabitCategory.CUSTOM)
            }
            ?: HabitCategory.CUSTOM
        val visibility = getString(FIELD_VISIBILITY)
            ?.uppercase()
            ?.let { raw ->
                runCatching { HabitVisibility.valueOf(raw) }.getOrDefault(HabitVisibility.PRIVATE)
            }
            ?: HabitVisibility.PRIVATE
        val reminderTime = getString(FIELD_REMINDER_TIME).orEmpty().ifBlank { "20:00" }
        val completionDates = get(FIELD_COMPLETION_DATES) as? List<*>
        val completionEpochDays = completionDates.orEmpty()
            .mapNotNull { item ->
                when (item) {
                    is Long -> item
                    is Int -> item.toLong()
                    is Double -> item.toLong()
                    else -> null
                }
            }
            .distinct()
            .sorted()
        val completionTimestamps = (get(FIELD_COMPLETION_TIMESTAMPS) as? List<*>).orEmpty()
            .mapNotNull { item ->
                when (item) {
                    is Long -> item
                    is Int -> item.toLong()
                    is Double -> item.toLong()
                    else -> null
                }
            }
            .sorted()

        return Habit(
            id = habitId,
            title = title,
            streak = streak.coerceAtLeast(0),
            isCompletedToday = isCompletedToday,
            lastCompletedDate = lastCompletedDate,
            proofImageUrl = proofImageUrl,
            category = category,
            reminderTime = reminderTime,
            visibility = visibility,
            completionDates = completionEpochDays,
            completionTimestamps = completionTimestamps,
        )
    }

    private fun DocumentSnapshot.wasCompletedToday(): Boolean {
        val rawCompletedToday = getBoolean(FIELD_IS_COMPLETED_TODAY) ?: false
        val completedDate = getLong(FIELD_LAST_COMPLETED_DATE)
        return rawCompletedToday && completedDate == currentEpochDay()
    }

    private fun currentEpochDay(): Long {
        val millisecondsPerDay = 24 * 60 * 60 * 1000L
        return System.currentTimeMillis() / millisecondsPerDay
    }

}
