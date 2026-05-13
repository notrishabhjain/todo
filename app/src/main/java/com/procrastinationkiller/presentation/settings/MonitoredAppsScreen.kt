package com.procrastinationkiller.presentation.settings

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.procrastinationkiller.domain.model.AppCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoredAppsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: MonitoredAppsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Monitored Apps (${uiState.monitoredApps.size} selected)")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search apps...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            CategoryFilterChips(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { viewModel.setSelectedCategory(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading) {
                Text(
                    text = "Loading apps...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = uiState.filteredApps,
                        key = { it.packageName }
                    ) { app ->
                        AppListItem(
                            label = app.label,
                            packageName = app.packageName,
                            isMonitored = uiState.monitoredApps.contains(app.packageName),
                            onToggle = { enabled -> viewModel.toggleApp(app.packageName, enabled) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterChips(
    selectedCategory: AppCategory?,
    onCategorySelected: (AppCategory?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") }
            )
        }
        item {
            FilterChip(
                selected = selectedCategory == AppCategory.COMMUNICATION,
                onClick = {
                    onCategorySelected(
                        if (selectedCategory == AppCategory.COMMUNICATION) null
                        else AppCategory.COMMUNICATION
                    )
                },
                label = { Text("Communication") }
            )
        }
        item {
            FilterChip(
                selected = selectedCategory == AppCategory.PRODUCTIVITY,
                onClick = {
                    onCategorySelected(
                        if (selectedCategory == AppCategory.PRODUCTIVITY) null
                        else AppCategory.PRODUCTIVITY
                    )
                },
                label = { Text("Productivity") }
            )
        }
        item {
            FilterChip(
                selected = selectedCategory == AppCategory.SOCIAL,
                onClick = {
                    onCategorySelected(
                        if (selectedCategory == AppCategory.SOCIAL) null
                        else AppCategory.SOCIAL
                    )
                },
                label = { Text("Social") }
            )
        }
        item {
            FilterChip(
                selected = selectedCategory == AppCategory.OTHER,
                onClick = {
                    onCategorySelected(
                        if (selectedCategory == AppCategory.OTHER) null
                        else AppCategory.OTHER
                    )
                },
                label = { Text("Other") }
            )
        }
    }
}

@Composable
private fun AppListItem(
    label: String,
    packageName: String,
    isMonitored: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = isMonitored,
            onCheckedChange = onToggle
        )
    }
}
