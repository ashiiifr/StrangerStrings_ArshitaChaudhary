package com.strangerstrings.habitsync.navigation

sealed class HabitSyncDestination(val route: String) {
    data object Login : HabitSyncDestination("login")
    data object Home : HabitSyncDestination("home")
    data object Leaderboard : HabitSyncDestination("leaderboard")
    data object Feed : HabitSyncDestination("feed")
}
