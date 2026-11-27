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

    @Query("SELECT * FROM tasks WHERE parentTaskId IS NULL AND status IN ('PENDING', 'IN_PROGRESS')")
    fun getEligibleParentTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentId")
    fun getSubtasksForParent(parentId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :idTask")
    suspend fun getTaskById(idTask: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :idTask")
    suspend fun deleteTaskById(idTask: String)
}
