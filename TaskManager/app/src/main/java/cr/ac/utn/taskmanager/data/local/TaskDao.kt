package cr.ac.utn.taskmanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :idTask")
    suspend fun getTaskById(idTask: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :idTask")
    suspend fun deleteTaskById(idTask: String)

    @Query("SELECT * FROM tasks WHERE parentTaskId IS NULL AND status IN (:allowedStatuses)")
    fun getEligibleParentTasks(
        allowedStatuses: List<TaskStatus> = listOf(TaskStatus .PENDING, TaskStatus.IN_PROGRESS)
    ): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentId")
    fun getSubtasksByParentId(parentId: String): Flow<List<TaskEntity>>

    @Query("SELECT id FROM tasks")
    suspend fun getAllTaskIds(): List<String>
}