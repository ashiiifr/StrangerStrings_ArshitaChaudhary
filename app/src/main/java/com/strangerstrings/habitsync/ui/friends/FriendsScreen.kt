package com.strangerstrings.habitsync.ui.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.strangerstrings.habitsync.viewmodel.FriendsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    uiState: FriendsUiState,
    onQueryChange: (String) -> Unit,
    onSendFriendRequest: (String) -> Unit,
    onOpenFriendProfile: (String) -> Unit,
    onCloseFriendProfile: () -> Unit,
    onChallengeFriend: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Friends List") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Leaderboard") })
        }

        if (selectedTab == 0) {
            TextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search by username") },
                singleLine = true,
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp))
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                uiState.errorMessage?.let { message ->
                    item {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                }

                if (uiState.query.length >= 2) {
                    items(uiState.searchResults, key = { it.userId }) { user ->
                        Card(
                            onClick = { onOpenFriendProfile(user.userId) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(user.displayName, style = MaterialTheme.typography.titleMedium)
                                    Text("@${user.username}")
                                }
                                OutlinedButton(onClick = { onSendFriendRequest(user.userId) }) {
                                    Text("Add")
                                }
                            }
                        }
                    }
                }

                items(uiState.friends, key = { it.userId }) { user ->
                    Card(
                        onClick = { onOpenFriendProfile(user.userId) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(user.displayName, style = MaterialTheme.typography.titleMedium)
                            Text("@${user.username}")
                            Text("Open profile • Challenge this friend")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = contentPadding.calculateBottomPadding() + 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(uiState.leaderboard, key = { it.userId }) { row ->
                    val trendText = when {
                        row.rankDelta > 0 -> "▲ +${row.rankDelta}"
                        row.rankDelta < 0 -> "▼ ${kotlin.math.abs(row.rankDelta)}"
                        else -> "• 0"
                    }
                    val trendColor = when {
                        row.rankDelta > 0 -> MaterialTheme.colorScheme.primary
                        row.rankDelta < 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "#${row.rank} ${row.username} • ${row.weeklyScore}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                trendText,
                                style = MaterialTheme.typography.labelLarge,
                                color = trendColor,
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isFriendProfileLoading || uiState.selectedFriendProfile != null) {
        AlertDialog(
            onDismissRequest = onCloseFriendProfile,
            title = {
                Text(
                    text = uiState.selectedFriendProfile?.displayName
                        ?: "Loading profile...",
                )
            },
            text = {
                if (uiState.isFriendProfileLoading && uiState.selectedFriendProfile == null) {
                    CircularProgressIndicator()
                } else {
                    val profile = uiState.selectedFriendProfile
                    if (profile != null) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 380.dp),
                        ) {
                            Text("@${profile.username}")
                            if (profile.bio.isNotBlank()) {
                                Text(profile.bio)
                            }
                            Text("Public Habits", style = MaterialTheme.typography.titleSmall)
                            if (profile.publicHabits.isEmpty()) {
                                Text("No public habits yet.")
                            } else {
                                profile.publicHabits.take(8).forEach { habit ->
                                    Text("${habit.title} • 🔥 ${habit.streak}")
                                }
                            }
                            Text("Badges", style = MaterialTheme.typography.titleSmall)
                            if (profile.badges.isEmpty()) {
                                Text("No badges yet.")
                            } else {
                                Text(profile.badges.take(6).joinToString(" • "))
                            }
                        }
                    } else {
                        Text("Could not load profile.")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        uiState.selectedFriendProfile?.username?.let(onChallengeFriend)
                        onCloseFriendProfile()
                    },
                    enabled = uiState.selectedFriendProfile != null,
                ) {
                    Text("Challenge this friend")
                }
            },
            dismissButton = {
                TextButton(onClick = onCloseFriendProfile) {
                    Text("Close")
                }
            },
        )
    }
}
