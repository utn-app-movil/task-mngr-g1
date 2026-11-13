package cr.ac.utn.taskmanager.data.memory

import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository

class TaskMemoryManager: ITaskRepository {
    private val tasks = mutableListOf<Task>()
    override fun addTask(task: Task){
        tasks.add(task)
    }

    override fun deleteTask(id: String) {
        tasks.removeIf { it.id == id }
    }

    override fun updateTask(task: Task) {
        deleteTask(task.id)
        addTask(task)
    }

    override fun getTasks() = tasks

    override fun getById(id: String): Task? {
        val result = tasks.filter { it.id == id }
        return if(result.any()) result[0] else null
    }
}