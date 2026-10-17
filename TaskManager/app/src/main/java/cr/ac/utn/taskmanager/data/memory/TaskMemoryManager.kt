package cr.ac.utn.taskmanager.data.memory

import cr.ac.utn.taskmanager.data.source.TaskDataSource
import cr.ac.utn.taskmanager.domain.model.Task

class TaskMemoryManager : TaskDataSource {

    private val tasks = mutableListOf<Task>()

    @Synchronized
    override fun getTasks(): List<Task> {
        return tasks.toList()
    }

    @Synchronized
    override fun getById(id: String): Task? {
        val normalizedId = id.trim()
        return tasks.firstOrNull { it.id.trim() == normalizedId }
    }

    @Synchronized
    override fun addTask(task: Task) {
        tasks.add(task)
    }

    @Synchronized
    override fun updateTask(task: Task) {
        val normalizedId = task.id.trim()
        val index = tasks.indexOfFirst { it.id.trim() == normalizedId }

        if (index == -1) {
            throw NoSuchElementException("Task not found: $normalizedId")
        }

        tasks[index] = task.copy(id = normalizedId)
    }

    @Synchronized
    override fun deleteTask(id: String) {
        val normalizedId = id.trim()
        val removed = tasks.removeIf { it.id.trim() == normalizedId }

        if (!removed) {
            throw NoSuchElementException("Task not found: $normalizedId")
        }
    }
}