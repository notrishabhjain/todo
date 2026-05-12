package com.procrastinationkiller.presentation.inbox.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.procrastinationkiller.domain.engine.whatsapp.ChatType
import com.procrastinationkiller.domain.engine.whatsapp.WhatsAppContext
import com.procrastinationkiller.domain.model.ContactPriority
import com.procrastinationkiller.domain.model.TaskSuggestion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WhatsAppSuggestionCard(
    suggestion: TaskSuggestion,
    onCreateTask: (TaskSuggestion) -> Unit,
    onIgnore: (TaskSuggestion) -> Unit,
    onAutoApprove: (TaskSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = suggestion.whatsAppContext ?: return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: sender name with priority badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = context.senderName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                PriorityBadge(priority = context.contactPriority)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Group name if applicable
            if (context.chatType == ChatType.GROUP && context.groupName != null) {
                Text(
                    text = "in ${context.groupName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Message snippet
            Text(
                text = context.messageSnippet,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Timestamp
            Text(
                text = formatTimestamp(context.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Suggested task title
            Text(
                text = suggestion.suggestedTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onAutoApprove(suggestion) }) {
                    Text("Always Auto-Approve")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = { onIgnore(suggestion) }) {
                    Text("Ignore")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onCreateTask(suggestion) }) {
                    Text("Create Task")
                }
            }
        }
    }
}

@Composable
private fun PriorityBadge(priority: ContactPriority) {
    val (text, color) = when (priority) {
        ContactPriority.VIP -> "VIP" to MaterialTheme.colorScheme.error
        ContactPriority.HIGH_PRIORITY -> "High" to MaterialTheme.colorScheme.tertiary
        ContactPriority.NORMAL -> "Normal" to MaterialTheme.colorScheme.outline
        ContactPriority.IGNORE -> "Ignore" to MaterialTheme.colorScheme.outlineVariant
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Bold
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
