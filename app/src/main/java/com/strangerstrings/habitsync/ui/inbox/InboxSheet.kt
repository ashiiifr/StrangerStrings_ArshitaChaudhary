package com.strangerstrings.habitsync.ui.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strangerstrings.habitsync.ui.theme.CharcoalDark
import com.strangerstrings.habitsync.ui.theme.CharcoalLight
import com.strangerstrings.habitsync.ui.theme.CharcoalMid
import com.strangerstrings.habitsync.ui.theme.Cream
import com.strangerstrings.habitsync.ui.theme.ErrorRed
import com.strangerstrings.habitsync.ui.theme.GoldSoft
import com.strangerstrings.habitsync.ui.theme.OrangeGlow

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
    onDeleteNotification: (InboxItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = CharcoalDark,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Inbox",
                    style = MaterialTheme.typography.titleLarge,
                    color = Cream,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onMarkAllRead) {
                    Text("Mark all read", color = OrangeGlow)
                }
            }

            // Items
            items.forEach { item ->
                val dismissible = item.type != InboxType.FRIEND_REQUEST && item.type != InboxType.CHALLENGE_INVITE
                if (dismissible) {
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                                onDeleteNotification(item)
                                true
                            } else {
                                false
                            }
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ErrorRed, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 20.dp, vertical = 24.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Delete",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        },
                    ) {
                        InboxCard(item = item, onAccept = onAccept, onDecline = onDecline)
                    }
                } else {
                    InboxCard(item = item, onAccept = onAccept, onDecline = onDecline)
                }
            }
        }
    }
}

@Composable
private fun InboxCard(
    item: InboxItem,
    onAccept: (InboxItem) -> Unit,
    onDecline: (InboxItem) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isRead) CharcoalMid else OrangeGlow.copy(alpha = 0.12f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isRead) 0.dp else 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (!item.isRead) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(8.dp)
                        .background(OrangeGlow, CircleShape),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (item.isRead) Cream.copy(alpha = 0.7f) else Cream,
                    fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.SemiBold,
                )
                Text(
                    item.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.isRead) Cream.copy(alpha = 0.5f) else Cream.copy(alpha = 0.8f),
                )
                if (item.type == InboxType.FRIEND_REQUEST || item.type == InboxType.CHALLENGE_INVITE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onAccept(item) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangeGlow,
                                contentColor = CharcoalDark,
                            ),
                        ) {
                            Text("Accept", fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(onClick = { onDecline(item) }) {
                            Text("Decline", color = Cream.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}
