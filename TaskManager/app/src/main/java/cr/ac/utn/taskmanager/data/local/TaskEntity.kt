package cr.ac.utn.taskmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import cr.ac.utn.taskmanager.domain.model.TaskPriority
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import java.time.LocalDate

data class TaskEntity (
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val dueDate: LocalDate,
    val creationDate: LocalDate,
    val status: TaskStatus,
    val assignedTo: String,
    val priority: TaskPriority,
    val notes: String

)
