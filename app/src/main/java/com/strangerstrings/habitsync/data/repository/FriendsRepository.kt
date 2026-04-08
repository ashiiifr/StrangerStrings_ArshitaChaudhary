package com.strangerstrings.habitsync.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.strangerstrings.habitsync.data.FriendLeaderboardEntry
import com.strangerstrings.habitsync.data.FriendProfileDetails
import com.strangerstrings.habitsync.data.FriendPublicHabit
import com.strangerstrings.habitsync.data.FriendRequest
import com.strangerstrings.habitsync.data.FriendUser
import com.strangerstrings.habitsync.data.HabitVisibility
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FriendsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val authRepository: AuthRepository = AuthRepository(),
) {
    fun observeFriends(): Flow<List<FriendUser>> = callbackFlow {
        val userId = authRepository.getCurrentUserId()
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = usersCollection()
            .document(userId)
            .collection(COLLECTION_FRIENDS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val friends = snapshot?.documents.orEmpty().map { doc ->
                    FriendUser(
                        userId = doc.id,
                        username = doc.getString(FIELD_USERNAME).orEmpty(),
                        displayName = doc.getString(FIELD_DISPLAY_NAME).orEmpty().ifBlank { doc.getString(FIELD_USERNAME).orEmpty() },
                        profileImageUrl = doc.getString(FIELD_PROFILE_IMAGE_URL),
                    )
                }
                trySend(friends)
            }

        awaitClose { listener.remove() }
    }

    fun observeIncomingFriendRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val userId = authRepository.getCurrentUserId()
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = usersCollection()
            .document(userId)
            .collection(COLLECTION_FRIEND_REQUESTS)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents.orEmpty().map { doc ->
                    FriendRequest(
                        id = doc.id,
                        fromUserId = doc.getString(FIELD_FROM_USER_ID).orEmpty(),
                        fromUsername = doc.getString(FIELD_FROM_USERNAME).orEmpty(),
                        fromDisplayName = doc.getString(FIELD_FROM_DISPLAY_NAME).orEmpty(),
                        status = doc.getString(FIELD_STATUS).orEmpty(),
                        createdAtMillis = doc.getTimestamp(FIELD_CREATED_AT)?.toDate()?.time ?: 0L,
                    )
                }
                trySend(requests.filter { it.status == STATUS_PENDING })
            }

        awaitClose { listener.remove() }
    }

    fun observeWeeklyLeaderboard(): Flow<List<FriendLeaderboardEntry>> = callbackFlow {
        val userId = authRepository.getCurrentUserId()
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = usersCollection()
            .document(userId)
            .collection(COLLECTION_FRIEND_LEADERBOARD)
            .orderBy(FIELD_WEEKLY_SCORE, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val entries = snapshot?.documents.orEmpty().mapIndexed { index, doc ->
                    FriendLeaderboardEntry(
                        userId = doc.id,
                        username = doc.getString(FIELD_USERNAME).orEmpty(),
                        profileImageUrl = doc.getString(FIELD_PROFILE_IMAGE_URL),
                        weeklyScore = (doc.getLong(FIELD_WEEKLY_SCORE) ?: 0L).toInt(),
                        rank = index + 1,
                        rankDelta = (doc.getLong(FIELD_RANK_DELTA) ?: 0L).toInt(),
                    )
                }
                trySend(entries)
            }

        awaitClose { listener.remove() }
    }

    suspend fun searchUsersByUsername(query: String): List<FriendUser> {
        val normalized = query.trim().lowercase()
        if (normalized.length < 2) return emptyList()

        val usernameDocs = firestore.collection(COLLECTION_USERNAMES)
            .whereGreaterThanOrEqualTo("__name__", normalized)
            .whereLessThanOrEqualTo("__name__", normalized + "\uf8ff")
            .limit(20)
            .get()
            .await()

        val results = mutableListOf<FriendUser>()
        usernameDocs.documents.forEach { usernameDoc ->
            val userId = usernameDoc.getString(FIELD_USER_ID).orEmpty()
            if (userId.isBlank()) return@forEach
            val userDoc = usersCollection().document(userId).get().await()
            if (!userDoc.exists()) return@forEach
            results += FriendUser(
                userId = userId,
                username = userDoc.getString(FIELD_USERNAME).orEmpty(),
                displayName = userDoc.getString(FIELD_DISPLAY_NAME).orEmpty().ifBlank { userDoc.getString(FIELD_USERNAME).orEmpty() },
                profileImageUrl = userDoc.getString(FIELD_PROFILE_IMAGE_URL),
            )
        }
        return results
    }

    suspend fun sendFriendRequest(toUserId: String) {
        val fromUserId = authRepository.getCurrentUserId()
        if (fromUserId.isBlank() || toUserId.isBlank() || fromUserId == toUserId) return

        val fromUserDoc = usersCollection().document(fromUserId).get().await()
        val fromUsername = fromUserDoc.getString(FIELD_USERNAME).orEmpty()
        val fromDisplayName = fromUserDoc.getString(FIELD_DISPLAY_NAME).orEmpty().ifBlank { fromUsername }

        usersCollection().document(toUserId)
            .collection(COLLECTION_FRIEND_REQUESTS)
            .document(fromUserId)
            .set(
                mapOf(
                    FIELD_FROM_USER_ID to fromUserId,
                    FIELD_FROM_USERNAME to fromUsername,
                    FIELD_FROM_DISPLAY_NAME to fromDisplayName,
                    FIELD_STATUS to STATUS_PENDING,
                    FIELD_CREATED_AT to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                ),
            )
            .await()
    }

    suspend fun respondToFriendRequest(fromUserId: String, accept: Boolean) {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId.isBlank() || fromUserId.isBlank()) return

        val requestRef = usersCollection().document(currentUserId)
            .collection(COLLECTION_FRIEND_REQUESTS)
            .document(fromUserId)

        if (!accept) {
            requestRef.update(FIELD_STATUS, STATUS_DECLINED).await()
            return
        }

        val currentUserDoc = usersCollection().document(currentUserId).get().await()
        val fromUserDoc = usersCollection().document(fromUserId).get().await()

        val batch = firestore.batch()
        batch.update(requestRef, FIELD_STATUS, STATUS_ACCEPTED)
        batch.set(
            usersCollection().document(currentUserId).collection(COLLECTION_FRIENDS).document(fromUserId),
            mapOf(
                FIELD_USERNAME to fromUserDoc.getString(FIELD_USERNAME).orEmpty(),
                FIELD_DISPLAY_NAME to fromUserDoc.getString(FIELD_DISPLAY_NAME).orEmpty().ifBlank { fromUserDoc.getString(FIELD_USERNAME).orEmpty() },
                FIELD_PROFILE_IMAGE_URL to fromUserDoc.getString(FIELD_PROFILE_IMAGE_URL),
            ),
        )
        batch.set(
            usersCollection().document(fromUserId).collection(COLLECTION_FRIENDS).document(currentUserId),
            mapOf(
                FIELD_USERNAME to currentUserDoc.getString(FIELD_USERNAME).orEmpty(),
                FIELD_DISPLAY_NAME to currentUserDoc.getString(FIELD_DISPLAY_NAME).orEmpty().ifBlank { currentUserDoc.getString(FIELD_USERNAME).orEmpty() },
                FIELD_PROFILE_IMAGE_URL to currentUserDoc.getString(FIELD_PROFILE_IMAGE_URL),
            ),
        )
        batch.commit().await()
    }

    suspend fun fetchFriendProfile(userId: String): FriendProfileDetails? {
        if (userId.isBlank()) return null
        val userDoc = usersCollection().document(userId).get().await()
        if (!userDoc.exists()) return null

        val username = userDoc.getString(FIELD_USERNAME).orEmpty()
        val displayName = userDoc.getString(FIELD_DISPLAY_NAME).orEmpty().ifBlank { username }
        val badges = usersCollection().document(userId)
            .collection(COLLECTION_BADGES)
            .orderBy(FIELD_EARNED_AT, Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .await()
            .documents
            .mapNotNull { it.getString(FIELD_TITLE) }

        val publicHabits = usersCollection().document(userId)
            .collection(COLLECTION_HABITS)
            .whereEqualTo(FIELD_VISIBILITY, HabitVisibility.PUBLIC.name.lowercase())
            .limit(20)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val title = doc.getString(FIELD_TITLE).orEmpty()
                if (title.isBlank()) return@mapNotNull null
                FriendPublicHabit(
                    id = doc.id,
                    title = title,
                    streak = (doc.getLong(FIELD_STREAK) ?: 0L).toInt(),
                )
            }
            .sortedByDescending { it.streak }

        return FriendProfileDetails(
            userId = userId,
            username = username,
            displayName = displayName,
            bio = userDoc.getString(FIELD_BIO).orEmpty(),
            profileImageUrl = userDoc.getString(FIELD_PROFILE_IMAGE_URL),
            badges = badges,
            publicHabits = publicHabits,
        )
    }

    private fun usersCollection() = firestore.collection(COLLECTION_USERS)

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_USERNAMES = "usernames"
        const val COLLECTION_FRIENDS = "friends"
        const val COLLECTION_FRIEND_REQUESTS = "friendRequests"
        const val COLLECTION_FRIEND_LEADERBOARD = "friendLeaderboard"
        const val COLLECTION_HABITS = "habits"
        const val COLLECTION_BADGES = "badges"
        const val FIELD_USER_ID = "userId"
        const val FIELD_FROM_USER_ID = "fromUserId"
        const val FIELD_FROM_USERNAME = "fromUsername"
        const val FIELD_FROM_DISPLAY_NAME = "fromDisplayName"
        const val FIELD_USERNAME = "username"
        const val FIELD_DISPLAY_NAME = "displayName"
        const val FIELD_PROFILE_IMAGE_URL = "profileImageUrl"
        const val FIELD_BIO = "bio"
        const val FIELD_STATUS = "status"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_EARNED_AT = "earnedAtMillis"
        const val FIELD_TITLE = "title"
        const val FIELD_STREAK = "streak"
        const val FIELD_VISIBILITY = "visibility"
        const val FIELD_WEEKLY_SCORE = "weeklyScore"
        const val FIELD_RANK_DELTA = "rankDelta"
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_DECLINED = "declined"
    }
}
