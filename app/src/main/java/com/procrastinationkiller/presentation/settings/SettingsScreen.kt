package com.procrastinationkiller.presentation.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import com.procrastinationkiller.domain.engine.ReminderFrequency
import com.procrastinationkiller.domain.model.ReminderMode

@Composable
fun SettingsScreen(
    onNavigateToKeywords: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToExportImport: () -> Unit = {},
    onNavigateToTranscript: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Reminder Settings Section
        item {
            SectionHeader("Reminder Settings")
        }

        item {
            ReminderModeSelector(
                selectedMode = uiState.reminderMode,
                onModeSelected = { viewModel.setReminderMode(it) }
            )
        }

        item {
            ReminderFrequencySelector(
                selectedFrequency = uiState.reminderFrequency,
                onFrequencySelected = { viewModel.setReminderFrequency(it) }
            )
        }

        // Notification Settings Section
        item {
            SectionHeader("Monitored Apps")
        }

        item {
            MonitoredAppsSection(
                monitoredApps = uiState.monitoredApps,
                onToggleApp = { app, enabled -> viewModel.toggleApp(app, enabled) }
            )
        }

        // VIP Contacts Section
        item {
            SectionHeader("VIP Contacts")
        }

        items(uiState.contacts) { contact ->
            ContactItem(
                name = contact.name,
                isVip = contact.isEscalationTarget,
                onDelete = { viewModel.deleteContact(contact) }
            )
        }

        // Keyword Management Section
        item {
            SectionHeader("Keyword Management")
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToKeywords() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Custom Keywords",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Add or remove action, urgency, and time keywords",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.Edit, contentDescription = "Edit Keywords")
                }
            }
        }

        // Tools & Reports Section
        item {
            SectionHeader("Tools & Reports")
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAnalytics() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Analytics Dashboard",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "View task completion trends and statistics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.BarChart, contentDescription = "Analytics")
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToInsights() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Productivity Insights",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "See productivity patterns and recommendations",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.Lightbulb, contentDescription = "Insights")
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToExportImport() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Export/Import Data",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Backup or restore your task data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Export/Import")
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToTranscript() },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Meeting Transcript",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Extract tasks from meeting notes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.Description, contentDescription = "Meeting Transcript")
                }
            }
        }

        // About Section
        item {
            SectionHeader("About")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Procrastination Killer",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version 1.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ReminderModeSelector(
    selectedMode: ReminderMode,
    onModeSelected: (ReminderMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Reminder Mode",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedMode.name.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ReminderMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                        },
                        onClick = {
                            onModeSelected(mode)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderFrequencySelector(
    selectedFrequency: ReminderFrequency,
    onFrequencySelected: (ReminderFrequency) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Reminder Frequency",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedFrequency.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ReminderFrequency.entries.forEach { frequency ->
                    DropdownMenuItem(
                        text = { Text(frequency.displayName) },
                        onClick = {
                            onFrequencySelected(frequency)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MonitoredAppsSection(
    monitoredApps: Set<String>,
    onToggleApp: (String, Boolean) -> Unit
) {
    val availableApps = listOf(
        "com.whatsapp" to "WhatsApp",
        "org.telegram.messenger" to "Telegram",
        "com.slack" to "Slack",
        "com.google.android.gm" to "Gmail",
        "com.android.mms" to "Messages"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            availableApps.forEach { (packageName, displayName) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = displayName, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = monitoredApps.contains(packageName),
                        onCheckedChange = { enabled -> onToggleApp(packageName, enabled) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactItem(
    name: String,
    isVip: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = name, style = MaterialTheme.typography.bodyLarge)
                if (isVip) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VIP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete contact")
            }
        }
    }
}
