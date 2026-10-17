package cr.ac.utn.taskmanager.viewmodel

import androidx.lifecycle.ViewModel
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.result.TaskOperationResult
import cr.ac.utn.taskmanager.domain.service.TaskService

class TaskViewModel(
    private val service: TaskService
) : ViewModel() {

    fun getTasks(): TaskOperationResult<List<Task>> {
        return service.getTasks()
    }

    fun getTaskById(id: String): TaskOperationResult<Task> {
        return service.getTaskById(id)
    }

    fun saveTask(task: Task): TaskOperationResult<Task> {
        return service.saveTask(task)
    }

    fun updateTask(task: Task): TaskOperationResult<Task> {
        return service.updateTask(task)
    }

    fun removeTask(id: String): TaskOperationResult<Unit> {
        return service.deleteTask(id)
    }

    fun cancelTask(id: String): TaskOperationResult<Task> {
        return service.cancelTask(id)
    }
}