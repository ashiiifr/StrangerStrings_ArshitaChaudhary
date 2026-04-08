package com.strangerstrings.habitsync.ui.auth

import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.strangerstrings.habitsync.viewmodel.AuthUiState
import java.util.Date

private enum class RecoveryStep { Username, Email, Dob }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    uiState: AuthUiState,
    onBackClick: () -> Unit,
    onRecoveryUsernameChange: (String) -> Unit,
    onRecoveryEmailChange: (String) -> Unit,
    onRecoveryDobSelected: (Long?) -> Unit,
    onSubmitRecovery: () -> Unit,
) {
    val context = LocalContext.current
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = RecoveryStep.entries[stepIndex]
    var showDatePicker by remember { mutableStateOf(false) }
    val picker = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = uiState.recoveryDobMillis,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBackClick) { Text("Back to Sign In") }
        Text("Recover Password", style = MaterialTheme.typography.headlineSmall)
        RecoveryProgressBar(progress = (stepIndex + 1f) / RecoveryStep.entries.size.toFloat())

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 5.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (step) {
                    RecoveryStep.Username -> OutlinedTextField(
                        value = uiState.recoveryUsername,
                        onValueChange = onRecoveryUsernameChange,
                        label = { Text("Username") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    RecoveryStep.Email -> OutlinedTextField(
                        value = uiState.recoveryEmail,
                        onValueChange = onRecoveryEmailChange,
                        label = { Text("Email") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    RecoveryStep.Dob -> Button(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(uiState.recoveryDobMillis?.let { toDate(it) } ?: "Select Date of Birth")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { if (stepIndex > 0) stepIndex-- },
                        enabled = stepIndex > 0,
                        modifier = Modifier.weight(1f),
                    ) { Text("Back") }

                    Button(
                        onClick = {
                            if (stepIndex == RecoveryStep.entries.lastIndex) {
                                onSubmitRecovery()
                            } else {
                                stepIndex++
                            }
                        },
                        enabled = !uiState.isRecoveryLoading && isRecoveryStepValid(uiState, step),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        if (stepIndex == RecoveryStep.entries.lastIndex && uiState.isRecoveryLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(if (stepIndex == RecoveryStep.entries.lastIndex) "Verify" else "Next")
                        }
                    }
                }
            }
        }

        uiState.recoveryErrorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        uiState.recoverySuccessMessage?.let {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(it, color = androidx.compose.ui.graphics.Color(0xFF16A34A), style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_APP_EMAIL)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                },
                            )
                        }.onFailure {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    },
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open Email App")
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRecoveryDobSelected(picker.selectedDateMillis)
                        showDatePicker = false
                    },
                ) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = picker)
        }
    }
}

@Composable
private fun RecoveryProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = CircleShape,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(6.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                ),
        )
    }
}

private fun isRecoveryStepValid(uiState: AuthUiState, step: RecoveryStep): Boolean {
    return when (step) {
        RecoveryStep.Username -> uiState.recoveryUsername.length in 3..12
        RecoveryStep.Email -> uiState.recoveryEmail.contains("@")
        RecoveryStep.Dob -> uiState.recoveryDobMillis != null
    }
}

private fun toDate(millis: Long): String = DateFormat.format("dd MMM yyyy", Date(millis)).toString()
