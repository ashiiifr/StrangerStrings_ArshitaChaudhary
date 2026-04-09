package com.strangerstrings.habitsync.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.strangerstrings.habitsync.data.Badge
import com.strangerstrings.habitsync.data.EditableProfile
import com.strangerstrings.habitsync.data.Habit
import com.strangerstrings.habitsync.data.HabitCategory
import com.strangerstrings.habitsync.data.HabitVisibility
import com.strangerstrings.habitsync.data.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class ProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    suspend fun updateMyProfile(profile: EditableProfile) {
        val userId = authRepository.getCurrentUserId()
        if (userId.isBlank()) error("Sign in to update your profile.")

        val normalizedUsername = profile.username.trim().lowercase()
        if (!USERNAME_REGEX.matches(normalizedUsername)) {
            error("Username must be 3-12 lowercase letters or numbers.")
        }

        val firstName = profile.firstName.trim()
        val lastName = profile.lastName.trim()
        if (firstName.isBlank() || lastName.isBlank()) {
            error("First and last name are required.")
        }

        val heightCm = profile.heightCm.toFloatOrNull() ?: error("Enter a valid height.")
        val weightKg = profile.weightKg.toFloatOrNull() ?: error("Enter a valid weight.")
        if (heightCm !in 50f..250f) error("Height should be between 50 and 250 cm.")
        if (weightKg !in 20f..400f) error("Weight should be between 20 and 400 kg.")

        val fullName = "$firstName $lastName".trim()
        val userRef = usersCollection().document(userId)

        firestore.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val previousUsername = userSnapshot.getString(FIELD_USERNAME).orEmpty()

            if (previousUsername != normalizedUsername) {
                val newUsernameRef = firestore.collection(COLLECTION_USERNAMES).document(normalizedUsername)
                val newUsernameSnapshot = transaction.get(newUsernameRef)
                if (newUsernameSnapshot.exists()) {
                    throw IllegalStateException("Username already taken.")
                }

                if (previousUsername.isNotBlank()) {
                    val previousUsernameRef = firestore.collection(COLLECTION_USERNAMES).document(previousUsername)
                    transaction.delete(previousUsernameRef)
                }

                transaction.set(
                    newUsernameRef,
                    mapOf(
                        FIELD_USER_ID to userId,
                        FIELD_CREATED_AT to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    ),
                    com.google.firebase.firestore.SetOptions.merge(),
                )
            }

            transaction.set(
                userRef,
                mapOf(
                    FIELD_FIRST_NAME to firstName,
                    FIELD_LAST_NAME to lastName,
                    FIELD_USERNAME to normalizedUsername,
                    FIELD_DISPLAY_NAME to fullName,
                    FIELD_BIO to profile.bio.trim(),
                    FIELD_GENDER to profile.gender.trim(),
                    FIELD_HEIGHT_CM to heightCm,
                    FIELD_WEIGHT_KG to weightKg,
                    FIELD_UPDATED_AT to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                ),
                com.google.firebase.firestore.SetOptions.merge(),
            )
            true
        }.await()

        firebaseAuth.currentUser?.updateProfile(
            UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build(),
        )?.await()
    }

    suspend fun syncFreezeTokensForCurrentMonth() {
        val userId = authRepository.getCurrentUserId()
        if (userId.isBlank()) return

        firestore.runTransaction { transaction ->
            val userRef = usersCollection().document(userId)
            val snapshot = transaction.get(userRef)
            val nowMonthKey = currentMonthKey()
            val monthKey = snapshot.getString(FIELD_FREEZE_TOKENS_MONTH_KEY).orEmpty()
            val hasTokensField = snapshot.contains(FIELD_FREEZE_TOKENS_THIS_MONTH)
            if (monthKey != nowMonthKey || !hasTokensField) {
                transaction.set(
                    userRef,
                    mapOf(
                        FIELD_FREEZE_TOKENS_THIS_MONTH to DEFAULT_FREEZE_TOKENS,
                        FIELD_FREEZE_TOKENS_MONTH_KEY to nowMonthKey,
                    ),
                    com.google.firebase.firestore.SetOptions.merge(),
                )
            }
            true
        }.await()
    }

    suspend fun consumeFreezeTokenIfAvailable(): Boolean {
        val userId = authRepository.getCurrentUserId()
        if (userId.isBlank()) return false

        return firestore.runTransaction { transaction ->
            val userRef = usersCollection().document(userId)
            val snapshot = transaction.get(userRef)
            consumeFreezeTokenInTransaction(transaction, userRef, snapshot)
        }.await()
    }

    fun observeMyProfile(): Flow<UserProfile?> = callbackFlow {
        val userId = authRepository.getCurrentUserId()
        if (userId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = usersCollection()
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toUserProfile(userId))
            }

        awaitClose { listener.remove() }
    }

    fun observeMyBadges(): Flow<List<Badge>> = callbackFlow {
        val userId = authRepository.getCurrentUserId()
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = usersCollection()
            .document(userId)
            .collection(COLLECTION_BADGES)
            .orderBy(FIELD_EARNED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val badges = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val title = doc.getString(FIELD_TITLE).orEmpty()
                    if (title.isBlank()) return@mapNotNull null
                    Badge(
                        id = doc.id,
                        title = title,
                        description = doc.getString(FIELD_DESCRIPTION).orEmpty(),
                        earnedAtMillis = doc.getLong(FIELD_EARNED_AT)
                            ?: doc.getTimestamp(FIELD_EARNED_AT)?.toDate()?.time
                            ?: 0L,
                    )
                }
                trySend(badges)
            }

        awaitClose { listener.remove() }
    }

    fun observeMyPublicHabits(): Flow<List<Habit>> = callbackFlow {
        val userId = authRepository.getCurrentUserId()
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = usersCollection()
            .document(userId)
            .collection(COLLECTION_HABITS)
            .whereEqualTo(FIELD_VISIBILITY, HabitVisibility.PUBLIC.name.lowercase())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val habits = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    doc.toHabitOrNull()
                }
                trySend(habits)
            }

        awaitClose { listener.remove() }
    }

    private fun usersCollection() = firestore.collection(COLLECTION_USERS)

    private fun consumeFreezeTokenInTransaction(
        transaction: Transaction,
        userRef: DocumentReference,
        snapshot: DocumentSnapshot,
    ): Boolean {
        val nowMonthKey = currentMonthKey()
        val monthKey = snapshot.getString(FIELD_FREEZE_TOKENS_MONTH_KEY).orEmpty()
        val baseTokens = if (monthKey == nowMonthKey) {
            snapshot.getLong(FIELD_FREEZE_TOKENS_THIS_MONTH) ?: DEFAULT_FREEZE_TOKENS
        } else {
            DEFAULT_FREEZE_TOKENS
        }

        if (baseTokens <= 0L) {
            transaction.set(
                userRef,
                mapOf(
                    FIELD_FREEZE_TOKENS_THIS_MONTH to 0L,
                    FIELD_FREEZE_TOKENS_MONTH_KEY to nowMonthKey,
                ),
                com.google.firebase.firestore.SetOptions.merge(),
            )
            return false
        }

        transaction.set(
            userRef,
            mapOf(
                FIELD_FREEZE_TOKENS_THIS_MONTH to (baseTokens - 1L),
                FIELD_FREEZE_TOKENS_MONTH_KEY to nowMonthKey,
            ),
            com.google.firebase.firestore.SetOptions.merge(),
        )
        return true
    }

    private fun DocumentSnapshot.toUserProfile(userId: String): UserProfile? {
        if (!exists()) return null
        val firstName = getString(FIELD_FIRST_NAME).orEmpty()
        val lastName = getString(FIELD_LAST_NAME).orEmpty()
        val username = getString(FIELD_USERNAME).orEmpty()
        val fallbackDisplay = listOf(firstName, lastName).joinToString(" ").trim()
        val displayName = getString(FIELD_DISPLAY_NAME).orEmpty().ifBlank { fallbackDisplay.ifBlank { username } }
        return UserProfile(
            userId = userId,
            firstName = firstName,
            lastName = lastName,
            username = username,
            displayName = displayName,
            bio = getString(FIELD_BIO).orEmpty(),
            email = getString(FIELD_EMAIL).orEmpty(),
            gender = getString(FIELD_GENDER).orEmpty(),
            profileImageUrl = getString(FIELD_PROFILE_IMAGE_URL),
            weightKg = getDouble(FIELD_WEIGHT_KG)?.toFloat() ?: 0f,
            heightCm = getDouble(FIELD_HEIGHT_CM)?.toFloat() ?: 0f,
            totalHabitsCreated = (getLong(FIELD_TOTAL_HABITS_CREATED) ?: 0L).toInt(),
            longestStreakEver = (getLong(FIELD_LONGEST_STREAK_EVER) ?: 0L).toInt(),
            challengesCompleted = (getLong(FIELD_CHALLENGES_COMPLETED) ?: 0L).toInt(),
            freezeTokensThisMonth = (getLong(FIELD_FREEZE_TOKENS_THIS_MONTH) ?: DEFAULT_FREEZE_TOKENS).toInt(),
        )
    }

    private fun DocumentSnapshot.toHabitOrNull(): Habit? {
        val title = getString(FIELD_TITLE).orEmpty()
        if (title.isBlank()) return null

        val category = getString(FIELD_CATEGORY)
            ?.uppercase()
            ?.let { raw -> runCatching { HabitCategory.valueOf(raw) }.getOrDefault(HabitCategory.CUSTOM) }
            ?: HabitCategory.CUSTOM
        val visibility = getString(FIELD_VISIBILITY)
            ?.uppercase()
            ?.let { raw -> runCatching { HabitVisibility.valueOf(raw) }.getOrDefault(HabitVisibility.PRIVATE) }
            ?: HabitVisibility.PRIVATE
        val completionDates = (get(FIELD_COMPLETION_DATES) as? List<*>).orEmpty()
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

        return Habit(
            id = id,
            title = title,
            streak = (getLong(FIELD_STREAK) ?: 0L).toInt(),
            isCompletedToday = getBoolean(FIELD_IS_COMPLETED_TODAY) ?: false,
            lastCompletedDate = getLong(FIELD_LAST_COMPLETED_DATE),
            proofImageUrl = getString(FIELD_PROOF_IMAGE_URL),
            category = category,
            reminderTime = getString(FIELD_REMINDER_TIME).orEmpty().ifBlank { "20:00" },
            visibility = visibility,
            completionDates = completionDates,
        )
    }

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_HABITS = "habits"
        const val COLLECTION_BADGES = "badges"
        const val COLLECTION_USERNAMES = "usernames"
        const val FIELD_FIRST_NAME = "firstName"
        const val FIELD_LAST_NAME = "lastName"
        const val FIELD_USERNAME = "username"
        const val FIELD_DISPLAY_NAME = "displayName"
        const val FIELD_BIO = "bio"
        const val FIELD_EMAIL = "email"
        const val FIELD_GENDER = "gender"
        const val FIELD_PROFILE_IMAGE_URL = "profileImageUrl"
        const val FIELD_WEIGHT_KG = "weightKg"
        const val FIELD_HEIGHT_CM = "heightCm"
        const val FIELD_TOTAL_HABITS_CREATED = "totalHabitsCreated"
        const val FIELD_LONGEST_STREAK_EVER = "longestStreakEver"
        const val FIELD_CHALLENGES_COMPLETED = "challengesCompleted"
        const val FIELD_FREEZE_TOKENS_THIS_MONTH = "freezeTokensThisMonth"
        const val FIELD_FREEZE_TOKENS_MONTH_KEY = "freezeTokensMonthKey"
        const val FIELD_TITLE = "title"
        const val FIELD_STREAK = "streak"
        const val FIELD_IS_COMPLETED_TODAY = "isCompletedToday"
        const val FIELD_LAST_COMPLETED_DATE = "lastCompletedDate"
        const val FIELD_PROOF_IMAGE_URL = "proofImageUrl"
        const val FIELD_CATEGORY = "category"
        const val FIELD_REMINDER_TIME = "reminderTime"
        const val FIELD_VISIBILITY = "visibility"
        const val FIELD_COMPLETION_DATES = "completionDates"
        const val FIELD_EARNED_AT = "earnedAtMillis"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_USER_ID = "userId"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val DEFAULT_FREEZE_TOKENS = 2L
        val USERNAME_REGEX = Regex("^[a-z0-9]{3,12}$")
    }

    private fun currentMonthKey(): String {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH) + 1
        return "%04d-%02d".format(year, month)
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val user = firebaseAuth.currentUser ?: error("Not signed in.")
        val email = user.email ?: error("No email associated with this account.")
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential).await()
        user.updatePassword(newPassword).await()
    }
}
