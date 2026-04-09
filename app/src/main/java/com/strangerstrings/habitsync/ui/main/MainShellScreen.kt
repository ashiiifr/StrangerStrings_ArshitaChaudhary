package com.strangerstrings.habitsync.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strangerstrings.habitsync.ui.challenges.ChallengesScreen
import com.strangerstrings.habitsync.ui.friends.FriendsScreen
import com.strangerstrings.habitsync.ui.home.HomeOverviewScreen
import com.strangerstrings.habitsync.ui.inbox.InboxSheet
import com.strangerstrings.habitsync.ui.profile.ProfileScreen
import com.strangerstrings.habitsync.ui.theme.CharcoalDark
import com.strangerstrings.habitsync.ui.theme.CharcoalLight
import com.strangerstrings.habitsync.ui.theme.CharcoalMid
import com.strangerstrings.habitsync.ui.theme.Cream
import com.strangerstrings.habitsync.ui.theme.NavBarOrangeEnd
import com.strangerstrings.habitsync.ui.theme.NavBarOrangeStart
import com.strangerstrings.habitsync.ui.theme.OrangeGlow
import com.strangerstrings.habitsync.data.HabitCategory
import com.strangerstrings.habitsync.data.HabitType
import com.strangerstrings.habitsync.viewmodel.ChallengesViewModel
import com.strangerstrings.habitsync.viewmodel.FriendsViewModel
import com.strangerstrings.habitsync.viewmodel.HomeUiState
import com.strangerstrings.habitsync.viewmodel.InboxViewModel
import com.strangerstrings.habitsync.viewmodel.ProfileViewModel

private enum class MainTab(val title: String, val subtitle: String) {
    HOME("Health and Wellness", "Today's activity"),
    CHALLENGES("Your Goals", "Daily challenges"),
    FRIENDS("Your Circle", "Find and grow connections"),
    PROFILE("Profile", "Everything about you"),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainShellScreen(
    homeUiState: HomeUiState,
    onMarkHabitDone: (String, ByteArray?) -> Unit,
    onUpdateCustomHabit: (String, String, String, String) -> Unit,
    onDeleteCustomHabit: (String) -> Unit,
    onCreateHabit: (String, HabitCategory, HabitType, String, String) -> Unit,
    onLogoutClick: () -> Unit,
    onOpenGlobalLeaderboard: () -> Unit,
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
    var showHabitCreator by remember { mutableStateOf(false) }
    var challengeCreateToken by remember { mutableStateOf(0) }
    var showInbox by remember { mutableStateOf(false) }
    var showAddFriendsSheet by remember { mutableStateOf(false) }
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ShellTopBar(
                tab = selectedTab,
                unreadCount = inboxUiState.unreadCount,
                onInboxClick = { showInbox = true },
                onLogoutClick = onLogoutClick,
                onLeaderboardClick = onOpenGlobalLeaderboard,
                showLogout = selectedTab == MainTab.PROFILE,
                showAddFriends = selectedTab == MainTab.FRIENDS,
                onAddFriendsClick = { showAddFriendsSheet = true },
                showLeaderboard = selectedTab != MainTab.PROFILE,
            )
        },
        bottomBar = {
            FloatingNavBar(
                selectedTab = selectedTab,
                onTabSelect = { selectedTab = it },
                challengeBadge = challengesUiState.pendingInviteCount,
                friendsBadge = friendsUiState.pendingRequestCount,
                showCreateMenu = showCreateMenu,
                onFabClick = { showCreateMenu = true },
            )
        },
    ) { contentPadding ->
        // Smooth crossfade between tabs
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                (fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.96f, animationSpec = tween(280, easing = FastOutSlowInEasing)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                            scaleOut(targetScale = 0.96f, animationSpec = tween(200, easing = FastOutSlowInEasing)),
                    )
            },
            label = "tab_transition",
        ) { tab ->
            when (tab) {
                MainTab.HOME -> HomeOverviewScreen(
                    uiState = homeUiState,
                    onMarkHabitDone = onMarkHabitDone,
                    onUpdateCustomHabit = onUpdateCustomHabit,
                    onDeleteCustomHabit = onDeleteCustomHabit,
                    onAddHabit = { showCreateMenu = true },
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
                    openCreateDialogToken = challengeCreateToken,
                )
                MainTab.FRIENDS -> FriendsScreen(
                    uiState = friendsUiState,
                    onQueryChange = friendsViewModel::onQueryChange,
                    onSubmitSearch = friendsViewModel::submitSearch,
                    onSendFriendRequest = friendsViewModel::sendFriendRequest,
                    onOpenFriendProfile = friendsViewModel::openFriendProfile,
                    onCloseFriendProfile = friendsViewModel::closeFriendProfile,
                    onChallengeFriend = { username ->
                        challengePrefillUsername = username
                        selectedTab = MainTab.CHALLENGES
                    },
                    onRemoveFriend = friendsViewModel::removeFriend,
                    onSelectLeaderboardFilter = friendsViewModel::selectLeaderboardFilter,
                    showAddFriendsSheet = showAddFriendsSheet,
                    onDismissAddFriendsSheet = { showAddFriendsSheet = false },
                    contentPadding = contentPadding,
                )
                MainTab.PROFILE -> ProfileScreen(
                    uiState = profileUiState,
                    contentPadding = contentPadding,
                    onStartEditing = profileViewModel::startEditing,
                    onCancelEditing = profileViewModel::cancelEditing,
                    onSaveProfile = profileViewModel::saveProfile,
                    onFirstNameChange = profileViewModel::onFirstNameChange,
                    onLastNameChange = profileViewModel::onLastNameChange,
                    onUsernameChange = profileViewModel::onUsernameChange,
                    onBioChange = profileViewModel::onBioChange,
                    onGenderChange = profileViewModel::onGenderChange,
                    onAvatarSelected = profileViewModel::onAvatarSelected,
                    onHeightChange = profileViewModel::onHeightChange,
                    onWeightChange = profileViewModel::onWeightChange,
                    onOpenChangePassword = profileViewModel::openChangePassword,
                    onCloseChangePassword = profileViewModel::closeChangePassword,
                    onCurrentPasswordChange = profileViewModel::onCurrentPasswordChange,
                    onNewPasswordChange = profileViewModel::onNewPasswordChange,
                    onConfirmNewPasswordChange = profileViewModel::onConfirmNewPasswordChange,
                    onSubmitPasswordChange = profileViewModel::submitPasswordChange,
                )
            }
        }
    }

    if (showInbox) {
        InboxSheet(
            items = inboxUiState.items,
            onDismiss = { showInbox = false },
            onMarkAllRead = inboxViewModel::markAllRead,
            onAccept = inboxViewModel::accept,
            onDecline = inboxViewModel::decline,
            onDeleteNotification = inboxViewModel::dismissNotification,
        )
    }

    if (showCreateMenu) {
        CreateChooserSheet(
            onDismiss = { showCreateMenu = false },
            onSelectHabit = {
                showCreateMenu = false
                showHabitCreator = true
            },
            onSelectChallenge = {
                showCreateMenu = false
                selectedTab = MainTab.CHALLENGES
                challengeCreateToken += 1
            },
        )
    }

    if (showHabitCreator) {
        HabitCreatorSheet(
            onDismiss = { showHabitCreator = false },
            onCreateHabit = { title, category, type, target, note ->
                onCreateHabit(title, category, type, target, note)
                showHabitCreator = false
            },
        )
    }
}

// ── Floating nav bar with center FAB ────────────────────────────────
@Composable
private fun FloatingNavBar(
    selectedTab: MainTab,
    onTabSelect: (MainTab) -> Unit,
    challengeBadge: Int,
    friendsBadge: Int,
    showCreateMenu: Boolean,
    onFabClick: () -> Unit,
) {
    // Slide-in animation for the nav bar itself
    val navBarVisible = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        navBarVisible.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .graphicsLayer {
                translationY = (1f - navBarVisible.value) * 200f
                alpha = navBarVisible.value
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        // -- Nav pill background --
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 18.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(NavBarOrangeStart, NavBarOrangeEnd),
                        ),
                        RoundedCornerShape(32.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavIcon(
                    icon = Icons.Default.Home,
                    label = "Home",
                    selected = selectedTab == MainTab.HOME,
                    badgeCount = 0,
                    onClick = { onTabSelect(MainTab.HOME) },
                )
                NavIcon(
                    icon = Icons.Default.SportsScore,
                    label = "Goals",
                    selected = selectedTab == MainTab.CHALLENGES,
                    badgeCount = challengeBadge,
                    onClick = { onTabSelect(MainTab.CHALLENGES) },
                )

                // Center FAB placeholder
                Spacer(modifier = Modifier.width(56.dp))

                NavIcon(
                    icon = Icons.Default.Groups,
                    label = "Friends",
                    selected = selectedTab == MainTab.FRIENDS,
                    badgeCount = friendsBadge,
                    onClick = { onTabSelect(MainTab.FRIENDS) },
                )
                NavIcon(
                    icon = Icons.Default.Person,
                    label = "Profile",
                    selected = selectedTab == MainTab.PROFILE,
                    badgeCount = 0,
                    onClick = { onTabSelect(MainTab.PROFILE) },
                )
            }
        }

        // -- Center FAB (raised above the bar) --
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-18).dp),
        ) {
            // Subtle breathing animation for the FAB
            val infiniteTransition = rememberInfiniteTransition(label = "fab_breathing")
            val fabBreathing by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.06f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "fab_scale_breathing",
            )
            // Rotation animation when menu opens
            val fabRotation by animateFloatAsState(
                targetValue = if (showCreateMenu) 45f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "fab_rotation",
            )

            Box(
                modifier = Modifier
                    .size(58.dp)
                    .scale(fabBreathing)
                    .shadow(12.dp, CircleShape)
                    .background(
                        Brush.verticalGradient(listOf(Color.White, Cream)),
                        CircleShape,
                    )
                    .clip(CircleShape)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onFabClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create",
                    tint = CharcoalDark,
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer { rotationZ = fabRotation },
                )
            }
        }
    }
}

private data class HabitPreset(
    val title: String,
    val category: HabitCategory,
    val type: HabitType,
    val icon: ImageVector,
    val targetHint: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateChooserSheet(
    onDismiss: () -> Unit,
    onSelectHabit: () -> Unit,
    onSelectChallenge: () -> Unit,
) {
    val chooserSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CharcoalMid,
        sheetState = chooserSheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Create New",
                style = MaterialTheme.typography.headlineSmall,
                color = Cream,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Pick what you want to create. This replaces the tiny old dropdown with a proper flow.",
                color = Cream.copy(alpha = 0.7f),
            )
            CreateOptionCard(
                title = "New Habit",
                subtitle = "Choose a default habit type, add target and notes",
                icon = Icons.Default.TaskAlt,
                onClick = onSelectHabit,
            )
            CreateOptionCard(
                title = "New Challenge",
                subtitle = "Set rules, duration, and invite friends",
                icon = Icons.Default.SportsScore,
                onClick = onSelectChallenge,
            )
        }
    }
}

@Composable
private fun CreateOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalLight),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(OrangeGlow, NavBarOrangeEnd))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = CharcoalDark)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Cream, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Cream.copy(alpha = 0.7f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun HabitCreatorSheet(
    onDismiss: () -> Unit,
    onCreateHabit: (String, HabitCategory, HabitType, String, String) -> Unit,
) {
    val presets = remember {
        listOf(
            HabitPreset("Reading", HabitCategory.READING, HabitType.READING, Icons.Default.MenuBook, "30 min"),
            HabitPreset("Writing", HabitCategory.CUSTOM, HabitType.WRITING, Icons.Default.Create, "500 words"),
            HabitPreset("Studying", HabitCategory.CUSTOM, HabitType.STUDYING, Icons.Default.AutoStories, "2 hours"),
            HabitPreset("Drink Water", HabitCategory.HYDRATION, HabitType.DRINK_WATER, Icons.Default.Opacity, "3 litres"),
            HabitPreset("Running", HabitCategory.FITNESS, HabitType.RUNNING, Icons.Default.DirectionsRun, "2 km"),
            HabitPreset("Walking", HabitCategory.FITNESS, HabitType.WALKING, Icons.Default.DirectionsWalk, "6000 steps"),
            HabitPreset("Sleep Early", HabitCategory.SLEEP, HabitType.SLEEP_EARLY, Icons.Default.Bedtime, "Before 11 PM"),
            HabitPreset("Meditation", HabitCategory.CUSTOM, HabitType.MEDITATION, Icons.Default.SelfImprovement, "15 min"),
            HabitPreset("Workout", HabitCategory.FITNESS, HabitType.WORKOUT, Icons.Default.MonitorHeart, "45 min"),
            HabitPreset("Other", HabitCategory.CUSTOM, HabitType.OTHER, Icons.Default.Tune, "Custom target"),
        )
    }
    var selectedPreset by remember { mutableStateOf(presets.first()) }
    var customTitle by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val creatorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CharcoalMid,
        sheetState = creatorSheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "New Habit",
                style = MaterialTheme.typography.headlineSmall,
                color = Cream,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Select a habit type first, then add a target or duration and a note.",
                color = Cream.copy(alpha = 0.72f),
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                presets.chunked(3).forEach { rowPresets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowPresets.forEach { preset ->
                            Card(
                                onClick = {
                                    selectedPreset = preset
                                    if (target.isBlank()) target = preset.targetHint
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedPreset.title == preset.title) OrangeGlow else CharcoalLight,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = preset.icon,
                                        contentDescription = null,
                                        tint = if (selectedPreset.title == preset.title) CharcoalDark else Cream,
                                    )
                                    Text(
                                        text = preset.title,
                                        color = if (selectedPreset.title == preset.title) CharcoalDark else Cream,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                        repeat(3 - rowPresets.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            if (selectedPreset.title == "Other") {
                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Custom habit name") },
                    shape = RoundedCornerShape(18.dp),
                    colors = creatorFieldColors(),
                )
            }

            OutlinedTextField(
                value = target,
                onValueChange = { target = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Quantity or duration") },
                placeholder = { Text(selectedPreset.targetHint) },
                shape = RoundedCornerShape(18.dp),
                colors = creatorFieldColors(),
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Note") },
                placeholder = { Text("Example: morning run in the park") },
                minLines = 2,
                shape = RoundedCornerShape(18.dp),
                colors = creatorFieldColors(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel", color = Cream.copy(alpha = 0.72f))
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = OrangeGlow),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) {
                                val title = if (selectedPreset.title == "Other") customTitle.trim() else selectedPreset.title
                                onCreateHabit(title, selectedPreset.category, selectedPreset.type, target, note)
                            }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Create Habit", color = CharcoalDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun creatorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Cream,
    unfocusedTextColor = Cream,
    focusedBorderColor = OrangeGlow,
    unfocusedBorderColor = Cream.copy(alpha = 0.22f),
    focusedLabelColor = OrangeGlow,
    unfocusedLabelColor = Cream.copy(alpha = 0.7f),
    cursorColor = OrangeGlow,
)

@Composable
private fun NavIcon(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
) {
    // Smooth scale bounce when selected
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "nav_icon_scale",
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.55f,
        animationSpec = tween(220),
        label = "nav_icon_alpha",
    )
    val dotSize by animateDpAsState(
        targetValue = if (selected) 5.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "nav_dot_size",
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BadgedBox(
            badge = {
                if (badgeCount > 0) {
                    Badge(
                        containerColor = Color.White,
                        contentColor = CharcoalDark,
                    ) {
                        Text(if (badgeCount > 9) "9+" else badgeCount.toString())
                    }
                }
            },
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier
                    .size(24.dp)
                    .scale(iconScale),
                tint = Color.White.copy(alpha = iconAlpha),
            )
        }
        Box(
            modifier = Modifier
                .size(dotSize)
                .background(Color.White, CircleShape),
        )
    }
}

// ── Top bar ─────────────────────────────────────────────────────────
@Composable
private fun ShellTopBar(
    tab: MainTab,
    unreadCount: Int,
    onInboxClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onLeaderboardClick: () -> Unit,
    showLogout: Boolean,
    showAddFriends: Boolean,
    onAddFriendsClick: () -> Unit,
    showLeaderboard: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
        // Animated title transitions when switching tabs
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(300)) + slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                )).togetherWith(
                    fadeOut(tween(200)) + slideOutVertically(
                        targetOffsetY = { -it / 4 },
                        animationSpec = tween(200),
                    ),
                )
            },
            modifier = Modifier.weight(1f),
            label = "top_bar_title",
        ) { currentTab ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "WELCOME BACK",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing,
                )
                val titleParts = currentTab.title.split(" ")
                if (titleParts.size >= 2) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                                append(titleParts.first())
                            }
                            for (i in 1 until titleParts.size) {
                                append(" ")
                                val isAccentWord = titleParts[i].lowercase() in listOf("and", "goals", "circle")
                                withStyle(
                                    SpanStyle(
                                        color = if (isAccentWord) OrangeGlow else MaterialTheme.colorScheme.onSurface,
                                    ),
                                ) {
                                    append(titleParts[i])
                                }
                            }
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                } else {
                    Text(
                        text = currentTab.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Text(
                    text = currentTab.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
            AnimatedVisibility(
                visible = showLeaderboard,
                enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.8f, animationSpec = tween(200)),
                exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.8f, animationSpec = tween(150)),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(onClick = onLeaderboardClick) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Global leaderboard",
                            tint = MaterialTheme.colorScheme.onSecondary,
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = showAddFriends,
                enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.8f, animationSpec = tween(200)),
                exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.8f, animationSpec = tween(150)),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(onClick = onAddFriendsClick) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add friends",
                            tint = MaterialTheme.colorScheme.onSecondary,
                        )
                    }
                }
            }
            Box {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(onClick = onInboxClick) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSecondary,
                        )
                    }
                }
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(10.dp)
                            .background(Color(0xFFFF5B3A), CircleShape),
                    )
                }
            }
            AnimatedVisibility(
                visible = showLogout,
                enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.8f, animationSpec = tween(200)),
                exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.8f, animationSpec = tween(150)),
            ) {
                Text(
                    text = "Logout",
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { onLogoutClick() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            }
        }
    }
}
