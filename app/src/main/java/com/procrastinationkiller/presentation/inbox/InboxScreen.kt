package com.procrastinationkiller.presentation.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.procrastinationkiller.presentation.components.EmptyStateView
import com.procrastinationkiller.presentation.inbox.components.TaskSuggestionCard

@Composable
fun InboxScreen(
    viewModel: InboxViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Inbox",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (uiState.suggestions.isEmpty() && !uiState.isLoading) {
            item {
                EmptyStateView(
                    title = "No new suggestions",
                    subtitle = "Task suggestions from your notifications will appear here."
                )
            }
        } else {
            items(uiState.suggestions, key = { it.suggestedTitle + it.originalText }) { suggestion ->
                TaskSuggestionCard(
                    suggestion = suggestion,
                    onApprove = { viewModel.approveSuggestion(suggestion) },
                    onReject = { viewModel.rejectSuggestion(suggestion) },
                    onEdit = { viewModel.editSuggestion(suggestion, suggestion.suggestedTitle, suggestion.priority) }
                )
            }
        }
    }
}
