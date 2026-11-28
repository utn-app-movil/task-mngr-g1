package cr.ac.utn.taskmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_id_sequence")
data class TaskIdSequence(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0
)