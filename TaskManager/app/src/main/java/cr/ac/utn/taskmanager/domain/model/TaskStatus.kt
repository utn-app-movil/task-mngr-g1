package cr.ac.utn.taskmanager.domain.model

enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    fun canChangeTo(newStatus: TaskStatus): Boolean {
        return when (this) {
            PENDING -> newStatus == IN_PROGRESS || newStatus == CANCELLED
            IN_PROGRESS -> newStatus == COMPLETED || newStatus == CANCELLED
            COMPLETED -> false
            CANCELLED -> false
        }
    }
}