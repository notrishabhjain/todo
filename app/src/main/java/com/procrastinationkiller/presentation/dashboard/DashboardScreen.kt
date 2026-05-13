package com.procrastinationkiller.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.procrastinationkiller.presentation.components.EmptyStateView
import com.procrastinationkiller.presentation.components.TaskCard

@Composable
fun DashboardScreen(
    onTaskClick: (Long) -> Unit,
    isNotificationListenerEnabled: Boolean = true,
    isPostNotificationsGranted: Boolean = true,
    onOpenNotificationSettings: () -> Unit = {},
    onRequestPostNotifications: () -> Unit = {},
    onNavigateToTranscript: () -> Unit = {},
    onAddTaskManually: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFabMenu by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                AnimatedVisibility(
                    visible = showFabMenu,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Paste Meeting Transcript",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            SmallFloatingActionButton(
                                onClick = {
                                    showFabMenu = false
                                    onNavigateToTranscript()
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = "Paste Meeting Transcript"
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Add Task Manually",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            SmallFloatingActionButton(
                                onClick = {
                                    showFabMenu = false
                                    onAddTaskManually()
                                },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Add Task Manually"
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (showFabMenu) "Close menu" else "Add"
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (!isNotificationListenerEnabled) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Notification access is required for task detection",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = onOpenNotificationSettings) {
                                Text("Grant Access")
                            }
                        }
                    }
                }
            }

            if (!isNotificationListenerEnabled || !isPostNotificationsGranted) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Notification Health",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (!isNotificationListenerEnabled) {
                                Text(
                                    text = "Warning: Notification listener is disabled. Task detection will not work.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            if (!isPostNotificationsGranted) {
                                Text(
                                    text = "Warning: POST_NOTIFICATIONS permission not granted. Reminders will be silent.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = onRequestPostNotifications) {
                                    Text("Grant Permission")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Pending",
                        value = uiState.pendingTaskCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "High Priority",
                        value = uiState.highPriorityCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Done Today",
                        value = uiState.completedTodayCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Completion",
                        value = "${(uiState.completionRate * 100).toInt()}%",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Today's Tasks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (uiState.todaysTasks.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyStateView(
                        title = "No tasks due today",
                        subtitle = "You're all caught up! Check inbox for new suggestions."
                    )
                }
            } else {
                items(uiState.todaysTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onClick = { onTaskClick(task.id) },
                        onComplete = { viewModel.completeTask(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
