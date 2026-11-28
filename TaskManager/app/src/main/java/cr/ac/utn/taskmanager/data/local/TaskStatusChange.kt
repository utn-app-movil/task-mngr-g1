package cr.ac.utn.taskmanager.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import java.time.LocalDateTime

@Entity(
    tableName = "task_status_changes",
    foreignKeys = [ForeignKey(
        entity = TaskEntity::class,
        parentColumns = ["id"],
        childColumns = ["taskId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("taskId")]
)
data class TaskStatusChange(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: String,
    val previousStatus: TaskStatus,
    val newStatus: TaskStatus,
    val changedAt: LocalDateTime
)
