package cr.ac.utn.taskmanager.data.source

import cr.ac.utn.taskmanager.domain.model.Task

interface TaskDataSource {
    fun getTasks(): List<Task>
    fun getById(id: String): Task?
    fun addTask(task: Task)
    fun updateTask(task: Task)
    fun deleteTask(id: String)
}