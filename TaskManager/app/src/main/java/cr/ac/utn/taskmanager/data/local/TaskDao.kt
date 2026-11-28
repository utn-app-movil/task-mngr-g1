package cr.ac.utn.taskmanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE (parentTaskId IS NULL OR parentTaskId = '') AND (status = 'PENDING' OR status = 'IN_PROGRESS')")
    fun getEligibleParentTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :idTask")
    suspend fun getTaskById(idTask: String): TaskEntity?

    @Query("SELECT MAX(CAST(id AS INTEGER)) FROM tasks")
    suspend fun getMaxId(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :idTask")
    suspend fun deleteTaskById(idTask: String)
}