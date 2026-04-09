package com.strangerstrings.habitsync.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import com.strangerstrings.habitsync.data.Challenge
import com.strangerstrings.habitsync.data.ChallengeInvite
import com.strangerstrings.habitsync.data.ChallengeMessage
import com.strangerstrings.habitsync.data.ChallengeParticipant
import com.strangerstrings.habitsync.data.HabitCategory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChallengesRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val proofStorageRepository: ProofStorageRepository = ProofStorageRepository(),
    private val socialRepository: SocialRepository = SocialRepository(),
) {
    fun observeMyChallenges(): Flow<List<Challenge>> = callbackFlow {
        val userId = authRepository.getCurrentUserId()
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = challengesCollection()
            .whereArrayContains(FIELD_PARTICIPANT_IDS, userId)
            .orderBy(FIELD_STARTED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val challenges = snapshot?.documents.orEmpty().mapNotNull { it.toChallengeOrNull() }
                trySend(challenges)
            }

        awaitClose { listener.remove() }
    }

    fun observeChallengeLeaderboard(challengeId: String): Flow<List<ChallengeParticipant>> = callbackFlow {
        if (challengeId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = challengesCollection()
            .document(challengeId)
            .collection(COLLECTION_PARTICIPANTS)
            .orderBy(FIELD_STREAK, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val participants = snapshot?.documents.orEmpty().map { doc ->
                    val completionDates = (doc.get(FIELD_COMPLETION_DATES) as? List<*>).orEmpty()
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
                    val lastCompletedDate = doc.getLong(FIELD_LAST_COMPLETED_DATE)
                    ChallengeParticipant(
                        userId = doc.id,
                        username = doc.getString(FIELD_USERNAME).orEmpty(),
                        streak = (doc.getLong(FIELD_STREAK) ?: 0L).toInt(),
                        isCompletedToday = (doc.getBoolean(FIELD_IS_COMPLETED_TODAY) ?: false) &&
                            lastCompletedDate == currentEpochDay(),
                        lastCompletedDate = lastCompletedDate,
                        completionDates = completionDates,
                        proofImageUrl = doc.getString(FIELD_PROOF_IMAGE_URL)?.takeIf { lastCompletedDate == currentEpochDay() },
                    )
                }
                trySend(participants)
            }
        awaitClose { listener.remove() }
    }

    fun observeChallengeChat(challengeId: String): Flow<List<ChallengeMessage>> = callbackFlow {
        if (challengeId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = challengesCollection()
            .document(challengeId)
            .collection(COLLECTION_MESSAGES)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.ASCENDING)
            .limit(300)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents.orEmpty().map { doc ->
                    ChallengeMessage(
                        id = doc.id,
                        userId = doc.getString(FIELD_USER_ID).orEmpty(),
                        username = doc.getString(FIELD_USERNAME).orEmpty(),
                        message = doc.getString(FIELD_MESSAGE).orEmpty(),
                        timestampMillis = doc.getTimestamp(FIELD_TIMESTAMP)?.toDate()?.time ?: 0L,
                    )
                }
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    fun observeChallengeInvites(): Flow<List<ChallengeInvite>> = callbackFlow {
        val userId = authRepository.getCurrentUserId()
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_CHALLENGE_INVITES)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val invites = snapshot?.documents.orEmpty()
                    .mapNotNull { doc ->
                        val challengeId = doc.getString(FIELD_CHALLENGE_ID).orEmpty().ifBlank { doc.id }
                        val name = doc.getString(FIELD_NAME).orEmpty()
                        if (name.isBlank()) return@mapNotNull null
                        ChallengeInvite(
                            challengeId = challengeId,
                            name = name,
                            category = doc.getString(FIELD_CATEGORY)
                                ?.uppercase()
                                ?.let { runCatching { HabitCategory.valueOf(it) }.getOrDefault(HabitCategory.CUSTOM) }
                                ?: HabitCategory.CUSTOM,
                            durationDays = (doc.getLong(FIELD_DURATION_DAYS) ?: 7L).toInt(),
                            rule = doc.getString(FIELD_RULE).orEmpty(),
                            status = doc.getString(FIELD_STATUS).orEmpty(),
                            createdAtMillis = doc.getTimestamp(FIELD_CREATED_AT)?.toDate()?.time ?: 0L,
                        )
                    }
                    .filter { it.status == STATUS_PENDING }
                trySend(invites)
            }

        awaitClose { listener.remove() }
    }

    suspend fun createChallenge(
        name: String,
        category: HabitCategory,
        rule: String,
        durationDays: Int,
        inviteUsernames: List<String>,
    ) {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId.isBlank()) return

        val currentUserDoc = firestore.collection(COLLECTION_USERS).document(currentUserId).get().await()
        val currentUsername = currentUserDoc.getString(FIELD_USERNAME).orEmpty()
        val challengeId = UUID.randomUUID().toString()
        val startedAt = System.currentTimeMillis()
        val endsAt = startedAt + (durationDays * 24L * 60L * 60L * 1000L)
        val friendIds = firestore.collection(COLLECTION_USERS)
            .document(currentUserId)
            .collection(COLLECTION_FRIENDS)
            .get()
            .await()
            .documents
            .map { it.id }
            .toSet()

        val inviteIds = mutableListOf<String>()
        inviteUsernames.map { it.trim().lowercase() }.filter { it.isNotBlank() }.forEach { username ->
            val userDoc = firestore.collection(COLLECTION_USERNAMES).document(username).get().await()
            val userId = userDoc.getString(FIELD_USER_ID).orEmpty()
            if (userId.isNotBlank() && userId != currentUserId && friendIds.contains(userId)) {
                inviteIds += userId
            }
        }

        val participantIds = listOf(currentUserId)
        val batch = firestore.batch()
        val challengeRef = challengesCollection().document(challengeId)
        batch.set(
            challengeRef,
            mapOf(
                FIELD_NAME to name,
                FIELD_CATEGORY to category.name.lowercase(),
                FIELD_RULE to rule,
                FIELD_DURATION_DAYS to durationDays,
                FIELD_CREATOR_USER_ID to currentUserId,
                FIELD_PARTICIPANT_IDS to participantIds,
                FIELD_STARTED_AT to startedAt,
                FIELD_ENDS_AT to endsAt,
            ),
        )

        batch.set(
            challengeRef.collection(COLLECTION_PARTICIPANTS).document(currentUserId),
            mapOf(
                FIELD_USERNAME to currentUsername,
                FIELD_STREAK to 0,
                FIELD_IS_COMPLETED_TODAY to false,
                FIELD_LAST_COMPLETED_DATE to null,
                FIELD_COMPLETION_DATES to emptyList<Long>(),
                FIELD_PROOF_IMAGE_URL to null,
            ),
        )

        inviteIds.forEach { userId ->
            batch.set(
                firestore.collection(COLLECTION_USERS).document(userId)
                    .collection(COLLECTION_CHALLENGE_INVITES)
                    .document(challengeId),
                mapOf(
                    FIELD_CHALLENGE_ID to challengeId,
                    FIELD_NAME to name,
                    FIELD_RULE to rule,
                    FIELD_CATEGORY to category.name.lowercase(),
                    FIELD_DURATION_DAYS to durationDays,
                    FIELD_STATUS to STATUS_PENDING,
                    FIELD_CREATED_AT to FieldValue.serverTimestamp(),
                ),
            )
        }
        batch.commit().await()
        socialRepository.incrementJoinedChallengesAndAwardIfEligible(currentUserId)
    }

    suspend fun updateChallenge(
        challengeId: String,
        name: String,
        category: HabitCategory,
        rule: String,
        durationDays: Int,
        inviteUsernames: List<String>,
    ) {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId.isBlank() || challengeId.isBlank()) return

        val challengeRef = challengesCollection().document(challengeId)
        val challengeDoc = challengeRef.get().await()
        if (!challengeDoc.exists()) error("Challenge not found.")
        if (challengeDoc.getString(FIELD_CREATOR_USER_ID) != currentUserId) {
            error("Only the creator can edit this challenge.")
        }

        val currentParticipantIds = (challengeDoc.get(FIELD_PARTICIPANT_IDS) as? List<*>).orEmpty()
            .mapNotNull { it as? String }
            .distinct()

        val friendIds = firestore.collection(COLLECTION_USERS)
            .document(currentUserId)
            .collection(COLLECTION_FRIENDS)
            .get()
            .await()
            .documents
            .map { it.id }
            .toSet()

        val invitedIds = mutableListOf<String>()
        inviteUsernames.map { it.trim().lowercase() }.filter { it.isNotBlank() }.forEach { username ->
            val userDoc = firestore.collection(COLLECTION_USERNAMES).document(username).get().await()
            val userId = userDoc.getString(FIELD_USER_ID).orEmpty()
            if (userId.isNotBlank() && userId != currentUserId && friendIds.contains(userId)) {
                invitedIds += userId
            }
        }

        val acceptedParticipantIds = currentParticipantIds
        val finalParticipantIds = (acceptedParticipantIds.filter { it == currentUserId || it in invitedIds } + listOf(currentUserId)).distinct()
        val toRemove = acceptedParticipantIds.filter { it != currentUserId && it !in invitedIds }
        val toAddInvites = invitedIds.filter { it !in acceptedParticipantIds }
        val batch = firestore.batch()
        batch.set(
            challengeRef,
            mapOf(
                FIELD_NAME to name,
                FIELD_CATEGORY to category.name.lowercase(),
                FIELD_RULE to rule,
                FIELD_DURATION_DAYS to durationDays,
                FIELD_ENDS_AT to (challengeDoc.getLong(FIELD_STARTED_AT) ?: System.currentTimeMillis()) + (durationDays * 24L * 60L * 60L * 1000L),
                FIELD_PARTICIPANT_IDS to finalParticipantIds,
            ),
            SetOptions.merge(),
        )

        toRemove.forEach { userId ->
            batch.delete(challengeRef.collection(COLLECTION_PARTICIPANTS).document(userId))
            batch.delete(
                firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_CHALLENGE_INVITES)
                    .document(challengeId),
            )
        }

        toAddInvites.forEach { userId ->
            batch.set(
                firestore.collection(COLLECTION_USERS).document(userId)
                    .collection(COLLECTION_CHALLENGE_INVITES)
                    .document(challengeId),
                mapOf(
                    FIELD_CHALLENGE_ID to challengeId,
                    FIELD_NAME to name,
                    FIELD_RULE to rule,
                    FIELD_CATEGORY to category.name.lowercase(),
                    FIELD_DURATION_DAYS to durationDays,
                    FIELD_STATUS to STATUS_PENDING,
                    FIELD_CREATED_AT to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
        }

        (currentParticipantIds.intersect(finalParticipantIds.toSet()) - currentUserId).forEach { userId ->
            val existingInvite = firestore.collection(COLLECTION_USERS).document(userId)
                .collection(COLLECTION_CHALLENGE_INVITES)
                .document(challengeId)
                .get()
                .await()
            if (existingInvite.exists() && existingInvite.getString(FIELD_STATUS) == STATUS_PENDING) {
                batch.set(
                    firestore.collection(COLLECTION_USERS).document(userId)
                        .collection(COLLECTION_CHALLENGE_INVITES)
                        .document(challengeId),
                    mapOf(
                        FIELD_NAME to name,
                        FIELD_RULE to rule,
                        FIELD_CATEGORY to category.name.lowercase(),
                        FIELD_DURATION_DAYS to durationDays,
                    ),
                    SetOptions.merge(),
                )
            }
        }

        batch.commit().await()
    }

    suspend fun deleteChallenge(challengeId: String) {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId.isBlank() || challengeId.isBlank()) return
        val challengeRef = challengesCollection().document(challengeId)
        val challengeDoc = challengeRef.get().await()
        if (!challengeDoc.exists()) return
        if (challengeDoc.getString(FIELD_CREATOR_USER_ID) != currentUserId) {
            error("Only the creator can delete this challenge.")
        }

        val participantIds = (challengeDoc.get(FIELD_PARTICIPANT_IDS) as? List<*>).orEmpty()
            .mapNotNull { it as? String }
            .distinct()
        val participantDocs = challengeRef.collection(COLLECTION_PARTICIPANTS).get().await().documents
        val messageDocs = challengeRef.collection(COLLECTION_MESSAGES).get().await().documents
        val batch = firestore.batch()

        participantIds.forEach { userId ->
            batch.delete(
                firestore.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_CHALLENGE_INVITES)
                    .document(challengeId),
            )
        }
        participantDocs.forEach { batch.delete(it.reference) }
        messageDocs.forEach { batch.delete(it.reference) }
        batch.delete(challengeRef)
        batch.commit().await()
    }

    suspend fun sendChallengeMessage(challengeId: String, message: String) {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId.isBlank() || challengeId.isBlank() || message.isBlank()) return
        val userDoc = firestore.collection(COLLECTION_USERS).document(currentUserId).get().await()
        val username = userDoc.getString(FIELD_USERNAME).orEmpty()

        challengesCollection().document(challengeId)
            .collection(COLLECTION_MESSAGES)
            .document()
            .set(
                mapOf(
                    FIELD_USER_ID to currentUserId,
                    FIELD_USERNAME to username,
                    FIELD_MESSAGE to message.trim(),
                    FIELD_TIMESTAMP to FieldValue.serverTimestamp(),
                ),
            )
            .await()
    }

    suspend fun settleChallengeIfEnded(challengeId: String) {
        if (challengeId.isBlank()) return
        val challengeRef = challengesCollection().document(challengeId)
        val challengeDoc = challengeRef.get().await()
        if (!challengeDoc.exists()) return

        val endsAtMillis = challengeDoc.getLong(FIELD_ENDS_AT) ?: return
        val settledAtMillis = challengeDoc.getLong(FIELD_SETTLED_AT)
            ?: challengeDoc.getTimestamp(FIELD_SETTLED_AT)?.toDate()?.time
        val now = System.currentTimeMillis()
        if (now < endsAtMillis || settledAtMillis != null) return

        val participantSnapshots = challengeRef.collection(COLLECTION_PARTICIPANTS).get().await().documents
        val sortedParticipants = participantSnapshots
            .map { doc ->
                Triple(
                    doc.id,
                    doc.getString(FIELD_USERNAME).orEmpty(),
                    (doc.getLong(FIELD_STREAK) ?: 0L).toInt(),
                )
            }
            .sortedWith(
                compareByDescending<Triple<String, String, Int>> { it.third }
                    .thenBy { it.second.ifBlank { "~" } },
            )

        val winnerUserId = sortedParticipants.firstOrNull()?.first.orEmpty()
        val participantIds = (challengeDoc.get(FIELD_PARTICIPANT_IDS) as? List<*>).orEmpty()
            .mapNotNull { it as? String }
            .distinct()

        val batch = firestore.batch()
        batch.set(
            challengeRef,
            mapOf(
                FIELD_WINNER_USER_ID to winnerUserId,
                FIELD_SETTLED_AT to now,
            ),
            SetOptions.merge(),
        )

        participantIds.forEach { userId ->
            val userRef = firestore.collection(COLLECTION_USERS).document(userId)
            batch.set(
                userRef,
                mapOf(
                    FIELD_CHALLENGES_COMPLETED to FieldValue.increment(1L),
                ),
                SetOptions.merge(),
            )
        }

        if (winnerUserId.isNotBlank()) {
            val badgeRef = firestore.collection(COLLECTION_USERS).document(winnerUserId)
                .collection(COLLECTION_BADGES)
                .document("finisher_$challengeId")
            batch.set(
                badgeRef,
                mapOf(
                    FIELD_TITLE to "Finisher",
                    FIELD_DESCRIPTION to "Won challenge ${challengeDoc.getString(FIELD_NAME).orEmpty()}",
                    FIELD_EARNED_AT to now,
                ),
                SetOptions.merge(),
            )
        }

        batch.commit().await()
    }

    suspend fun respondToChallengeInvite(challengeId: String, accept: Boolean) {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId.isBlank() || challengeId.isBlank()) return

        val inviteRef = firestore.collection(COLLECTION_USERS)
            .document(currentUserId)
            .collection(COLLECTION_CHALLENGE_INVITES)
            .document(challengeId)

        if (!accept) {
            inviteRef.update(FIELD_STATUS, STATUS_DECLINED).await()
            return
        }

        val challengeRef = challengesCollection().document(challengeId)
        val userDoc = firestore.collection(COLLECTION_USERS).document(currentUserId).get().await()
        val username = userDoc.getString(FIELD_USERNAME).orEmpty()

        val batch = firestore.batch()
        batch.update(inviteRef, FIELD_STATUS, STATUS_ACCEPTED)
        batch.update(challengeRef, FIELD_PARTICIPANT_IDS, FieldValue.arrayUnion(currentUserId))
        batch.set(
            challengeRef.collection(COLLECTION_PARTICIPANTS).document(currentUserId),
            mapOf(
                FIELD_USERNAME to username,
                FIELD_STREAK to 0,
                FIELD_IS_COMPLETED_TODAY to false,
                FIELD_LAST_COMPLETED_DATE to null,
                FIELD_COMPLETION_DATES to emptyList<Long>(),
                FIELD_PROOF_IMAGE_URL to null,
            ),
            com.google.firebase.firestore.SetOptions.merge(),
        )
        batch.commit().await()
        socialRepository.incrementJoinedChallengesAndAwardIfEligible(currentUserId)
    }

    suspend fun markChallengeDone(challengeId: String, proofImageBytes: ByteArray? = null) {
        val currentUserId = authRepository.getCurrentUserId()
        if (currentUserId.isBlank() || challengeId.isBlank()) return

        val participantRef = challengesCollection()
            .document(challengeId)
            .collection(COLLECTION_PARTICIPANTS)
            .document(currentUserId)

        val participantDoc = participantRef.get().await()
        if (!participantDoc.exists()) return

        val today = currentEpochDay()
        val lastCompletedDate = participantDoc.getLong(FIELD_LAST_COMPLETED_DATE)
        val isCompletedToday = (participantDoc.getBoolean(FIELD_IS_COMPLETED_TODAY) ?: false) &&
            lastCompletedDate == today
        if (isCompletedToday) return

        val currentStreak = (participantDoc.getLong(FIELD_STREAK) ?: 0L).toInt()
        val completionDates = (participantDoc.get(FIELD_COMPLETION_DATES) as? List<*>).orEmpty()
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
        val latestDay = completionDates.maxOrNull()
        val nextStreak = when {
            completionDates.contains(today) -> currentStreak
            latestDay == null -> 1
            latestDay == today - 1 -> currentStreak + 1
            else -> 1
        }
        val updatedCompletionDates = (completionDates + today).distinct().sorted()
        val proofImageUrl = if (proofImageBytes != null) {
            proofStorageRepository.uploadProofImage(
                habitId = "challenge_$challengeId",
                proofImageBytes = proofImageBytes,
            )
        } else {
            participantDoc.getString(FIELD_PROOF_IMAGE_URL)
        }

        participantRef.set(
            mapOf(
                FIELD_STREAK to nextStreak,
                FIELD_IS_COMPLETED_TODAY to true,
                FIELD_LAST_COMPLETED_DATE to today,
                FIELD_COMPLETION_DATES to updatedCompletionDates,
                FIELD_PROOF_IMAGE_URL to proofImageUrl,
            ),
            SetOptions.merge(),
        ).await()

        val username = participantDoc.getString(FIELD_USERNAME).orEmpty()
        if (username.isNotBlank()) {
            sendSystemChallengeActivity(
                challengeId = challengeId,
                message = if (proofImageUrl.isNullOrBlank()) {
                    "$username completed today's challenge check-in."
                } else {
                    "$username completed today's challenge check-in with proof."
                },
            )
        }
    }

    suspend fun syncChallengeState(challengeId: String) {
        if (challengeId.isBlank()) return
        val challengeRef = challengesCollection().document(challengeId)
        val challengeDoc = challengeRef.get().await()
        if (!challengeDoc.exists()) return

        val today = currentEpochDay()
        val participantDocs = challengeRef.collection(COLLECTION_PARTICIPANTS).get().await().documents
        val batch = firestore.batch()
        var hasUpdates = false

        participantDocs.forEach { doc ->
            val lastCompletedDate = doc.getLong(FIELD_LAST_COMPLETED_DATE)
            val rawCompletedToday = doc.getBoolean(FIELD_IS_COMPLETED_TODAY) ?: false
            val currentStreak = (doc.getLong(FIELD_STREAK) ?: 0L).toInt()

            val shouldMarkNotCompletedToday = rawCompletedToday && lastCompletedDate != today
            val shouldResetStreak = lastCompletedDate != null &&
                lastCompletedDate < today - 1 &&
                currentStreak > 0

            if (shouldMarkNotCompletedToday || shouldResetStreak) {
                hasUpdates = true
                applyParticipantStateSync(
                    batch = batch,
                    participantRef = doc.reference,
                    keepTodayCompleted = lastCompletedDate == today,
                    streak = if (shouldResetStreak) 0 else currentStreak,
                )
            }
        }

        if (hasUpdates) {
            batch.commit().await()
        }
    }

    private suspend fun sendSystemChallengeActivity(
        challengeId: String,
        message: String,
    ) {
        challengesCollection().document(challengeId)
            .collection(COLLECTION_MESSAGES)
            .document()
            .set(
                mapOf(
                    FIELD_USER_ID to "system",
                    FIELD_USERNAME to "HabitSync",
                    FIELD_MESSAGE to message,
                    FIELD_TIMESTAMP to FieldValue.serverTimestamp(),
                ),
            )
            .await()
    }

    private fun challengesCollection() = firestore.collection(COLLECTION_CHALLENGES)

    private fun applyParticipantStateSync(
        batch: WriteBatch,
        participantRef: com.google.firebase.firestore.DocumentReference,
        keepTodayCompleted: Boolean,
        streak: Int,
    ) {
        val updates = mutableMapOf<String, Any?>(
            FIELD_IS_COMPLETED_TODAY to keepTodayCompleted,
            FIELD_STREAK to streak,
        )
        if (!keepTodayCompleted) {
            updates[FIELD_PROOF_IMAGE_URL] = null
        }
        batch.set(participantRef, updates, SetOptions.merge())
    }

    private fun DocumentSnapshot.toChallengeOrNull(): Challenge? {
        val name = getString(FIELD_NAME).orEmpty()
        if (name.isBlank()) return null
        val category = getString(FIELD_CATEGORY)
            ?.uppercase()
            ?.let { runCatching { HabitCategory.valueOf(it) }.getOrDefault(HabitCategory.CUSTOM) }
            ?: HabitCategory.CUSTOM
        val participantIdsRaw = get(FIELD_PARTICIPANT_IDS) as? List<*>
        val participantIds = participantIdsRaw.orEmpty().mapNotNull { it as? String }
        return Challenge(
            id = id,
            name = name,
            category = category,
            rule = getString(FIELD_RULE).orEmpty(),
            durationDays = (getLong(FIELD_DURATION_DAYS) ?: 0L).toInt(),
            creatorUserId = getString(FIELD_CREATOR_USER_ID).orEmpty(),
            participantIds = participantIds,
            startedAtMillis = getLong(FIELD_STARTED_AT) ?: 0L,
            endsAtMillis = getLong(FIELD_ENDS_AT) ?: 0L,
            winnerUserId = getString(FIELD_WINNER_USER_ID),
            settledAtMillis = getLong(FIELD_SETTLED_AT)
                ?: getTimestamp(FIELD_SETTLED_AT)?.toDate()?.time,
        )
    }

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_USERNAMES = "usernames"
        const val COLLECTION_FRIENDS = "friends"
        const val COLLECTION_CHALLENGES = "challenges"
        const val COLLECTION_PARTICIPANTS = "participants"
        const val COLLECTION_MESSAGES = "messages"
        const val COLLECTION_CHALLENGE_INVITES = "challengeInvites"
        const val COLLECTION_BADGES = "badges"
        const val FIELD_USER_ID = "userId"
        const val FIELD_USERNAME = "username"
        const val FIELD_STREAK = "streak"
        const val FIELD_IS_COMPLETED_TODAY = "isCompletedToday"
        const val FIELD_LAST_COMPLETED_DATE = "lastCompletedDate"
        const val FIELD_COMPLETION_DATES = "completionDates"
        const val FIELD_PROOF_IMAGE_URL = "proofImageUrl"
        const val FIELD_CHALLENGE_ID = "challengeId"
        const val FIELD_NAME = "name"
        const val FIELD_CATEGORY = "category"
        const val FIELD_RULE = "rule"
        const val FIELD_DURATION_DAYS = "durationDays"
        const val FIELD_CREATOR_USER_ID = "creatorUserId"
        const val FIELD_PARTICIPANT_IDS = "participantIds"
        const val FIELD_STARTED_AT = "startedAt"
        const val FIELD_ENDS_AT = "endsAt"
        const val FIELD_WINNER_USER_ID = "winnerUserId"
        const val FIELD_SETTLED_AT = "settledAt"
        const val FIELD_CHALLENGES_COMPLETED = "challengesCompleted"
        const val FIELD_MESSAGE = "message"
        const val FIELD_TIMESTAMP = "timestamp"
        const val FIELD_STATUS = "status"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_TITLE = "title"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_EARNED_AT = "earnedAtMillis"
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_DECLINED = "declined"
    }

    private fun currentEpochDay(): Long {
        return System.currentTimeMillis() / (24L * 60L * 60L * 1000L)
    }
}
