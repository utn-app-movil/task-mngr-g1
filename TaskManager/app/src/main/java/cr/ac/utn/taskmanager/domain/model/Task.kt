package cr.ac.utn.taskmanager.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Task(
    val id: Long = 0L,
    val parentId: Long? = null, // Para subtareas (Req 1)
    val title: String,
    val description: String,
    val dueDate: LocalDate,
    val creationDate: LocalDate,
    val status: TaskStatus = TaskStatus.PENDING, // Estado inicial PENDING (Req 2)
    val assignedTo: String,
    val priority: TaskPriority,
    val notes: String,
    val statusChangedAt: LocalDateTime? = null, // Fecha y hora del cambio de estado (Req 9)
    val cancellationReason: String? = null // Razón si se cancela (Req 8)
)