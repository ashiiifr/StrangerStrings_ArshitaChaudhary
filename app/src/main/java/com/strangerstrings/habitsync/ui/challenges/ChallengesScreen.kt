package com.strangerstrings.habitsync.ui.challenges

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strangerstrings.habitsync.data.HabitCategory
import com.strangerstrings.habitsync.ui.theme.AmberDeep
import com.strangerstrings.habitsync.ui.theme.CharcoalDark
import com.strangerstrings.habitsync.ui.theme.CharcoalLight
import com.strangerstrings.habitsync.ui.theme.CharcoalMid
import com.strangerstrings.habitsync.ui.theme.Cream
import com.strangerstrings.habitsync.ui.theme.CreamDark
import com.strangerstrings.habitsync.ui.theme.GoldSoft
import com.strangerstrings.habitsync.ui.theme.OrangeGlow
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
    openCreateDialogToken: Int = 0,
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

    LaunchedEffect(openCreateDialogToken) {
        if (openCreateDialogToken > 0) {
            showCreateDialog = true
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Create challenge button
        item {
            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeGlow,
                    contentColor = CharcoalDark,
                ),
            ) {
                Text(
                    text = "Create Challenge",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }

        if (uiState.isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OrangeGlow)
                }
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

        // Challenge cards with staggered entrance animations
        itemsIndexed(uiState.challenges, key = { _, c -> c.id }) { index, challenge ->
            var visible by remember(challenge.id) { mutableStateOf(false) }
            LaunchedEffect(challenge.id) {
                kotlinx.coroutines.delay((100L + index * 80L).coerceAtMost(500L))
                visible = true
            }
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(350)) + slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(400, easing = FastOutSlowInEasing),
                ),
            ) {
            val isEnded = challenge.endsAtMillis <= System.currentTimeMillis()
            val daysLeft = if (!isEnded) {
                ((challenge.endsAtMillis - System.currentTimeMillis()) / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(0)
            } else 0
            val rawProgress = if (challenge.durationDays > 0) {
                1f - (daysLeft.toFloat() / challenge.durationDays.toFloat())
            } else 1f
            // Animated progress for the circular indicator
            val progressFraction by animateFloatAsState(
                targetValue = rawProgress.coerceIn(0f, 1f),
                animationSpec = tween(800, easing = FastOutSlowInEasing),
                label = "challenge_progress_${challenge.id}",
            )

            val isLightCard = index % 2 == 0

            Card(
                onClick = { onOpenChallengeRoom(challenge.id) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLightCard) CreamDark else CharcoalMid,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Icon + title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(
                                    if (isLightCard) OrangeGlow.copy(alpha = 0.15f) else OrangeGlow.copy(alpha = 0.2f),
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = if (isLightCard) AmberDeep else OrangeGlow,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = challenge.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isLightCard) CharcoalDark else Cream,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = challenge.rule,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLightCard) CharcoalDark.copy(alpha = 0.6f) else Cream.copy(alpha = 0.6f),
                            )
                        }
                    }

                    // Circular progress indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "PROGRESS",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLightCard) CharcoalDark.copy(alpha = 0.5f) else Cream.copy(alpha = 0.5f),
                            )
                            Text(
                                text = if (isEnded) "Completed" else "$daysLeft / ${challenge.durationDays} days",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isLightCard) CharcoalDark else Cream,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "${challenge.participantIds.size} participants",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLightCard) CharcoalDark.copy(alpha = 0.5f) else Cream.copy(alpha = 0.5f),
                            )
                        }

                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { progressFraction.coerceIn(0f, 1f) },
                                modifier = Modifier.size(52.dp),
                                color = OrangeGlow,
                                trackColor = if (isLightCard) CharcoalDark.copy(alpha = 0.1f) else Cream.copy(alpha = 0.1f),
                                strokeWidth = 5.dp,
                            )
                            Text(
                                text = "${(progressFraction * 100).toInt().coerceIn(0, 100)}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isLightCard) CharcoalDark else Cream,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    if (isEnded) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isLightCard) GoldSoft.copy(alpha = 0.5f) else CharcoalLight,
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = "🏆 Challenge Ended",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isLightCard) CharcoalDark else Cream,
                                )
                                Text(
                                    text = if (challenge.winnerUserId.isNullOrBlank()) {
                                        "Winner is being finalized."
                                    } else {
                                        "Winner selected. Finisher badge awarded."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isLightCard) CharcoalDark.copy(alpha = 0.7f) else Cream.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }
            }
            } // AnimatedVisibility close
        }
    }

    // ── Create challenge dialog ──
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Cream,
            title = {
                Text(
                    "Create Challenge",
                    fontWeight = FontWeight.Bold,
                    color = CharcoalDark,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = challengeName,
                        onValueChange = { challengeName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                    OutlinedTextField(
                        value = challengeRule,
                        onValueChange = { challengeRule = it },
                        label = { Text("Rule") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                    OutlinedTextField(
                        value = challengeInvites,
                        onValueChange = { challengeInvites = it },
                        label = { Text("Invite usernames (comma-separated)") },
                        minLines = 2,
                        shape = RoundedCornerShape(14.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(7, 14, 30).forEach { days ->
                            FilterChip(
                                selected = selectedDuration == days,
                                onClick = { selectedDuration = days },
                                label = { Text("$days d") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OrangeGlow,
                                    selectedLabelColor = CharcoalDark,
                                ),
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HabitCategory.entries.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OrangeGlow,
                                    selectedLabelColor = CharcoalDark,
                                ),
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
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeGlow,
                        contentColor = CharcoalDark,
                    ),
                ) {
                    Text(if (uiState.isCreating) "Creating..." else "Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = CharcoalDark.copy(alpha = 0.6f))
                }
            },
        )
    }

    // ── Challenge room bottom sheet ──
    if (uiState.selectedChallenge != null) {
        val currentParticipant = uiState.participantLeaderboard.firstOrNull { it.userId == uiState.currentUserId }
        val isEnded = uiState.selectedChallenge.endsAtMillis <= System.currentTimeMillis()
        ModalBottomSheet(
            onDismissRequest = onCloseChallengeRoom,
            containerColor = CharcoalDark,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = uiState.selectedChallenge.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Cream,
                    fontWeight = FontWeight.Bold,
                )

                if (isEnded) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = GoldSoft.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val winnerName = uiState.participantLeaderboard
                            .firstOrNull { it.userId == uiState.selectedChallenge.winnerUserId }
                            ?.username
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🏆 Final Result", style = MaterialTheme.typography.labelLarge, color = GoldSoft)
                            Text(
                                if (winnerName.isNullOrBlank()) "Winner is being finalized."
                                else "$winnerName won this challenge!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Cream.copy(alpha = 0.85f),
                            )
                        }
                    }
                }

                if (!isEnded) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CharcoalMid),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Today's Check-in", style = MaterialTheme.typography.labelLarge, color = Cream)
                                Text(
                                    if (currentParticipant?.isCompletedToday == true) {
                                        "You completed today's challenge ✓"
                                    } else {
                                        "Mark your daily challenge progress."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Cream.copy(alpha = 0.7f),
                                )
                            }
                            Button(
                                onClick = onMarkChallengeDone,
                                enabled = currentParticipant?.isCompletedToday != true && !uiState.isMarkingDone,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OrangeGlow,
                                    contentColor = CharcoalDark,
                                    disabledContainerColor = CharcoalLight,
                                    disabledContentColor = GoldSoft.copy(alpha = 0.6f),
                                ),
                            ) {
                                if (uiState.isMarkingDone) {
                                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp), color = OrangeGlow)
                                } else {
                                    Text(if (currentParticipant?.isCompletedToday == true) "Done" else "Mark Done")
                                }
                            }
                        }
                    }
                }

                // Leaderboard
                Text("Leaderboard", style = MaterialTheme.typography.titleMedium, color = Cream, fontWeight = FontWeight.SemiBold)
                if (uiState.isRoomLoading) {
                    CircularProgressIndicator(color = OrangeGlow)
                }
                uiState.participantLeaderboard.take(5).forEachIndexed { index, participant ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (index == 0) OrangeGlow.copy(alpha = 0.15f) else CharcoalMid,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "#${index + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (index == 0) OrangeGlow else Cream,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(participant.username, color = Cream)
                            }
                            Text(
                                "🔥 ${participant.streak}" + if (participant.isCompletedToday) " ✓" else "",
                                color = GoldSoft,
                            )
                        }
                    }
                }

                // Group chat
                Text("Group Chat", style = MaterialTheme.typography.titleMedium, color = Cream, fontWeight = FontWeight.SemiBold)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.chatMessages, key = { it.id }) { message ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CharcoalMid),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(message.username, style = MaterialTheme.typography.labelLarge, color = OrangeGlow)
                                Text(message.message, style = MaterialTheme.typography.bodyMedium, color = Cream.copy(alpha = 0.9f))
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
                        placeholder = { Text("Message the challenge", color = Cream.copy(alpha = 0.4f)) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Cream,
                            unfocusedTextColor = Cream,
                            focusedBorderColor = OrangeGlow,
                            unfocusedBorderColor = CharcoalLight,
                        ),
                    )
                    Button(
                        onClick = onSendMessage,
                        enabled = uiState.chatInput.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeGlow,
                            contentColor = CharcoalDark,
                        ),
                    ) {
                        Text("Send")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
