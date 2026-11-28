package cr.ac.utn.taskmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.model.TaskPriority
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val dueDate: LocalDate,
    val creationDate: LocalDate,
    val status: TaskStatus,
    val assignedTo: String,
    val priority: TaskPriority,
    val notes: String,
    val parentTaskId: Long?,
    val cancellationReason: String?,
    val statusChangedAt: LocalDateTime
)

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    description = description,
    dueDate = dueDate,
    creationDate = creationDate,
    status = status,
    assignedTo = assignedTo,
    priority = priority,
    notes = notes,
    parentTaskId = parentTaskId,
    cancellationReason = cancellationReason,
    statusChangedAt = statusChangedAt
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    dueDate = dueDate,
    creationDate = creationDate,
    status = status,
    assignedTo = assignedTo,
    priority = priority,
    notes = notes,
    parentTaskId = parentTaskId,
    cancellationReason = cancellationReason,
    statusChangedAt = statusChangedAt
)