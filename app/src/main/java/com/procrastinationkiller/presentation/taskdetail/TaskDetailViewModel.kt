package com.procrastinationkiller.presentation.taskdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Intent
import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.domain.model.TaskPriority
import com.procrastinationkiller.domain.model.TaskStatus
import com.procrastinationkiller.domain.usecase.CalendarIntegrationHelper
import com.procrastinationkiller.domain.usecase.UpdateTaskUseCase
import com.procrastinationkiller.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskDetailUiState(
    val task: TaskEntity? = null,
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val editTitle: String = "",
    val editDescription: String = "",
    val editPriority: TaskPriority = TaskPriority.MEDIUM,
    val message: String? = null,
    val calendarIntent: Intent? = null
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val calendarIntegrationHelper: CalendarIntegrationHelper
) : ViewModel() {

    private val taskId: Long = savedStateHandle.get<Long>("taskId") ?: -1L

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        loadTask()
    }

    private fun loadTask() {
        viewModelScope.launch {
            val task = taskRepository.getTaskById(taskId)
            if (task != null) {
                val priority = try {
                    TaskPriority.valueOf(task.priority)
                } catch (e: IllegalArgumentException) {
                    TaskPriority.MEDIUM
                }
                _uiState.value = TaskDetailUiState(
                    task = task,
                    isLoading = false,
                    editTitle = task.title,
                    editDescription = task.description,
                    editPriority = priority
                )
            } else {
                _uiState.value = TaskDetailUiState(isLoading = false)
            }
        }
    }

    fun startEditing() {
        _uiState.update { it.copy(isEditing = true) }
    }

    fun cancelEditing() {
        val task = _uiState.value.task
        _uiState.update {
            it.copy(
                isEditing = false,
                editTitle = task?.title ?: "",
                editDescription = task?.description ?: ""
            )
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(editTitle = title) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(editDescription = description) }
    }

    fun updatePriority(priority: TaskPriority) {
        _uiState.update { it.copy(editPriority = priority) }
    }

    fun saveChanges() {
        val task = _uiState.value.task ?: return
        viewModelScope.launch {
            val updated = task.copy(
                title = _uiState.value.editTitle,
                description = _uiState.value.editDescription,
                priority = _uiState.value.editPriority.name
            )
            updateTaskUseCase.updateTask(updated)
            _uiState.update {
                it.copy(
                    task = updated,
                    isEditing = false,
                    message = "Task updated"
                )
            }
        }
    }

    fun completeTask() {
        viewModelScope.launch {
            updateTaskUseCase.updateStatus(taskId, TaskStatus.COMPLETED)
            loadTask()
            _uiState.update { it.copy(message = "Task completed") }
        }
    }

    fun deleteTask() {
        viewModelScope.launch {
            updateTaskUseCase.updateStatus(taskId, TaskStatus.DELETED)
            _uiState.update { it.copy(message = "Task deleted", task = null) }
        }
    }

    fun archiveTask() {
        viewModelScope.launch {
            updateTaskUseCase.updateStatus(taskId, TaskStatus.ARCHIVED)
            _uiState.update { it.copy(message = "Task archived", task = null) }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun addToCalendar() {
        val task = _uiState.value.task ?: return
        val intent = calendarIntegrationHelper.createCalendarIntent(task)
        _uiState.update { it.copy(calendarIntent = intent) }
    }

    fun clearCalendarIntent() {
        _uiState.update { it.copy(calendarIntent = null) }
    }
}
