package cr.ac.utn.taskmanager.data.repository

import cr.ac.utn.taskmanager.data.source.TaskDataSource
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository

class TaskRepositoryImpl(
    private val dataSource: TaskDataSource
) : ITaskRepository {

    override fun getTasks(): List<Task> {
        return dataSource.getTasks()
    }

    override fun getById(id: String): Task? {
        return dataSource.getById(id.trim())
    }

    override fun addTask(task: Task) {
        dataSource.addTask(task)
    }

    override fun updateTask(task: Task) {
        dataSource.updateTask(task)
    }

    override fun deleteTask(id: String) {
        dataSource.deleteTask(id.trim())
    }
}