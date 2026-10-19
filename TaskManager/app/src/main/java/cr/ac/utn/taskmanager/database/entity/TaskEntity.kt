package cr.ac.utn.taskmanager.database.entity
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val dueDate: String,
    val creationDate: String,
    val status: String,
    val priority: String,
    val assignTo: String,
    val observation: String
)
