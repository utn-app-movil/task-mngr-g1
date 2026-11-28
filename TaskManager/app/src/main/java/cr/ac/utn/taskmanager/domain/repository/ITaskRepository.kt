package cr.ac.utn.taskmanager.domain.repository

import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.model.TaskError
import kotlinx.coroutines.flow.Flow

interface ITaskRepository {
    fun getTasks(): Flow<List<Task>>
    fun getEligibleParentTasks(): Flow<List<Task>>
    suspend fun getById(id: String): Task?
    suspend fun addTask(task: Task): TaskError?
    suspend fun updateTask(task: Task): TaskError?
    suspend fun deleteTask(id: String): TaskError?
}
