package cr.ac.utn.taskmanager.data.memory

import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TaskMemoryManager : ITaskRepository {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    private val tasksList = mutableListOf<Task>()

    override fun getTasks(): Flow<List<Task>> = _tasks.asStateFlow()

    override suspend fun addTask(task: Task) {
        tasksList.add(task)
        _tasks.value = tasksList.toList()
    }

    override suspend fun deleteTask(id: String) {
        tasksList.removeIf { it.id.trim() == id.trim() }
        _tasks.value = tasksList.toList()
    }

    override suspend fun updateTask(task: Task) {
        deleteTask(task.id)
        addTask(task)
    }

    override suspend fun getById(id: String): Task? {
        return tasksList.find { it.id.trim() == id.trim() }
    }
}
