package data.repository

import data.local.ITaskDao
import data.local.toDomain
import data.local.toEntity
import domain.model.Task
import domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepositoryImpl(private val taskDao: ITaskDao): ITaskRepository {
    override fun observeTasks(): Flow<List<Task>> =
        taskDao.observeTasks().map { tasks ->
            tasks.map { it.toDomain() }
        }

    override suspend fun addTask(task: Task) {
        taskDao.insert(task.toEntity())
    }

    override suspend fun deleteTask(task: Task) {
        taskDao.delete(task.toEntity())
    }

    override suspend fun updateTask(task: Task) {
        taskDao.update(task.toEntity())
    }
}