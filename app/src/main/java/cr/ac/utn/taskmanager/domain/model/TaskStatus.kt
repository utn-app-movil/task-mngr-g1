package cr.ac.utn.taskmanager.domain.model

enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;
    fun nextStatuses(): List<TaskStatus> {
        return when (this) {
            PENDING -> listOf(IN_PROGRESS, CANCELLED)
            IN_PROGRESS -> listOf(COMPLETED, CANCELLED)
            COMPLETED -> emptyList()
            CANCELLED -> emptyList()
        }
    }

    fun canMoveTo(newStatus: TaskStatus): Boolean {
        return this == newStatus || nextStatuses().contains(newStatus)
    }

    fun isClosed(): Boolean {
        return this == COMPLETED || this == CANCELLED
    }
}