package com.procrastinationkiller.domain.model

enum class AppCategory {
    COMMUNICATION,
    PRODUCTIVITY,
    SOCIAL,
    OTHER
}

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val category: AppCategory,
    val hasIcon: Boolean = true
)
