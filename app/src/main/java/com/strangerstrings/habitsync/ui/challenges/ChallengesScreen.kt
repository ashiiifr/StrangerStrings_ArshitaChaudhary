package com.strangerstrings.habitsync.ui.challenges

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strangerstrings.habitsync.data.Challenge
import com.strangerstrings.habitsync.data.FriendUser
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
import java.io.ByteArrayOutputStream

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
    onUpdateChallenge: (
        challengeId: String,
        name: String,
        rule: String,
        durationDays: Int,
        category: HabitCategory,
        inviteUsernames: List<String>,
    ) -> Unit,
    onDeleteChallenge: (String) -> Unit,
    onOpenChallengeRoom: (challengeId: String) -> Unit,
    onCloseChallengeRoom: () -> Unit,
    onChatInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onMarkChallengeDone: (ByteArray?) -> Unit,
    prefillInviteUsername: String?,
    onPrefillInviteConsumed: () -> Unit,
    openCreateDialogToken: Int = 0,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingChallenge by remember { mutableStateOf<Challenge?>(null) }
    var showProofOptions by remember { mutableStateOf(false) }
    var challengeName by remember { mutableStateOf("") }
    var challengeRule by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableIntStateOf(14) }
    var selectedCategory by remember { mutableStateOf(HabitCategory.FITNESS) }
    var selectedInviteUsernames by remember { mutableStateOf(setOf<String>()) }
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        showProofOptions = false
        val proofBytes = uri?.let { readBytesFromUri(context, it) } ?: return@rememberLauncherForActivityResult
        onMarkChallengeDone(proofBytes)
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap: Bitmap? ->
        showProofOptions = false
        val proofBytes = bitmap?.toJpegByteArray() ?: return@rememberLauncherForActivityResult
        onMarkChallengeDone(proofBytes)
    }

    LaunchedEffect(prefillInviteUsername) {
        val username = prefillInviteUsername.orEmpty().trim()
        if (username.isNotBlank()) {
            selectedInviteUsernames = selectedInviteUsernames + username
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCreateDialog = true },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(OrangeGlow, AmberDeep)),
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Cream.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = CharcoalDark,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Create Challenge",
                            color = CharcoalDark,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = "Start a streak battle with friends",
                            color = CharcoalDark.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        text = "Open",
                        color = CharcoalDark,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
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
        ModalBottomSheet(
            onDismissRequest = { showCreateDialog = false },
            containerColor = CharcoalMid,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (editingChallenge == null) "Create Challenge" else "Edit Challenge",
                        fontWeight = FontWeight.ExtraBold,
                        color = Cream,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        if (editingChallenge == null) {
                            "Set the goal, pick the duration, and invite your circle."
                        } else {
                            "Update the goal, edit members, or remove the challenge entirely."
                        },
                        color = Cream.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalLight),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Challenge details",
                            color = Cream,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        OutlinedTextField(
                            value = challengeName,
                            onValueChange = { challengeName = it },
                            label = { Text("Challenge name") },
                            placeholder = { Text("Spring Reading Race") },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = challengeFieldColors(),
                        )
                        OutlinedTextField(
                            value = challengeRule,
                            onValueChange = { challengeRule = it },
                            label = { Text("Rule") },
                            placeholder = { Text("Read every day") },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = challengeFieldColors(),
                        )
                        FriendInviteSelector(
                            friends = uiState.friends,
                            selectedUsernames = selectedInviteUsernames,
                            onToggleUsername = { username ->
                                selectedInviteUsernames = if (username in selectedInviteUsernames) {
                                    selectedInviteUsernames - username
                                } else {
                                    selectedInviteUsernames + username
                                }
                            },
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalLight),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "Duration",
                            color = Cream,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            listOf(7, 14, 30).forEach { days ->
                                val selected = selectedDuration == days
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedDuration = days },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) OrangeGlow else CharcoalMid,
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(
                                            text = "$days",
                                            color = if (selected) CharcoalDark else Cream,
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.titleLarge,
                                        )
                                        Text(
                                            text = "days",
                                            color = if (selected) CharcoalDark.copy(alpha = 0.72f) else Cream.copy(alpha = 0.62f),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalLight),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "Category",
                            color = Cream,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            userScrollEnabled = false,
                        ) {
                            items(HabitCategory.entries) { category ->
                                val selected = selectedCategory == category
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedCategory = category },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) OrangeGlow else CharcoalMid,
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            text = category.name.lowercase().replaceFirstChar { it.uppercase() },
                                            color = if (selected) CharcoalDark else Cream,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = when (category) {
                                                HabitCategory.FITNESS -> "Move together"
                                                HabitCategory.READING -> "Pages and progress"
                                                HabitCategory.HYDRATION -> "Daily water goal"
                                                HabitCategory.SLEEP -> "Sleep consistency"
                                                HabitCategory.CUSTOM -> "Anything custom"
                                            },
                                            color = if (selected) CharcoalDark.copy(alpha = 0.7f) else Cream.copy(alpha = 0.58f),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val invites = selectedInviteUsernames.toList()
                        val editing = editingChallenge
                        if (editing == null) {
                            onCreateChallenge(
                                challengeName,
                                challengeRule,
                                selectedDuration,
                                selectedCategory,
                                invites,
                            )
                        } else {
                            onUpdateChallenge(
                                editing.id,
                                challengeName,
                                challengeRule,
                                selectedDuration,
                                selectedCategory,
                                invites,
                            )
                        }
                        showCreateDialog = false
                        editingChallenge = null
                        challengeName = ""
                        challengeRule = ""
                        selectedInviteUsernames = emptySet()
                        selectedDuration = 14
                        selectedCategory = HabitCategory.FITNESS
                    },
                    enabled = !uiState.isCreating,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeGlow,
                        contentColor = CharcoalDark,
                    ),
                ) {
                    Text(
                        if (uiState.isCreating) {
                            if (editingChallenge == null) "Creating..." else "Saving..."
                        } else {
                            if (editingChallenge == null) "Create Challenge" else "Save Challenge"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        editingChallenge = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Cancel", color = Cream.copy(alpha = 0.72f))
                }
            }
        }
    }

    // ── Challenge room bottom sheet ──
    if (uiState.selectedChallenge != null) {
        val currentParticipant = uiState.participantLeaderboard.firstOrNull { it.userId == uiState.currentUserId }
        val isEnded = uiState.selectedChallenge.endsAtMillis <= System.currentTimeMillis()
        val challengeRoomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onCloseChallengeRoom,
            containerColor = CharcoalDark,
            sheetState = challengeRoomSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.96f)
                    .imePadding()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = uiState.selectedChallenge.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Cream,
                    fontWeight = FontWeight.Bold,
                )
                if (uiState.selectedChallenge.creatorUserId == uiState.currentUserId) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = {
                                val challenge = uiState.selectedChallenge
                                if (challenge != null) {
                                    editingChallenge = challenge
                                    challengeName = challenge.name
                                    challengeRule = challenge.rule
                                    selectedDuration = challenge.durationDays
                                    selectedCategory = challenge.category
                                    selectedInviteUsernames = uiState.friends
                                        .filter { it.userId in challenge.participantIds && it.userId != uiState.currentUserId }
                                        .map { it.username }
                                        .toSet()
                                    showCreateDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CharcoalLight, contentColor = Cream),
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Edit")
                        }
                        Button(
                            onClick = { onDeleteChallenge(uiState.selectedChallenge.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Delete")
                        }
                    }
                }

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
                                onClick = { showProofOptions = true },
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
                        if (!currentParticipant?.proofImageUrl.isNullOrBlank()) {
                            Text(
                                text = "Proof saved for today's challenge",
                                style = MaterialTheme.typography.bodySmall,
                                color = Cream.copy(alpha = 0.7f),
                            )
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
                        .weight(1f, fill = false)
                        .heightIn(min = 220.dp, max = 480.dp),
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

    if (showProofOptions) {
        ModalBottomSheet(
            onDismissRequest = { showProofOptions = false },
            containerColor = CharcoalMid,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Mark Challenge Done", style = MaterialTheme.typography.headlineSmall, color = Cream, fontWeight = FontWeight.Bold)
                Text("Complete this challenge with proof or without proof.", color = Cream.copy(alpha = 0.7f))
                Button(
                    onClick = { cameraLauncher.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeGlow, contentColor = CharcoalDark),
                ) { Text("Take Photo", fontWeight = FontWeight.SemiBold) }
                Button(
                    onClick = { galleryPickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldSoft, contentColor = CharcoalDark),
                ) { Text("Upload from Gallery", fontWeight = FontWeight.SemiBold) }
                TextButton(
                    onClick = {
                        showProofOptions = false
                        onMarkChallengeDone(null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Mark Without Proof", color = Cream) }
            }
        }
    }
}

@Composable
private fun challengeFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = CharcoalMid,
    unfocusedContainerColor = CharcoalMid,
    disabledContainerColor = CharcoalMid,
    focusedBorderColor = OrangeGlow,
    unfocusedBorderColor = Cream.copy(alpha = 0.12f),
    focusedLabelColor = OrangeGlow,
    unfocusedLabelColor = Cream.copy(alpha = 0.7f),
    focusedTextColor = Cream,
    unfocusedTextColor = Cream,
    cursorColor = OrangeGlow,
    focusedPlaceholderColor = Cream.copy(alpha = 0.36f),
    unfocusedPlaceholderColor = Cream.copy(alpha = 0.36f),
)

private fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.getOrNull()
}

private fun Bitmap.toJpegByteArray(): ByteArray {
    val outputStream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
    return outputStream.toByteArray()
}

@Composable
private fun FriendInviteSelector(
    friends: List<FriendUser>,
    selectedUsernames: Set<String>,
    onToggleUsername: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Invite friends",
            color = Cream,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CharcoalMid),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (selectedUsernames.isEmpty()) "Choose friends" else "${selectedUsernames.size} selected",
                        color = Cream,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (selectedUsernames.isEmpty()) {
                            "Only your friends can be invited"
                        } else {
                            selectedUsernames.joinToString(", ")
                        },
                        color = Cream.copy(alpha = 0.62f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Icon(Icons.Default.ExpandMore, contentDescription = null, tint = Cream.copy(alpha = 0.8f))
            }
        }
        AnimatedVisibility(visible = expanded) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalMid),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (friends.isEmpty()) {
                    Text(
                        text = "Add friends first to invite them into challenges.",
                        color = Cream.copy(alpha = 0.7f),
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        friends.forEach { friend ->
                            val selected = friend.username in selectedUsernames
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleUsername(friend.username) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) OrangeGlow else CharcoalLight,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text(
                                            text = friend.displayName,
                                            color = if (selected) CharcoalDark else Cream,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = "@${friend.username}",
                                            color = if (selected) CharcoalDark.copy(alpha = 0.72f) else Cream.copy(alpha = 0.62f),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    Text(
                                        text = if (selected) "Selected" else "Invite",
                                        color = if (selected) CharcoalDark else GoldSoft,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
