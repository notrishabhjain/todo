package com.procrastinationkiller.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val pendingTaskCount: Int = 0,
    val highPriorityCount: Int = 0,
    val completedTodayCount: Int = 0,
    val completionRate: Float = 0f,
    val todaysTasks: List<TaskEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            taskRepository.getAllTasks().collect { tasks ->
                val pendingTasks = tasks.filter { it.status == "PENDING" || it.status == "IN_PROGRESS" }
                val highPriorityCount = pendingTasks.count { it.priority == "HIGH" || it.priority == "CRITICAL" }

                val todayStart = getTodayStartMillis()
                val completedToday = tasks.count {
                    it.status == "COMPLETED" && (it.completedAt ?: 0) >= todayStart
                }

                val totalTasks = tasks.size
                val completedTasks = tasks.count { it.status == "COMPLETED" }
                val completionRate = if (totalTasks > 0) {
                    completedTasks.toFloat() / totalTasks.toFloat()
                } else 0f

                val todaysTasks = pendingTasks.filter { task ->
                    task.deadline?.let { it <= getTodayEndMillis() } ?: false
                }.sortedByDescending { priorityWeight(it.priority) }

                _uiState.value = DashboardUiState(
                    pendingTaskCount = pendingTasks.size,
                    highPriorityCount = highPriorityCount,
                    completedTodayCount = completedToday,
                    completionRate = completionRate,
                    todaysTasks = todaysTasks,
                    isLoading = false
                )
            }
        }
    }

    private fun priorityWeight(priority: String): Int {
        return when (priority) {
            "CRITICAL" -> 4
            "HIGH" -> 3
            "MEDIUM" -> 2
            "LOW" -> 1
            else -> 0
        }
    }

    private fun getTodayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getTodayEndMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}
