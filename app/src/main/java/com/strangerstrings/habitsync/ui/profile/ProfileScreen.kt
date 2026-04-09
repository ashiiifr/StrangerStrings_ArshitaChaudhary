package com.strangerstrings.habitsync.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.strangerstrings.habitsync.data.EditableProfile
import com.strangerstrings.habitsync.data.Habit
import com.strangerstrings.habitsync.data.UserProfile
import com.strangerstrings.habitsync.ui.theme.AmberDeep
import com.strangerstrings.habitsync.ui.theme.CharcoalDark
import com.strangerstrings.habitsync.ui.theme.CharcoalLight
import com.strangerstrings.habitsync.ui.theme.CharcoalMid
import com.strangerstrings.habitsync.ui.theme.Cream
import com.strangerstrings.habitsync.ui.theme.GoldSoft
import com.strangerstrings.habitsync.ui.theme.OrangeGlow
import com.strangerstrings.habitsync.viewmodel.ProfileUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    contentPadding: PaddingValues,
    onStartEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    onSaveProfile: () -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onAvatarSelected: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onOpenChangePassword: () -> Unit,
    onCloseChangePassword: () -> Unit,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmNewPasswordChange: (String) -> Unit,
    onSubmitPasswordChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = uiState.profile

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
        ),
    ) {
        if (uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = OrangeGlow)
                }
            }
        }

        profile?.let { p ->
            // ── HERO HEADER ─────────────────────────────────────
            item(key = "hero") {
                StaggeredItem(delay = 0) {
                    ProfileHero(profile = p, onEditClick = onStartEditing)
                }
            }

            // ── STAT ROW (seamless pills) ───────────────────────
            item(key = "stats") {
                StaggeredItem(delay = 80) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StatPill("Habits", p.totalHabitsCreated.toString(), Modifier.weight(1f))
                        StatPill("Best Streak", p.longestStreakEver.toString(), Modifier.weight(1f))
                        StatPill("Challenges", p.challengesCompleted.toString(), Modifier.weight(1f))
                    }
                }
            }

            // ── UNIFIED INFO SECTION ────────────────────────────
            item(key = "info") {
                StaggeredItem(delay = 160) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CharcoalMid),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                        ) {
                            // Personal Info
                            Text(
                                "Personal Info",
                                style = MaterialTheme.typography.labelLarge,
                                color = Cream.copy(alpha = 0.5f),
                                letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing,
                            )
                            Spacer(Modifier.height(12.dp))
                            InfoRow(Icons.Default.Email, "Email", p.email.ifBlank { "Not set" }, locked = true)
                            ThinDivider()
                            InfoRow(Icons.Default.Person, "Gender", displayGender(p.gender))
                            ThinDivider()
                            InfoRow(Icons.Default.FitnessCenter, "Height", "${displayFloat(p.heightCm)} cm")
                            ThinDivider()
                            InfoRow(Icons.Default.MonitorWeight, "Weight", "${displayFloat(p.weightKg)} kg")
                            ThinDivider()
                            InfoRow(Icons.Default.LocalFireDepartment, "Freeze Tokens", "${p.freezeTokensThisMonth} this month", accent = true)
                        }
                    }
                }
            }

            // ── BADGES ──────────────────────────────────────────
            if (uiState.badges.isNotEmpty()) {
                item(key = "badges") {
                    StaggeredItem(delay = 240) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = CharcoalMid),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    "BADGES",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Cream.copy(alpha = 0.5f),
                                    letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing,
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    uiState.badges.forEach { badge ->
                                        FilterChip(
                                            selected = true,
                                            onClick = {},
                                            label = { Text(badge.title, color = Cream) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = OrangeGlow.copy(alpha = 0.15f),
                                                selectedLabelColor = Cream,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── PUBLIC HABITS ───────────────────────────────────
            if (uiState.publicHabits.isNotEmpty()) {
                item(key = "habits_header") {
                    StaggeredItem(delay = 320) {
                        Text(
                            "PUBLIC HABITS",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                        )
                    }
                }
                items(uiState.publicHabits, key = Habit::id) { habit ->
                    PublicHabitRow(habit)
                }
            }

            // ── ACTIONS SECTION ─────────────────────────────────
            item(key = "actions") {
                StaggeredItem(delay = if (uiState.publicHabits.isNotEmpty()) 400 else 320) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Change password button
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onOpenChangePassword),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CharcoalMid),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(OrangeGlow.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = OrangeGlow,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Change Password",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Cream,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        "Update your account security",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Cream.copy(alpha = 0.5f),
                                    )
                                }
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Cream.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Success / error messages
        uiState.successMessage?.let { msg ->
            item {
                Text(
                    text = "✓ $msg",
                    color = OrangeGlow,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            }
        }
        uiState.passwordSuccess?.let { msg ->
            item {
                Text(
                    text = "✓ $msg",
                    color = OrangeGlow,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            }
        }
        uiState.errorMessage?.let { msg ->
            item {
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            }
        }
    }

    // ── Edit profile sheet ──────────────────────────────────
    if (uiState.isEditing) {
        val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onCancelEditing,
            containerColor = CharcoalDark,
            sheetState = editSheetState,
        ) {
            EditProfileSheet(
                editableProfile = uiState.editableProfile,
                email = profile?.email.orEmpty(),
                name = profile?.displayName.orEmpty(),
                isSaving = uiState.isSaving,
                errorMessage = uiState.errorMessage,
                onCancel = onCancelEditing,
                onSave = onSaveProfile,
                onUsernameChange = onUsernameChange,
                onBioChange = onBioChange,
                onGenderChange = onGenderChange,
                onAvatarSelected = onAvatarSelected,
                onHeightChange = onHeightChange,
                onWeightChange = onWeightChange,
            )
        }
    }

    // ── Change password sheet ───────────────────────────────
    if (uiState.isChangingPassword) {
        val passwordSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onCloseChangePassword,
            containerColor = CharcoalDark,
            sheetState = passwordSheetState,
        ) {
            ChangePasswordSheet(
                currentPassword = uiState.currentPassword,
                newPassword = uiState.newPassword,
                confirmNewPassword = uiState.confirmNewPassword,
                isSaving = uiState.isPasswordSaving,
                errorMessage = uiState.passwordError,
                onCurrentPasswordChange = onCurrentPasswordChange,
                onNewPasswordChange = onNewPasswordChange,
                onConfirmNewPasswordChange = onConfirmNewPasswordChange,
                onCancel = onCloseChangePassword,
                onSubmit = onSubmitPasswordChange,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// ── HERO ──────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════
@Composable
private fun ProfileHero(
    profile: UserProfile,
    onEditClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(OrangeGlow, AmberDeep, CharcoalDark.copy(alpha = 0f)),
                ),
            )
            .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 28.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Avatar + edit button row
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    val imageUrl = profile.profileImageUrl
                    if (!imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = imageUrl.toAppImageModel(),
                            contentDescription = "${profile.displayName} avatar",
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = initialsFor(profile),
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CharcoalDark)
                        .clickable(onClick = onEditClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = OrangeGlow,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = profile.displayName,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "@${profile.username}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
            )
            if (profile.bio.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = profile.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// ── STAT PILL ─────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════
@Composable
private fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalMid),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OrangeGlow,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Cream.copy(alpha = 0.6f),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// ── INFO ROW ──────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════
@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    locked: Boolean = false,
    accent: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (accent) OrangeGlow else Cream.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Cream.copy(alpha = 0.4f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = if (accent) OrangeGlow else Cream,
                fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
        if (locked) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = Cream.copy(alpha = 0.2f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun ThinDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = Cream.copy(alpha = 0.08f),
    )
}

// ═══════════════════════════════════════════════════════════
// ── PUBLIC HABIT ROW ──────────────────────────────────────
// ═══════════════════════════════════════════════════════════
@Composable
private fun PublicHabitRow(habit: Habit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalMid),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(OrangeGlow.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        tint = OrangeGlow,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Cream,
                    )
                    Text(
                        text = habit.category.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = Cream.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text(
                text = "🔥 ${habit.streak}",
                fontWeight = FontWeight.SemiBold,
                color = OrangeGlow,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// ── EDIT PROFILE SHEET (non-editable: name, email) ───────
// ═══════════════════════════════════════════════════════════
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProfileSheet(
    editableProfile: EditableProfile,
    email: String,
    name: String,
    isSaving: Boolean,
    errorMessage: String?,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onUsernameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onAvatarSelected: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val avatarOptions = remember {
        context.assets.list("avatars")
            ?.filter { it.endsWith(".jpg", ignoreCase = true) || it.endsWith(".jpeg", ignoreCase = true) || it.endsWith(".png", ignoreCase = true) }
            ?.sorted()
            ?.map { "avatars/$it" }
            .orEmpty()
    }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = OrangeGlow,
        unfocusedBorderColor = Cream.copy(alpha = 0.18f),
        focusedLabelColor = OrangeGlow,
        unfocusedLabelColor = Cream.copy(alpha = 0.5f),
        cursorColor = OrangeGlow,
        focusedTextColor = Cream,
        unfocusedTextColor = Cream,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 22.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Edit Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Cream,
            )
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Cream.copy(alpha = 0.5f))
            }
        }

        // Locked fields
        Text("LOCKED", style = MaterialTheme.typography.labelSmall, color = Cream.copy(alpha = 0.3f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LockedField("Name", name, Modifier.weight(1f))
            LockedField("Email", email, Modifier.weight(1f))
        }

        HorizontalDivider(thickness = 0.5.dp, color = Cream.copy(alpha = 0.08f))

        // Editable fields
        Text("EDITABLE", style = MaterialTheme.typography.labelSmall, color = OrangeGlow.copy(alpha = 0.6f))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Profile Icon", style = MaterialTheme.typography.labelMedium, color = Cream.copy(alpha = 0.6f))
            ProfileAvatarPicker(
                avatarOptions = avatarOptions,
                selectedAvatar = editableProfile.profileImageUrl,
                onAvatarSelected = onAvatarSelected,
            )
        }

        OutlinedTextField(
            value = editableProfile.username,
            onValueChange = onUsernameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Username") },
            supportingText = { Text("3-12 lowercase letters or numbers", color = Cream.copy(alpha = 0.4f)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = fieldColors,
        )

        OutlinedTextField(
            value = editableProfile.bio,
            onValueChange = onBioChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Bio") },
            minLines = 2,
            maxLines = 4,
            supportingText = { Text("${editableProfile.bio.length}/180", color = Cream.copy(alpha = 0.4f)) },
            shape = RoundedCornerShape(16.dp),
            colors = fieldColors,
        )

        Text("Gender", style = MaterialTheme.typography.labelMedium, color = Cream.copy(alpha = 0.6f))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf("Female", "Male", "Other", "Prefer not to say").forEach { option ->
                FilterChip(
                    selected = editableProfile.gender == option,
                    onClick = { onGenderChange(option) },
                    label = { Text(option, color = if (editableProfile.gender == option) CharcoalDark else Cream) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OrangeGlow,
                        selectedLabelColor = CharcoalDark,
                        containerColor = CharcoalLight,
                    ),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = editableProfile.heightCm,
                onValueChange = onHeightChange,
                modifier = Modifier.weight(1f),
                label = { Text("Height (cm)") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = fieldColors,
            )
            OutlinedTextField(
                value = editableProfile.weightKg,
                onValueChange = onWeightChange,
                modifier = Modifier.weight(1f),
                label = { Text("Weight (kg)") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = fieldColors,
            )
        }

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel", color = Cream.copy(alpha = 0.5f))
            }
            Button(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeGlow,
                    contentColor = CharcoalDark,
                ),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = CharcoalDark)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LockedField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Cream.copy(alpha = 0.3f))
        Text(
            text = value.ifBlank { "—" },
            style = MaterialTheme.typography.bodyMedium,
            color = Cream.copy(alpha = 0.5f),
        )
    }
}

// ═══════════════════════════════════════════════════════════
// ── CHANGE PASSWORD SHEET ────────────────────────────────
// ═══════════════════════════════════════════════════════════
@Composable
private fun ChangePasswordSheet(
    currentPassword: String,
    newPassword: String,
    confirmNewPassword: String,
    isSaving: Boolean,
    errorMessage: String?,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmNewPasswordChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
) {
    var currentVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = OrangeGlow,
        unfocusedBorderColor = Cream.copy(alpha = 0.18f),
        focusedLabelColor = OrangeGlow,
        unfocusedLabelColor = Cream.copy(alpha = 0.5f),
        cursorColor = OrangeGlow,
        focusedTextColor = Cream,
        unfocusedTextColor = Cream,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 22.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "Change Password",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Cream,
                )
                Text(
                    "Keep your account secure",
                    style = MaterialTheme.typography.bodySmall,
                    color = Cream.copy(alpha = 0.5f),
                )
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Cream.copy(alpha = 0.5f))
            }
        }

        OutlinedTextField(
            value = currentPassword,
            onValueChange = onCurrentPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Current Password") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            visualTransformation = if (currentVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { currentVisible = !currentVisible }) {
                    Icon(
                        if (currentVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle visibility",
                        tint = Cream.copy(alpha = 0.5f),
                    )
                }
            },
            colors = fieldColors,
        )

        OutlinedTextField(
            value = newPassword,
            onValueChange = onNewPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("New Password") },
            supportingText = { Text("At least 8 characters", color = Cream.copy(alpha = 0.4f)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            visualTransformation = if (newVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { newVisible = !newVisible }) {
                    Icon(
                        if (newVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle visibility",
                        tint = Cream.copy(alpha = 0.5f),
                    )
                }
            },
            colors = fieldColors,
        )

        OutlinedTextField(
            value = confirmNewPassword,
            onValueChange = onConfirmNewPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Re-enter New Password") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = confirmNewPassword.isNotBlank() && confirmNewPassword != newPassword,
            supportingText = {
                if (confirmNewPassword.isNotBlank() && confirmNewPassword != newPassword) {
                    Text("Passwords do not match", color = MaterialTheme.colorScheme.error)
                }
            },
            trailingIcon = {
                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                    Icon(
                        if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle visibility",
                        tint = Cream.copy(alpha = 0.5f),
                    )
                }
            },
            colors = fieldColors,
        )

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel", color = Cream.copy(alpha = 0.5f))
            }
            Button(
                onClick = onSubmit,
                enabled = !isSaving && currentPassword.isNotBlank() && newPassword.length >= 8 && newPassword == confirmNewPassword,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeGlow,
                    contentColor = CharcoalDark,
                ),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = CharcoalDark)
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Update", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ═══════════════════════════════════════════════════════════
// ── HELPERS ──────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════
private fun initialsFor(profile: UserProfile): String {
    val first = profile.firstName.firstOrNull()?.uppercaseChar()
    val last = profile.lastName.firstOrNull()?.uppercaseChar()
    return buildString {
        if (first != null) append(first)
        if (last != null) append(last)
    }.ifBlank { profile.username.take(2).uppercase() }
}

private fun displayFloat(value: Float): String {
    return if (value % 1f == 0f) value.toInt().toString() else value.toString()
}

private fun displayGender(value: String): String {
    return when (value.trim().lowercase()) {
        "man", "male" -> "Male"
        "woman", "female" -> "Female"
        "other" -> "Other"
        "prefer not to say" -> "Prefer not to say"
        else -> value.ifBlank { "Prefer not to say" }
    }
}

private fun String.toAppImageModel(): String {
    return if (startsWith("avatars/")) "file:///android_asset/$this" else this
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileAvatarPicker(
    avatarOptions: List<String>,
    selectedAvatar: String?,
    onAvatarSelected: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        avatarOptions.forEach { avatarPath ->
            val selected = avatarPath == selectedAvatar
            val scale by animateFloatAsState(
                targetValue = if (selected) 1.06f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "profile_avatar_scale",
            )
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .scale(scale)
                    .clickable { onAvatarSelected(avatarPath) }
                    .padding(2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if (selected) OrangeGlow.copy(alpha = 0.18f) else CharcoalLight)
                        .padding(4.dp),
                ) {
                    AsyncImage(
                        model = avatarPath.toAppImageModel(),
                        contentDescription = "Profile avatar option",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                    )
                }
                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(OrangeGlow),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = CharcoalDark,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StaggeredItem(
    delay: Int,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350)) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(400, easing = FastOutSlowInEasing),
        ),
    ) {
        content()
    }
}
