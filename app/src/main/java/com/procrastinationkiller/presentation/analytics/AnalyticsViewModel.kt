package com.procrastinationkiller.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.domain.usecase.AnalyticsData
import com.procrastinationkiller.domain.usecase.AnalyticsUseCase
import com.procrastinationkiller.domain.usecase.TimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsUiState(
    val analyticsData: AnalyticsData = AnalyticsData(),
    val selectedTimeRange: TimeRange = TimeRange.WEEK,
    val isLoading: Boolean = true
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsUseCase: AnalyticsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAnalytics(TimeRange.WEEK)
    }

    fun setTimeRange(timeRange: TimeRange) {
        _uiState.value = _uiState.value.copy(selectedTimeRange = timeRange)
        loadAnalytics(timeRange)
    }

    private fun loadAnalytics(timeRange: TimeRange) {
        viewModelScope.launch {
            analyticsUseCase.getAnalytics(timeRange).collect { data ->
                _uiState.value = _uiState.value.copy(
                    analyticsData = data,
                    isLoading = false
                )
            }
        }
    }
}
