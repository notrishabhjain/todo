package com.procrastinationkiller.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.procrastinationkiller.data.local.entity.ContactEntity
import com.procrastinationkiller.data.local.entity.KeywordEntity
import com.procrastinationkiller.data.repository.UserPreferencesRepository
import com.procrastinationkiller.domain.engine.ReminderFrequency
import com.procrastinationkiller.domain.model.ContactPriority
import com.procrastinationkiller.domain.model.ReminderMode
import com.procrastinationkiller.domain.repository.ContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val reminderMode: ReminderMode = ReminderMode.NORMAL,
    val reminderFrequency: ReminderFrequency = ReminderFrequency.EVERY_30_MIN,
    val monitoredApps: Set<String> = emptySet(),
    val isDarkMode: Boolean = true,
    val contacts: List<ContactEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            userPreferencesRepository.reminderMode.collect { mode ->
                _uiState.value = _uiState.value.copy(reminderMode = mode)
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.reminderFrequency.collect { frequency ->
                _uiState.value = _uiState.value.copy(reminderFrequency = frequency)
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.monitoredApps.collect { apps ->
                _uiState.value = _uiState.value.copy(monitoredApps = apps)
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.isDarkMode.collect { isDark ->
                _uiState.value = _uiState.value.copy(isDarkMode = isDark)
            }
        }
        viewModelScope.launch {
            contactRepository.getAllContacts().collect { contacts ->
                _uiState.value = _uiState.value.copy(contacts = contacts, isLoading = false)
            }
        }
    }

    fun setReminderMode(mode: ReminderMode) {
        viewModelScope.launch {
            userPreferencesRepository.setReminderMode(mode)
        }
    }

    fun setReminderFrequency(frequency: ReminderFrequency) {
        viewModelScope.launch {
            userPreferencesRepository.setReminderFrequency(frequency)
        }
    }

    fun setMonitoredApps(apps: Set<String>) {
        viewModelScope.launch {
            userPreferencesRepository.setMonitoredApps(apps)
        }
    }

    fun setDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDarkMode(isDark)
        }
    }

    fun addContact(name: String, priority: ContactPriority) {
        viewModelScope.launch {
            val contact = ContactEntity(
                name = name,
                priority = priority.name,
                isEscalationTarget = priority == ContactPriority.VIP || priority == ContactPriority.HIGH_PRIORITY
            )
            contactRepository.insertContact(contact)
        }
    }

    fun deleteContact(contact: ContactEntity) {
        viewModelScope.launch {
            contactRepository.deleteContact(contact)
        }
    }

    fun toggleApp(packageName: String, enabled: Boolean) {
        val currentApps = _uiState.value.monitoredApps.toMutableSet()
        if (enabled) {
            currentApps.add(packageName)
        } else {
            currentApps.remove(packageName)
        }
        setMonitoredApps(currentApps)
    }
}
