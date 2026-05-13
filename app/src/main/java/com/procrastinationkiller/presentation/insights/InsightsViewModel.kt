package com.procrastinationkiller.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.procrastinationkiller.domain.engine.insights.ProductivityInsights
import com.procrastinationkiller.domain.usecase.ProductivityInsightsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InsightsUiState(
    val insights: ProductivityInsights = ProductivityInsights(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val insightsUseCase: ProductivityInsightsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        loadInsights()
    }

    private fun loadInsights() {
        viewModelScope.launch {
            insightsUseCase.getInsights().collect { insights ->
                _uiState.value = InsightsUiState(
                    insights = insights,
                    isLoading = false
                )
            }
        }
    }
}
