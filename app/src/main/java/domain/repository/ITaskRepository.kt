package domain.repository

import domain.model.Task
import kotlinx.coroutines.flow.Flow


interface ITaskRepository {
    fun observeTasks(): Flow<List<Task>>

    suspend fun addTask(task: Task)

    suspend fun updateTask(task: Task)

    suspend fun deleteTask(task: Task)
}