package com.strangerstrings.habitsync.data.repository

import android.content.Context
import com.strangerstrings.habitsync.data.local.OnboardingPreferences
import kotlinx.coroutines.flow.Flow

class OnboardingRepository(
    context: Context,
) {
    private val onboardingPreferences = OnboardingPreferences(context)

    fun onboardingCompletedFlow(): Flow<Boolean> = onboardingPreferences.onboardingCompletedFlow

    suspend fun markOnboardingCompleted() {
        onboardingPreferences.setOnboardingCompleted(completed = true)
    }
}
