package com.strangerstrings.habitsync.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.strangerstrings.habitsync.ui.auth.ForgotPasswordScreen
import com.strangerstrings.habitsync.ui.auth.LoginScreen
import com.strangerstrings.habitsync.ui.feed.FeedScreen
import com.strangerstrings.habitsync.ui.leaderboard.LeaderboardScreen
import com.strangerstrings.habitsync.ui.main.MainShellScreen
import com.strangerstrings.habitsync.ui.onboarding.OnboardingScreen
import com.strangerstrings.habitsync.viewmodel.AppEntryViewModel
import com.strangerstrings.habitsync.viewmodel.AuthMode
import com.strangerstrings.habitsync.viewmodel.AuthViewModel
import com.strangerstrings.habitsync.viewmodel.FeedViewModel
import com.strangerstrings.habitsync.viewmodel.HomeViewModel
import com.strangerstrings.habitsync.viewmodel.LeaderboardViewModel
import com.strangerstrings.habitsync.viewmodel.OnboardingViewModel

@Composable
fun HabitSyncNavHost(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(authViewModel) {
        authViewModel.passwordSaveEvents.collect { request ->
            runCatching {
                CredentialManager.create(context).createCredential(
                    context = context,
                    request = CreatePasswordRequest(
                        id = request.email,
                        password = request.password,
                    ),
                )
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = HabitSyncDestination.Entry.route,
        modifier = modifier.safeDrawingPadding(),
        enterTransition = {
            fadeIn(animationSpec = tween(220)) +
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(300),
                )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(220)) +
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(260),
                )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(180)) +
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(260),
                )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(180)) +
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(240),
                )
        },
    ) {
        composable(HabitSyncDestination.Entry.route) {
            val entryViewModel: AppEntryViewModel = viewModel()
            val entryUiState by entryViewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(entryUiState.isLoading, entryUiState.destinationRoute) {
                if (!entryUiState.isLoading && entryUiState.destinationRoute.isNotBlank()) {
                    navController.navigate(entryUiState.destinationRoute) {
                        popUpTo(HabitSyncDestination.Entry.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }
            EntryLoadingScreen()
        }

        composable(HabitSyncDestination.Onboarding.route) {
            val onboardingViewModel: OnboardingViewModel = viewModel()
            val onboardingUiState by onboardingViewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(onboardingViewModel) {
                onboardingViewModel.navigationEvents.collect {
                    navController.navigate(HabitSyncDestination.Login.route) {
                        popUpTo(HabitSyncDestination.Onboarding.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }

            OnboardingScreen(
                uiState = onboardingUiState,
                onSkipClick = onboardingViewModel::completeOnboarding,
                onGetStartedClick = onboardingViewModel::completeOnboarding,
            )
        }

        composable(HabitSyncDestination.Login.route) {
            LaunchedEffect(authUiState.isLoggedIn) {
                if (authUiState.isLoggedIn) {
                    navController.navigate(HabitSyncDestination.Home.route) {
                        popUpTo(HabitSyncDestination.Login.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }

            LoginScreen(
                uiState = authUiState,
                onSwitchMode = authViewModel::switchMode,
                onFirstNameChange = authViewModel::onFirstNameChange,
                onLastNameChange = authViewModel::onLastNameChange,
                onUsernameChange = authViewModel::onUsernameChange,
                onDateOfBirthSelected = authViewModel::onDateOfBirthSelected,
                onHeightChange = authViewModel::onHeightChange,
                onWeightChange = authViewModel::onWeightChange,
                onGenderChange = authViewModel::onGenderChange,
                onEmailChange = authViewModel::onEmailChange,
                onPasswordChange = authViewModel::onPasswordChange,
                onConfirmPasswordChange = authViewModel::onConfirmPasswordChange,
                onForgotPasswordClick = {
                    navController.navigate(HabitSyncDestination.ForgotPassword.route)
                },
                onTogglePasswordVisibility = authViewModel::togglePasswordVisibility,
                onToggleConfirmPasswordVisibility = authViewModel::toggleConfirmPasswordVisibility,
                onSubmit = authViewModel::submit,
            )
        }

        composable(HabitSyncDestination.ForgotPassword.route) {
            ForgotPasswordScreen(
                uiState = authUiState,
                onBackClick = { navController.popBackStack() },
                onRecoveryUsernameChange = authViewModel::onRecoveryUsernameChange,
                onRecoveryEmailChange = authViewModel::onRecoveryEmailChange,
                onRecoveryDobSelected = authViewModel::onRecoveryDobSelected,
                onSubmitRecovery = authViewModel::submitRecovery,
            )
        }

        composable(HabitSyncDestination.Home.route) {
            val viewModel: HomeViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            MainShellScreen(
                homeUiState = uiState,
                onMarkHabitDone = { habitId -> viewModel.markHabitDone(habitId) },
                onAddHabitClick = viewModel::addHabit,
                onLogoutClick = {
                    authViewModel.signOut()
                    authViewModel.switchMode(AuthMode.SIGN_IN)
                    navController.navigate(HabitSyncDestination.Login.route) {
                        popUpTo(HabitSyncDestination.Home.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(HabitSyncDestination.Leaderboard.route) {
            val leaderboardViewModel: LeaderboardViewModel = viewModel()
            val uiState by leaderboardViewModel.uiState.collectAsStateWithLifecycle()
            LeaderboardScreen(
                uiState = uiState,
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(HabitSyncDestination.Feed.route) {
            val feedViewModel: FeedViewModel = viewModel()
            val uiState by feedViewModel.uiState.collectAsStateWithLifecycle()
            FeedScreen(
                uiState = uiState,
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun EntryLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
