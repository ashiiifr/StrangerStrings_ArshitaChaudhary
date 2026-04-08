package com.strangerstrings.habitsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.strangerstrings.habitsync.navigation.HabitSyncNavHost
import com.strangerstrings.habitsync.ui.theme.HabitSyncTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HabitSyncTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HabitSyncNavHost()
                }
            }
        }
    }
}
