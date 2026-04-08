package com.strangerstrings.habitsync.navigation

sealed class HabitSyncDestination(val route: String) {
    data object Entry : HabitSyncDestination("entry")
    data object Onboarding : HabitSyncDestination("onboarding")
    data object Login : HabitSyncDestination("login")
    data object ForgotPassword : HabitSyncDestination("forgot_password")
    data object Home : HabitSyncDestination("home")
    data object Leaderboard : HabitSyncDestination("leaderboard")
    data object Feed : HabitSyncDestination("feed")
}
