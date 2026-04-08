package com.strangerstrings.habitsync.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.strangerstrings.habitsync.viewmodel.ProfileUiState

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val profile = uiState.profile

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding() + 12.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 80.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uiState.isLoading) {
            item {
                CircularProgressIndicator()
            }
        }

        uiState.errorMessage?.let { message ->
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        item {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(profile?.displayName.orEmpty().ifBlank { "Profile" }, style = MaterialTheme.typography.headlineSmall)
                    if (!profile?.username.isNullOrBlank()) {
                        Text("@${profile?.username}")
                    }
                    if (!profile?.bio.isNullOrBlank()) {
                        Text(profile?.bio.orEmpty())
                    }
                    Text("Weight: ${profile?.weightKg ?: 0f} kg • Height: ${profile?.heightCm ?: 0f} cm")
                    Text("Freeze tokens this month: ${profile?.freezeTokensThisMonth ?: 0}")
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Stats", style = MaterialTheme.typography.titleMedium)
                    Text("Total habits created: ${profile?.totalHabitsCreated ?: 0}")
                    Text("Longest streak ever: ${profile?.longestStreakEver ?: 0}")
                    Text("Challenges completed: ${profile?.challengesCompleted ?: 0}")
                }
            }
        }

        item {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Badges", style = MaterialTheme.typography.titleMedium)
                    if (uiState.badges.isEmpty()) {
                        Text("No badges yet. Keep your streak alive to unlock your first one.")
                    } else {
                        uiState.badges.forEach { badge ->
                            Text("${badge.title} • ${badge.description}")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Public Habits",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        items(uiState.publicHabits, key = { it.id }) { habit ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(habit.title, style = MaterialTheme.typography.titleMedium)
                    Text("🔥 ${habit.streak} day streak")
                }
            }
        }
    }
}
