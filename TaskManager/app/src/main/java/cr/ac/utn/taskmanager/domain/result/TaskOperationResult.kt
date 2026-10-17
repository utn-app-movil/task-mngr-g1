package cr.ac.utn.taskmanager.domain.result

enum class TaskError {
    INVALID_ID,
    INVALID_TITLE,
    DUPLICATE_TASK,
    TASK_NOT_FOUND,
    COMPLETED_TASK_RESTRICTION,
    STORAGE_FAILURE,
    UNKNOWN_ERROR
}

sealed class TaskOperationResult<out T> {
    data class Success<T>(val data: T) : TaskOperationResult<T>()

    data class Failure(
        val error: TaskError
    ) : TaskOperationResult<Nothing>()
}