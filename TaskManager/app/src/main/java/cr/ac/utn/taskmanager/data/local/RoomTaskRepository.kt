package cr.ac.utn.taskmanager.data.local

import cr.ac.utn.taskmanager.data.mapper.toDomain
import cr.ac.utn.taskmanager.data.mapper.toEntity
import cr.ac.utn.taskmanager.database.dao.TaskDao
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import cr.ac.utn.taskmanager.domain.model.Task
import kotlinx.coroutines.flow.map

class RoomTaskRepository (private val dao: TaskDao): ITaskRepository {
    override fun getTasks(): Flow<List<Task>> {
        return dao.getAll().map { entities ->
                entities.map { it.toDomain() }
            }
    }

    override suspend fun getById(id: String): Task? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun addTask(task: Task) {
        dao.insert(task.toEntity())
    }

    override suspend fun updateTask(task: Task) {
        dao.update(task.toEntity())
    }

    override suspend fun deleteTask(id: String) {
        dao.delete(id)
    }
}