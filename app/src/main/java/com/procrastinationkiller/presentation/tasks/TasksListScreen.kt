package com.procrastinationkiller.presentation.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.procrastinationkiller.domain.model.TaskPriority
import com.procrastinationkiller.presentation.components.EmptyStateView
import com.procrastinationkiller.presentation.components.FilterChipData
import com.procrastinationkiller.presentation.components.FilterChipsRow
import com.procrastinationkiller.presentation.components.TaskCard
import com.procrastinationkiller.presentation.tasks.components.ManualTaskCreateDialog

@Composable
fun TasksListScreen(
    onTaskClick: (Long) -> Unit,
    viewModel: TasksListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Task"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Tasks",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                val priorityChips = TaskPriority.entries.map { priority ->
                    FilterChipData(
                        label = priority.name,
                        selected = uiState.selectedPriority == priority,
                        onClick = { viewModel.setFilter(priority) }
                    )
                }
                FilterChipsRow(chips = priorityChips)
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (uiState.tasks.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyStateView(
                        title = "No tasks yet",
                        subtitle = "Tap + to create a task or check your inbox for suggestions."
                    )
                }
            } else {
                items(uiState.tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onClick = { onTaskClick(task.id) }
                    )
                }
            }
        }
    }

    if (uiState.showCreateDialog) {
        ManualTaskCreateDialog(
            onDismiss = { viewModel.hideCreateDialog() },
            onCreate = { title, description, priority, deadline ->
                viewModel.createTask(title, description, priority, deadline)
            }
        )
    }
}
