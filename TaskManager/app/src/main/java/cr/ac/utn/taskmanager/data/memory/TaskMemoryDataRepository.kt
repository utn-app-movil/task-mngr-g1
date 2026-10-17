package cr.ac.utn.taskmanager.data.memory

import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository

class TaskMemoryDataRepository : ITaskRepository {
    private val tasks = mutableListOf<Task>()

    override fun getTasks(): List<Task> {
        return tasks.toList()
    }

    override fun getById(id: String): Task? {
        return tasks.firstOrNull { it.id.trim() == id.trim() }
    }

    override fun addTask(task: Task) {
        tasks.add(task)
    }

    override fun updateTask(task: Task) {
        val index = tasks.indexOfFirst { it.id.trim() == task.id.trim() }
        if (index != -1) {
            tasks[index] = task
        }
    }

    override fun deleteTask(id: String) {
        tasks.removeIf { it.id.trim() == id.trim() }
    }
}