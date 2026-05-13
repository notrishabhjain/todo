package com.procrastinationkiller.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.procrastinationkiller.data.repository.InstalledAppsProvider
import com.procrastinationkiller.data.repository.UserPreferencesRepository
import com.procrastinationkiller.domain.model.AppCategory
import com.procrastinationkiller.domain.model.InstalledAppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MonitoredAppsUiState(
    val allApps: List<InstalledAppInfo> = emptyList(),
    val filteredApps: List<InstalledAppInfo> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: AppCategory? = null,
    val monitoredApps: Set<String> = emptySet(),
    val isLoading: Boolean = true
)

@HiltViewModel
class MonitoredAppsViewModel @Inject constructor(
    private val installedAppsProvider: InstalledAppsProvider,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonitoredAppsUiState())
    val uiState: StateFlow<MonitoredAppsUiState> = _uiState.asStateFlow()

    private val recommendedPackages = setOf(
        "com.whatsapp",
        "org.telegram.messenger",
        "com.slack",
        "com.google.android.gm",
        "com.android.mms",
        "com.google.android.apps.messaging",
        "com.microsoft.teams",
        "org.thoughtcrime.securesms"
    )

    init {
        loadApps()
        observeMonitoredApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val apps = installedAppsProvider.getInstalledApps()
            _uiState.value = _uiState.value.copy(
                allApps = apps,
                isLoading = false
            )
            applyFilters()
        }
    }

    private fun observeMonitoredApps() {
        viewModelScope.launch {
            userPreferencesRepository.monitoredApps.collect { apps ->
                _uiState.value = _uiState.value.copy(monitoredApps = apps)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun setSelectedCategory(category: AppCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        applyFilters()
    }

    fun toggleApp(packageName: String, enabled: Boolean) {
        val currentApps = _uiState.value.monitoredApps.toMutableSet()
        if (enabled) {
            currentApps.add(packageName)
        } else {
            currentApps.remove(packageName)
        }
        viewModelScope.launch {
            userPreferencesRepository.setMonitoredApps(currentApps)
        }
    }

    private fun applyFilters() {
        val state = _uiState.value
        val query = state.searchQuery.lowercase().trim()
        val category = state.selectedCategory

        val filtered = state.allApps.filter { app ->
            val matchesSearch = query.isEmpty() || app.label.lowercase().contains(query)
            val matchesCategory = category == null || app.category == category
            matchesSearch && matchesCategory
        }

        val recommended = filtered.filter { it.packageName in recommendedPackages }
        val others = filtered.filter { it.packageName !in recommendedPackages }

        _uiState.value = state.copy(filteredApps = recommended + others)
    }
}
