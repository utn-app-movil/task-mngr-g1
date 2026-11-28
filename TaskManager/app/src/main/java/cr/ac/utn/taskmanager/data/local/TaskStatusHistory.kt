package cr.ac.utn.taskmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import cr.ac.utn.taskmanager.domain.model.TaskStatus

@Entity(tableName = "task_status_history")
data class TaskStatusHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: String,
    val previousStatus: TaskStatus?,
    val newStatus: TaskStatus,
    val changedAt: Long,
    val cancellationReason: String = ""
)