package cr.ac.utn.taskmanager.domain.model

enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    fun availableStatuses(): List<TaskStatus> = when (this) {
        PENDING -> listOf(PENDING, IN_PROGRESS, CANCELLED)
        IN_PROGRESS -> listOf(IN_PROGRESS, COMPLETED, CANCELLED)
        COMPLETED, CANCELLED -> listOf(this)
    }

    val isActive: Boolean
        get() = this == PENDING || this == IN_PROGRESS
}
