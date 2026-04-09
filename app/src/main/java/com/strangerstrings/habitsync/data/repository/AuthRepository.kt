package com.strangerstrings.habitsync.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class SignUpRequest(
    val firstName: String,
    val lastName: String,
    val username: String,
    val age: Int,
    val dobMillis: Long,
    val heightCm: Float,
    val weightKg: Float,
    val gender: String,
    val profileImageUrl: String?,
    val email: String,
    val password: String,
)

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    suspend fun signIn(email: String, password: String): Result<String> = runCatching {
        val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        authResult.user?.uid ?: error("Authentication succeeded but no user was returned.")
    }

    suspend fun signUp(request: SignUpRequest): Result<String> = runCatching {
        val authResult = firebaseAuth.createUserWithEmailAndPassword(request.email, request.password).await()
        val user = authResult.user ?: error("Account created but user is unavailable.")
        val userId = user.uid
        val username = request.username.lowercase()
        val fullName = listOf(request.firstName, request.lastName)
            .joinToString(" ")
            .trim()
            .ifBlank { request.firstName }

        runCatching {
            firestore.runTransaction { transaction ->
                val usernameRef = firestore.collection(USERNAMES_COLLECTION).document(username)
                val usernameSnapshot = transaction.get(usernameRef)
                if (usernameSnapshot.exists()) {
                    throw IllegalStateException("Username already taken.")
                }

                val userRef = firestore.collection(USERS_COLLECTION).document(userId)
                transaction.set(
                    usernameRef,
                    mapOf(
                        FIELD_USER_ID to userId,
                        FIELD_CREATED_AT to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
                transaction.set(
                    userRef,
                    mapOf(
                        FIELD_USER_ID to userId,
                        FIELD_FIRST_NAME to request.firstName.trim(),
                        FIELD_LAST_NAME to request.lastName.trim(),
                        FIELD_USERNAME to username,
                        FIELD_AGE to request.age,
                        FIELD_DOB_MILLIS to request.dobMillis,
                        FIELD_HEIGHT_CM to request.heightCm,
                        FIELD_WEIGHT_KG to request.weightKg,
                        FIELD_GENDER to request.gender,
                        FIELD_PROFILE_IMAGE_URL to request.profileImageUrl,
                        FIELD_EMAIL to request.email.trim(),
                        FIELD_EMAIL_LOWERCASE to request.email.trim().lowercase(),
                        FIELD_DISPLAY_NAME to fullName,
                        FIELD_CREATED_AT to FieldValue.serverTimestamp(),
                        FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
                userId
            }.await()

            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName)
                    .setPhotoUri(request.profileImageUrl?.let(Uri::parse))
                    .build(),
            ).await()
        }.onFailure {
            runCatching { user.delete().await() }
            runCatching { firebaseAuth.signOut() }
            throw it
        }

        userId
    }

    suspend fun isUsernameAvailable(username: String): Result<Boolean> = runCatching {
        val normalized = username.trim().lowercase()
        if (normalized.isBlank()) return@runCatching false
        val usernameDoc = firestore.collection(USERNAMES_COLLECTION).document(normalized).get().await()
        !usernameDoc.exists()
    }

    suspend fun isEmailAvailable(email: String): Result<Boolean> = runCatching {
        val normalized = email.trim()
        if (normalized.isBlank()) return@runCatching false
        val lower = normalized.lowercase()

        val existingLowercase = firestore.collection(USERS_COLLECTION)
            .whereEqualTo(FIELD_EMAIL_LOWERCASE, lower)
            .limit(1)
            .get()
            .await()
            .documents
            .isNotEmpty()

        val existingExact = if (existingLowercase) {
            true
        } else {
            firestore.collection(USERS_COLLECTION)
                .whereEqualTo(FIELD_EMAIL, normalized)
                .limit(1)
                .get()
                .await()
                .documents
                .isNotEmpty()
        }

        val existingInFirestore = existingLowercase || existingExact
        if (existingInFirestore) {
            false
        } else {
            val methods = runCatching {
                firebaseAuth.fetchSignInMethodsForEmail(normalized).await().signInMethods.orEmpty()
            }.getOrDefault(emptyList())
            methods.isEmpty()
        }
    }

    suspend fun verifyRecoveryInfo(
        email: String,
        username: String,
        dobMillis: Long,
    ): Result<Boolean> = runCatching {
        val normalizedEmail = email.trim()
        val normalizedUsername = username.trim().lowercase()
        if (normalizedEmail.isBlank() || normalizedUsername.isBlank()) return@runCatching false

        val usernameDoc = firestore.collection(USERNAMES_COLLECTION).document(normalizedUsername).get().await()
        val userId = usernameDoc.getString(FIELD_USER_ID).orEmpty()
        if (userId.isBlank()) return@runCatching false

        val userDoc = firestore.collection(USERS_COLLECTION).document(userId).get().await()
        if (!userDoc.exists()) return@runCatching false

        val storedEmail = userDoc.getString(FIELD_EMAIL).orEmpty()
        if (!storedEmail.equals(normalizedEmail, ignoreCase = true)) return@runCatching false

        val storedDobMillis = userDoc.getLong(FIELD_DOB_MILLIS) ?: return@runCatching false
        sameEpochDay(storedDobMillis, dobMillis)
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        firebaseAuth.sendPasswordResetEmail(email.trim()).await()
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    fun getCurrentUserId(): String {
        return firebaseAuth.currentUser?.uid.orEmpty()
    }

    fun getCurrentUserName(): String {
        val user = firebaseAuth.currentUser
        val displayName = user?.displayName?.trim().orEmpty()
        if (displayName.isNotBlank()) return displayName

        val emailName = user?.email
            ?.substringBefore("@")
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            .orEmpty()
        if (emailName.isNotBlank()) return emailName

        return "Habit Hero"
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val USERNAMES_COLLECTION = "usernames"
        const val FIELD_USER_ID = "userId"
        const val FIELD_FIRST_NAME = "firstName"
        const val FIELD_LAST_NAME = "lastName"
        const val FIELD_USERNAME = "username"
        const val FIELD_AGE = "age"
        const val FIELD_DOB_MILLIS = "dobMillis"
        const val FIELD_HEIGHT_CM = "heightCm"
        const val FIELD_WEIGHT_KG = "weightKg"
        const val FIELD_GENDER = "gender"
        const val FIELD_EMAIL = "email"
        const val FIELD_EMAIL_LOWERCASE = "emailLowercase"
        const val FIELD_DISPLAY_NAME = "displayName"
        const val FIELD_PROFILE_IMAGE_URL = "profileImageUrl"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }

    private fun sameEpochDay(first: Long, second: Long): Boolean {
        return first / MILLIS_PER_DAY == second / MILLIS_PER_DAY
    }
}
