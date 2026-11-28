package cr.ac.utn.taskmanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks " + "ORDER BY COALESCE(parentTaskId, id), parentTaskId IS NOT NULL, id")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentTaskId IS NULL " + "AND status IN ('PENDING', 'IN_PROGRESS') ORDER BY id")
    fun getEligibleParentTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :idTask")
    suspend fun getTaskById(idTask: Long): TaskEntity?

    @Insert
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :idTask OR parentTaskId = :idTask")
    suspend fun deleteTaskById(idTask: Long)
}