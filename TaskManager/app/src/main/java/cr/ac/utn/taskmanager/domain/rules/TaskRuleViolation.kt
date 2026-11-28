package cr.ac.utn.taskmanager.domain.rules

enum class TaskRuleViolation {
    TASK_IS_FINAL,
    INVALID_TRANSITION,
    CANCELLATION_REASON_REQUIRED,
    OVERDUE_CANNOT_START,
    ASSIGNEE_REQUIRED,
    PARENT_NOT_FOUND,
    PARENT_NOT_ELIGIBLE,
    TASK_NOT_FOUND
}

class TaskRuleException(val violation: TaskRuleViolation) : Exception(violation.name)