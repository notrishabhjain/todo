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
    val message: String? = null
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
            taskSuggestionDao.getByStatus("PENDING").collect { entities ->
                val suggestions = entities.map { entity ->
                    TaskSuggestion(
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

                // Process auto-approve suggestions
                val autoApproved = suggestions.filter { it.autoApprove }
                val remaining = suggestions.filter { !it.autoApprove }

                autoApproved.forEach { suggestion ->
                    approveTaskUseCase(suggestion)
                    updateSuggestionStatus(suggestion, "APPROVED")
                }

                _uiState.update { it.copy(suggestions = remaining, isLoading = false) }
            }
        }
    }

    fun approveSuggestion(suggestion: TaskSuggestion) {
        viewModelScope.launch {
            approveTaskUseCase(suggestion)
            updateSuggestionStatus(suggestion, "APPROVED")
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
            updateSuggestionStatus(suggestion, "REJECTED")
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

    fun addSuggestion(suggestion: TaskSuggestion) {
        _uiState.update { state ->
            state.copy(suggestions = state.suggestions + suggestion)
        }
    }

    private suspend fun updateSuggestionStatus(suggestion: TaskSuggestion, status: String) {
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
