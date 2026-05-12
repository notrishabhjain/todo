package com.procrastinationkiller.domain.engine.learning

enum class UserFeedbackType(val weightDirection: Float) {
    APPROVED(1.0f),
    REJECTED(-1.0f),
    EDITED(0.5f),
    COMPLETED_QUICKLY(1.5f),
    COMPLETED_SLOWLY(0.25f),
    IGNORED(-0.5f)
}
