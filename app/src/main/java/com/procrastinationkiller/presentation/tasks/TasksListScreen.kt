package com.procrastinationkiller.presentation.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.procrastinationkiller.domain.model.TaskPriority
import com.procrastinationkiller.presentation.components.EmptyStateView
import com.procrastinationkiller.presentation.components.FilterChipData
import com.procrastinationkiller.presentation.components.FilterChipsRow
import com.procrastinationkiller.presentation.components.TaskCard
import com.procrastinationkiller.presentation.tasks.components.ManualTaskCreateDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksListScreen(
    onTaskClick: (Long) -> Unit,
    viewModel: TasksListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val hapticFeedback = LocalHapticFeedback.current
    var dismissedTaskIds by remember { mutableStateOf(setOf<Long>()) }

    LaunchedEffect(uiState.tasks) {
        dismissedTaskIds = emptySet()
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                val statusChips = listOf(
                    FilterChipData(
                        label = "Active",
                        selected = !uiState.showCompleted,
                        onClick = { viewModel.toggleShowCompleted(false) }
                    ),
                    FilterChipData(
                        label = "Completed",
                        selected = uiState.showCompleted,
                        onClick = { viewModel.toggleShowCompleted(true) }
                    )
                )
                FilterChipsRow(chips = statusChips)
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
                val displayedTasks = uiState.tasks.filter { it.id !in dismissedTaskIds }
                items(displayedTasks, key = { it.id }) { task ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.StartToEnd) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                dismissedTaskIds = dismissedTaskIds + task.id
                                viewModel.completeTask(task.id)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    LaunchedEffect(task.id) {
                        dismissState.reset()
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color by animateColorAsState(
                                targetValue = when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50)
                                    else -> Color.Transparent
                                },
                                label = "swipe_bg_color"
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Complete",
                                        tint = Color.White
                                    )
                                }
                            }
                        },
                        enableDismissFromEndToStart = false,
                        content = {
                            TaskCard(
                                task = task,
                                onClick = { onTaskClick(task.id) },
                                onComplete = { viewModel.completeTask(task.id) }
                            )
                        }
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
