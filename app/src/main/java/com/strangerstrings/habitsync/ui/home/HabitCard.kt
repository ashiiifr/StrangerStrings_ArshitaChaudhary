package com.strangerstrings.habitsync.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.strangerstrings.habitsync.data.Habit
import com.strangerstrings.habitsync.ui.theme.HabitSyncTheme

@Composable
fun HabitCard(
    habit: Habit,
    onMarkDoneClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    streakIcon: ImageVector = Icons.Default.LocalFireDepartment,
    isUploading: Boolean = false,
) {
    val buttonInteractionSource = remember { MutableInteractionSource() }
    val buttonPressed by buttonInteractionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (buttonPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "habit_button_press_scale",
    )
    val completionScale = remember(habit.id) { Animatable(1f) }
    val streakScale = remember(habit.id) { Animatable(1f) }
    var previousCompleted by remember(habit.id) { mutableStateOf(habit.isCompletedToday) }
    var previousStreak by remember(habit.id) { mutableIntStateOf(habit.streak) }
    var showFullProof by remember(habit.id) { mutableStateOf(false) }

    LaunchedEffect(habit.isCompletedToday) {
        if (habit.isCompletedToday && !previousCompleted) {
            completionScale.animateTo(
                targetValue = 1.08f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
            completionScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
        previousCompleted = habit.isCompletedToday
    }

    LaunchedEffect(habit.streak) {
        if (habit.streak > previousStreak) {
            streakScale.snapTo(0.94f)
            streakScale.animateTo(
                targetValue = 1.12f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
            streakScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
        previousStreak = habit.streak
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = streakIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    AnimatedContent(
                        targetState = habit.streak,
                        transitionSpec = { streakTransform() },
                        label = "streak_value_animation",
                    ) { animatedStreak ->
                        Text(
                            text = "$animatedStreak day streak",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.graphicsLayer(
                                scaleX = streakScale.value,
                                scaleY = streakScale.value,
                            ),
                        )
                    }
                }

                if (!habit.proofImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = habit.proofImageUrl,
                        contentDescription = "Proof image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showFullProof = true },
                    )
                }
            }

            Button(
                onClick = { onMarkDoneClick(habit.id) },
                enabled = !habit.isCompletedToday && !isUploading,
                interactionSource = buttonInteractionSource,
                modifier = Modifier.graphicsLayer(
                    scaleX = pressScale * completionScale.value,
                    scaleY = pressScale * completionScale.value,
                ),
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(text = if (habit.isCompletedToday) "Completed" else "Mark Done")
                }
            }
        }
    }

    if (showFullProof && !habit.proofImageUrl.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showFullProof = false },
            title = { Text("Proof") },
            text = {
                AsyncImage(
                    model = habit.proofImageUrl,
                    contentDescription = "Full proof image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = { showFullProof = false }) {
                    Text("Close")
                }
            },
        )
    }
}

private fun streakTransform(): ContentTransform {
    return (fadeIn() + slideInVertically(initialOffsetY = { fullHeight -> fullHeight / 2 }))
        .togetherWith(fadeOut() + slideOutVertically(targetOffsetY = { fullHeight -> -fullHeight / 2 }))
}

@Preview(showBackground = true)
@Composable
private fun HabitCardPreview() {
    HabitSyncTheme {
        HabitCard(
            habit = Habit(
                id = "habit_1",
                title = "Morning Run",
                streak = 8,
                isCompletedToday = false,
            ),
            onMarkDoneClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
