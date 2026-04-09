package com.strangerstrings.habitsync.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.strangerstrings.habitsync.ui.inbox.InboxItem
import com.strangerstrings.habitsync.ui.inbox.InboxType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await

class InboxRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val friendsRepository: FriendsRepository = FriendsRepository(),
    private val challengesRepository: ChallengesRepository = ChallengesRepository(),
) {
    fun observeInboxItems(): Flow<List<InboxItem>> {
        return combine(
            friendsRepository.observeIncomingFriendRequests(),
            challengesRepository.observeChallengeInvites(),
            observeNotifications(),
        ) { requests, invites, notifications ->
            val requestItems = requests.map { request ->
                InboxItem(
                    id = request.fromUserId,
                    title = "Friend request",
                    message = "${request.fromDisplayName.ifBlank { request.fromUsername }} sent you a friend request",
                    type = InboxType.FRIEND_REQUEST,
                    isRead = false,
                    timestampMillis = request.createdAtMillis,
                )
            }
            val inviteItems = invites.map { invite ->
                val categoryLabel = invite.category.name.lowercase().replaceFirstChar { it.uppercase() }
                InboxItem(
                    id = invite.challengeId,
                    title = "Challenge invite",
                    message = "${invite.name}: ${invite.rule} • $categoryLabel • ${invite.durationDays}d",
                    type = InboxType.CHALLENGE_INVITE,
                    isRead = false,
                    timestampMillis = invite.createdAtMillis,
                )
            }
            (requestItems + inviteItems + notifications)
                .sortedByDescending { it.timestampMillis }
        }
    }

    suspend fun respondToFriendRequest(fromUserId: String, accept: Boolean) {
        friendsRepository.respondToFriendRequest(fromUserId = fromUserId, accept = accept)
    }

    suspend fun respondToChallengeInvite(challengeId: String, accept: Boolean) {
        challengesRepository.respondToChallengeInvite(challengeId = challengeId, accept = accept)
    }

    suspend fun markAllNotificationItemsRead(notificationIds: List<String>) {
        val userId = authRepository.getCurrentUserId()
        if (userId.isBlank() || notificationIds.isEmpty()) return

        val batch = firestore.batch()
        notificationIds.forEach { id ->
            val ref = usersCollection().document(userId)
                .collection(COLLECTION_NOTIFICATIONS)
                .document(id)
            batch.update(ref, FIELD_IS_READ, true)
        }
        batch.commit().await()
    }

    suspend fun deleteNotificationItem(notificationId: String) {
        val userId = authRepository.getCurrentUserId()
        if (userId.isBlank() || notificationId.isBlank()) return

        usersCollection().document(userId)
            .collection(COLLECTION_NOTIFICATIONS)
            .document(notificationId)
            .delete()
            .await()
    }

    suspend fun deleteFriendRequestNotification(fromUserId: String) {
        val userId = authRepository.getCurrentUserId()
        if (userId.isBlank() || fromUserId.isBlank()) return

        usersCollection().document(userId)
            .collection(COLLECTION_NOTIFICATIONS)
            .document("friend_request_$fromUserId")
            .delete()
            .await()
    }

    private fun observeNotifications(): Flow<List<InboxItem>> = callbackFlow {
        val userId = authRepository.getCurrentUserId()
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = usersCollection()
            .document(userId)
            .collection(COLLECTION_NOTIFICATIONS)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .limit(80)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val items = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val message = doc.getString(FIELD_MESSAGE).orEmpty()
                    if (message.isBlank()) return@mapNotNull null
                    val type = doc.getString(FIELD_TYPE).toInboxType()
                    if (type == InboxType.FRIEND_REQUEST || type == InboxType.CHALLENGE_INVITE) {
                        return@mapNotNull null
                    }
                    InboxItem(
                        id = doc.id,
                        title = doc.getString(FIELD_TITLE).orEmpty().ifBlank { "Update" },
                        message = message,
                        type = type,
                        isRead = doc.getBoolean(FIELD_IS_READ) ?: false,
                        timestampMillis = doc.getTimestamp(FIELD_TIMESTAMP)?.toDate()?.time ?: 0L,
                    )
                }
                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    private fun usersCollection() = firestore.collection(COLLECTION_USERS)

    private fun String?.toInboxType(): InboxType {
        return when (this.orEmpty()) {
            "friend_request" -> InboxType.FRIEND_REQUEST
            "challenge_invite" -> InboxType.CHALLENGE_INVITE
            "challenge_activity" -> InboxType.CHALLENGE_ACTIVITY
            "evening_reminder" -> InboxType.EVENING_REMINDER
            else -> InboxType.STREAK_MILESTONE
        }
    }

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_NOTIFICATIONS = "notifications"
        const val FIELD_TITLE = "title"
        const val FIELD_MESSAGE = "message"
        const val FIELD_TYPE = "type"
        const val FIELD_IS_READ = "isRead"
        const val FIELD_TIMESTAMP = "timestamp"
    }
}
