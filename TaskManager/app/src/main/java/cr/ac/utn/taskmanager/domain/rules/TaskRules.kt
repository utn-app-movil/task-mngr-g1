package cr.ac.utn.taskmanager.domain.rules

import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import java.time.LocalDate

object TaskRules {

    fun validateCreate(task: Task, parent: Task?) {
        if (task.status != TaskStatus.PENDING) fail(TaskRuleViolation.INVALID_TRANSITION)
        if (task.assignedTo.isBlank()) fail(TaskRuleViolation.ASSIGNEE_REQUIRED)
        if (task.parentTaskId != null) {
            if (parent == null) fail(TaskRuleViolation.PARENT_NOT_FOUND)
            if (!parent.isEligibleParent()) fail(TaskRuleViolation.PARENT_NOT_ELIGIBLE)
        }
    }

    fun validateUpdate(current: Task, updated: Task, today: LocalDate = LocalDate.now()) {
        if (current.status.isFinal) fail(TaskRuleViolation.TASK_IS_FINAL)
        if (updated.assignedTo.isBlank()) fail(TaskRuleViolation.ASSIGNEE_REQUIRED)

        if (updated.status != current.status) {
            if (updated.status !in current.status.allowedTransitions()) {
                fail(TaskRuleViolation.INVALID_TRANSITION)
            }
            if (updated.status == TaskStatus.CANCELLED &&
                updated.cancellationReason.isNullOrBlank()
            ) {
                fail(TaskRuleViolation.CANCELLATION_REASON_REQUIRED)
            }
            if (updated.status == TaskStatus.IN_PROGRESS && updated.dueDate.isBefore(today)) {
                fail(TaskRuleViolation.OVERDUE_CANNOT_START)
            }
        }
    }

    fun validateDelete(current: Task) {
        if (current.status.isFinal) fail(TaskRuleViolation.TASK_IS_FINAL)
    }

    private fun fail(violation: TaskRuleViolation): Nothing =
        throw TaskRuleException(violation)
}