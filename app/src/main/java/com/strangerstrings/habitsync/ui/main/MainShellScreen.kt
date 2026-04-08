package com.strangerstrings.habitsync.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strangerstrings.habitsync.ui.challenges.ChallengesScreen
import com.strangerstrings.habitsync.ui.friends.FriendsScreen
import com.strangerstrings.habitsync.ui.home.HomeOverviewScreen
import com.strangerstrings.habitsync.ui.inbox.InboxSheet
import com.strangerstrings.habitsync.ui.profile.ProfileScreen
import com.strangerstrings.habitsync.viewmodel.ChallengesViewModel
import com.strangerstrings.habitsync.viewmodel.FriendsViewModel
import com.strangerstrings.habitsync.viewmodel.HomeUiState
import com.strangerstrings.habitsync.viewmodel.InboxViewModel
import com.strangerstrings.habitsync.viewmodel.ProfileViewModel

private enum class MainTab(val title: String) {
    HOME("Home"),
    CHALLENGES("Challenges"),
    FRIENDS("Friends"),
    PROFILE("Profile"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShellScreen(
    homeUiState: HomeUiState,
    onMarkHabitDone: (String) -> Unit,
    onAddHabitClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val friendsViewModel: FriendsViewModel = viewModel()
    val friendsUiState by friendsViewModel.uiState.collectAsStateWithLifecycle()
    val challengesViewModel: ChallengesViewModel = viewModel()
    val challengesUiState by challengesViewModel.uiState.collectAsStateWithLifecycle()
    val profileViewModel: ProfileViewModel = viewModel()
    val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val inboxViewModel: InboxViewModel = viewModel()
    val inboxUiState by inboxViewModel.uiState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(MainTab.HOME) }
    var showCreateMenu by remember { mutableStateOf(false) }
    var showInbox by remember { mutableStateOf(false) }
    var challengePrefillUsername by remember { mutableStateOf<String?>(null) }
    var pendingChallengeRoomId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(inboxViewModel) {
        inboxViewModel.challengeRoomOpenEvents.collect { challengeId ->
            pendingChallengeRoomId = challengeId
            selectedTab = MainTab.CHALLENGES
            showInbox = false
        }
    }

    LaunchedEffect(selectedTab, challengesUiState.challenges, pendingChallengeRoomId) {
        val targetId = pendingChallengeRoomId.orEmpty()
        if (selectedTab == MainTab.CHALLENGES && targetId.isNotBlank()) {
            val exists = challengesUiState.challenges.any { it.id == targetId }
            if (exists) {
                challengesViewModel.openChallengeRoom(targetId)
                pendingChallengeRoomId = null
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(selectedTab.title) },
                navigationIcon = {
                    if (selectedTab == MainTab.PROFILE) {
                        IconButton(onClick = onLogoutClick) {
                            Text("Logout")
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showInbox = true }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Inbox")
                        }
                        if (inboxUiState.unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 10.dp, top = 10.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape),
                            ) {
                                Text(" ", modifier = Modifier.padding(4.dp))
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateMenu = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create")
            }
            DropdownMenu(
                expanded = showCreateMenu,
                onDismissRequest = { showCreateMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text("New Habit") },
                    onClick = {
                        showCreateMenu = false
                        onAddHabitClick()
                    },
                )
                DropdownMenuItem(
                    text = { Text("New Challenge") },
                    onClick = {
                        showCreateMenu = false
                        selectedTab = MainTab.CHALLENGES
                    },
                )
            }
        },
        bottomBar = {
            BottomAppBar {
                NavigationBarItem(
                    selected = selectedTab == MainTab.HOME,
                    onClick = { selectedTab = MainTab.HOME },
                    icon = { TabIconWithBadge(0) { Icon(Icons.Default.Home, contentDescription = null) } },
                    label = { Text("Home") },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.CHALLENGES,
                    onClick = { selectedTab = MainTab.CHALLENGES },
                    icon = {
                        TabIconWithBadge(challengesUiState.pendingInviteCount) {
                            Icon(Icons.Default.SportsScore, contentDescription = null)
                        }
                    },
                    label = { Text("Challenges") },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.FRIENDS,
                    onClick = { selectedTab = MainTab.FRIENDS },
                    icon = {
                        TabIconWithBadge(friendsUiState.pendingRequestCount) {
                            Icon(Icons.Default.Groups, contentDescription = null)
                        }
                    },
                    label = { Text("Friends") },
                )
                NavigationBarItem(
                    selected = selectedTab == MainTab.PROFILE,
                    onClick = { selectedTab = MainTab.PROFILE },
                    icon = { TabIconWithBadge(0) { Icon(Icons.Default.Person, contentDescription = null) } },
                    label = { Text("Profile") },
                )
            }
        },
    ) { contentPadding ->
        when (selectedTab) {
            MainTab.HOME -> HomeOverviewScreen(
                uiState = homeUiState,
                onMarkHabitDone = onMarkHabitDone,
                onAddHabit = onAddHabitClick,
                contentPadding = contentPadding,
            )
            MainTab.CHALLENGES -> ChallengesScreen(
                uiState = challengesUiState,
                contentPadding = contentPadding,
                onCreateChallenge = challengesViewModel::createChallenge,
                onOpenChallengeRoom = challengesViewModel::openChallengeRoom,
                onCloseChallengeRoom = challengesViewModel::closeChallengeRoom,
                onChatInputChange = challengesViewModel::onChatInputChange,
                onSendMessage = challengesViewModel::sendMessage,
                onMarkChallengeDone = challengesViewModel::markChallengeDone,
                prefillInviteUsername = challengePrefillUsername,
                onPrefillInviteConsumed = { challengePrefillUsername = null },
            )
            MainTab.FRIENDS -> FriendsScreen(
                uiState = friendsUiState,
                onQueryChange = friendsViewModel::onQueryChange,
                onSendFriendRequest = friendsViewModel::sendFriendRequest,
                onOpenFriendProfile = friendsViewModel::openFriendProfile,
                onCloseFriendProfile = friendsViewModel::closeFriendProfile,
                onChallengeFriend = { username ->
                    challengePrefillUsername = username
                    selectedTab = MainTab.CHALLENGES
                },
                contentPadding = contentPadding,
            )
            MainTab.PROFILE -> ProfileScreen(
                uiState = profileUiState,
                contentPadding = contentPadding,
            )
        }
    }

    if (showInbox) {
        InboxSheet(
            items = inboxUiState.items,
            onDismiss = { showInbox = false },
            onMarkAllRead = inboxViewModel::markAllRead,
            onAccept = inboxViewModel::accept,
            onDecline = inboxViewModel::decline,
        )
    }
}

@Composable
private fun TabIconWithBadge(
    count: Int,
    icon: @Composable () -> Unit,
) {
    BadgedBox(
        badge = {
            if (count > 0) {
                Badge {
                    Text(if (count > 9) "9+" else count.toString())
                }
            }
        },
    ) {
        icon()
    }
}
