package cr.ac.utn.taskmanager.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.model.TaskPriority
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentTaskId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["parentTaskId"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String,
    val dueDate: LocalDate,
    val creationDate: LocalDate,
    val status: TaskStatus,
    val assignedTo: String,
    val priority: TaskPriority,
    val notes: String,
    val cancellationReason: String? = null,
    val statusChangeDate: LocalDateTime? = null,
    val parentTaskId: Long? = null
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
        cancellationReason = cancellationReason,
        statusChangeDate = statusChangeDate,
        parentTaskId = parentTaskId
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
        cancellationReason = cancellationReason,
        statusChangeDate = statusChangeDate,
        parentTaskId = parentTaskId
    )
}