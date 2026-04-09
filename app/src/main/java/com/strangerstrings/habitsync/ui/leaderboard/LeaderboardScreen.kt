package com.strangerstrings.habitsync.ui.leaderboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strangerstrings.habitsync.data.LeaderboardFilter
import com.strangerstrings.habitsync.data.LeaderboardUser
import com.strangerstrings.habitsync.ui.theme.CharcoalDark
import com.strangerstrings.habitsync.ui.theme.CharcoalMid
import com.strangerstrings.habitsync.ui.theme.Cream
import com.strangerstrings.habitsync.ui.theme.HabitSyncTheme
import com.strangerstrings.habitsync.ui.theme.MedalBronze
import com.strangerstrings.habitsync.ui.theme.MedalGold
import com.strangerstrings.habitsync.ui.theme.MedalSilver
import com.strangerstrings.habitsync.ui.theme.OrangeGlow
import com.strangerstrings.habitsync.viewmodel.LeaderboardUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun LeaderboardScreen(
    uiState: LeaderboardUiState,
    onBackClick: () -> Unit,
    onFilterSelected: (LeaderboardFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Leaderboard",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
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
                CircularProgressIndicator(color = OrangeGlow)
            }

            if (!uiState.isLoading) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    uiState.errorMessage?.let { message ->
                        item {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }

                    item {
                        LeaderboardFilterRow(
                            selectedFilter = uiState.selectedFilter,
                            onFilterSelected = onFilterSelected,
                        )
                    }

                    itemsIndexed(uiState.users, key = { _, user -> user.userId }) { index, user ->
                        // Staggered entrance animations
                        var visible by remember(user.userId) { mutableStateOf(false) }
                        LaunchedEffect(user.userId) {
                            kotlinx.coroutines.delay((80L + index * 60L).coerceAtMost(400L))
                            visible = true
                        }
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(300)) + slideInVertically(
                                initialOffsetY = { it / 3 },
                                animationSpec = tween(350, easing = FastOutSlowInEasing),
                            ),
                        ) {
                            LeaderboardRow(rank = index + 1, user = user)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    rank: Int,
    user: LeaderboardUser,
    modifier: Modifier = Modifier,
) {
    val isTop3 = rank <= 3
    val medalColor = when (rank) {
        1 -> MedalGold
        2 -> MedalSilver
        3 -> MedalBronze
        else -> null
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTop3) CharcoalMid else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isTop3) 6.dp else 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            medalColor?.copy(alpha = 0.2f) ?: MaterialTheme.colorScheme.surfaceContainerLow,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = medalColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isTop3) Cream else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "@${user.username.ifBlank { "habitsync" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isTop3) Cream.copy(alpha = 0.68f) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = OrangeGlow,
                )
                Text(
                    text = user.score.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isTop3) OrangeGlow else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (user.activeHabitsCount > 0 || user.bestHabitTitle.isNotBlank()) {
            Text(
                text = buildString {
                    append("${user.activeHabitsCount} active habits")
                    if (user.bestHabitTitle.isNotBlank()) {
                        append(" • Best: ${user.bestHabitTitle}")
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp).padding(bottom = 14.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (isTop3) Cream.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LeaderboardFilterRow(
    selectedFilter: LeaderboardFilter,
    onFilterSelected: (LeaderboardFilter) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Categories",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LeaderboardFilter.entries.forEach { filter ->
                AssistChip(
                    onClick = { onFilterSelected(filter) },
                    modifier = Modifier.width(92.dp),
                    label = { Text(compactFilterLabel(filter), maxLines = 1) },
                    shape = RoundedCornerShape(18.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selectedFilter == filter) OrangeGlow else MaterialTheme.colorScheme.surfaceContainerHigh,
                        labelColor = if (selectedFilter == filter) CharcoalDark else MaterialTheme.colorScheme.onSurface,
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = leaderboardFilterIcon(filter),
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                    },
                )
            }
        }
    }
}

private fun compactFilterLabel(filter: LeaderboardFilter): String {
    return when (filter) {
        LeaderboardFilter.OVERALL -> "Overall"
        LeaderboardFilter.READING -> "Read"
        LeaderboardFilter.WRITING -> "Write"
        LeaderboardFilter.STUDYING -> "Study"
        LeaderboardFilter.DRINK_WATER -> "Water"
        LeaderboardFilter.RUNNING -> "Run"
        LeaderboardFilter.WALKING -> "Walk"
        LeaderboardFilter.SLEEP_EARLY -> "Sleep"
        LeaderboardFilter.MEDITATION -> "Meditate"
        LeaderboardFilter.WORKOUT -> "Workout"
    }
}

private fun leaderboardFilterIcon(filter: LeaderboardFilter): ImageVector {
    return when (filter) {
        LeaderboardFilter.OVERALL -> Icons.Default.EmojiEvents
        LeaderboardFilter.READING -> Icons.Default.MenuBook
        LeaderboardFilter.WRITING -> Icons.Default.Create
        LeaderboardFilter.STUDYING -> Icons.Default.AutoStories
        LeaderboardFilter.DRINK_WATER -> Icons.Default.Opacity
        LeaderboardFilter.RUNNING -> Icons.Default.DirectionsRun
        LeaderboardFilter.WALKING -> Icons.Default.DirectionsWalk
        LeaderboardFilter.SLEEP_EARLY -> Icons.Default.NightsStay
        LeaderboardFilter.MEDITATION -> Icons.Default.SelfImprovement
        LeaderboardFilter.WORKOUT -> Icons.Default.MonitorHeart
    }
}

@Preview(showBackground = true)
@Composable
private fun LeaderboardPreview() {
    HabitSyncTheme {
        LeaderboardScreen(
            uiState = LeaderboardUiState(
                users = listOf(
                    LeaderboardUser("1", "Mudit", "mudit", 25, 4, "Reading", 0),
                    LeaderboardUser("2", "Arshita", "arshita", 19, 3, "Running", 0),
                    LeaderboardUser("3", "Aditi", "aditi", 16, 2, "Hydration", 0),
                ),
            ),
            onBackClick = {},
            onFilterSelected = {},
        )
    }
}
