package cr.ac.utn.taskmanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY CAST(id AS INTEGER), id")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :idTask")
    suspend fun getTaskById(idTask: String): TaskEntity?

    @Insert
    suspend fun insertTask(task: TaskEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM tasks WHERE parentId = :id)")
    suspend fun hasSubtasks(id: String): Boolean

    @Query("SELECT lastId FROM task_id_sequence WHERE sequenceKey = 1")
    suspend fun readLastId(): Long?

    @Upsert
    suspend fun saveLastId(sequence: TaskIdSequence)

    @Insert
    suspend fun insertStatusChange(change: TaskStatusChange)

    @Query("SELECT * FROM task_status_changes WHERE taskId = :id ORDER BY id")
    suspend fun getStatusChanges(id: String): List<TaskStatusChange>

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :idTask")
    suspend fun deleteTaskById(idTask: String)
}
