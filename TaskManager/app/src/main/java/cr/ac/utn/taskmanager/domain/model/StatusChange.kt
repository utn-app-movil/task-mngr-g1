package cr.ac.utn.taskmanager.domain.model

import java.time.LocalDateTime

data class StatusChange(
    val previousStatus: TaskStatus,
    val newStatus: TaskStatus,
    val changedAt: LocalDateTime
)
