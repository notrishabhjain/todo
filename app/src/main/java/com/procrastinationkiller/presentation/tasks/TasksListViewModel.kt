package com.procrastinationkiller.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.domain.model.TaskPriority
import com.procrastinationkiller.domain.model.TaskStatus
import com.procrastinationkiller.domain.repository.TaskRepository
import com.procrastinationkiller.domain.usecase.GetTasksUseCase
import com.procrastinationkiller.domain.usecase.TaskFilter
import com.procrastinationkiller.domain.usecase.TaskSortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasksListUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val isLoading: Boolean = true,
    val selectedPriority: TaskPriority? = null,
    val selectedStatus: TaskStatus? = null,
    val sortOrder: TaskSortOrder = TaskSortOrder.CREATED_DESC,
    val showCreateDialog: Boolean = false
)

@HiltViewModel
class TasksListViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksListUiState())
    val uiState: StateFlow<TasksListUiState> = _uiState.asStateFlow()

    private var collectJob: Job? = null

    init {
        loadTasks()
    }

    private fun loadTasks() {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            val filter = TaskFilter(
                priority = _uiState.value.selectedPriority,
                status = _uiState.value.selectedStatus
            )
            getTasksUseCase(filter, _uiState.value.sortOrder).collect { tasks ->
                _uiState.update { it.copy(tasks = tasks, isLoading = false) }
            }
        }
    }

    fun setFilter(priority: TaskPriority?) {
        _uiState.update {
            it.copy(
                selectedPriority = if (it.selectedPriority == priority) null else priority
            )
        }
        loadTasks()
    }

    fun setStatusFilter(status: TaskStatus?) {
        _uiState.update {
            it.copy(
                selectedStatus = if (it.selectedStatus == status) null else status
            )
        }
        loadTasks()
    }

    fun setSortOrder(sortOrder: TaskSortOrder) {
        _uiState.update { it.copy(sortOrder = sortOrder) }
        loadTasks()
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun createTask(title: String, description: String, priority: TaskPriority, deadline: Long?) {
        viewModelScope.launch {
            val taskEntity = TaskEntity(
                title = title,
                description = description,
                priority = priority.name,
                status = TaskStatus.PENDING.name,
                deadline = deadline,
                createdAt = System.currentTimeMillis()
            )
            taskRepository.insertTask(taskEntity)
            hideCreateDialog()
        }
    }
}
