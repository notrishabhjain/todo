package com.procrastinationkiller.domain.engine.prioritization

import com.procrastinationkiller.domain.model.TaskPriority

data class PrioritizationResult(
    val priority: TaskPriority,
    val urgencyScore: Float,
    val contributingFactors: List<String>,
    val shouldEscalate: Boolean
)
