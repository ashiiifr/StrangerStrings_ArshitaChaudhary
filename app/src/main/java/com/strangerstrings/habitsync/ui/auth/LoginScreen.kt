package com.strangerstrings.habitsync.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.strangerstrings.habitsync.ui.theme.HabitSyncTheme
import com.strangerstrings.habitsync.viewmodel.AuthMode
import com.strangerstrings.habitsync.viewmodel.AuthUiState
import com.strangerstrings.habitsync.viewmodel.PasswordStrength

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onSwitchMode: (AuthMode) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val buttonScale by animateFloatAsState(
        targetValue = if (uiState.isLoading) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "auth_button_scale",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ),
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "HabitSync",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "Consistency, proof, and competition in one place.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            Surface(
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    AuthModeSwitch(
                        selectedMode = uiState.mode,
                        onModeSelected = onSwitchMode,
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    AnimatedContent(
                        targetState = uiState.mode,
                        label = "auth_mode_content",
                    ) { mode ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (mode == AuthMode.SIGN_UP) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    AuthTextField(
                                        value = uiState.firstName,
                                        onValueChange = onFirstNameChange,
                                        label = "First name",
                                        modifier = Modifier.weight(1f),
                                    )
                                    AuthTextField(
                                        value = uiState.lastName,
                                        onValueChange = onLastNameChange,
                                        label = "Last name",
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                AuthTextField(
                                    value = uiState.username,
                                    onValueChange = onUsernameChange,
                                    label = "Username",
                                    supportingText = "${uiState.username.length}/12 · lowercase + numbers",
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                        )
                                    },
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    AuthTextField(
                                        value = uiState.age,
                                        onValueChange = onAgeChange,
                                        label = "Age",
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Next,
                                        ),
                                        modifier = Modifier.weight(1f),
                                    )
                                    AuthTextField(
                                        value = uiState.heightCm,
                                        onValueChange = onHeightChange,
                                        label = "Height (cm)",
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal,
                                            imeAction = ImeAction.Next,
                                        ),
                                        modifier = Modifier.weight(1f),
                                    )
                                    AuthTextField(
                                        value = uiState.weightKg,
                                        onValueChange = onWeightChange,
                                        label = "Weight (kg)",
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal,
                                            imeAction = ImeAction.Next,
                                        ),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                GenderSelector(
                                    selectedGender = uiState.gender,
                                    onGenderChange = onGenderChange,
                                )
                            }

                            AuthTextField(
                                value = uiState.email,
                                onValueChange = onEmailChange,
                                label = "Email",
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next,
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                    )
                                },
                            )

                            AuthTextField(
                                value = uiState.password,
                                onValueChange = onPasswordChange,
                                label = "Password",
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = if (mode == AuthMode.SIGN_UP) ImeAction.Next else ImeAction.Done,
                                ),
                                visualTransformation = if (uiState.passwordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = onTogglePasswordVisibility) {
                                        Icon(
                                            imageVector = if (uiState.passwordVisible) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = "Toggle password visibility",
                                        )
                                    }
                                },
                            )

                            AnimatedVisibility(
                                visible = mode == AuthMode.SIGN_UP,
                                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 5 }),
                                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 5 }),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PasswordStrengthIndicator(strength = uiState.passwordStrength)
                                    AuthTextField(
                                        value = uiState.confirmPassword,
                                        onValueChange = onConfirmPasswordChange,
                                        label = "Confirm password",
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Done,
                                        ),
                                        visualTransformation = if (uiState.confirmPasswordVisible) {
                                            VisualTransformation.None
                                        } else {
                                            PasswordVisualTransformation()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = onToggleConfirmPasswordVisibility) {
                                                Icon(
                                                    imageVector = if (uiState.confirmPasswordVisible) {
                                                        Icons.Default.VisibilityOff
                                                    } else {
                                                        Icons.Default.Visibility
                                                    },
                                                    contentDescription = "Toggle confirm password visibility",
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = onSubmit,
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .height(54.dp)
                            .scale(buttonScale),
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        } else {
                            Text(
                                text = if (uiState.mode == AuthMode.SIGN_IN) "Sign In" else "Create Account",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }

                    uiState.errorMessage?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .fillMaxWidth(),
                        )
                    }

                    TextButton(
                        onClick = {
                            onSwitchMode(
                                if (uiState.mode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN,
                            )
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        val label = if (uiState.mode == AuthMode.SIGN_IN) {
                            "New here? Create account"
                        } else {
                            "Already have an account? Sign in"
                        }
                        Text(label)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AuthModeSwitch(
    selectedMode: AuthMode,
    onModeSelected: (AuthMode) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AuthModeChip(
                label = "Sign In",
                selected = selectedMode == AuthMode.SIGN_IN,
                onClick = { onModeSelected(AuthMode.SIGN_IN) },
                modifier = Modifier.weight(1f),
            )
            AuthModeChip(
                label = "Create Account",
                selected = selectedMode == AuthMode.SIGN_UP,
                onClick = { onModeSelected(AuthMode.SIGN_UP) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AuthModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (selected) 2.dp else 0.dp),
        modifier = modifier.height(44.dp),
    ) {
        Text(text = label)
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        supportingText = supportingText?.let { { Text(it) } },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun GenderSelector(
    selectedGender: String,
    onGenderChange: (String) -> Unit,
) {
    Column {
        Text(
            text = "Gender",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val options = listOf("Female", "Male", "Other", "Prefer not to say")
            options.forEach { option ->
                FilterChip(
                    selected = selectedGender == option,
                    onClick = { onGenderChange(option) },
                    label = { Text(option) },
                )
            }
        }
    }
}

@Composable
private fun PasswordStrengthIndicator(strength: PasswordStrength) {
    val (label, activeCount, color) = when (strength) {
        PasswordStrength.EMPTY -> Triple("Use 8+ chars, mix symbols for strong password", 0, MaterialTheme.colorScheme.outline)
        PasswordStrength.WEAK -> Triple("Weak password", 1, Color(0xFFE11D48))
        PasswordStrength.MEDIUM -> Triple("Medium password", 2, Color(0xFFF59E0B))
        PasswordStrength.STRONG -> Triple("Strong password", 3, Color(0xFF10B981))
    }

    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { index ->
                val active = index < activeCount
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .weight(1f)
                        .clip(CircleShape)
                        .background(
                            if (active) color else MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    HabitSyncTheme {
        LoginScreen(
            uiState = AuthUiState(
                mode = AuthMode.SIGN_UP,
                firstName = "Arshita",
                lastName = "Chaudhary",
                username = "arshita01",
                age = "22",
                heightCm = "163",
                weightKg = "52",
                gender = "Female",
                email = "hello@habitsync.app",
                password = "Pass@1234",
                passwordStrength = PasswordStrength.STRONG,
            ),
            onSwitchMode = {},
            onFirstNameChange = {},
            onLastNameChange = {},
            onUsernameChange = {},
            onAgeChange = {},
            onHeightChange = {},
            onWeightChange = {},
            onGenderChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onTogglePasswordVisibility = {},
            onToggleConfirmPasswordVisibility = {},
            onSubmit = {},
        )
    }
}
