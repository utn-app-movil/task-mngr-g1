package cr.ac.utn.taskmanager.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.model.TaskPriority
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import java.time.LocalDate

@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(
        entity = TaskEntity::class,
        parentColumns = ["id"],
        childColumns = ["parentId"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index("parentId")]
)
data class TaskEntity (
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val dueDate: LocalDate,
    val creationDate: LocalDate,
    val status: TaskStatus,
    val assignedTo: String,
    val priority: TaskPriority,
    val notes: String,
    val parentId: String? = null,
    val cancellationReason: String? = null
)

fun TaskEntity.toDomain(): Task {
    return Task(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate,
        creationDate = creationDate,
        status = status,
        assignedTo = assignedTo,
        priority = priority,
        notes = notes,
        parentId = parentId,
        cancellationReason = cancellationReason
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate,
        creationDate = creationDate,
        status = status,
        assignedTo = assignedTo,
        priority = priority,
        notes = notes,
        parentId = parentId,
        cancellationReason = cancellationReason
    )
}