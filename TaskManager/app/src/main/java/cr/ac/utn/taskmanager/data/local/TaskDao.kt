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

    @Query("SELECT * FROM tasks WHERE id = :idTask")
    suspend fun getTaskById(idTask: String): TaskEntity?

    @Query("SELECT id FROM tasks")
    suspend fun getExistingIds(): List<String>

    @Query("SELECT lastId FROM task_id_counter WHERE `key` = 1")
    suspend fun getLastGeneratedId(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveIdCounter(counter: TaskIdCounter)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :idTask")
    suspend fun deleteTaskById(idTask: String)
}