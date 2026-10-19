package cr.ac.utn.taskmanager.domain.model

import java.time.LocalDate

data class Task(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val dueDate: LocalDate = LocalDate.now(),
    val creationDate: LocalDate = LocalDate.now(),
    val status: TaskStatus = TaskStatus.PENDING,
    val assignedTo: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val notes: String = ""
)
