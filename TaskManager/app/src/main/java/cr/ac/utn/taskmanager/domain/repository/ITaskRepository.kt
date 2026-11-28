package cr.ac.utn.taskmanager.domain.repository

import cr.ac.utn.taskmanager.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface ITaskRepository {
    fun getTasks(): Flow<List<Task>>
    fun getEligibleParentTasks(): Flow<List<Task>>
    suspend fun getById(id: String): Task?
    suspend fun getNextId(): String
    suspend fun addTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(id: String)
}