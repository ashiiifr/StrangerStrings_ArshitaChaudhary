package com.strangerstrings.habitsync.data.repository

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.strangerstrings.habitsync.data.FriendLeaderboardEntry
import com.strangerstrings.habitsync.data.FriendProfileDetails
import com.strangerstrings.habitsync.data.FriendPublicHabit
import com.strangerstrings.habitsync.data.FriendRelationshipState
import com.strangerstrings.habitsync.data.FriendRequest
import com.strangerstrings.habitsync.data.FriendUser
import com.strangerstrings.habitsync.data.HabitVisibility
import com.strangerstrings.habitsync.data.LeaderboardFilter
import com.strangerstrings.habitsync.data.SearchFriendResult
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

    fun observeFriendsLeaderboard(filter: LeaderboardFilter): Flow<List<FriendLeaderboardEntry>> = callbackFlow {
        val userId = authRepository.getCurrentUserId()
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var leaderboardListeners = emptyList<com.google.firebase.firestore.ListenerRegistration>()
        val leaderboardDocs = linkedMapOf<String, Map<String, Any?>>()
        var trackedIds = emptyList<String>()

        fun emitEntries() {
            val scoreField = scoreFieldFor(filter)
            val entries = trackedIds.mapNotNull { trackedUserId ->
                val data = leaderboardDocs[trackedUserId] ?: return@mapNotNull null
                FriendLeaderboardEntry(
                    userId = trackedUserId,
                    username = (data[FIELD_USERNAME] as? String).orEmpty(),
                    displayName = ((data[FIELD_NAME] as? String).orEmpty())
                        .ifBlank { (data[FIELD_USERNAME] as? String).orEmpty() },
                    profileImageUrl = data[FIELD_PROFILE_IMAGE_URL] as? String,
                    score = (data[scoreField] as? Number)?.toInt() ?: 0,
                    activeHabitsCount = (data[FIELD_ACTIVE_HABITS_COUNT] as? Number)?.toInt() ?: 0,
                    rank = 0,
                    rankDelta = 0,
                )
            }
                .sortedWith(
                    compareByDescending<FriendLeaderboardEntry> { it.score }
                        .thenBy { it.username },
                )
                .mapIndexed { index, entry ->
                    entry.copy(rank = index + 1)
                }
            trySend(entries)
        }

        fun restartLeaderboardListeners(friendIds: List<String>) {
            leaderboardListeners.forEach { it.remove() }
            leaderboardListeners = emptyList()
            leaderboardDocs.clear()
            trackedIds = (listOf(userId) + friendIds).distinct()

            if (trackedIds.isEmpty()) {
                trySend(emptyList())
                return
            }

            leaderboardListeners = trackedIds.chunked(10).map { chunk ->
                firestore.collection(COLLECTION_LEADERBOARD)
                    .whereIn(FieldPath.documentId(), chunk)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }

                        chunk.forEach { trackedUserId ->
                            leaderboardDocs.remove(trackedUserId)
                        }
                        snapshot?.documents.orEmpty().forEach { document ->
                            leaderboardDocs[document.id] = document.data.orEmpty()
                        }
                        emitEntries()
                    }
            }
        }

        val listener = usersCollection()
            .document(userId)
            .collection(COLLECTION_FRIENDS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                restartLeaderboardListeners(snapshot?.documents.orEmpty().map { it.id })
            }

        awaitClose {
            listener.remove()
            leaderboardListeners.forEach { it.remove() }
        }
    }

    suspend fun searchUsersByUsername(query: String): List<SearchFriendResult> {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId.isBlank()) return emptyList()

        val normalized = query.trim().lowercase()
        if (normalized.length < 2) return emptyList()

        val currentFriendsIds = usersCollection()
            .document(currentUserId)
            .collection(COLLECTION_FRIENDS)
            .get()
            .await()
            .documents
            .map { it.id }
            .toSet()

        val incomingRequestIds = usersCollection()
            .document(currentUserId)
            .collection(COLLECTION_FRIEND_REQUESTS)
            .whereEqualTo(FIELD_STATUS, STATUS_PENDING)
            .get()
            .await()
            .documents
            .map { it.id }
            .toSet()

        val usernameDocs = firestore.collection(COLLECTION_USERNAMES)
            .whereGreaterThanOrEqualTo("__name__", normalized)
            .whereLessThanOrEqualTo("__name__", normalized + "\uf8ff")
            .limit(20)
            .get()
            .await()

        val results = mutableListOf<SearchFriendResult>()
        usernameDocs.documents.forEach { usernameDoc ->
            val userId = usernameDoc.getString(FIELD_USER_ID).orEmpty()
            if (userId.isBlank() || userId == currentUserId) return@forEach
            val userDoc = usersCollection().document(userId).get().await()
            if (!userDoc.exists()) return@forEach
            val outgoingPending = usersCollection()
                .document(userId)
                .collection(COLLECTION_FRIEND_REQUESTS)
                .document(currentUserId)
                .get()
                .await()

            val relationshipState = when {
                currentFriendsIds.contains(userId) -> FriendRelationshipState.FRIEND
                incomingRequestIds.contains(userId) -> FriendRelationshipState.REQUEST_RECEIVED
                outgoingPending.exists() && outgoingPending.getString(FIELD_STATUS) == STATUS_PENDING ->
                    FriendRelationshipState.REQUEST_SENT
                else -> FriendRelationshipState.NONE
            }

            results += SearchFriendResult(
                userId = userId,
                username = userDoc.getString(FIELD_USERNAME).orEmpty(),
                displayName = userDoc.getString(FIELD_DISPLAY_NAME).orEmpty().ifBlank { userDoc.getString(FIELD_USERNAME).orEmpty() },
                profileImageUrl = userDoc.getString(FIELD_PROFILE_IMAGE_URL),
                relationshipState = relationshipState,
            )
        }
        return results
    }

    suspend fun fetchSuggestedUsers(limit: Int = 18): List<SearchFriendResult> {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId.isBlank()) return emptyList()

        val currentFriendsIds = usersCollection()
            .document(currentUserId)
            .collection(COLLECTION_FRIENDS)
            .get()
            .await()
            .documents
            .map { it.id }
            .toSet()

        val incomingRequestIds = usersCollection()
            .document(currentUserId)
            .collection(COLLECTION_FRIEND_REQUESTS)
            .whereEqualTo(FIELD_STATUS, STATUS_PENDING)
            .get()
            .await()
            .documents
            .map { it.id }
            .toSet()

        val results = mutableListOf<SearchFriendResult>()
        val docs = usersCollection()
            .limit((limit * 3).toLong())
            .get()
            .await()
            .documents

        for (userDoc in docs) {
            val userId = userDoc.id
            if (userId == currentUserId) continue

            val username = userDoc.getString(FIELD_USERNAME).orEmpty()
            if (username.isBlank()) continue

            val outgoingPending = usersCollection()
                .document(userId)
                .collection(COLLECTION_FRIEND_REQUESTS)
                .document(currentUserId)
                .get()
                .await()

            val relationshipState = when {
                currentFriendsIds.contains(userId) -> FriendRelationshipState.FRIEND
                incomingRequestIds.contains(userId) -> FriendRelationshipState.REQUEST_RECEIVED
                outgoingPending.exists() && outgoingPending.getString(FIELD_STATUS) == STATUS_PENDING ->
                    FriendRelationshipState.REQUEST_SENT
                else -> FriendRelationshipState.NONE
            }

            results += SearchFriendResult(
                userId = userId,
                username = username,
                displayName = userDoc.getString(FIELD_DISPLAY_NAME).orEmpty().ifBlank { username },
                profileImageUrl = userDoc.getString(FIELD_PROFILE_IMAGE_URL),
                relationshipState = relationshipState,
            )
        }

        return results
            .sortedBy { it.relationshipState != FriendRelationshipState.NONE }
            .take(limit)
    }

    suspend fun sendFriendRequest(toUserId: String) {
        val fromUserId = authRepository.getCurrentUserId()
        if (fromUserId.isBlank() || toUserId.isBlank() || fromUserId == toUserId) return

        val alreadyFriend = usersCollection().document(fromUserId)
            .collection(COLLECTION_FRIENDS)
            .document(toUserId)
            .get()
            .await()
            .exists()
        if (alreadyFriend) error("You're already friends.")

        val incomingRequest = usersCollection().document(fromUserId)
            .collection(COLLECTION_FRIEND_REQUESTS)
            .document(toUserId)
            .get()
            .await()
        if (incomingRequest.exists() && incomingRequest.getString(FIELD_STATUS) == STATUS_PENDING) {
            error("This user has already sent you a request. Check your inbox.")
        }

        val outgoingRequest = usersCollection().document(toUserId)
            .collection(COLLECTION_FRIEND_REQUESTS)
            .document(fromUserId)
            .get()
            .await()
        if (outgoingRequest.exists() && outgoingRequest.getString(FIELD_STATUS) == STATUS_PENDING) {
            error("Friend request already sent.")
        }

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

        val notificationRef = usersCollection().document(toUserId)
            .collection(COLLECTION_NOTIFICATIONS)
            .document("friend_request_$fromUserId")
        notificationRef.set(
            mapOf(
                FIELD_TITLE to "Friend request",
                FIELD_MESSAGE to "$fromDisplayName sent you a friend request",
                FIELD_TYPE to TYPE_FRIEND_REQUEST,
                FIELD_IS_READ to false,
                FIELD_TIMESTAMP to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
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

    suspend fun removeFriend(friendUserId: String) {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId.isBlank() || friendUserId.isBlank()) return

        val batch = firestore.batch()
        batch.delete(
            usersCollection().document(currentUserId)
                .collection(COLLECTION_FRIENDS)
                .document(friendUserId),
        )
        batch.delete(
            usersCollection().document(friendUserId)
                .collection(COLLECTION_FRIENDS)
                .document(currentUserId),
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
        const val COLLECTION_LEADERBOARD = "leaderboard"
        const val COLLECTION_HABITS = "habits"
        const val COLLECTION_BADGES = "badges"
        const val COLLECTION_NOTIFICATIONS = "notifications"
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
        const val FIELD_NAME = "name"
        const val FIELD_ACTIVE_HABITS_COUNT = "activeHabitsCount"
        const val FIELD_SCORE = "score"
        const val FIELD_READING_SCORE = "readingScore"
        const val FIELD_WRITING_SCORE = "writingScore"
        const val FIELD_STUDYING_SCORE = "studyingScore"
        const val FIELD_DRINK_WATER_SCORE = "drinkWaterScore"
        const val FIELD_RUNNING_SCORE = "runningScore"
        const val FIELD_WALKING_SCORE = "walkingScore"
        const val FIELD_SLEEP_EARLY_SCORE = "sleepEarlyScore"
        const val FIELD_MEDITATION_SCORE = "meditationScore"
        const val FIELD_WORKOUT_SCORE = "workoutScore"
        const val FIELD_MESSAGE = "message"
        const val FIELD_TYPE = "type"
        const val FIELD_IS_READ = "isRead"
        const val FIELD_TIMESTAMP = "timestamp"
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_DECLINED = "declined"
        const val TYPE_FRIEND_REQUEST = "friend_request"
    }

    private fun scoreFieldFor(filter: LeaderboardFilter): String {
        return when (filter) {
            LeaderboardFilter.OVERALL -> FIELD_SCORE
            LeaderboardFilter.READING -> FIELD_READING_SCORE
            LeaderboardFilter.WRITING -> FIELD_WRITING_SCORE
            LeaderboardFilter.STUDYING -> FIELD_STUDYING_SCORE
            LeaderboardFilter.DRINK_WATER -> FIELD_DRINK_WATER_SCORE
            LeaderboardFilter.RUNNING -> FIELD_RUNNING_SCORE
            LeaderboardFilter.WALKING -> FIELD_WALKING_SCORE
            LeaderboardFilter.SLEEP_EARLY -> FIELD_SLEEP_EARLY_SCORE
            LeaderboardFilter.MEDITATION -> FIELD_MEDITATION_SCORE
            LeaderboardFilter.WORKOUT -> FIELD_WORKOUT_SCORE
        }
    }
}
