package cr.ac.utn.taskmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cr.ac.utn.taskmanager.data.memory.TaskMemoryManager
import cr.ac.utn.taskmanager.data.repository.TaskRepositoryImpl
import cr.ac.utn.taskmanager.domain.service.TaskService

class TaskViewModelFactory : ViewModelProvider.Factory {

    private val dataSource = TaskMemoryManager()
    private val repository = TaskRepositoryImpl(dataSource)
    private val service = TaskService(repository)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            return TaskViewModel(service) as T
        }

        throw IllegalArgumentException(
            "Unsupported ViewModel class: ${modelClass.name}"
        )
    }
}