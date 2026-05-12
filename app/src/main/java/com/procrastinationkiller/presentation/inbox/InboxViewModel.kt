package com.procrastinationkiller.presentation.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.procrastinationkiller.domain.model.TaskPriority
import com.procrastinationkiller.domain.model.TaskSuggestion
import com.procrastinationkiller.domain.usecase.ApproveTaskUseCase
import com.procrastinationkiller.domain.usecase.RejectTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InboxUiState(
    val suggestions: List<TaskSuggestion> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null
)

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val approveTaskUseCase: ApproveTaskUseCase,
    private val rejectTaskUseCase: RejectTaskUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        loadSuggestions()
    }

    private fun loadSuggestions() {
        // In a full implementation, suggestions would come from a database/repository
        // For now we initialize with empty state
        _uiState.update { it.copy(isLoading = false) }
    }

    fun approveSuggestion(suggestion: TaskSuggestion) {
        viewModelScope.launch {
            approveTaskUseCase(suggestion)
            _uiState.update { state ->
                state.copy(
                    suggestions = state.suggestions - suggestion,
                    message = "Task approved: ${suggestion.suggestedTitle}"
                )
            }
        }
    }

    fun rejectSuggestion(suggestion: TaskSuggestion) {
        viewModelScope.launch {
            rejectTaskUseCase(suggestion)
            _uiState.update { state ->
                state.copy(
                    suggestions = state.suggestions - suggestion,
                    message = "Suggestion rejected"
                )
            }
        }
    }

    fun editSuggestion(original: TaskSuggestion, editedTitle: String, editedPriority: TaskPriority) {
        val edited = original.copy(
            suggestedTitle = editedTitle,
            priority = editedPriority
        )
        viewModelScope.launch {
            approveTaskUseCase(edited)
            _uiState.update { state ->
                state.copy(
                    suggestions = state.suggestions - original,
                    message = "Task saved: $editedTitle"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun addSuggestion(suggestion: TaskSuggestion) {
        _uiState.update { state ->
            state.copy(suggestions = state.suggestions + suggestion)
        }
    }
}
