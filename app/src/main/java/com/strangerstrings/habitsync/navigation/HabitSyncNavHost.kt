package com.strangerstrings.habitsync.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.strangerstrings.habitsync.ui.auth.LoginScreen
import com.strangerstrings.habitsync.ui.feed.FeedScreen
import com.strangerstrings.habitsync.ui.home.HomeScreen
import com.strangerstrings.habitsync.ui.leaderboard.LeaderboardScreen
import com.strangerstrings.habitsync.viewmodel.AuthViewModel
import com.strangerstrings.habitsync.viewmodel.FeedViewModel
import com.strangerstrings.habitsync.viewmodel.HomeViewModel
import com.strangerstrings.habitsync.viewmodel.LeaderboardViewModel

@Composable
fun HabitSyncNavHost(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    NavHost(
        navController = navController,
        startDestination = if (authUiState.isLoggedIn) {
            HabitSyncDestination.Home.route
        } else {
            HabitSyncDestination.Login.route
        },
        modifier = modifier,
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
        composable(HabitSyncDestination.Login.route) {
            LaunchedEffect(authUiState.isLoggedIn) {
                if (authUiState.isLoggedIn) {
                    navController.navigate(HabitSyncDestination.Home.route) {
                        popUpTo(HabitSyncDestination.Login.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                    password = ""
                }
            }

            LoginScreen(
                uiState = authUiState,
                email = email,
                password = password,
                onEmailChange = { email = it },
                onPasswordChange = { password = it },
                onLoginClick = {
                    authViewModel.signIn(email = email, password = password)
                },
            )
        }

        composable(HabitSyncDestination.Home.route) {
            val viewModel: HomeViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            HomeScreen(
                uiState = uiState,
                onMarkHabitDone = viewModel::markHabitDone,
                onAddHabitClick = viewModel::addHabit,
                onLeaderboardClick = {
                    navController.navigate(HabitSyncDestination.Leaderboard.route)
                },
                onFeedClick = {
                    navController.navigate(HabitSyncDestination.Feed.route)
                },
                onLogoutClick = {
                    authViewModel.signOut()
                    password = ""
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
