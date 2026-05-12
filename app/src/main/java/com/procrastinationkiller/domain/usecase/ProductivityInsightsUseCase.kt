package com.procrastinationkiller.domain.usecase

import com.procrastinationkiller.data.local.dao.TaskSuggestionDao
import com.procrastinationkiller.domain.engine.insights.ProductivityInsights
import com.procrastinationkiller.domain.engine.insights.ProductivityInsightsEngine
import com.procrastinationkiller.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductivityInsightsUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val taskSuggestionDao: TaskSuggestionDao,
    private val insightsEngine: ProductivityInsightsEngine
) {

    fun getInsights(): Flow<ProductivityInsights> {
        return taskRepository.getAllTasks().combine(taskSuggestionDao.getAll()) { tasks, suggestions ->
            insightsEngine.computeInsights(tasks, suggestions)
        }
    }
}
