package cr.ac.utn.taskmanager.domain.model

enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    val isFinal: Boolean
        get() = this == COMPLETED || this == CANCELLED

    fun allowedTransitions(): List<TaskStatus> = when (this) {
        PENDING -> listOf(PENDING, IN_PROGRESS, CANCELLED)
        IN_PROGRESS -> listOf(IN_PROGRESS, COMPLETED, CANCELLED)
        COMPLETED -> listOf(COMPLETED)
        CANCELLED -> listOf(CANCELLED)
    }
}