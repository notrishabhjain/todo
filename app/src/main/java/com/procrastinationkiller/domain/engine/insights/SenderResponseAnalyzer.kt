package com.procrastinationkiller.domain.engine.insights

import com.procrastinationkiller.data.local.entity.TaskEntity
import com.procrastinationkiller.data.local.entity.TaskSuggestionEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SenderResponseAnalyzer @Inject constructor() {

    fun analyzeResponsePatterns(
        tasks: List<TaskEntity>,
        suggestions: List<TaskSuggestionEntity>
    ): List<SenderResponsePattern> {
        val acceptedSuggestions = suggestions.filter { it.status == "ACCEPTED" || it.status == "APPROVED" }

        if (acceptedSuggestions.isEmpty()) {
            return emptyList()
        }

        val bySender = acceptedSuggestions
            .filter { it.sender.isNotBlank() }
            .groupBy { it.sender }

        return bySender.map { (sender, senderSuggestions) ->
            val responseTimes = senderSuggestions.mapNotNull { suggestion ->
                val matchingTask = tasks.find { task ->
                    task.title == suggestion.suggestedTitle && task.completedAt != null
                }
                matchingTask?.let { it.completedAt!! - suggestion.createdAt }
            }

            val avgResponseTime = if (responseTimes.isNotEmpty()) {
                responseTimes.average().toLong()
            } else {
                senderSuggestions.map { System.currentTimeMillis() - it.createdAt }.average().toLong()
            }

            SenderResponsePattern(
                sender = sender,
                avgResponseTimeMs = avgResponseTime,
                taskCount = senderSuggestions.size
            )
        }.sortedBy { it.avgResponseTimeMs }
    }
}
