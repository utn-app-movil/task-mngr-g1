package cr.ac.utn.taskmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.model.TaskPriority
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "tasks")
data class TaskEntity (
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val parentId: Long? = null,
    val title: String,
    val description: String,
    val dueDate: LocalDate,
    val creationDate: LocalDate,
    val status: TaskStatus,
    val assignedTo: String,
    val priority: TaskPriority,
    val notes: String,
    val statusChangedAt: LocalDateTime? = null,
    val cancellationReason: String? = null
)

fun TaskEntity.toDomain(): Task {
    return Task(
        id = id,
        parentId = parentId,
        title = title,
        description = description,
        dueDate = dueDate,
        creationDate = creationDate,
        status = status,
        assignedTo = assignedTo,
        priority = priority,
        notes = notes,
        statusChangedAt = statusChangedAt,
        cancellationReason = cancellationReason
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        parentId = parentId,
        title = title,
        description = description,
        dueDate = dueDate,
        creationDate = creationDate,
        status = status,
        assignedTo = assignedTo,
        priority = priority,
        notes = notes,
        statusChangedAt = statusChangedAt,
        cancellationReason = cancellationReason
    )
}