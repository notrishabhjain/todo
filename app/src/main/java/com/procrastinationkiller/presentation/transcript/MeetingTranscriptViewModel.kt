package com.procrastinationkiller.presentation.transcript

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.domain.engine.TranscriptActionItem
import com.procrastinationkiller.domain.engine.TranscriptAnalyzer
import com.procrastinationkiller.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MeetingTranscriptUiState(
    val transcriptText: String = "",
    val actionItems: List<ActionItemUi> = emptyList(),
    val isAnalyzing: Boolean = false,
    val isAnalyzed: Boolean = false,
    val approvedCount: Int = 0,
    val rejectedCount: Int = 0
)

data class ActionItemUi(
    val actionItem: TranscriptActionItem,
    val isApproved: Boolean = false,
    val isRejected: Boolean = false
)

@HiltViewModel
class MeetingTranscriptViewModel @Inject constructor(
    private val transcriptAnalyzer: TranscriptAnalyzer,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeetingTranscriptUiState())
    val uiState: StateFlow<MeetingTranscriptUiState> = _uiState.asStateFlow()

    fun updateTranscript(text: String) {
        _uiState.value = _uiState.value.copy(transcriptText = text)
    }

    fun analyzeTranscript() {
        val text = _uiState.value.transcriptText
        if (text.isBlank()) return

        _uiState.value = _uiState.value.copy(isAnalyzing = true)

        viewModelScope.launch {
            val items = transcriptAnalyzer.analyze(text)
            _uiState.value = _uiState.value.copy(
                actionItems = items.map { ActionItemUi(it) },
                isAnalyzing = false,
                isAnalyzed = true
            )
        }
    }

    fun approveItem(index: Int) {
        val items = _uiState.value.actionItems.toMutableList()
        if (index in items.indices) {
            items[index] = items[index].copy(isApproved = true, isRejected = false)
            _uiState.value = _uiState.value.copy(
                actionItems = items,
                approvedCount = items.count { it.isApproved }
            )
        }
    }

    fun rejectItem(index: Int) {
        val items = _uiState.value.actionItems.toMutableList()
        if (index in items.indices) {
            items[index] = items[index].copy(isRejected = true, isApproved = false)
            _uiState.value = _uiState.value.copy(
                actionItems = items,
                rejectedCount = items.count { it.isRejected }
            )
        }
    }

    fun bulkApproveAll() {
        val items = _uiState.value.actionItems.map { it.copy(isApproved = true, isRejected = false) }
        _uiState.value = _uiState.value.copy(
            actionItems = items,
            approvedCount = items.size,
            rejectedCount = 0
        )
    }

    fun bulkRejectAll() {
        val items = _uiState.value.actionItems.map { it.copy(isRejected = true, isApproved = false) }
        _uiState.value = _uiState.value.copy(
            actionItems = items,
            approvedCount = 0,
            rejectedCount = items.size
        )
    }

    fun saveApprovedTasks() {
        viewModelScope.launch {
            val approved = _uiState.value.actionItems.filter { it.isApproved }
            for (item in approved) {
                val task = TaskEntity(
                    title = item.actionItem.text.take(60),
                    description = item.actionItem.text,
                    priority = item.actionItem.priority.name,
                    status = "PENDING",
                    deadline = item.actionItem.dueDate,
                    createdAt = System.currentTimeMillis()
                )
                taskRepository.insertTask(task)
            }
        }
    }
}
