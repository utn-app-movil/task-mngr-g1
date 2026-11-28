package cr.ac.utn.taskmanager.data.local

import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRoomRepository(private val taskDao: TaskDao) :
    ITaskRepository {

    override fun getTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getEligibleParentTasks(): Flow<List<Task>> {
        return taskDao.getEligibleParentTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getById(id: String): Task? {
        return taskDao.getTaskById(id.trim())?.toDomain()
    }

    override suspend fun addTask(task: Task) {
        taskDao.insertTask(task.toEntity())
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(id: String) {
        taskDao.deleteTaskById(id.trim())
    }
}