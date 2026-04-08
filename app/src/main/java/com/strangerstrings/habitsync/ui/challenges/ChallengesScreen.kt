package com.strangerstrings.habitsync.ui.challenges

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.strangerstrings.habitsync.data.HabitCategory
import com.strangerstrings.habitsync.viewmodel.ChallengesUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen(
    uiState: ChallengesUiState,
    onCreateChallenge: (
        name: String,
        rule: String,
        durationDays: Int,
        category: HabitCategory,
        inviteUsernames: List<String>,
    ) -> Unit,
    onOpenChallengeRoom: (challengeId: String) -> Unit,
    onCloseChallengeRoom: () -> Unit,
    onChatInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onMarkChallengeDone: () -> Unit,
    prefillInviteUsername: String?,
    onPrefillInviteConsumed: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var challengeName by remember { mutableStateOf("") }
    var challengeRule by remember { mutableStateOf("") }
    var challengeInvites by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableIntStateOf(14) }
    var selectedCategory by remember { mutableStateOf(HabitCategory.FITNESS) }

    LaunchedEffect(prefillInviteUsername) {
        val username = prefillInviteUsername.orEmpty().trim()
        if (username.isNotBlank()) {
            challengeInvites = if (challengeInvites.isBlank()) username else challengeInvites
            showCreateDialog = true
            onPrefillInviteConsumed()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 80.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Create Challenge")
            }
        }

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

        items(uiState.challenges, key = { it.id }) { challenge ->
            val isEnded = challenge.endsAtMillis <= System.currentTimeMillis()
            Card(
                onClick = { onOpenChallengeRoom(challenge.id) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(challenge.name, style = MaterialTheme.typography.titleMedium)
                    Text("Rule: ${challenge.rule}", style = MaterialTheme.typography.bodyMedium)
                    Text("${challenge.durationDays} days • ${challenge.participantIds.size} participants")
                    if (isEnded) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Challenge Ended", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    if (challenge.winnerUserId.isNullOrBlank()) {
                                        "Winner is being finalized."
                                    } else {
                                        "Winner selected. Finisher badge awarded."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    } else {
                        Text("Mini leaderboard and challenge chat will appear here.")
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Challenge") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = challengeName,
                        onValueChange = { challengeName = it },
                        label = { Text("Name") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = challengeRule,
                        onValueChange = { challengeRule = it },
                        label = { Text("Rule") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = challengeInvites,
                        onValueChange = { challengeInvites = it },
                        label = { Text("Invite usernames (comma-separated)") },
                        minLines = 2,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(7, 14, 30).forEach { days ->
                            FilterChip(
                                selected = selectedDuration == days,
                                onClick = { selectedDuration = days },
                                label = { Text("$days d") },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HabitCategory.entries.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateChallenge(
                            challengeName,
                            challengeRule,
                            selectedDuration,
                            selectedCategory,
                            challengeInvites.split(",").map { it.trim() }.filter { it.isNotBlank() },
                        )
                        showCreateDialog = false
                        challengeName = ""
                        challengeRule = ""
                        challengeInvites = ""
                        selectedDuration = 14
                        selectedCategory = HabitCategory.FITNESS
                    },
                    enabled = !uiState.isCreating,
                ) {
                    Text(if (uiState.isCreating) "Creating..." else "Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (uiState.selectedChallenge != null) {
        val currentParticipant = uiState.participantLeaderboard.firstOrNull { it.userId == uiState.currentUserId }
        val isEnded = uiState.selectedChallenge.endsAtMillis <= System.currentTimeMillis()
        ModalBottomSheet(
            onDismissRequest = onCloseChallengeRoom,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = uiState.selectedChallenge.name,
                    style = MaterialTheme.typography.titleLarge,
                )
                if (uiState.selectedChallenge.endsAtMillis <= System.currentTimeMillis()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val winnerName = uiState.participantLeaderboard
                            .firstOrNull { it.userId == uiState.selectedChallenge.winnerUserId }
                            ?.username
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Final Result", style = MaterialTheme.typography.labelLarge)
                            Text(
                                if (winnerName.isNullOrBlank()) "Winner is being finalized."
                                else "🏆 $winnerName won this challenge and received Finisher badge.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                if (!isEnded) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Today's Check-in", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    if (currentParticipant?.isCompletedToday == true) {
                                        "You already completed today's challenge."
                                    } else {
                                        "Mark your daily challenge progress."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Button(
                                onClick = onMarkChallengeDone,
                                enabled = currentParticipant?.isCompletedToday != true && !uiState.isMarkingDone,
                            ) {
                                if (uiState.isMarkingDone) {
                                    CircularProgressIndicator(strokeWidth = 2.dp)
                                } else {
                                    Text(if (currentParticipant?.isCompletedToday == true) "Done" else "Mark Done")
                                }
                            }
                        }
                    }
                }
                Text(
                    text = "Leaderboard",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (uiState.isRoomLoading) {
                    CircularProgressIndicator()
                }
                uiState.participantLeaderboard.take(5).forEachIndexed { index, participant ->
                    Text(
                        "#${index + 1} ${participant.username} • 🔥 ${participant.streak}" +
                            if (participant.isCompletedToday) " • today done" else "",
                    )
                }

                Text(
                    text = "Group Chat",
                    style = MaterialTheme.typography.titleMedium,
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.chatMessages, key = { it.id }) { message ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(message.username, style = MaterialTheme.typography.labelLarge)
                                Text(message.message, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = uiState.chatInput,
                        onValueChange = onChatInputChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message the challenge") },
                    )
                    Button(
                        onClick = onSendMessage,
                        enabled = uiState.chatInput.isNotBlank(),
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    }
}
