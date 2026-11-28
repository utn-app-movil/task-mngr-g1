package cr.ac.utn.taskmanager.domain.model

import java.time.LocalDate

object TaskRules {
    fun isClosed(task: Task): Boolean {
        return task.status == TaskStatus.COMPLETED || task.status == TaskStatus.CANCELLED
    }

    fun getAllowedStatuses(task: Task?): List<TaskStatus> {
        return when (task?.status) {
            null -> listOf(TaskStatus.PENDING)
            TaskStatus.PENDING -> listOf(
                TaskStatus.PENDING, TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED
            )
            TaskStatus.IN_PROGRESS -> listOf(
                TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED, TaskStatus.COMPLETED
            )
            else -> listOf(task.status)
        }
    }

    fun validate(task: Task, original: Task?, today: LocalDate): TaskError? {
        if (original != null && isClosed(original)) return TaskError.CLOSED_TASK
        if (task.title.isBlank() || task.description.isBlank() || task.assignedTo.isBlank()) {
            return TaskError.REQUIRED_FIELDS
        }
        if (!getAllowedStatuses(original).contains(task.status)) return TaskError.INVALID_STATUS
        if (task.status == TaskStatus.CANCELLED && task.cancellationReason.isBlank()) {
            return TaskError.CANCELLATION_REASON
        }
        if (task.status == TaskStatus.IN_PROGRESS && original?.status != TaskStatus.IN_PROGRESS
            && task.dueDate.isBefore(today)) {
            return TaskError.PAST_DUE_DATE
        }
        if (original != null && task.parentTaskId != original.parentTaskId) {
            return TaskError.INVALID_PARENT
        }
        return null
    }
}
