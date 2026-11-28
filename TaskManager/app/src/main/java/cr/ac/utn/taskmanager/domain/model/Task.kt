package cr.ac.utn.taskmanager.domain.model

import java.time.LocalDate

data class Task(
    val id: Long,
    val title: String,
    val description: String,
    val dueDate: LocalDate,
    val creationDate: LocalDate,
    val status: TaskStatus,
    val assignedTo: String,
    val priority: TaskPriority,
    val notes: String,
    val parentTaskId: Long? = null
)