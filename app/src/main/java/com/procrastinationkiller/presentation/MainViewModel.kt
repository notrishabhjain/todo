package com.procrastinationkiller.presentation

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.procrastinationkiller.data.repository.UserPreferencesRepository
import com.procrastinationkiller.domain.repository.OnboardingRepository
import com.procrastinationkiller.presentation.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    private val _isNotificationListenerEnabled = MutableStateFlow(false)
    val isNotificationListenerEnabled: StateFlow<Boolean> = _isNotificationListenerEnabled.asStateFlow()

    val isDarkMode: StateFlow<Boolean> = userPreferencesRepository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        determineStartDestination()
    }

    private fun determineStartDestination() {
        viewModelScope.launch {
            val onboardingCompleted = onboardingRepository.onboardingCompleted.first()
            val listenerEnabled = checkNotificationListenerEnabled()
            _isNotificationListenerEnabled.value = listenerEnabled

            _startDestination.value = if (!onboardingCompleted || !listenerEnabled) {
                Routes.ONBOARDING
            } else {
                Routes.DASHBOARD
            }
        }
    }

    private fun checkNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val componentName = ComponentName(
            context,
            "com.procrastinationkiller.service.NotificationCaptureService"
        )
        return enabledListeners.contains(componentName.flattenToString())
    }

    fun refreshNotificationListenerStatus() {
        _isNotificationListenerEnabled.value = checkNotificationListenerEnabled()
    }
}
