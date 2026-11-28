package cr.ac.utn.taskmanager.domain.model

enum class TaskError {
    REQUIRED_FIELDS,
    CLOSED_TASK,
    INVALID_STATUS,
    PAST_DUE_DATE,
    CANCELLATION_REASON,
    INVALID_PARENT,
    TASK_NOT_FOUND,
    HAS_SUBTASKS
}
