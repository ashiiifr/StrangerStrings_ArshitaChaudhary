package com.strangerstrings.habitsync.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.strangerstrings.habitsync.data.Habit
import com.strangerstrings.habitsync.data.HabitCategory
import com.strangerstrings.habitsync.viewmodel.HomeUiState

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
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 80.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "$doneCount of $totalCount habits completed today",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Freeze tokens left this month: ${uiState.freezeTokensThisMonth}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .height(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                        )
                    }
                }
            }
        }

        uiState.infoMessage?.let { info ->
            item {
                Text(
                    text = info,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
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
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("No habits yet", style = MaterialTheme.typography.titleMedium)
                        Text("Create your first daily habit to start building streaks.")
                        Button(onClick = onAddHabit) { Text("Create Habit") }
                    }
                }
            }
        } else {
            items(uiState.habits, key = { it.id }) { habit ->
                HabitTodayCard(
                    habit = habit,
                    onMarkDone = { onMarkHabitDone(habit.id) },
                )
            }
        }
    }
}

@Composable
private fun HabitTodayCard(
    habit: Habit,
    onMarkDone: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = categoryIcon(habit.category),
                    contentDescription = habit.category.name,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "🔥 ${habit.streak}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                last7Completion(habit).forEach { done ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = CircleShape,
                            ),
                    )
                }
            }

            Button(
                onClick = onMarkDone,
                enabled = !habit.isCompletedToday,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(if (habit.isCompletedToday) "Completed Today" else "✅ Mark Done")
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
