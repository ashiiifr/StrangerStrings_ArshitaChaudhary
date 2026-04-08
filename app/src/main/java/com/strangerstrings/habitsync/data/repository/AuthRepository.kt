package com.strangerstrings.habitsync.data.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    suspend fun signIn(email: String, password: String): Result<String> = runCatching {
        val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        authResult.user?.uid ?: error("Authentication succeeded but no user was returned.")
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
}
