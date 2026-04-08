package com.strangerstrings.habitsync.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.strangerstrings.habitsync.data.Habit
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class ReminderRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val authRepository: AuthRepository = AuthRepository(),
) {
    suspend fun maybeSendEveningReminder(habits: List<Habit>) {
        val userId = authRepository.getCurrentUserId()
        if (userId.isBlank() || habits.isEmpty()) return

        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        if (hour < EVENING_REMINDER_HOUR) return

        val pendingCount = habits.count { !it.isCompletedToday }
        if (pendingCount <= 0) return

        val reminderId = buildEveningReminderId(now)
        val reminderRef = usersCollection()
            .document(userId)
            .collection(COLLECTION_NOTIFICATIONS)
            .document(reminderId)

        val snapshot = reminderRef.get().await()
        if (snapshot.exists()) return

        reminderRef.set(
            mapOf(
                FIELD_TITLE to "Evening reminder",
                FIELD_MESSAGE to "You still have $pendingCount habits left for today.",
                FIELD_TYPE to TYPE_EVENING_REMINDER,
                FIELD_IS_READ to false,
                FIELD_TIMESTAMP to FieldValue.serverTimestamp(),
            ),
            SetOptions.merge(),
        ).await()
    }

    private fun usersCollection() = firestore.collection(COLLECTION_USERS)

    private fun buildEveningReminderId(calendar: Calendar): String {
        val year = calendar.get(Calendar.YEAR)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        return "evening_${year}_$dayOfYear"
    }

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_NOTIFICATIONS = "notifications"
        const val FIELD_TITLE = "title"
        const val FIELD_MESSAGE = "message"
        const val FIELD_TYPE = "type"
        const val FIELD_IS_READ = "isRead"
        const val FIELD_TIMESTAMP = "timestamp"
        const val TYPE_EVENING_REMINDER = "evening_reminder"
        const val EVENING_REMINDER_HOUR = 20
    }
}
