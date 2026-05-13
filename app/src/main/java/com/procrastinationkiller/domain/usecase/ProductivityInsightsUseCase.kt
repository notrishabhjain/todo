package com.procrastinationkiller.domain.usecase

import com.procrastinationkiller.data.local.dao.TaskSuggestionDao
import com.procrastinationkiller.domain.engine.insights.ProductivityInsights
import com.procrastinationkiller.domain.engine.insights.ProductivityInsightsEngine
import com.procrastinationkiller.domain.repository.TaskRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductivityInsightsUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val taskSuggestionDao: TaskSuggestionDao,
    private val insightsEngine: ProductivityInsightsEngine
) {

    companion object {
        private const val DEBOUNCE_MS = 500L
    }

    @OptIn(FlowPreview::class)
    fun getInsights(): Flow<ProductivityInsights> {
        return taskRepository.getAllTasks()
            .combine(taskSuggestionDao.getAll()) { tasks, suggestions ->
                tasks to suggestions
            }
            .debounce(DEBOUNCE_MS)
            .map { (tasks, suggestions) ->
                insightsEngine.computeInsights(tasks, suggestions)
            }
    }
}
