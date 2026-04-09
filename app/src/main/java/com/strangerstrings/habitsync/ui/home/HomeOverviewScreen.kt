package com.strangerstrings.habitsync.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.strangerstrings.habitsync.data.Habit
import com.strangerstrings.habitsync.data.HabitCategory
import com.strangerstrings.habitsync.data.HabitCompletionRecord
import com.strangerstrings.habitsync.data.HabitType
import com.strangerstrings.habitsync.ui.theme.AmberDeep
import com.strangerstrings.habitsync.ui.theme.CharcoalDark
import com.strangerstrings.habitsync.ui.theme.CharcoalLight
import com.strangerstrings.habitsync.ui.theme.CharcoalMid
import com.strangerstrings.habitsync.ui.theme.Cream
import com.strangerstrings.habitsync.ui.theme.CreamDark
import com.strangerstrings.habitsync.ui.theme.GoldSoft
import com.strangerstrings.habitsync.ui.theme.OrangeGlow
import com.strangerstrings.habitsync.viewmodel.HomeUiState
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeOverviewScreen(
    uiState: HomeUiState,
    onMarkHabitDone: (String, ByteArray?) -> Unit,
    onUpdateCustomHabit: (String, String, String, String) -> Unit,
    onDeleteCustomHabit: (String) -> Unit,
    onAddHabit: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val doneCount = uiState.habits.count { it.isCompletedToday }
    val totalCount = uiState.habits.size
    val progress = if (totalCount == 0) 0f else doneCount.toFloat() / totalCount.toFloat()
    var pendingProofHabitId by remember { mutableStateOf<String?>(null) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var editedTitle by remember { mutableStateOf("") }
    var editedTarget by remember { mutableStateOf("") }
    var editedNote by remember { mutableStateOf("") }
    var showHistorySheet by remember { mutableStateOf(false) }
    var selectedHistoryDay by remember { mutableStateOf(currentEpochDay()) }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        val habitId = pendingProofHabitId ?: return@rememberLauncherForActivityResult
        pendingProofHabitId = null
        val proofBytes = uri?.let { readBytesFromUri(context, it) } ?: return@rememberLauncherForActivityResult
        onMarkHabitDone(habitId, proofBytes)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap: Bitmap? ->
        val habitId = pendingProofHabitId ?: return@rememberLauncherForActivityResult
        pendingProofHabitId = null
        val proofBytes = bitmap?.toJpegByteArray() ?: return@rememberLauncherForActivityResult
        onMarkHabitDone(habitId, proofBytes)
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Hero card with entrance animation
        item(key = "hero") {
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400)) + slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = tween(500, easing = FastOutSlowInEasing),
                ),
            ) {
                TodayHeroCard(
                    doneCount = doneCount,
                    totalCount = totalCount,
                    progress = progress,
                    freezeTokens = uiState.freezeTokensThisMonth,
                    onHistoryClick = { showHistorySheet = true },
                )
            }
        }

        // Section label with entrance animation
        item(key = "section_label") {
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(120)
                visible = true
            }
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(300)) + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                ),
            ) {
                Text(
                    text = "PREVIOUS WORKOUTS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing,
                )
            }
        }

        uiState.infoMessage?.let { info ->
            item {
                Text(
                    text = info,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }

        uiState.errorMessage?.let { message ->
            item {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        if (uiState.habits.isEmpty()) {
            item {
                EmptyHabitCard(onAddHabit = onAddHabit)
            }
        } else {
            // Staggered entrance animations for habit cards
            itemsIndexed(uiState.habits, key = { _, h -> h.id }) { index, habit ->
                var visible by remember(habit.id) { mutableStateOf(false) }
                LaunchedEffect(habit.id) {
                    kotlinx.coroutines.delay((180L + index * 80L).coerceAtMost(600L))
                    visible = true
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(350)) + slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = tween(400, easing = FastOutSlowInEasing),
                    ),
                ) {
                    HabitTodayCard(
                        habit = habit,
                        onMarkDone = { pendingProofHabitId = habit.id },
                        isUploading = uiState.uploadingHabitId == habit.id,
                        onEditCustomHabit = {
                            editingHabit = habit
                            editedTitle = habit.title
                            editedTarget = habit.target
                            editedNote = habit.note
                        },
                    )
                }
            }
        }
    }

    if (pendingProofHabitId != null) {
        ModalBottomSheet(
            onDismissRequest = { pendingProofHabitId = null },
            containerColor = CharcoalMid,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Mark Habit Done", style = MaterialTheme.typography.headlineSmall, color = Cream, fontWeight = FontWeight.Bold)
                Text("Complete this habit with proof or without proof.", color = Cream.copy(alpha = 0.7f))
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
                        val habitId = pendingProofHabitId
                        pendingProofHabitId = null
                        if (habitId != null) onMarkHabitDone(habitId, null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Mark Without Proof", color = Cream) }
            }
        }
    }

    if (editingHabit != null) {
        ModalBottomSheet(
            onDismissRequest = { editingHabit = null },
            containerColor = CharcoalMid,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val habitBeingEdited = editingHabit
                val isCustomHabit = habitBeingEdited?.type == HabitType.OTHER
                Text(
                    if (isCustomHabit) "Edit Custom Habit" else "Edit Habit Details",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Cream,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (isCustomHabit) {
                        "Update the name, target, duration, or note for this custom habit."
                    } else {
                        "Default habits keep their name, but you can still update duration, note, or delete them."
                    },
                    color = Cream.copy(alpha = 0.7f),
                )
                if (isCustomHabit) {
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Habit name") },
                        singleLine = true,
                    )
                } else {
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Habit name") },
                        singleLine = true,
                        enabled = false,
                    )
                }
                OutlinedTextField(
                    value = editedTarget,
                    onValueChange = { editedTarget = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Target or duration") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = editedNote,
                    onValueChange = { editedNote = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note") },
                    minLines = 3,
                    maxLines = 5,
                )
                Button(
                    onClick = {
                        val habitId = editingHabit?.id
                        editingHabit = null
                        if (habitId != null) {
                            onUpdateCustomHabit(habitId, editedTitle, editedTarget, editedNote)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeGlow, contentColor = CharcoalDark),
                ) {
                    Text("Save Changes", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = {
                        val habitId = editingHabit?.id
                        editingHabit = null
                        if (habitId != null) onDeleteCustomHabit(habitId)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete Habit", fontWeight = FontWeight.SemiBold)
                }
                TextButton(
                    onClick = { editingHabit = null },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Cancel", color = Cream)
                }
            }
        }
    }

    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            containerColor = CharcoalMid,
        ) {
            HabitHistorySheet(
                habits = uiState.habits,
                selectedDay = selectedHistoryDay,
                onSelectDay = { selectedHistoryDay = it },
            )
        }
    }

    if (uiState.uploadingHabitId != null) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            containerColor = CharcoalMid,
            textContentColor = Cream,
            titleContentColor = Cream,
            title = { Text("Processing proof") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CircularProgressIndicator(
                        color = OrangeGlow,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(26.dp),
                    )
                    Text(
                        "Uploading your image and saving the habit. Please wait a moment.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Cream.copy(alpha = 0.82f),
                    )
                }
            },
        )
    }
}

// ── HERO CARD ───────────────────────────────────────────────────────
@Composable
private fun TodayHeroCard(
    doneCount: Int,
    totalCount: Int,
    progress: Float,
    freezeTokens: Int,
    onHistoryClick: () -> Unit,
) {
    val todayDate = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date()).uppercase()

    // Animated progress bar
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "hero_progress",
    )

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(CreamDark, Cream),
                    ),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = todayDate,
                    style = MaterialTheme.typography.labelLarge,
                    color = CharcoalDark.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onHistoryClick) {
                        Text("History", color = AmberDeep, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text = "🔥 $freezeTokens",
                        style = MaterialTheme.typography.labelLarge,
                        color = AmberDeep,
                    )
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Menu",
                        tint = CharcoalDark.copy(alpha = 0.4f),
                    )
                }
            }

            // Wave chart area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                OrangeGlow.copy(alpha = 0.25f),
                                GoldSoft.copy(alpha = 0.35f),
                                OrangeGlow.copy(alpha = 0.12f),
                            ),
                        ),
                        RoundedCornerShape(22.dp),
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "$doneCount of $totalCount",
                            style = MaterialTheme.typography.headlineMedium,
                            color = CharcoalDark,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = "Habits Done",
                            style = MaterialTheme.typography.bodySmall,
                            color = CharcoalDark.copy(alpha = 0.65f),
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    MetricColumn(
                        label = "Completion",
                        value = "${(progress * 100).toInt()}%",
                    )
                    MetricColumn(
                        label = "Avg.",
                        value = if (totalCount == 0) "0" else "${((doneCount.toFloat() / totalCount) * 10).toInt()}/10",
                    )
                }
            }

            // Animated progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(CharcoalDark.copy(alpha = 0.1f), CircleShape),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(8.dp)
                        .background(
                            Brush.horizontalGradient(listOf(OrangeGlow, AmberDeep)),
                            CircleShape,
                        ),
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(
    label: String,
    value: String,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = CharcoalDark,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = CharcoalDark.copy(alpha = 0.6f),
        )
    }
}

// ── EMPTY STATE ─────────────────────────────────────────────────────
@Composable
private fun EmptyHabitCard(
    onAddHabit: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalMid),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "No habits yet",
                style = MaterialTheme.typography.titleLarge,
                color = Cream,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Create your first daily habit to start building streaks.",
                color = Cream.copy(alpha = 0.8f),
            )
            Button(
                onClick = onAddHabit,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeGlow,
                    contentColor = CharcoalDark,
                ),
            ) {
                Text("Create Habit", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── HABIT CARD (Previous Workouts style) ────────────────────────────
@Composable
private fun HabitTodayCard(
    habit: Habit,
    onMarkDone: () -> Unit,
    isUploading: Boolean,
    onEditCustomHabit: (() -> Unit)?,
) {
    // Animate checkmark state change
    val cardScale = remember(habit.id) { Animatable(1f) }

    LaunchedEffect(habit.isCompletedToday) {
        if (habit.isCompletedToday) {
            cardScale.animateTo(
                0.97f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
            )
            cardScale.animateTo(
                1f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
            )
        }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalMid),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.graphicsLayer {
            scaleX = cardScale.value
            scaleY = cardScale.value
        },
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = habit.category.name.lowercase().replaceFirstChar { it.uppercase() }.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = GoldSoft.copy(alpha = 0.7f),
                letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(OrangeGlow.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = categoryIcon(habit.category),
                        contentDescription = habit.category.name,
                        tint = OrangeGlow,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Cream,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (onEditCustomHabit != null) {
                            IconButton(
                                onClick = onEditCustomHabit,
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "Edit custom habit",
                                    tint = GoldSoft,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                    Text(
                        text = "🔥 ${habit.streak} day streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = OrangeGlow,
                    )
                    if (habit.target.isNotBlank() || habit.note.isNotBlank()) {
                        Text(
                            text = listOf(habit.target, habit.note).filter { it.isNotBlank() }.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Cream.copy(alpha = 0.62f),
                        )
                    }
                }
            }

            // 7-day completion dots with animated appearance
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                last7Completion(habit).forEachIndexed { idx, done ->
                    val dotScale = remember { Animatable(0f) }
                    LaunchedEffect(habit.id, idx) {
                        kotlinx.coroutines.delay(idx * 40L)
                        dotScale.animateTo(
                            1f,
                            animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .graphicsLayer {
                                scaleX = dotScale.value
                                scaleY = dotScale.value
                            }
                            .background(
                                color = if (done) OrangeGlow else Cream.copy(alpha = 0.15f),
                                shape = CircleShape,
                            ),
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${last7Completion(habit).count { it }}/7 this week",
                    style = MaterialTheme.typography.labelSmall,
                    color = Cream.copy(alpha = 0.5f),
                )
            }

            Button(
                onClick = onMarkDone,
                enabled = !habit.isCompletedToday && !isUploading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (habit.isCompletedToday) CharcoalLight else OrangeGlow,
                    contentColor = if (habit.isCompletedToday) GoldSoft else CharcoalDark,
                    disabledContainerColor = CharcoalLight,
                    disabledContentColor = GoldSoft.copy(alpha = 0.6f),
                ),
                ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            color = CharcoalDark,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = when {
                            isUploading -> "Processing..."
                            habit.isCompletedToday -> "✓ Completed Today"
                            else -> "Mark Done"
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            if (!habit.proofImageUrl.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AsyncImage(
                        model = habit.proofImageUrl,
                        contentDescription = "Habit proof",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                    Text(
                        text = "Proof saved for today",
                        style = MaterialTheme.typography.bodySmall,
                        color = Cream.copy(alpha = 0.72f),
                    )
                }
            }
        }
    }
}

private fun last7Completion(habit: Habit): List<Boolean> {
    val today = System.currentTimeMillis() / (24 * 60 * 60 * 1000L)
    val set = habit.completionDates.toSet()
    return (6L downTo 0L).map { dayOffset ->
        set.contains(today - dayOffset)
    }
}

private fun categoryIcon(category: HabitCategory): ImageVector {
    return when (category) {
        HabitCategory.FITNESS -> Icons.Default.DirectionsRun
        HabitCategory.READING -> Icons.Default.MenuBook
        HabitCategory.HYDRATION -> Icons.Default.Opacity
        HabitCategory.SLEEP -> Icons.Default.Bedtime
        HabitCategory.CUSTOM -> Icons.Default.Tune
    }
}

@Composable
private fun HabitHistorySheet(
    habits: List<Habit>,
    selectedDay: Long,
    onSelectDay: (Long) -> Unit,
) {
    val monthDays = remember { currentMonthEpochDays() }
    val completionCounts = remember(habits) {
        habits.flatMap { habit ->
            habit.completionHistory.map { it.epochDay }
        }.groupingBy { it }.eachCount()
    }
    val selectedEntries = remember(habits, selectedDay) {
        habits.flatMap { habit ->
            habit.completionHistory
                .filter { it.epochDay == selectedDay }
                .map { record -> habit to record }
        }.sortedByDescending { it.second.completedAt }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Habit History", style = MaterialTheme.typography.headlineSmall, color = Cream, fontWeight = FontWeight.Bold)
        }
        item {
            Text("Tap a date to see the habits you completed and the proof you uploaded that day.", color = Cream.copy(alpha = 0.7f))
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                monthDays.chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            val isSelected = day == selectedDay
                            val completionCount = completionCounts[day] ?: 0
                            Card(
                                onClick = { onSelectDay(day) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) OrangeGlow else CharcoalLight,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = dayOfMonthLabel(day),
                                        color = if (isSelected) CharcoalDark else Cream,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = if (completionCount > 0) "$completionCount done" else "0",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) CharcoalDark.copy(alpha = 0.7f) else Cream.copy(alpha = 0.55f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Text(
                text = "Completed on ${formattedDay(selectedDay)}",
                style = MaterialTheme.typography.titleMedium,
                color = Cream,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (selectedEntries.isEmpty()) {
            item {
                Text("No habits completed on this date.", color = Cream.copy(alpha = 0.6f))
            }
        } else {
            itemsIndexed(selectedEntries, key = { index, entry -> "${entry.first.id}_${entry.second.epochDay}_$index" }) { _, entry ->
                HistoryHabitCard(habit = entry.first, record = entry.second)
            }
        }
    }
}

@Composable
private fun HistoryHabitCard(
    habit: Habit,
    record: HabitCompletionRecord,
) {
    var showProof by remember(record.epochDay, habit.id) { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalLight),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(habit.title, color = Cream, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = buildString {
                    append(formattedTimestamp(record.completedAt))
                    if (habit.target.isNotBlank()) append(" • ${habit.target}")
                    if (habit.note.isNotBlank()) append(" • ${habit.note}")
                },
                color = Cream.copy(alpha = 0.65f),
                style = MaterialTheme.typography.bodySmall,
            )
            if (!record.proofImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = record.proofImageUrl,
                    contentDescription = "Completion proof",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showProof = true },
                )
            } else {
                Text("Completed without proof", color = Cream.copy(alpha = 0.55f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (showProof && !record.proofImageUrl.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showProof = false },
            title = { Text(habit.title) },
            text = {
                AsyncImage(
                    model = record.proofImageUrl,
                    contentDescription = "Proof image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = { showProof = false }) { Text("Close") }
            },
        )
    }
}

private fun currentEpochDay(): Long {
    return System.currentTimeMillis() / (24L * 60L * 60L * 1000L)
}

private fun currentMonthEpochDays(): List<Long> {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val month = calendar.get(Calendar.MONTH)
    val days = mutableListOf<Long>()
    while (calendar.get(Calendar.MONTH) == month) {
        days += calendar.timeInMillis / (24L * 60L * 60L * 1000L)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
    }
    return days
}

private fun dayOfMonthLabel(epochDay: Long): String {
    return SimpleDateFormat("d", Locale.getDefault()).format(Date(epochDay * 24L * 60L * 60L * 1000L))
}

private fun formattedDay(epochDay: Long): String {
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(epochDay * 24L * 60L * 60L * 1000L))
}

private fun formattedTimestamp(timestamp: Long): String {
    return SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault()).format(Date(timestamp))
}

private fun Bitmap.toJpegByteArray(quality: Int = 90): ByteArray {
    val outputStream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    return outputStream.toByteArray()
}

private fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { input -> input.readBytes() }
    }.getOrNull()
}
