package com.procrastinationkiller.domain.engine.learning

import com.procrastinationkiller.domain.model.TaskPriority

data class LearningAdjustment(
    val confidenceBoost: Float,
    val priorityAdjustment: TaskPriority?,
    val shouldAutoApprove: Boolean
)
