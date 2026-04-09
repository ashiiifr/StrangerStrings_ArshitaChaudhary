package com.strangerstrings.habitsync.ui.home

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strangerstrings.habitsync.data.Habit
import com.strangerstrings.habitsync.data.HabitCategory
import com.strangerstrings.habitsync.ui.theme.AmberDeep
import com.strangerstrings.habitsync.ui.theme.CharcoalDark
import com.strangerstrings.habitsync.ui.theme.CharcoalLight
import com.strangerstrings.habitsync.ui.theme.CharcoalMid
import com.strangerstrings.habitsync.ui.theme.Cream
import com.strangerstrings.habitsync.ui.theme.CreamDark
import com.strangerstrings.habitsync.ui.theme.GoldSoft
import com.strangerstrings.habitsync.ui.theme.OrangeGlow
import com.strangerstrings.habitsync.viewmodel.HomeUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeOverviewScreen(
    uiState: HomeUiState,
    onMarkHabitDone: (String) -> Unit,
    onAddHabit: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val doneCount = uiState.habits.count { it.isCompletedToday }
    val totalCount = uiState.habits.size
    val progress = if (totalCount == 0) 0f else doneCount.toFloat() / totalCount.toFloat()

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
                        onMarkDone = { onMarkHabitDone(habit.id) },
                    )
                }
            }
        }
    }
}

// ── HERO CARD ───────────────────────────────────────────────────────
@Composable
private fun TodayHeroCard(
    doneCount: Int,
    totalCount: Int,
    progress: Float,
    freezeTokens: Int,
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
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Cream,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "🔥 ${habit.streak} day streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = OrangeGlow,
                    )
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
                enabled = !habit.isCompletedToday,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (habit.isCompletedToday) CharcoalLight else OrangeGlow,
                    contentColor = if (habit.isCompletedToday) GoldSoft else CharcoalDark,
                    disabledContainerColor = CharcoalLight,
                    disabledContentColor = GoldSoft.copy(alpha = 0.6f),
                ),
            ) {
                Text(
                    text = if (habit.isCompletedToday) "✓ Completed Today" else "Mark Done",
                    fontWeight = FontWeight.SemiBold,
                )
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
