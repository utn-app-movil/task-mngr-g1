package cr.ac.utn.taskmanager.data.local

import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import androidx.room.withTransaction
class TaskRoomRepository(private val database: AppDatabase) : ITaskRepository {

    private val taskDao = database.taskDao()
    override fun getTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getEligibleParentTasks(): Flow<List<Task>> {
        return getTasks().map { tasks ->
            tasks.filter { task ->
                task.status == TaskStatus.PENDING ||
                        task.status == TaskStatus.IN_PROGRESS
            }
        }
    }

    override suspend fun getById(id: String): Task? {
        return taskDao.getTaskById(id.trim())?.toDomain()
    }

    override suspend fun addTask(task: Task) {
        database.withTransaction {
            val lastId = taskDao.getLastGeneratedId()
                ?: taskDao.getExistingIds().mapNotNull { it.toLongOrNull() }.maxOrNull()
                ?: 0L

            val nextId = Math.addExact(maxOf(lastId, 0L), 1L)
            val newTask = task.copy(id = nextId.toString(), status = TaskStatus.PENDING)

            taskDao.insertTask(newTask.toEntity())
            taskDao.saveIdCounter(TaskIdCounter(lastId = nextId))
        }
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(id: String) {
        taskDao.deleteTaskById(id.trim())
    }
}