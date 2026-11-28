package cr.ac.utn.taskmanager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_id_counter")
data class TaskIdCounter(
    @PrimaryKey val key: Int = 1,
    val lastId: Long
)