package cr.ac.utn.taskmanager.util

import cr.ac.utn.taskmanager.domain.model.TaskStatus
import java.time.LocalDate

object TaskValidation {

    fun isFinalStatus(status: TaskStatus): Boolean {
        return status == TaskStatus.COMPLETED ||
                status == TaskStatus.CANCELLED
    }

    fun getAllowedStatuses(status: TaskStatus): List<TaskStatus> {
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

            TaskStatus.COMPLETED -> listOf(
                TaskStatus.COMPLETED
            )

            TaskStatus.CANCELLED -> listOf(
                TaskStatus.CANCELLED
            )
        }
    }

    fun validateStatusChange(
        currentStatus: TaskStatus,
        newStatus: TaskStatus,
        dueDate: LocalDate
    ): String? {

        if (isFinalStatus(currentStatus)) {
            return "A completed or cancelled task cannot be modified."
        }

        if (newStatus !in getAllowedStatuses(currentStatus)) {
            return "Invalid task status change."
        }

        if (currentStatus != newStatus &&
            newStatus == TaskStatus.IN_PROGRESS &&
            dueDate.isBefore(LocalDate.now())
        ) {
            return "An overdue task cannot be moved to In Progress."
        }

        return null
    }
}