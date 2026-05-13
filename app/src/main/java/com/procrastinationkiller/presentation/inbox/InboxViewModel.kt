package com.procrastinationkiller.presentation.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.procrastinationkiller.data.local.dao.TaskSuggestionDao
import com.procrastinationkiller.domain.model.ContactPriority
import com.procrastinationkiller.domain.model.TaskPriority
import com.procrastinationkiller.domain.model.TaskSuggestion
import com.procrastinationkiller.domain.repository.ContactRepository
import com.procrastinationkiller.domain.usecase.ApproveTaskUseCase
import com.procrastinationkiller.domain.usecase.RejectTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InboxUiState(
    val suggestions: List<TaskSuggestion> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null,
    val snackbarMessage: String? = null
)

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val approveTaskUseCase: ApproveTaskUseCase,
    private val rejectTaskUseCase: RejectTaskUseCase,
    private val taskSuggestionDao: TaskSuggestionDao,
    private val contactRepository: ContactRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        loadSuggestions()
    }

    private fun loadSuggestions() {
        viewModelScope.launch {
            taskSuggestionDao.getPendingNonAutoApprove("PENDING").collect { entities ->
                val suggestions = entities
                    .map { entity ->
                    TaskSuggestion(
                        id = entity.id,
                        suggestedTitle = entity.suggestedTitle,
                        description = entity.description,
                        priority = try {
                            TaskPriority.valueOf(entity.priority)
                        } catch (_: IllegalArgumentException) {
                            TaskPriority.MEDIUM
                        },
                        dueDate = entity.dueDate,
                        sourceApp = entity.sourceApp,
                        sender = entity.sender,
                        originalText = entity.originalText,
                        confidence = entity.confidence,
                        autoApprove = entity.autoApprove
                    )
                }

                _uiState.update { it.copy(suggestions = suggestions, isLoading = false) }
            }
        }
    }

    fun approveSuggestion(suggestion: TaskSuggestion) {
        val currentSuggestions = _uiState.value.suggestions
        val suggestionIndex = currentSuggestions.indexOf(suggestion)

        // Optimistic removal
        _uiState.update { state ->
            state.copy(suggestions = state.suggestions - suggestion)
        }

        viewModelScope.launch {
            try {
                approveTaskUseCase(suggestion)
                updateSuggestionStatus(suggestion, "APPROVED")
                _uiState.update { state ->
                    state.copy(message = "Task approved: ${suggestion.suggestedTitle}")
                }
            } catch (_: Exception) {
                // Rollback on failure
                _uiState.update { state ->
                    val mutableSuggestions = state.suggestions.toMutableList()
                    val insertIndex = suggestionIndex.coerceAtMost(mutableSuggestions.size)
                    mutableSuggestions.add(insertIndex, suggestion)
                    state.copy(
                        suggestions = mutableSuggestions,
                        snackbarMessage = "Failed to approve suggestion"
                    )
                }
            }
        }
    }

    fun rejectSuggestion(suggestion: TaskSuggestion) {
        val currentSuggestions = _uiState.value.suggestions
        val suggestionIndex = currentSuggestions.indexOf(suggestion)

        // Optimistic removal
        _uiState.update { state ->
            state.copy(suggestions = state.suggestions - suggestion)
        }

        viewModelScope.launch {
            try {
                rejectTaskUseCase(suggestion)
                updateSuggestionStatus(suggestion, "REJECTED")
                _uiState.update { state ->
                    state.copy(message = "Suggestion rejected")
                }
            } catch (_: Exception) {
                // Rollback on failure
                _uiState.update { state ->
                    val mutableSuggestions = state.suggestions.toMutableList()
                    val insertIndex = suggestionIndex.coerceAtMost(mutableSuggestions.size)
                    mutableSuggestions.add(insertIndex, suggestion)
                    state.copy(
                        suggestions = mutableSuggestions,
                        snackbarMessage = "Failed to reject suggestion"
                    )
                }
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
            updateSuggestionStatus(original, "APPROVED")
            _uiState.update { state ->
                state.copy(
                    suggestions = state.suggestions - original,
                    message = "Task saved: $editedTitle"
                )
            }
        }
    }

    fun setContactAutoApprove(suggestion: TaskSuggestion, autoApprove: Boolean) {
        viewModelScope.launch {
            val contactName = suggestion.whatsAppContext?.senderName ?: suggestion.sender
            val contact = contactRepository?.getContactByName(contactName)
            if (contact != null) {
                contactRepository?.updateAutoApprove(contact.id, autoApprove)
                _uiState.update { state ->
                    state.copy(message = "Auto-approve ${if (autoApprove) "enabled" else "disabled"} for $contactName")
                }
            }
        }
    }

    fun setContactPriority(contactName: String, priority: ContactPriority) {
        viewModelScope.launch {
            val contact = contactRepository?.getContactByName(contactName)
            if (contact != null) {
                contactRepository?.updatePriority(contact.id, priority.name)
                _uiState.update { state ->
                    state.copy(message = "Priority set to ${priority.displayName} for $contactName")
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun addSuggestion(suggestion: TaskSuggestion) {
        _uiState.update { state ->
            state.copy(suggestions = state.suggestions + suggestion)
        }
    }

    private suspend fun updateSuggestionStatus(suggestion: TaskSuggestion, status: String) {
        if (suggestion.id != 0L) {
            taskSuggestionDao.updateStatus(suggestion.id, status)
            return
        }
        val entities = taskSuggestionDao.getByStatus("PENDING").first()
        val match = entities.find {
            it.suggestedTitle == suggestion.suggestedTitle &&
                it.originalText == suggestion.originalText
        }
        if (match != null) {
            taskSuggestionDao.updateStatus(match.id, status)
        }
    }
}
