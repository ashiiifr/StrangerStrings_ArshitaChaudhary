package com.strangerstrings.habitsync.ui.auth

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.strangerstrings.habitsync.ui.theme.HabitSyncTheme
import com.strangerstrings.habitsync.viewmodel.AuthMode
import com.strangerstrings.habitsync.viewmodel.AuthUiState
import com.strangerstrings.habitsync.viewmodel.PasswordStrength
import java.util.Date

private enum class SignUpStep { FirstName, LastName, Username, Avatar, Gender, Dob, Height, Weight, Email, Password, ConfirmPassword }

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onSwitchMode: (AuthMode) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onDateOfBirthSelected: (Long?) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onAvatarSelected: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onForgotPasswordClick: () -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSignIn = uiState.mode == AuthMode.SIGN_IN
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AuthHeroBadge(
            icon = if (isSignIn) Icons.Default.Login else Icons.Default.PersonAddAlt1,
            contentDescription = if (isSignIn) "Login icon" else "Create account icon",
        )
        Text(if (isSignIn) "Login your account" else "Create your account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        Text(
            if (isSignIn) "Welcome back, we missed your streak." else "Welcome to your new journey.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(18.dp))
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToggleChip("Sign In", uiState.mode == AuthMode.SIGN_IN, Modifier.weight(1f)) { onSwitchMode(AuthMode.SIGN_IN) }
            ToggleChip("Sign Up", uiState.mode == AuthMode.SIGN_UP, Modifier.weight(1f)) { onSwitchMode(AuthMode.SIGN_UP) }
        }
        Spacer(Modifier.height(18.dp))
        AnimatedContent(uiState.mode, label = "mode") { mode ->
            if (mode == AuthMode.SIGN_IN) {
                SignInPart(uiState, onEmailChange, onPasswordChange, onTogglePasswordVisibility, onForgotPasswordClick, onSubmit)
            } else {
                SignUpPart(uiState, onFirstNameChange, onLastNameChange, onUsernameChange, onAvatarSelected, onDateOfBirthSelected, onHeightChange, onWeightChange, onGenderChange, onEmailChange, onPasswordChange, onConfirmPasswordChange, onTogglePasswordVisibility, onToggleConfirmPasswordVisibility, onSubmit)
            }
        }
        uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
    }
}

@Composable
private fun AuthHeroBadge(
    icon: ImageVector,
    contentDescription: String,
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun ToggleChip(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(40.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant), elevation = ButtonDefaults.buttonElevation(0.dp)) { Text(text) }
}

@Composable
private fun SignInPart(
    ui: AuthUiState,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSubmit: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppField(ui.email, onEmail, "Email", KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next), leading = { Icon(Icons.Default.Email, null) }, isError = ui.emailValidationError != null, help = ui.emailValidationError)
            AppField(ui.password, onPassword, "Password", KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done), if (ui.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), leading = { Icon(Icons.Default.Lock, null) }, trailing = { IconButton(onTogglePassword) { Icon(if (ui.passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } })
            TextButton(onForgotPasswordClick, modifier = Modifier.align(Alignment.End)) { Text("Forgot password?") }
            SubmitButton(ui.isLoading, "Sign In", onSubmit)
        }
    }
}

@Composable
private fun SignUpPart(
    ui: AuthUiState,
    onFirst: (String) -> Unit,
    onLast: (String) -> Unit,
    onUser: (String) -> Unit,
    onAvatarSelected: (String) -> Unit,
    onDob: (Long?) -> Unit,
    onHeight: (String) -> Unit,
    onWeight: (String) -> Unit,
    onGender: (String) -> Unit,
    onEmail: (String) -> Unit,
    onPassword: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onToggleConfirm: () -> Unit,
    onSubmit: () -> Unit,
) {
    val steps = SignUpStep.entries
    var idx by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[idx]
    val context = androidx.compose.ui.platform.LocalContext.current
    val avatarOptions = remember {
        context.assets.list("avatars")
            ?.filter { it.endsWith(".jpg", ignoreCase = true) || it.endsWith(".jpeg", ignoreCase = true) || it.endsWith(".png", ignoreCase = true) }
            ?.sorted()
            ?.map { "avatars/$it" }
            .orEmpty()
    }
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WizardProgressBar(progress = (idx + 1f) / steps.size.toFloat())
            when (step) {
                SignUpStep.FirstName -> AppField(ui.firstName, onFirst, "First name", leading = { Icon(Icons.Default.Person, null) })
                SignUpStep.LastName -> AppField(ui.lastName, onLast, "Last name", leading = { Icon(Icons.Default.Person, null) })
                SignUpStep.Username -> AppField(ui.username, onUser, "Username", leading = { Icon(Icons.Default.Person, null) }, isError = ui.usernameValidationError != null && !ui.isCheckingUsername, help = if (ui.isCheckingUsername) "Checking availability..." else ui.usernameValidationError ?: "${ui.username.length}/12")
                SignUpStep.Avatar -> AvatarPickerStep(avatarOptions, ui.selectedAvatarAssetPath, onAvatarSelected)
                SignUpStep.Gender -> GenderRows(ui.gender, onGender)
                SignUpStep.Dob -> DobStep(ui.dobMillis, ui.ageYears, onDob)
                SignUpStep.Height -> NumStep("Height", ui.heightCm.toIntOrNull() ?: 170, "cm", 120, 220) { onHeight(it.toString()) }
                SignUpStep.Weight -> NumStep("Weight", ui.weightKg.toIntOrNull() ?: 65, "kg", 30, 200) { onWeight(it.toString()) }
                SignUpStep.Email -> AppField(ui.email, onEmail, "Email", KeyboardOptions(keyboardType = KeyboardType.Email), leading = { Icon(Icons.Default.Email, null) }, isError = ui.emailValidationError != null, help = when {
                    ui.isCheckingEmail -> "Checking email..."
                    ui.emailValidationError != null -> ui.emailValidationError
                    ui.email.isNotBlank() && !ui.isCheckingEmail -> "Email is available"
                    else -> null
                })
                SignUpStep.Password -> {
                    AppField(ui.password, onPassword, "Password", KeyboardOptions(keyboardType = KeyboardType.Password), if (ui.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), leading = { Icon(Icons.Default.Lock, null) }, trailing = { IconButton(onTogglePassword) { Icon(if (ui.passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } })
                    PassBar(ui.passwordStrength)
                }
                SignUpStep.ConfirmPassword -> AppField(ui.confirmPassword, onConfirm, "Confirm password", KeyboardOptions(keyboardType = KeyboardType.Password), if (ui.confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(), leading = { Icon(Icons.Default.Lock, null) }, trailing = { IconButton(onToggleConfirm) { Icon(if (ui.confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } }, isError = ui.confirmPasswordError != null, help = ui.confirmPasswordError)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton({ if (idx > 0) idx-- }, enabled = idx > 0, modifier = Modifier.weight(1f)) { Text("Back") }
                Button(onClick = { if (idx == steps.lastIndex) onSubmit() else { if (step == SignUpStep.Height && ui.heightCm.isBlank()) onHeight("170"); if (step == SignUpStep.Weight && ui.weightKg.isBlank()) onWeight("65"); idx++ } }, enabled = !ui.isLoading && stepValid(ui, step), modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(12.dp)) {
                    if (idx == steps.lastIndex && ui.isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary) else Text(if (idx == steps.lastIndex) "Create Account" else "Next")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AvatarPickerStep(
    avatarOptions: List<String>,
    selectedAvatar: String?,
    onAvatarSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Choose your profile icon", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("This avatar will be shown on your profile, friends list, and leaderboards.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val heroAvatar = selectedAvatar ?: avatarOptions.firstOrNull()
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                ),
                            ),
                        )
                        .padding(6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    heroAvatar?.let {
                        AsyncImage(
                            model = assetModel(it),
                            contentDescription = "Selected avatar",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                        )
                    }
                }
                Text(
                    text = if (selectedAvatar == null) "Pick the icon that matches your vibe" else "Selected profile icon",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    avatarOptions.forEach { avatarPath ->
                        PremiumAvatarOption(
                            avatarPath = avatarPath,
                            selected = avatarPath == selectedAvatar,
                            onClick = { onAvatarSelected(avatarPath) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumAvatarOption(
    avatarPath: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "avatar_scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.9f,
        animationSpec = spring(),
        label = "avatar_alpha",
    )
    Box(
        modifier = Modifier
            .size(74.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clickable(onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                .padding(5.dp),
        ) {
            AsyncImage(
                model = assetModel(avatarPath),
                contentDescription = "Avatar option",
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(1f)
                    .clip(CircleShape),
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(22.dp)
                    .offset(x = 4.dp, y = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun WizardProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DobStep(dob: Long?, age: Int?, onDob: (Long?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val picker = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = dob)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { open = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, contentColor = MaterialTheme.colorScheme.onSurface)) { Text(dob?.let(::toDate) ?: "Select Date of Birth") }
        if (age != null) Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) { Text("Age preview: $age years", modifier = Modifier.padding(10.dp), color = MaterialTheme.colorScheme.onPrimaryContainer) }
    }
    if (open) DatePickerDialog(onDismissRequest = { open = false }, confirmButton = { TextButton({ onDob(picker.selectedDateMillis); open = false }) { Text("Done") } }, dismissButton = { TextButton({ open = false }) { Text("Cancel") } }) { DatePicker(state = picker) }
}

@Composable
private fun GenderRows(selected: String, onGender: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Female", "Male", "Other", "Prefer not to say").forEach { g ->
            val s = selected == g
            Surface(onClick = { onGender(g) }, shape = RoundedCornerShape(12.dp), color = if (s) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                Text(g, modifier = Modifier.padding(12.dp), color = if (s) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun NumStep(label: String, value: Int, unit: String, min: Int, max: Int, onValue: (Int) -> Unit) {
    val safe = value.coerceIn(min, max)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StepBtn("-") { onValue((safe - 1).coerceAtLeast(min)) }
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) { Text("$safe $unit", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer) }
            StepBtn("+") { onValue((safe + 1).coerceAtMost(max)) }
        }
    }
}

@Composable private fun StepBtn(text: String, onClick: () -> Unit) { IconButton(onClick, modifier = Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerLow)) { Text(text, color = MaterialTheme.colorScheme.primary) } }
@Composable private fun SubmitButton(loading: Boolean, text: String, onClick: () -> Unit) { Button(onClick, enabled = !loading, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(52.dp)) { if (loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary) else Text(text) } }

@Composable
private fun AppField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    vt: VisualTransformation = VisualTransformation.None,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    help: String? = null,
) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = true, shape = RoundedCornerShape(14.dp), keyboardOptions = keyboard, visualTransformation = vt, leadingIcon = leading, trailingIcon = trailing, isError = isError, supportingText = help?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun PassBar(p: PasswordStrength) {
    val (n, c) = when (p) {
        PasswordStrength.EMPTY -> 0 to MaterialTheme.colorScheme.outline
        PasswordStrength.WEAK -> 1 to Color(0xFFEF4444)
        PasswordStrength.MEDIUM -> 2 to Color(0xFFF59E0B)
        PasswordStrength.STRONG -> 3 to Color(0xFF10B981)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { repeat(3) { i -> Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(if (i < n) c else MaterialTheme.colorScheme.surfaceContainerHighest)) } }
}

private fun stepValid(ui: AuthUiState, s: SignUpStep): Boolean = when (s) {
    SignUpStep.FirstName -> ui.firstName.isNotBlank()
    SignUpStep.LastName -> ui.lastName.isNotBlank()
    SignUpStep.Username -> ui.username.length in 3..12 && !ui.isCheckingUsername && ui.usernameValidationError == null
    SignUpStep.Avatar -> !ui.selectedAvatarAssetPath.isNullOrBlank()
    SignUpStep.Gender -> ui.gender.isNotBlank()
    SignUpStep.Dob -> ui.dobMillis != null && (ui.ageYears ?: 0) in 10..120
    SignUpStep.Height -> ui.heightCm.isBlank() || (ui.heightCm.toIntOrNull() ?: 0) in 120..220
    SignUpStep.Weight -> ui.weightKg.isBlank() || (ui.weightKg.toIntOrNull() ?: 0) in 30..200
    SignUpStep.Email -> ui.email.isNotBlank() && !ui.isCheckingEmail && ui.emailValidationError == null
    SignUpStep.Password -> ui.password.length >= 8
    SignUpStep.ConfirmPassword -> ui.confirmPassword.isNotBlank() && ui.confirmPasswordError == null
}

private fun toDate(millis: Long): String = DateFormat.format("dd MMM yyyy", Date(millis)).toString()
private fun assetModel(path: String): String = "file:///android_asset/$path"

@Preview(showBackground = true)
@Composable
private fun PreviewLogin() {
    HabitSyncTheme {
        LoginScreen(
            uiState = AuthUiState(mode = AuthMode.SIGN_UP, firstName = "Ari", lastName = "C", username = "ari01", dobMillis = 1104537600000L, ageYears = 21, heightCm = "165", weightKg = "58", gender = "Woman", selectedAvatarAssetPath = "avatars/3a562137e8a344da1671229d18aae2ff.jpg", email = "a@h.com", password = "Pass@1234", confirmPassword = "Pass@1234", passwordStrength = PasswordStrength.STRONG),
            onSwitchMode = {}, onFirstNameChange = {}, onLastNameChange = {}, onUsernameChange = {}, onDateOfBirthSelected = {}, onHeightChange = {}, onWeightChange = {}, onGenderChange = {}, onAvatarSelected = {}, onEmailChange = {}, onPasswordChange = {}, onConfirmPasswordChange = {}, onForgotPasswordClick = {}, onTogglePasswordVisibility = {}, onToggleConfirmPasswordVisibility = {}, onSubmit = {},
        )
    }
}
