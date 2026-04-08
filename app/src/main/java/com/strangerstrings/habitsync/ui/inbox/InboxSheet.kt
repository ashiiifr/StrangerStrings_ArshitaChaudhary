package com.strangerstrings.habitsync.ui.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class InboxType {
    FRIEND_REQUEST,
    CHALLENGE_INVITE,
    CHALLENGE_ACTIVITY,
    STREAK_MILESTONE,
    EVENING_REMINDER,
}

data class InboxItem(
    val id: String,
    val title: String,
    val message: String,
    val type: InboxType,
    val isRead: Boolean = false,
    val timestampMillis: Long = 0L,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxSheet(
    items: List<InboxItem>,
    onDismiss: () -> Unit,
    onMarkAllRead: () -> Unit,
    onAccept: (InboxItem) -> Unit,
    onDecline: (InboxItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Inbox", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onMarkAllRead) { Text("Mark all read") }
            }

            items.forEach { item ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.isRead) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        Text(item.message, style = MaterialTheme.typography.bodyMedium)
                        if (item.type == InboxType.FRIEND_REQUEST || item.type == InboxType.CHALLENGE_INVITE) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onAccept(item) }) { Text("Accept") }
                                TextButton(onClick = { onDecline(item) }) { Text("Decline") }
                            }
                        }
                    }
                }
            }
        }
    }
}
