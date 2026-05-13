package com.procrastinationkiller.presentation.inbox

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.procrastinationkiller.presentation.components.EmptyStateView
import com.procrastinationkiller.presentation.inbox.components.TaskSuggestionCard

@OptIn(ExperimentalMaterial3Api::class)
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
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        when (value) {
                            SwipeToDismissBoxValue.StartToEnd -> {
                                viewModel.approveSuggestion(suggestion)
                                true
                            }
                            SwipeToDismissBoxValue.EndToStart -> {
                                viewModel.rejectSuggestion(suggestion)
                                true
                            }
                            else -> false
                        }
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val color by animateColorAsState(
                            targetValue = when (dismissState.targetValue) {
                                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50)
                                SwipeToDismissBoxValue.EndToStart -> Color(0xFFF44336)
                                else -> Color.Transparent
                            },
                            label = "inbox_swipe_bg_color"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color)
                                .padding(horizontal = 20.dp),
                            contentAlignment = when (dismissState.targetValue) {
                                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                else -> Alignment.CenterEnd
                            }
                        ) {
                            when (dismissState.targetValue) {
                                SwipeToDismissBoxValue.StartToEnd -> {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Approve",
                                        tint = Color.White
                                    )
                                }
                                SwipeToDismissBoxValue.EndToStart -> {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Reject",
                                        tint = Color.White
                                    )
                                }
                                else -> {}
                            }
                        }
                    },
                    content = {
                        TaskSuggestionCard(
                            suggestion = suggestion,
                            onApprove = { viewModel.approveSuggestion(suggestion) },
                            onReject = { viewModel.rejectSuggestion(suggestion) },
                            onEdit = { viewModel.editSuggestion(suggestion, suggestion.suggestedTitle, suggestion.priority) }
                        )
                    }
                )
            }
        }
    }
}
