package cr.ac.utn.taskmanager.domain.model

enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    val isTerminal: Boolean
        get() = this == COMPLETED || this == CANCELLED

    fun allowedStatuses(): List<TaskStatus> = when (this) {
        PENDING -> listOf(PENDING, IN_PROGRESS, CANCELLED)
        IN_PROGRESS -> listOf(IN_PROGRESS, COMPLETED, CANCELLED)
        COMPLETED -> listOf(COMPLETED)
        CANCELLED -> listOf(CANCELLED)
    }
}