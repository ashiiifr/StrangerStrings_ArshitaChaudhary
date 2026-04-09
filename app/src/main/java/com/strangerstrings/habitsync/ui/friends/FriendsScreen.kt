package com.strangerstrings.habitsync.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.strangerstrings.habitsync.data.FriendLeaderboardEntry
import com.strangerstrings.habitsync.data.FriendProfileDetails
import com.strangerstrings.habitsync.data.FriendRelationshipState
import com.strangerstrings.habitsync.data.FriendUser
import com.strangerstrings.habitsync.data.LeaderboardFilter
import com.strangerstrings.habitsync.data.SearchFriendResult
import com.strangerstrings.habitsync.ui.theme.AmberDeep
import com.strangerstrings.habitsync.ui.theme.CharcoalDark
import com.strangerstrings.habitsync.ui.theme.CharcoalLight
import com.strangerstrings.habitsync.ui.theme.CharcoalMid
import com.strangerstrings.habitsync.ui.theme.Cream
import com.strangerstrings.habitsync.ui.theme.CreamDark
import com.strangerstrings.habitsync.ui.theme.GoldSoft
import com.strangerstrings.habitsync.ui.theme.MedalBronze
import com.strangerstrings.habitsync.ui.theme.MedalGold
import com.strangerstrings.habitsync.ui.theme.MedalSilver
import com.strangerstrings.habitsync.ui.theme.OrangeGlow
import com.strangerstrings.habitsync.viewmodel.FriendsUiState
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FriendsScreen(
    uiState: FriendsUiState,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: (String) -> Unit,
    onSendFriendRequest: (String) -> Unit,
    onOpenFriendProfile: (String) -> Unit,
    onCloseFriendProfile: () -> Unit,
    onChallengeFriend: (String) -> Unit,
    onRemoveFriend: () -> Unit,
    onSelectLeaderboardFilter: (LeaderboardFilter) -> Unit,
    showAddFriendsSheet: Boolean,
    onDismissAddFriendsSheet: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showLeaderboardFilterSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        // Custom segmented tab row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 4.dp)
                .background(CharcoalMid.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SegmentButton(
                label = "Friends",
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f),
            )
            SegmentButton(
                label = "Leaderboard",
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f),
            )
        }

        if (selectedTab == 0) {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = 14.dp,
                    bottom = contentPadding.calculateBottomPadding() + 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                uiState.errorMessage?.let { message ->
                    item {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = OrangeGlow)
                        }
                    }
                }

                item {
                    SectionHeader(
                        title = "Your Connection",
                        subtitle = "${uiState.friends.size} connected",
                    )
                }
                if (uiState.friends.isEmpty()) {
                    item { EmptyFriendsCard() }
                } else {
                    items(uiState.friends, key = FriendUser::userId) { user ->
                        FriendCard(
                            user = user,
                            onOpenProfile = onOpenFriendProfile,
                        )
                    }
                }
            }
        } else {
            // Leaderboard tab
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = 14.dp,
                    bottom = contentPadding.calculateBottomPadding() + 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SectionHeader(
                        title = "Circle Leaderboard",
                        subtitle = "Scores are the sum of current streaks across active habits.",
                    )
                }
                item {
                    LeaderboardFilterPickerCard(
                        selectedFilter = uiState.selectedLeaderboardFilter,
                        onOpenSelector = { showLeaderboardFilterSheet = true },
                    )
                }
                items(uiState.leaderboard, key = FriendLeaderboardEntry::userId) { row ->
                    LeaderboardCard(row = row)
                }
            }
        }
    }

    if (uiState.isFriendProfileLoading || uiState.selectedFriendProfile != null) {
        ModalBottomSheet(
            onDismissRequest = onCloseFriendProfile,
            containerColor = CharcoalDark,
        ) {
            FriendProfileSheet(
                profile = uiState.selectedFriendProfile,
                isLoading = uiState.isFriendProfileLoading,
                onChallengeFriend = {
                    uiState.selectedFriendProfile?.username?.let(onChallengeFriend)
                    onCloseFriendProfile()
                },
                onRemoveFriend = onRemoveFriend,
            )
        }
    }

    if (showAddFriendsSheet) {
        val addFriendsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissAddFriendsSheet,
            containerColor = CharcoalDark,
            sheetState = addFriendsSheetState,
        ) {
            LazyColumn(
                modifier = Modifier.imePadding(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    AddFriendsSearchCard(
                        query = uiState.query,
                        searchMessage = uiState.searchMessage,
                        onQueryChange = onQueryChange,
                        onSearchClick = onSubmitSearch,
                    )
                }

                if (uiState.query.length >= 2) {
                    item {
                        SectionHeader(
                            title = "Search Results",
                            subtitle = "People matching @${uiState.query.trim()}",
                        )
                    }
                    if (uiState.searchResults.isEmpty() && !uiState.isLoading && uiState.errorMessage == null) {
                        item { EmptySearchCard() }
                    } else {
                        items(uiState.searchResults, key = SearchFriendResult::userId) { user ->
                            SearchResultCard(
                                user = user,
                                onAddFriend = onSendFriendRequest,
                                onOpenProfile = onOpenFriendProfile,
                            )
                        }
                    }
                } else {
                    item {
                        EmptyAddFriendSearchCard()
                    }
                }
            }
        }
    }

    if (showLeaderboardFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLeaderboardFilterSheet = false },
            containerColor = CharcoalDark,
        ) {
            LeaderboardFilterSheet(
                selectedFilter = uiState.selectedLeaderboardFilter,
                onSelectFilter = { filter ->
                    onSelectLeaderboardFilter(filter)
                    showLeaderboardFilterSheet = false
                },
            )
        }
    }
}

// ── Search Hero Card ────────────────────────────────────────────────
@Composable
private fun AddFriendsSearchCard(
    query: String,
    searchMessage: String?,
    onQueryChange: (String) -> Unit,
    onSearchClick: (String) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
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
                        colors = listOf(OrangeGlow, AmberDeep),
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Find Friends",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Search by username and build your habit circle.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonSearch,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search username", color = Color.White.copy(alpha = 0.6f)) },
                    prefix = { Text("@", color = Color.White.copy(alpha = 0.7f)) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        onSearchClick(query)
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        cursorColor = Color.White,
                    ),
                )
                Button(
                    onClick = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        onSearchClick(query)
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = AmberDeep,
                    ),
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }

            if (!searchMessage.isNullOrBlank()) {
                Text(
                    text = searchMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f),
                )
            }
        }
    }
}

@Composable
private fun SegmentButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) OrangeGlow else Color.Transparent)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) CharcoalDark else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun EmptyAddFriendSearchCard() {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalMid),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.PersonSearch,
                contentDescription = null,
                tint = OrangeGlow,
                modifier = Modifier.size(34.dp),
            )
            Text(
                text = "Search a username",
                style = MaterialTheme.typography.titleMedium,
                color = Cream,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Type an exact username and tap search to find that person.",
                color = Cream.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Cards ───────────────────────────────────────────────────────────
@Composable
private fun SearchResultCard(
    user: SearchFriendResult,
    onAddFriend: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
) {
    Card(
        onClick = { onOpenProfile(user.userId) },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarBubble(
                label = initialsFor(user.displayName, user.username),
                imageUrl = user.profileImageUrl,
                accent = OrangeGlow.copy(alpha = 0.2f),
                textColor = AmberDeep,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "@${user.username}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RelationshipLabel(user.relationshipState)
            }

            when (user.relationshipState) {
                FriendRelationshipState.NONE -> {
                    IconButton(
                        onClick = { onAddFriend(user.userId) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(OrangeGlow),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add friend",
                            tint = CharcoalDark,
                        )
                    }
                }
                FriendRelationshipState.REQUEST_SENT -> {
                    AssistChip(
                        onClick = {},
                        label = { Text("Sent") },
                        leadingIcon = { Icon(Icons.Default.HourglassTop, contentDescription = null) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = GoldSoft.copy(alpha = 0.3f)),
                    )
                }
                FriendRelationshipState.REQUEST_RECEIVED -> {
                    AssistChip(
                        onClick = {},
                        label = { Text("Check inbox") },
                        leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null) },
                    )
                }
                FriendRelationshipState.FRIEND -> {
                    AssistChip(
                        onClick = {},
                        label = { Text("Friends") },
                        leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = OrangeGlow.copy(alpha = 0.15f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendCard(
    user: FriendUser,
    onOpenProfile: (String) -> Unit,
) {
    Card(
        onClick = { onOpenProfile(user.userId) },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarBubble(
                label = initialsFor(user.displayName, user.username),
                imageUrl = user.profileImageUrl,
                accent = MaterialTheme.colorScheme.secondaryContainer,
                textColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "@${user.username}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Tap to view profile & challenge",
                    style = MaterialTheme.typography.bodySmall,
                    color = OrangeGlow,
                )
            }
        }
    }
}

// ── Leaderboard ─────────────────────────────────────────────────────
@Composable
private fun LeaderboardCard(row: FriendLeaderboardEntry) {
    val trendText = when {
        row.rankDelta > 0 -> "▲ +${row.rankDelta}"
        row.rankDelta < 0 -> "▼ ${abs(row.rankDelta)}"
        else -> "• 0"
    }
    val trendColor = when {
        row.rankDelta > 0 -> OrangeGlow
        row.rankDelta < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val medalColor = when (row.rank) {
        1 -> MedalGold
        2 -> MedalSilver
        3 -> MedalBronze
        else -> null
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (row.rank <= 3) {
                CharcoalMid
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (row.rank <= 3) 6.dp else 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            medalColor?.copy(alpha = 0.2f) ?: MaterialTheme.colorScheme.surfaceContainerHigh,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "#${row.rank}",
                        fontWeight = FontWeight.Bold,
                        color = medalColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = row.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (row.rank <= 3) Cream else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "@${row.username} • ${row.score} streak score • ${row.activeHabitsCount} active habits",
                        color = if (row.rank <= 3) Cream.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text(
                text = trendText,
                style = MaterialTheme.typography.labelLarge,
                color = trendColor,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun LeaderboardFilterPickerCard(
    selectedFilter: LeaderboardFilter,
    onOpenSelector: () -> Unit,
) {
    Card(
        onClick = onOpenSelector,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalMid),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(OrangeGlow.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = leaderboardFilterIcon(selectedFilter),
                        contentDescription = null,
                        tint = OrangeGlow,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Board",
                        style = MaterialTheme.typography.labelMedium,
                        color = Cream.copy(alpha = 0.62f),
                    )
                    Text(
                        text = selectedFilter.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = Cream,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Choose leaderboard category",
                tint = Cream.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun LeaderboardFilterSheet(
    selectedFilter: LeaderboardFilter,
    onSelectFilter: (LeaderboardFilter) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Choose board",
            style = MaterialTheme.typography.titleLarge,
            color = Cream,
            fontWeight = FontWeight.SemiBold,
        )
        LeaderboardFilter.entries.forEach { filter ->
            val selected = filter == selectedFilter
            Card(
                onClick = { onSelectFilter(filter) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) Color.Transparent else CharcoalLight,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (selected) Brush.linearGradient(listOf(OrangeGlow, AmberDeep))
                            else Brush.linearGradient(listOf(CharcoalLight, CharcoalLight)),
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) Color.White.copy(alpha = 0.18f) else OrangeGlow.copy(alpha = 0.16f),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = leaderboardFilterIcon(filter),
                                contentDescription = null,
                                tint = if (selected) Color.White else OrangeGlow,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = filter.label,
                                color = if (selected) Color.White else Cream,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = if (filter == LeaderboardFilter.OVERALL) "All habits combined" else "${filter.label} habits only",
                                color = if (selected) Color.White.copy(alpha = 0.74f) else Cream.copy(alpha = 0.58f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
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

// ── Section Header ──────────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptySearchCard() {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Text(
            text = "No matching usernames yet. Try another spelling or ask your friend for their exact handle.",
            modifier = Modifier.padding(18.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyFriendsCard() {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = OrangeGlow,
                modifier = Modifier.size(36.dp),
            )
            Text(
                text = "No friends added yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Search a username above and send your first friend request.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RelationshipLabel(state: FriendRelationshipState) {
    val text = when (state) {
        FriendRelationshipState.NONE -> "Ready to add"
        FriendRelationshipState.REQUEST_SENT -> "Request pending"
        FriendRelationshipState.REQUEST_RECEIVED -> "Sent you a request"
        FriendRelationshipState.FRIEND -> "Already friends"
    }
    val color = when (state) {
        FriendRelationshipState.NONE -> OrangeGlow
        FriendRelationshipState.REQUEST_SENT -> GoldSoft
        FriendRelationshipState.REQUEST_RECEIVED -> AmberDeep
        FriendRelationshipState.FRIEND -> OrangeGlow
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        fontWeight = FontWeight.Medium,
    )
}

// ── Friend Profile Sheet ────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FriendProfileSheet(
    profile: FriendProfileDetails?,
    isLoading: Boolean,
    onChallengeFriend: () -> Unit,
    onRemoveFriend: () -> Unit,
) {
    if (isLoading && profile == null) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = OrangeGlow)
        }
        return
    }

    if (profile == null) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Could not load profile.",
                style = MaterialTheme.typography.titleMedium,
                color = Cream,
            )
        }
        return
    }

    val topStreak = profile.publicHabits.maxOfOrNull { it.streak } ?: 0

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Hero card
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(OrangeGlow, AmberDeep),
                            ),
                        )
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AvatarBubble(
                            label = initialsFor(profile.displayName, profile.username),
                            imageUrl = profile.profileImageUrl,
                            accent = Color.White.copy(alpha = 0.2f),
                            textColor = Color.White,
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = profile.displayName,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "@${profile.username}",
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    Text(
                        text = profile.bio.ifBlank { "Consistency, streaks, and public habits on display." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center,
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FriendMetricChip(
                            icon = Icons.Default.Groups,
                            value = profile.publicHabits.size.toString(),
                            label = "Habits",
                        )
                        FriendMetricChip(
                            icon = Icons.Default.EmojiEvents,
                            value = profile.badges.size.toString(),
                            label = "Badges",
                        )
                        FriendMetricChip(
                            icon = Icons.Default.LocalFireDepartment,
                            value = topStreak.toString(),
                            label = "Streak",
                        )
                    }
                }
            }
        }

        // Badges
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalMid),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Badges",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Cream,
                        textAlign = TextAlign.Center,
                    )
                    if (profile.badges.isEmpty()) {
                        Text("No badges yet.", color = Cream.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            profile.badges.take(12).forEach { badge ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(badge, color = Cream) },
                                    leadingIcon = { Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldSoft) },
                                    colors = AssistChipDefaults.assistChipColors(containerColor = CharcoalLight),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Public Habits
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CharcoalMid),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Public Habits",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Cream,
                        textAlign = TextAlign.Center,
                    )
                    if (profile.publicHabits.isEmpty()) {
                        Text("No public habits yet.", color = Cream.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                    } else {
                        profile.publicHabits.take(10).forEach { habit ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CharcoalLight),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = habit.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Cream,
                                    )
                                    Text(
                                        text = "🔥 ${habit.streak}",
                                        fontWeight = FontWeight.SemiBold,
                                        color = OrangeGlow,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onRemoveFriend,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CharcoalLight,
                        contentColor = Cream,
                    ),
                ) {
                    Text("Remove Friend", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onChallengeFriend,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeGlow,
                        contentColor = CharcoalDark,
                    ),
                ) {
                    Text("Challenge", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun FriendMetricChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = value,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun AvatarBubble(
    label: String,
    imageUrl: String? = null,
    accent: Color,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(accent),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl.toAppImageModel(),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
            )
        } else {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                color = textColor,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private fun String.toAppImageModel(): String {
    return if (startsWith("avatars/")) "file:///android_asset/$this" else this
}

private fun initialsFor(displayName: String, username: String): String {
    val parts = displayName.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> username.take(2).uppercase()
    }
}
