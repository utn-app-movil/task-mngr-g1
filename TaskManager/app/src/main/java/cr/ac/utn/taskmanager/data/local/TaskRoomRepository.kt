package cr.ac.utn.taskmanager.data.local

import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRoomRepository(private val taskDao: TaskDao) : ITaskRepository {

    override fun getTasks(): Flow<List<Task>> =
        taskDao.getAllTasks().map { list -> list.map { it.toDomain() } }

    override fun getEligibleParentTasks(): Flow<List<Task>> =
        taskDao.getEligibleParentTasks().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Task? =
        taskDao.getTaskById(id)?.toDomain()

    override suspend fun addTask(task: Task) {
        taskDao.insertTask(task.toEntity())
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(id: Long) {
        taskDao.deleteTaskById(id)
    }
}