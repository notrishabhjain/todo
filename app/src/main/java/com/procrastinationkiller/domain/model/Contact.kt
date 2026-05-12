package com.procrastinationkiller.domain.model

data class Contact(
    val id: Long = 0,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val isEscalationTarget: Boolean = false
)
