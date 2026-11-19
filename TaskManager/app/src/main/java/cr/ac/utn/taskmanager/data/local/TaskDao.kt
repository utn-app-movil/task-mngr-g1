package cr.ac.utn.taskmanager.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT + FROM tasks")
    fun getAllTasks(): Flow<List<TaskEntity>>
}