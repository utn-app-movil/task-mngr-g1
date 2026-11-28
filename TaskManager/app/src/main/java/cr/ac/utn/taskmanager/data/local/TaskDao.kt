package cr.ac.utn.taskmanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :idTask")
    suspend fun getTaskById(idTask: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTask(task: TaskEntity)

    @Insert
    suspend fun generateTaskId(sequence: TaskIdSequence): Long

    @Insert
    suspend fun insertStatusHistory(
        history: TaskStatusHistory
    )

    @Query(
        """
        SELECT * FROM task_status_history
        WHERE taskId = :taskId
        ORDER BY changedAt ASC, id ASC
        """
    )
    fun getStatusHistory(
        taskId: String
    ): Flow<List<TaskStatusHistory>>

    @Transaction
    suspend fun insertTaskWithGeneratedId(task: TaskEntity) {

        val newId = generateTaskId(TaskIdSequence())

        val timestamp = System.currentTimeMillis()

        val newTask = task.copy(
            id = newId.toString(),
            statusChangedAt = timestamp
        )

        insertTask(newTask)

        insertStatusHistory(
            TaskStatusHistory(
                taskId = newTask.id,
                previousStatus = null,
                newStatus = newTask.status,
                changedAt = timestamp
            )
        )
    }

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Transaction
    suspend fun updateTaskWithHistory(
        task: TaskEntity,
        history: TaskStatusHistory?
    ) {
        updateTask(task)

        if (history != null) {
            insertStatusHistory(history)
        }
    }

    @Query("DELETE FROM tasks WHERE id = :idTask")
    suspend fun deleteTaskById(idTask: String)
}