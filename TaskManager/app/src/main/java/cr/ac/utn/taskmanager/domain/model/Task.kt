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
    val parentTaskid: String? = null,
    val subtasks: List<String> = emptyList(),
    val cancellationReason: String? = null,
    val statusChangedAt: LocalDateTime? = null
) {
    val isSubTask: Boolean
        get() = !parentTaskid.isNullOrBlank()

    val isEditable: Boolean
        get() = status != TaskStatus.COMPLETED && status != TaskStatus.CANCELLED

    fun allowedNextStatuses(): List<TaskStatus> {
        return when (status) {
            TaskStatus.PENDING -> listOf(
                TaskStatus.PENDING,
                TaskStatus.IN_PROGRESS,
                TaskStatus.CANCELLED
            )
            TaskStatus.IN_PROGRESS -> listOf(
                TaskStatus.IN_PROGRESS,
                TaskStatus.COMPLETED,
                TaskStatus.CANCELLED
            )
            TaskStatus.COMPLETED -> listOf(TaskStatus.COMPLETED)
            TaskStatus.CANCELLED -> listOf(TaskStatus.CANCELLED)
        }
    }

    fun canTransitionTo(targetStatus: TaskStatus): Boolean {
        return targetStatus in allowedNextStatuses()
    }

    fun canStartProgress(referenceDate: LocalDate = LocalDate.now()): Boolean {
        return !dueDate.isBefore(referenceDate)
    }
}