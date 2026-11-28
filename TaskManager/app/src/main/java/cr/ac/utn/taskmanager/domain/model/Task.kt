package cr.ac.utn.taskmanager.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Task(
    val id: String,
    val title: String,
    val description: String,
    val dueDate: LocalDate,
    val creationDate: LocalDate,
    val status: TaskStatus,
    val assignedTo: String,
    val priority: TaskPriority,
    val notes: String,
    val parentId: String?,
    val cancelReason: String,
    val statusChangeDate: LocalDateTime
)