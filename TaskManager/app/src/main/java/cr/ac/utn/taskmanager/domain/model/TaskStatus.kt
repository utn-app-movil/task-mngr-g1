package cr.ac.utn.taskmanager.domain.model

enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    fun isTerminal(): Boolean = this == COMPLETED || this == CANCELLED

    fun allowedStatuses(): List<TaskStatus> = when (this) {
        PENDING -> listOf(PENDING, IN_PROGRESS, CANCELLED)
        IN_PROGRESS -> listOf(IN_PROGRESS, COMPLETED, CANCELLED)
        COMPLETED, CANCELLED -> listOf(this)
    }
}