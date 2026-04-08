package com.strangerstrings.habitsync.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strangerstrings.habitsync.data.Habit
import com.strangerstrings.habitsync.ui.theme.HabitSyncTheme
import com.strangerstrings.habitsync.viewmodel.HomeUiState
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onMarkHabitDone: (String, ByteArray?) -> Unit,
    onAddHabitClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onFeedClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val fabInteractionSource = remember { MutableInteractionSource() }
    val fabPressed by fabInteractionSource.collectIsPressedAsState()
    val fabScale by animateFloatAsState(
        targetValue = if (fabPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "home_fab_scale",
    )
    var pendingProofHabitId by remember { mutableStateOf<String?>(null) }

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HabitSync",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddHabitClick,
                interactionSource = fabInteractionSource,
                modifier = Modifier.graphicsLayer(
                    scaleX = fabScale,
                    scaleY = fabScale,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add habit",
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AnimatedVisibility(
                visible = uiState.isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                CircularProgressIndicator()
            }

            AnimatedVisibility(
                visible = !uiState.isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = onLeaderboardClick,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Leaderboard,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                                Text("Leaderboard")
                            }
                            Button(
                                onClick = onFeedClick,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DynamicFeed,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                                Text("Feed")
                            }
                        }
                    }

                    if (uiState.errorMessage != null) {
                        item {
                            Text(
                                text = uiState.errorMessage,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    if (uiState.habits.isEmpty()) {
                        item {
                            EmptyHabitsState(onAddHabitClick = onAddHabitClick)
                        }
                    } else {
                        itemsIndexed(
                            items = uiState.habits,
                            key = { _, habit -> habit.id },
                        ) { index, habit ->
                            var visible by remember(habit.id) { mutableStateOf(false) }
                            LaunchedEffect(habit.id) {
                                kotlinx.coroutines.delay((index * 45L).coerceAtMost(220L))
                                visible = true
                            }

                            AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
                                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 4 }),
                            ) {
                                HabitCard(
                                    habit = habit,
                                    onMarkDoneClick = { habitId ->
                                        pendingProofHabitId = habitId
                                    },
                                    modifier = Modifier,
                                    isUploading = uiState.uploadingHabitId == habit.id,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingProofHabitId != null) {
        AlertDialog(
            onDismissRequest = { pendingProofHabitId = null },
            title = { Text("Add proof") },
            text = { Text("Would you like to upload proof for this completion?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        cameraLauncher.launch(null)
                    },
                ) {
                    Text("Take Photo")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            galleryPickerLauncher.launch("image/*")
                        },
                    ) {
                        Text("Gallery")
                    }
                    TextButton(
                        onClick = {
                            val habitId = pendingProofHabitId
                            pendingProofHabitId = null
                            if (habitId != null) {
                                onMarkHabitDone(habitId, null)
                            }
                        },
                    ) {
                        Text("Skip")
                    }
                }
            },
        )
    }
}

@Composable
private fun EmptyHabitsState(
    onAddHabitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.SelfImprovement,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = "No habits yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Start your streak journey with one small daily action.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onAddHabitClick,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Add your first habit")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HabitSyncTheme {
        HomeScreen(
            uiState = HomeUiState(
                habits = listOf(
                    Habit("habit_1", "Read 20 pages", 12, false),
                    Habit("habit_2", "Meditate", 6, true),
                    Habit("habit_3", "Drink 3L water", 3, false),
                ),
            ),
            onMarkHabitDone = { _, _ -> },
            onAddHabitClick = {},
            onLeaderboardClick = {},
            onFeedClick = {},
            onLogoutClick = {},
        )
    }
}

private fun Bitmap.toJpegByteArray(quality: Int = 90): ByteArray {
    val outputStream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
    return outputStream.toByteArray()
}

private fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        }
    }.getOrNull()
}
